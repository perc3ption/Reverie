package com.perceptiveus.reverie.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.perceptiveus.reverie.playback.audiofx.AudioFxController
import com.perceptiveus.reverie.playback.audiofx.EqualizerAudioProcessor

/**
 * Hosts ExoPlayer + MediaSession so playback continues in the background
 * with a system media notification.
 *
 * PCM chain: EQ → tee (visualizer). Each service instance creates **fresh**
 * processors (Media3 forbids sharing across sinks).
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var equalizerProcessor: EqualizerAudioProcessor? = null

    override fun onCreate() {
        super.onCreate()
        val equalizer = EqualizerAudioProcessor()
        equalizerProcessor = equalizer
        AudioFxController.attach(equalizer)
        val tee = PlaybackAudioAnalyzer.createTeeProcessor()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(equalizer, tee))
                    .build()
            }
        }
        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        equalizerProcessor?.let { AudioFxController.detach(it) }
        equalizerProcessor = null
        PlaybackAudioAnalyzer.reset()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
