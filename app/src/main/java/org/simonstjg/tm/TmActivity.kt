package org.simonstjg.tm

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import org.simonstjg.tm.databinding.ActivityTmBinding
import kotlin.properties.Delegates

class TmActivity : AppCompatActivity(), SurfaceHolder.Callback, SoundPool.OnLoadCompleteListener,
    Choreographer.FrameCallback {

    init {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
    }

    private lateinit var binding: ActivityTmBinding
    private lateinit var mainSurface: SurfaceView
    private lateinit var soundPool: SoundPool
    private lateinit var renderThread: RenderThread

    private var soundId by Delegates.notNull<Int>()
    private var soundReady: Boolean = false

    private val playSoundListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                if (soundReady) {
                    renderThread.handler.addPulse(motionEvent.x, motionEvent.y)
                    val rate = (1.5f - (motionEvent.y / view.height))
                    if (soundPool.play(soundId, 1f, 1f, 1, 0, rate) == 0) {
                        Log.e(TAG, "soundPool play failed")
                    }
                } else {
                    Log.e(TAG, "Sound not ready")
                }
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        binding = ActivityTmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mainSurface = binding.mainSurface
        mainSurface.setOnTouchListener(playSoundListener)
        mainSurface.holder.addCallback(this)

        hideSystemUi()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_CONCURRENT_SOUNDS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            .build()
        soundId = soundPool.load(this.applicationContext, R.raw.s1, 1)
        soundPool.setOnLoadCompleteListener(this)
    }

    private fun hideSystemUi() {
        Log.i(TAG, "hideSystemUi")
        supportActionBar?.hide()
        mainSurface.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated")

        renderThread = RenderThread(mainSurface.holder, getString(R.string.background_text))
        renderThread.start()
        // Doesn't take long, fine to block the UI thread
        renderThread.waitUntilReady()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "surfaceChanged")
        renderThread.handler.surfaceChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed")
        Choreographer.getInstance().removeFrameCallback(this)
        renderThread.handler.shutdown()
        renderThread.join()
    }

    override fun doFrame(frameTimeNanos: Long) {
        renderThread.handler.doFrame(frameTimeNanos)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onLoadComplete(soundPool: SoundPool?, sampleId: Int, status: Int) {
        Log.i(TAG, "onLoadComplete")
        assert(status == 0)
        assert(sampleId == soundId)
        soundReady = true
    }

    companion object {
        private val TAG: String = TmActivity::class.java.name
        private const val MAX_CONCURRENT_SOUNDS = 8
    }
}