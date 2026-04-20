package com.pegasus.videoplayer

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // ──────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        playerView = findViewById(R.id.playerView)
        titleView  = findViewById(R.id.videoTitle)
        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }

        setupImmersive()
        handleIntent(intent)
    }

    /** singleTask: una nuova chiamata da Pegasus riusa l'Activity esistente. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        releasePlayer()
        handleIntent(intent)
    }

    override fun onStart()   { super.onStart();   player?.play()  }
    override fun onStop()    { super.onStop();    player?.pause() }

    override fun onDestroy() {
        releasePlayer()
        releaseAudioFocus()
        super.onDestroy()
    }

    // ──────────────────────────────────────────────────────────────
    // Intent handling
    // ──────────────────────────────────────────────────────────────

    private fun handleIntent(src: Intent?) {
        val params = UriParams.fromIntentData(src?.data)
        if (params == null) {
            Toast.makeText(this, R.string.err_invalid_uri, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        titleView.text = params.title
        startPlayback(params.url)
    }

    // ──────────────────────────────────────────────────────────────
    // Playback
    // ──────────────────────────────────────────────────────────────

    private fun startPlayback(url: String) {
        requestAudioFocus()

        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@VideoPlayerActivity,
                    PlayerErrorMapper.humanMessage(error),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) finish()
            }
        })

        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true

        playerView.player = p
        playerView.setShowNextButton(false)
        playerView.setShowPreviousButton(false)
        playerView.controllerShowTimeoutMs   = 5000
        playerView.controllerHideOnTouch     = true
        playerView.setKeepContentOnPlayerReset(true)

        player = p
    }

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
    }

    // ──────────────────────────────────────────────────────────────
    // Immersive mode
    // ──────────────────────────────────────────────────────────────

    private fun setupImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY          or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE             or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION    or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN         or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION           or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Audio focus
    // ──────────────────────────────────────────────────────────────

    private fun requestAudioFocus() {
        val am = (audioManager ?: getSystemService(AUDIO_SERVICE) as AudioManager)
            .also { audioManager = it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener { focus ->
                    when (focus) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.pause()
                        AudioManager.AUDIOFOCUS_GAIN           -> player?.play()
                    }
                }
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        }
    }
}
