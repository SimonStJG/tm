package org.simonstjg.tm

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.simonstjg.tm.databinding.ActivityTmBinding
import kotlin.math.min
import kotlin.properties.Delegates

class TmActivity : AppCompatActivity(), SurfaceHolder.Callback, SoundPool.OnLoadCompleteListener {
    init {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
    }

    private lateinit var binding: ActivityTmBinding
    private lateinit var mainSurface: SurfaceView
    private lateinit var soundPool: SoundPool
    private lateinit var drawThread: Thread

    // TODO Don't need lateinit here?
    private lateinit var paint: Paint
    private lateinit var backgroundTextPaint: Paint

    private var requestThreadStop: Boolean = false
    private var soundId by Delegates.notNull<Int>()
    private var soundReady: Boolean = false

    private var pulses: MutableList<Pulse> = mutableListOf()

    private var drawRunnable = Runnable {
        while (!requestThreadStop) {
            if (mainSurface.holder != null) {
                val canvas = mainSurface.holder.lockCanvas()

                try {
                    canvas.drawColor(Color.BLACK)

                    canvas.drawText(getString(R.string.background_text), canvas.width/2f, canvas.height/2f, backgroundTextPaint)

                    // TODO Could be more performant, e.g. because we could always push onto the end of
                    // the queue and pop off the other end.  But that's a premature optimisation
                    // right now.  Also, this lock could be held for ages if I have a lot of ticks
                    synchronized(this) {
                        pulses.removeIf { pulse ->
                            !pulse.tick(canvas, paint)
                        }
                    }
                } finally {
                    mainSurface.holder.unlockCanvasAndPost(canvas)
                }
            }
            Thread.sleep(TICK_SPEED)
        }
    }

    private val playSoundListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                if (soundReady) {
                    // TODO Precalculate initial size
                    val m = min(view.height, view.width)

                    synchronized(this) {
                        pulses.add(Pulse.randomPulse(motionEvent.x, motionEvent.y, PULSE_RATIO * m))
                    }

                    val rate = (1.5f-(motionEvent.y / view.height))
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
            .setMaxStreams(5)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            .build()
        soundId = soundPool.load(this.applicationContext, R.raw.s1, 1)
        soundPool.setOnLoadCompleteListener(this)

        paint = Paint().apply {
            color = Color.BLUE
        }
        backgroundTextPaint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            // TODO Calculate from screen size
            textSize = 200f
        }
    }

    @SuppressLint("InlinedApi")
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
        requestThreadStop = false
        drawThread = Thread(drawRunnable).apply {
            start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed")
        requestThreadStop = true
    }

    override fun onLoadComplete(soundPool: SoundPool?, sampleId: Int, status: Int) {
        Log.i(TAG, "onLoadComplete")
        assert(status == 0)
        assert(sampleId == soundId)
        soundReady = true
    }

    companion object {
        private val TAG: String = TmActivity::class.java.name
        private const val PULSE_RATIO: Float = 0.1f
        // TODO Surely this isn't the best thing to do, should I use some sort of timer, or draw
        // as fast as possible?
        private const val TICK_SPEED: Long = 100
    }
}