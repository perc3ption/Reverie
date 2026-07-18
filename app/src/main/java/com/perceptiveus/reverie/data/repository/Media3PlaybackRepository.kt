package com.perceptiveus.reverie.data.repository

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.perceptiveus.reverie.data.local.dao.PlayHistoryDao
import com.perceptiveus.reverie.data.local.entity.PlayHistoryEntity
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import com.perceptiveus.reverie.playback.PlaybackService
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
) : PlaybackRepository {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    @Volatile
    private var controller: MediaController? = null

    private var queue: List<Track> = emptyList()
    private var queueSource: QueueSource = QueueSource.Unknown
    private var disabledTrackIds: Set<String> = emptySet()
    private var positionJob: Job? = null
    private var lastRecordedTrackId: String? = null
    private val connecting = AtomicBoolean(false)

    private var pendingAction: (() -> Unit)? = null
    /** Avoid recursive skip loops when auto-advancing past muted tracks. */
    private var skippingDisabled = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                skipCurrentIfDisabled(player)
            }
            syncFromPlayer(player)
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED)
            ) {
                maybeRecordPlayHistory(player)
            }
        }
    }

    init {
        connect()
    }

    private fun connect() {
        if (controller != null || !connecting.compareAndSet(false, true)) return

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
                    mediaController.addListener(playerListener)
                    syncFromPlayer(mediaController)
                    startPositionUpdates()
                    pendingAction?.invoke()
                    pendingAction = null
                } catch (_: Exception) {
                    // Controller failed; next play() will retry connect().
                } finally {
                    connecting.set(false)
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
        if (playable.isEmpty()) return
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
        val mediaItems = playable.map { it.toMediaItem() }
        val player = controller ?: return
        val index = startIndex.coerceIn(0, playable.lastIndex)
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

    override fun togglePlayPause() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            if (player.isPlaying) player.pause() else player.play()
            syncFromPlayer(player)
        }
    }

    override fun skipToNext() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            seekToNextEnabled(player)
            syncFromPlayer(player)
        }
    }

    override fun skipToPrevious() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            if (player.currentPosition > RESTART_THRESHOLD_MS) {
                player.seekTo(0L)
            } else {
                seekToPreviousEnabled(player)
            }
            syncFromPlayer(player)
        }
    }

    override fun seekTo(positionMs: Long) {
        runWhenReady {
            val player = controller ?: return@runWhenReady
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
        if (player != null) {
            mainHandler.post(action)
        } else {
            pendingAction = action
            connect()
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null) {
                    mainHandler.post { syncFromPlayer(player) }
                }
                delay(POSITION_POLL_MS)
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
                audioSessionId = player.audioSessionId,
                queueSource = queueSource,
                disabledTrackIds = disabledTrackIds,
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
        private const val POSITION_POLL_MS = 500L
        private const val RESTART_THRESHOLD_MS = 3_000L
    }
}
