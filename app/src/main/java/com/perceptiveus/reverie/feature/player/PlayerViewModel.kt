package com.perceptiveus.reverie.feature.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.lyrics.LyricsImportResult
import com.perceptiveus.reverie.data.lyrics.LyricsLoader
import com.perceptiveus.reverie.data.lyrics.LyricsSidecarImporter
import com.perceptiveus.reverie.data.repository.AlbumArtAccessException
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.domain.model.LyricsDocument
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.PlayerProgress
import com.perceptiveus.reverie.playback.PlaybackAudioAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(
    application: Application,
    private val playbackRepository: PlaybackRepository,
    private val musicLibraryRepository: MusicLibraryRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : AndroidViewModel(application) {

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    val playerProgress: StateFlow<PlayerProgress> = playbackRepository.playerProgress

    val visualizerFrame = playbackRepository.visualizerFrame
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PlaybackAudioAnalyzer.Frame(),
        )

    private val _lyrics = MutableStateFlow<LyricsDocument?>(null)
    val lyrics: StateFlow<LyricsDocument?> = _lyrics.asStateFlow()

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    private val _sleepTimerEndMs = MutableStateFlow<Long?>(null)
    val sleepTimerEndMs: StateFlow<Long?> = _sleepTimerEndMs.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        viewModelScope.launch {
            playbackRepository.playbackState
                .map { it.currentTrack?.filePath.orEmpty() }
                .distinctUntilChanged()
                .collect { path ->
                    _lyrics.value = if (path.isBlank()) {
                        null
                    } else {
                        withContext(Dispatchers.IO) { LyricsLoader.loadForAudioFile(path) }
                    }
                }
        }
    }

    fun togglePlayPause() = playbackRepository.togglePlayPause()
    fun skipToNext() = playbackRepository.skipToNext()
    fun skipToPrevious() = playbackRepository.skipToPrevious()
    fun seekTo(positionMs: Long) = playbackRepository.seekTo(positionMs)
    fun toggleShuffle() = playbackRepository.toggleShuffle()
    fun cycleRepeatMode() = playbackRepository.cycleRepeatMode()
    fun playQueueIndex(index: Int) = playbackRepository.playQueueIndex(index)
    fun toggleQueueTrackEnabled(trackId: String) =
        playbackRepository.toggleQueueTrackEnabled(trackId)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) =
        playbackRepository.moveQueueItem(fromIndex, toIndex)

    fun canAccessAdvancedVisualizers(): Boolean =
        featureAccessChecker.canAccess(AppFeature.ADVANCED_VISUALIZERS)

    fun canAccessLyrics(): Boolean =
        featureAccessChecker.canAccess(AppFeature.LYRICS)

    fun canAccessAlbumArtEditing(): Boolean =
        featureAccessChecker.canAccess(AppFeature.ALBUM_ART_EDITING)

    fun canAccessAudioFx(): Boolean =
        featureAccessChecker.canAccess(AppFeature.AUDIO_FX)

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0 || minutes > 24 * 60) return
        cancelSleepTimer()
        val endMs = System.currentTimeMillis() + minutes * 60_000L
        _sleepTimerEndMs.value = endMs
        sleepTimerJob = viewModelScope.launch {
            while (isActive) {
                val remaining = endMs - System.currentTimeMillis()
                if (remaining <= 0L) {
                    _sleepTimerEndMs.value = null
                    if (playerProgress.value.isPlaying) {
                        playbackRepository.togglePlayPause()
                    }
                    _userMessages.emit("Sleep timer ended — playback paused.")
                    break
                }
                delay(minOf(remaining, 1_000L))
            }
        }
        viewModelScope.launch {
            _userMessages.emit(formatSleepTimerSetMessage(minutes))
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerEndMs.value = null
    }

    fun sleepTimerRemainingMs(nowMs: Long = System.currentTimeMillis()): Long {
        val end = _sleepTimerEndMs.value ?: return 0L
        return (end - nowMs).coerceAtLeast(0L)
    }

    fun importLyrics(uri: Uri) {
        if (!canAccessLyrics()) {
            viewModelScope.launch {
                _userMessages.emit("Lyrics are a Premium feature.")
            }
            return
        }

        val audioPath = playbackState.value.currentTrack?.filePath.orEmpty()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                LyricsSidecarImporter.importForTrack(
                    context = getApplication(),
                    audioPath = audioPath,
                    sourceUri = uri,
                )
            }
            when (result) {
                is LyricsImportResult.Success -> {
                    _lyrics.value = result.document
                    _userMessages.emit("Lyrics imported.")
                }
                is LyricsImportResult.Failure -> {
                    _userMessages.emit(result.message)
                }
            }
        }
    }

    fun importAlbumArt(uri: Uri) {
        if (!canAccessAlbumArtEditing()) {
            viewModelScope.launch {
                _userMessages.emit("Album art editing is a Premium feature.")
            }
            return
        }
        val track = playbackState.value.currentTrack ?: return
        viewModelScope.launch {
            musicLibraryRepository.updateTrackArtwork(track.id, uri)
                .onSuccess { path ->
                    playbackRepository.updateQueueArtwork(
                        artist = track.artist,
                        album = track.album,
                        artworkPath = path,
                    )
                    _userMessages.emit("Album art updated.")
                }
                .onFailure { error ->
                    _userMessages.emit(
                        when (error) {
                            AlbumArtAccessException -> "Album art editing is a Premium feature."
                            else -> error.message ?: "Could not import album art."
                        },
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelSleepTimer()
    }
}

private fun formatSleepTimerSetMessage(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours > 0 && remainder == 0 ->
            if (hours == 1) "Sleep timer set for 1 hour." else "Sleep timer set for $hours hours."
        hours > 0 -> "Sleep timer set for ${hours}h ${remainder}m."
        minutes == 1 -> "Sleep timer set for 1 minute."
        else -> "Sleep timer set for $minutes minutes."
    }
}
