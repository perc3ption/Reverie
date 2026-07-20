package com.perceptiveus.reverie.data.repository

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.entity.PlayHistoryEntity
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.playback.PlaybackAudioAnalyzer
import com.perceptiveus.reverie.playback.PlaybackService
import com.perceptiveus.reverie.playback.audiofx.AudioFxSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Media3-backed playback controller connected to [PlaybackService] via [MediaController].
 */
class Media3PlaybackRepository(
    context: Context,
    private val playHistoryDao: PlayHistoryDao,
    private val scope: CoroutineScope,
    private val audioFxSettings: StateFlow<AudioFxSettings>,
) : PlaybackRepository {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    override val visualizerFrame: StateFlow<PlaybackAudioAnalyzer.Frame> =
        PlaybackAudioAnalyzer.frame

    @Volatile
    private var controller: MediaController? = null

    private var queue: List<Track> = emptyList()
    private var queueSource: QueueSource = QueueSource.Unknown
    private var disabledTrackIds: Set<String> = emptySet()
    private var positionJob: Job? = null
    private var fadeInJob: Job? = null
    private var gapJob: Job? = null
    private var lastRecordedTrackId: String? = null
    private val connecting = AtomicBoolean(false)
    private var fadeInActive = false
    private var connectAttempts = 0

    private var pendingAction: (() -> Unit)? = null
    /** Avoid recursive skip loops when auto-advancing past muted tracks. */
    private var skippingDisabled = false
    /** Invalidates in-flight crossfade volume updates when a newer fade/play starts. */
    private var fadeGeneration = 0
    /** Consecutive playback failures while trying to keep music going. */
    private var consecutiveFailures = 0
    /** True while an automatic error-recovery seek/prepare is in progress. */
    private var recoveringFromError = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                skipCurrentIfDisabled(player)
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) {
                consecutiveFailures = 0
            }
            syncFromPlayer(player)
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED)
            ) {
                maybeRecordPlayHistory(player)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error: ${error.errorCodeName} — ${error.message}", error)
            val player = controller ?: return
            mainHandler.post { handlePlayerError(player, error) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = controller ?: return
            val fx = audioFxSettings.value
            when {
                fx.crossfadeMs > 0 -> startFadeIn(player, fx.crossfadeMs)
                !fx.gaplessEnabled &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> insertTrackGap(player)
                else -> {
                    fadeInActive = false
                    player.volume = 1f
                }
            }
        }
    }

    init {
        connect()
    }

    private fun connect() {
        if (controller?.isConnected == true || !connecting.compareAndSet(false, true)) return

        val token = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                try {
                    val mediaController = future.get()
                    controller = mediaController
                    connectAttempts = 0
                    mediaController.addListener(playerListener)
                    syncFromPlayer(mediaController)
                    startPositionUpdates()
                    val pending = pendingAction
                    pendingAction = null
                    pending?.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "MediaController connect failed", e)
                    controller = null
                    connectAttempts++
                    // Keep pendingAction so the next play() retries connect().
                } finally {
                    connecting.set(false)
                    if (pendingAction != null &&
                        controller == null &&
                        connectAttempts in 1..MAX_CONNECT_ATTEMPTS
                    ) {
                        mainHandler.postDelayed({ connect() }, CONNECT_RETRY_MS)
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    override fun play(
        tracks: List<Track>,
        startIndex: Int,
        source: QueueSource,
    ) {
        val intended = tracks.getOrNull(startIndex.coerceIn(0, tracks.lastIndex.coerceAtLeast(0)))
        val playable = tracks.filterPlayable()
        if (playable.isEmpty()) {
            Log.w(TAG, "play() ignored — no playable files (missing on disk?)")
            _playbackState.update {
                it.copy(errorMessage = "Couldn't play — file missing or unreadable.")
            }
            return
        }
        val index = intended?.let { target ->
            playable.indexOfFirst { it.id == target.id }
        }?.takeIf { it >= 0 } ?: 0

        runWhenReady {
            playInternal(playable, startIndex = index, source = source)
        }
    }

    private fun playInternal(
        playable: List<Track>,
        startIndex: Int,
        source: QueueSource,
    ) {
        queue = playable
        queueSource = source
        disabledTrackIds = emptySet()
        lastRecordedTrackId = null
        consecutiveFailures = 0
        val mediaItems = playable.map { it.toMediaItem() }
        val player = controller ?: return
        val index = startIndex.coerceIn(0, playable.lastIndex)
        resetVolumeAndFades(player)
        // Clearing a prior error requires leaving the errored state before loading.
        if (player.playerError != null) {
            player.stop()
        }
        player.setMediaItems(mediaItems, index, /* startPositionMs = */ 0L)
        player.prepare()
        player.play()
        syncFromPlayer(player)
        maybeRecordPlayHistory(player)
    }

    override fun playQueueIndex(index: Int) {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            if (index !in 0 until player.mediaItemCount) return@runWhenReady
            val trackId = queue.getOrNull(index)?.id
            if (trackId != null && trackId in disabledTrackIds) {
                // Explicitly choosing a muted song re-enables it for this session.
                disabledTrackIds = disabledTrackIds - trackId
            }
            resetVolumeAndFades(player)
            recoverFromError(player)
            player.seekTo(index, /* positionMs = */ 0L)
            player.play()
            syncFromPlayer(player)
            maybeRecordPlayHistory(player)
        }
    }

    override fun toggleQueueTrackEnabled(trackId: String) {
        if (trackId.isBlank() || queue.none { it.id == trackId }) return
        runWhenReady {
            val enabling = trackId in disabledTrackIds
            if (!enabling) {
                val enabledCount = queue.count { it.id !in disabledTrackIds }
                if (enabledCount <= 1 && trackId !in disabledTrackIds) {
                    // Keep at least one playable song in the session queue.
                    syncFromPlayer(controller ?: return@runWhenReady)
                    return@runWhenReady
                }
                disabledTrackIds = disabledTrackIds + trackId
            } else {
                disabledTrackIds = disabledTrackIds - trackId
            }

            val player = controller
            if (player != null && !enabling && player.currentMediaItem?.mediaId == trackId) {
                seekToNextEnabled(player)
            }
            if (player != null) syncFromPlayer(player)
        }
    }

    override fun addToQueue(tracks: List<Track>) {
        val playable = tracks.filterPlayable()
        if (playable.isEmpty()) return
        runWhenReady {
            val player = controller ?: return@runWhenReady
            if (queue.isEmpty() || player.mediaItemCount == 0) {
                playInternal(playable, startIndex = 0, source = QueueSource.Unknown)
                return@runWhenReady
            }
            player.addMediaItems(playable.map { it.toMediaItem() })
            queue = queue + playable
            syncFromPlayer(player)
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        runWhenReady {
            val player = controller ?: return@runWhenReady
            if (fromIndex !in queue.indices || toIndex !in queue.indices) return@runWhenReady
            if (fromIndex !in 0 until player.mediaItemCount) return@runWhenReady
            if (toIndex !in 0 until player.mediaItemCount) return@runWhenReady
            player.moveMediaItem(fromIndex, toIndex)
            queue = queue.toMutableList().also { list ->
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            syncFromPlayer(player)
        }
    }

    override fun updateQueueArtwork(artist: String, album: String, artworkPath: String) {
        if (artworkPath.isBlank()) return
        runWhenReady {
            queue = queue.map { track ->
                if (track.artist.equals(artist, ignoreCase = true) &&
                    track.album.equals(album, ignoreCase = true)
                ) {
                    track.copy(artworkPath = artworkPath)
                } else {
                    track
                }
            }
            val player = controller
            if (player != null) syncFromPlayer(player)
        }
    }

    override fun updateQueueTrackMetadata(
        trackId: String,
        title: String,
        artist: String,
        album: String,
    ) {
        runWhenReady {
            var updatedIndex = -1
            queue = queue.mapIndexed { index, track ->
                if (track.id == trackId) {
                    updatedIndex = index
                    track.copy(title = title, artist = artist, album = album)
                } else {
                    track
                }
            }
            val player = controller
            if (player != null && updatedIndex >= 0 && updatedIndex < queue.size) {
                // Keep Media3 item metadata (notification / fallback) aligned with the edit.
                player.replaceMediaItem(updatedIndex, queue[updatedIndex].toMediaItem())
                syncFromPlayer(player)
            } else if (player != null) {
                syncFromPlayer(player)
            }
        }
    }

    override fun togglePlayPause() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.mediaItemCount == 0) {
                    syncFromPlayer(player)
                    return@runWhenReady
                }
                resetVolumeAndFades(player)
                recoverFromError(player)
                player.play()
            }
            syncFromPlayer(player)
        }
    }

    override fun skipToNext() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            recoverFromError(player)
            seekToNextEnabled(player)
            player.play()
            syncFromPlayer(player)
        }
    }

    override fun skipToPrevious() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            recoverFromError(player)
            if (player.currentPosition > RESTART_THRESHOLD_MS) {
                player.seekTo(0L)
            } else {
                seekToPreviousEnabled(player)
            }
            player.play()
            syncFromPlayer(player)
        }
    }

    override fun seekTo(positionMs: Long) {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            recoverFromError(player)
            player.seekTo(positionMs.coerceAtLeast(0L))
            syncFromPlayer(player)
        }
    }

    override fun toggleShuffle() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            player.shuffleModeEnabled = !player.shuffleModeEnabled
            syncFromPlayer(player)
        }
    }

    override fun cycleRepeatMode() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            syncFromPlayer(player)
        }
    }

    private fun runWhenReady(action: () -> Unit) {
        val player = controller
        when {
            player != null && player.isConnected -> mainHandler.post(action)
            else -> {
                if (player != null && !player.isConnected) {
                    runCatching { player.release() }
                    controller = null
                }
                pendingAction = action
                connect()
            }
        }
    }

    /**
     * Clears [Player.playerError] / IDLE so subsequent play/seek commands work.
     * Plain [Player.play] is a no-op while the player is in STATE_ERROR.
     */
    private fun recoverFromError(player: Player) {
        if (player.playerError != null || player.playbackState == Player.STATE_IDLE) {
            if (player.mediaItemCount > 0) {
                player.prepare()
            }
        }
        if (!fadeInActive && player.volume < 0.05f) {
            player.volume = 1f
        }
    }

    /**
     * Auto-recover from runtime/IO errors so the user isn't stuck with a dead play button.
     * Strategy: retry current item once, then skip forward a few times, then surface the error.
     */
    private fun handlePlayerError(player: Player, error: PlaybackException) {
        if (recoveringFromError) return
        recoveringFromError = true
        try {
            resetVolumeAndFades(player)
            consecutiveFailures++
            val message = error.message?.takeIf { it.isNotBlank() }
                ?: error.errorCodeName
            _playbackState.update { it.copy(isPlaying = false, errorMessage = message) }

            val canSkip = player.mediaItemCount > 1 &&
                consecutiveFailures <= MAX_AUTO_SKIP_ON_ERROR
            val shouldRetrySame = consecutiveFailures == 1

            when {
                shouldRetrySame && player.mediaItemCount > 0 -> {
                    Log.i(TAG, "Retrying current item after error")
                    player.prepare()
                    player.play()
                }
                canSkip -> {
                    Log.i(TAG, "Skipping to next item after error ($consecutiveFailures)")
                    seekToNextEnabled(player)
                    player.prepare()
                    player.play()
                }
                else -> {
                    Log.w(TAG, "Giving up auto-recovery after $consecutiveFailures failures")
                    // Leave queue loaded; next user tap on play will recoverFromError().
                    if (player.mediaItemCount > 0) {
                        player.prepare()
                    }
                }
            }
            syncFromPlayer(player)
        } finally {
            recoveringFromError = false
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null && player.isConnected) {
                    mainHandler.post {
                        applyOutgoingCrossfade(player)
                        syncFromPlayer(player)
                    }
                }
                delay(POSITION_POLL_MS)
            }
        }
    }

    private fun applyOutgoingCrossfade(player: Player) {
        val crossfadeMs = audioFxSettings.value.crossfadeMs
        if (crossfadeMs <= 0 || fadeInActive || !player.isPlaying) return
        val duration = player.duration
        if (duration <= 0L) return
        val remaining = duration - player.currentPosition
        if (remaining in 1 until crossfadeMs) {
            player.volume = (remaining.toFloat() / crossfadeMs).coerceIn(0f, 1f)
        }
    }

    private fun resetVolumeAndFades(player: Player) {
        fadeGeneration++
        fadeInJob?.cancel()
        gapJob?.cancel()
        fadeInJob = null
        gapJob = null
        fadeInActive = false
        player.volume = 1f
    }

    private fun startFadeIn(player: Player, crossfadeMs: Int) {
        fadeInJob?.cancel()
        gapJob?.cancel()
        fadeInActive = true
        player.volume = 0f
        val steps = (crossfadeMs / POSITION_POLL_MS.toInt()).coerceIn(4, 40)
        val stepMs = (crossfadeMs / steps).coerceAtLeast(16)
        val fadeToken = ++fadeGeneration
        fadeInJob = scope.launch {
            for (i in 1..steps) {
                delay(stepMs.toLong())
                val volume = i / steps.toFloat()
                mainHandler.post {
                    if (fadeGeneration == fadeToken) {
                        player.volume = volume.coerceIn(0f, 1f)
                    }
                }
            }
            mainHandler.post {
                if (fadeGeneration == fadeToken) {
                    player.volume = 1f
                    fadeInActive = false
                }
            }
        }
    }

    private fun insertTrackGap(player: Player) {
        resetVolumeAndFades(player)
        player.pause()
        gapJob = scope.launch {
            delay(TRACK_GAP_MS)
            mainHandler.post {
                if (!player.isPlaying) player.play()
            }
        }
    }

    private fun syncFromPlayer(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId
        val current = queue.firstOrNull { it.id == mediaId }
            ?: player.currentMediaItem?.toTrackFallback()
        val queueIndex = player.currentMediaItemIndex.takeIf { it in queue.indices } ?: -1
        val next = findNextEnabledTrack(fromIndex = queueIndex)
        val duration = when {
            player.duration > 0 -> player.duration
            current != null && current.durationMs > 0 -> current.durationMs
            else -> 0L
        }
        val position = player.currentPosition.coerceAtLeast(0L).let { pos ->
            if (duration > 0) pos.coerceAtMost(duration) else pos
        }

        _playbackState.update {
            PlaybackState(
                currentTrack = current?.copy(durationMs = duration.takeIf { it > 0 } ?: current.durationMs),
                isPlaying = player.isPlaying,
                positionMs = position,
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode.toDomain(),
                queueSize = player.mediaItemCount.takeIf { it > 0 } ?: queue.size,
                nextTrack = next,
                queue = queue,
                queueIndex = queueIndex,
                queueSource = queueSource,
                disabledTrackIds = disabledTrackIds,
                errorMessage = when {
                    player.isPlaying -> null
                    player.playerError != null ->
                        player.playerError?.message?.takeIf { it.isNotBlank() }
                            ?: player.playerError?.errorCodeName
                            ?: it.errorMessage
                    else -> it.errorMessage
                },
            )
        }
    }

    private fun skipCurrentIfDisabled(player: Player) {
        if (skippingDisabled) return
        val id = player.currentMediaItem?.mediaId ?: return
        if (id !in disabledTrackIds) return
        skippingDisabled = true
        try {
            seekToNextEnabled(player)
        } finally {
            skippingDisabled = false
        }
    }

    private fun seekToNextEnabled(player: Player) {
        val count = player.mediaItemCount
        if (count <= 0) return
        repeat(count) {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else {
                val next = when {
                    player.repeatMode != Player.REPEAT_MODE_OFF ->
                        findNextEnabledIndex(fromIndex = player.currentMediaItemIndex, wrap = true)
                    else ->
                        findNextEnabledIndex(fromIndex = player.currentMediaItemIndex, wrap = false)
                }
                if (next >= 0) {
                    player.seekTo(next, 0L)
                } else {
                    return
                }
            }
            val id = player.currentMediaItem?.mediaId
            if (id != null && id !in disabledTrackIds) return
        }
    }

    private fun seekToPreviousEnabled(player: Player) {
        val count = player.mediaItemCount
        if (count <= 0) return
        repeat(count) {
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                val prev = findPreviousEnabledIndex(fromIndex = player.currentMediaItemIndex)
                if (prev >= 0) {
                    player.seekTo(prev, 0L)
                } else {
                    player.seekTo(0L)
                    return
                }
            }
            val id = player.currentMediaItem?.mediaId
            if (id != null && id !in disabledTrackIds) return
        }
    }

    private fun findNextEnabledTrack(fromIndex: Int): Track? {
        if (queue.isEmpty() || fromIndex !in queue.indices) return null
        val wrap = _playbackState.value.repeatMode != RepeatMode.OFF
        val index = findNextEnabledIndex(fromIndex = fromIndex, wrap = wrap)
        return index.takeIf { it >= 0 }?.let { queue[it] }
    }

    private fun findNextEnabledIndex(fromIndex: Int, wrap: Boolean): Int {
        if (queue.isEmpty() || fromIndex !in queue.indices) return -1
        val maxOffset = if (wrap) queue.size - 1 else queue.size - fromIndex - 1
        for (offset in 1..maxOffset) {
            val index = if (wrap) (fromIndex + offset) % queue.size else fromIndex + offset
            if (queue[index].id !in disabledTrackIds) return index
        }
        return -1
    }

    private fun findPreviousEnabledIndex(fromIndex: Int): Int {
        if (queue.isEmpty() || fromIndex !in queue.indices) return -1
        for (offset in 1 until queue.size) {
            val index = (fromIndex - offset).mod(queue.size)
            if (queue[index].id !in disabledTrackIds) return index
        }
        return -1
    }

    private fun maybeRecordPlayHistory(player: Player) {
        if (!player.isPlaying) return
        val trackId = player.currentMediaItem?.mediaId ?: return
        if (trackId in disabledTrackIds) return
        if (trackId == lastRecordedTrackId) return
        lastRecordedTrackId = trackId
        scope.launch {
            playHistoryDao.insert(PlayHistoryEntity(trackId = trackId))
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val uri = File(filePath).toUri()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build(),
            )
            .build()
    }

    private fun List<Track>.filterPlayable(): List<Track> =
        filter { it.filePath.isNotBlank() && File(it.filePath).exists() }

    private fun MediaItem.toTrackFallback(): Track {
        val meta = mediaMetadata
        return Track(
            id = mediaId,
            title = meta.title?.toString() ?: "Unknown",
            artist = meta.artist?.toString() ?: "Unknown",
            album = meta.albumTitle?.toString() ?: "",
            filePath = localConfiguration?.uri?.path.orEmpty(),
        )
    }

    private fun Int.toDomain(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    companion object {
        private const val TAG = "Media3Playback"
        private const val POSITION_POLL_MS = 500L
        private const val RESTART_THRESHOLD_MS = 3_000L
        private const val TRACK_GAP_MS = 400L
        private const val CONNECT_RETRY_MS = 400L
        private const val MAX_CONNECT_ATTEMPTS = 3
        private const val MAX_AUTO_SKIP_ON_ERROR = 5
    }
}
