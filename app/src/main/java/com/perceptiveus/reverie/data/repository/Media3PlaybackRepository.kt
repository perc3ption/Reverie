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
    private var positionJob: Job? = null
    private var lastRecordedTrackId: String? = null
    private val connecting = AtomicBoolean(false)

    private var pendingAction: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
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

    override fun play(tracks: List<Track>, startIndex: Int) {
        val intended = tracks.getOrNull(startIndex.coerceIn(0, tracks.lastIndex.coerceAtLeast(0)))
        val playable = tracks.filter { it.filePath.isNotBlank() && File(it.filePath).exists() }
        if (playable.isEmpty()) return
        val index = intended?.let { target ->
            playable.indexOfFirst { it.id == target.id }
        }?.takeIf { it >= 0 } ?: 0

        runWhenReady {
            queue = playable
            lastRecordedTrackId = null
            val mediaItems = playable.map { it.toMediaItem() }
            val player = controller ?: return@runWhenReady
            player.setMediaItems(mediaItems, index, /* startPositionMs = */ 0L)
            player.prepare()
            player.play()
            syncFromPlayer(player)
            maybeRecordPlayHistory(player)
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
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            }
            syncFromPlayer(player)
        }
    }

    override fun skipToPrevious() {
        runWhenReady {
            val player = controller ?: return@runWhenReady
            if (player.currentPosition > RESTART_THRESHOLD_MS) {
                player.seekTo(0L)
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                player.seekTo(0L)
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
        val nextIndex = player.currentMediaItemIndex + 1
        val next = queue.getOrNull(nextIndex)
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
            )
        }
    }

    private fun maybeRecordPlayHistory(player: Player) {
        if (!player.isPlaying) return
        val trackId = player.currentMediaItem?.mediaId ?: return
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
