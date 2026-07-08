package com.perceptiveus.reverie.data.repository

import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.RepeatMode
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Placeholder playback controller.
 * Replace internals with Media3 ExoPlayer when audio pipeline is ready.
 */
class FakePlaybackRepository : PlaybackRepository {

    private val nowPlaying = Track(
        id = "1",
        title = "Afterglow",
        artist = "Echos",
        album = "Silent Skies",
        durationMs = 293_000,
    )

    private val _playbackState = MutableStateFlow(
        PlaybackState(
            currentTrack = nowPlaying,
            isPlaying = false,
            positionMs = 134_000,
            shuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
            queueSize = 9,
        ),
    )
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    override fun togglePlayPause() {
        _playbackState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    override fun skipToNext() {
        // TODO: Advance queue via ExoPlayer.
    }

    override fun skipToPrevious() {
        // TODO: Rewind or skip to previous via ExoPlayer.
    }

    override fun toggleShuffle() {
        _playbackState.update { it.copy(shuffleEnabled = !it.shuffleEnabled) }
    }

    override fun cycleRepeatMode() {
        _playbackState.update { state ->
            val next = when (state.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            state.copy(repeatMode = next)
        }
    }
}
