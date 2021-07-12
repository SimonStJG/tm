package org.simonstjg.tm

import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import kotlin.math.min

class RenderThread(private val surfaceHolder: SurfaceHolder): Thread() {
    private var initialPulseSize: Double = 0.0
    lateinit var handler: RenderHandler

    private val startLock = Object()
    private var ready: Boolean = false
    private var lastFrameTimeNanos: Long? = null

    private var pulses: MutableList<Pulse> = mutableListOf()

    private var paint = Paint().apply {
        color = Color.BLUE
    }
    private var backgroundTextPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    override fun run() {
        Log.i(TAG, "run")
        Looper.prepare()
        handler = RenderHandler(Looper.myLooper()!!, this)
        synchronized (startLock) {
            ready = true;
            startLock.notify();    // signal waitUntilReady()
        }
        Looper.loop()
    }

    /**
     * Waits until the render thread is ready to receive messages.
     *
     * Call from the UI thread.
     *
     * TODO This is pretty "old school java" - surely there's a newer Android-y way?
     */
    fun waitUntilReady() {
        synchronized(startLock) {
            while (!ready) {
                startLock.wait()
            }
        }
    }

    fun surfaceChanged(width: Int, height: Int) {
        Log.i(TAG, "surfaceChanged")
        precalculateUsefulConstants(width, height)
    }

    private fun precalculateUsefulConstants(width: Int, height: Int) {
        Log.i(TAG, "precalculateUsefulConstants")
        initialPulseSize = (min(width, height) * PULSE_RATIO).toDouble()

        // Set the font size to something in the right ballpark, then scale it until it's correct.
        // I want
        //      textWidth / canvasWidth = .8
        val textSizeGuess = 300f
        backgroundTextPaint.textSize = textSizeGuess
        // TODO Extract string resource
        val actualSizeOfGuess = backgroundTextPaint.measureText("TOUCH ME")
        backgroundTextPaint.textSize = (textSizeGuess / actualSizeOfGuess) * .8f * width
    }

    fun doFrame(frameTimeNanos: Long) {
        val lastFrameDuration = lastFrameTimeNanos?.let {frameTimeNanos - it}
        lastFrameTimeNanos = frameTimeNanos

        val canvas = surfaceHolder.lockCanvas()

        try {
            canvas.drawColor(Color.BLACK)

            canvas.drawText(
                "TOUCH ME",
                canvas.width / 2f,
                canvas.height / 2f,
                backgroundTextPaint
            )

            if (lastFrameDuration != null) {
                // Could be more performant, e.g. because we could always push onto the end of
                // the queue and pop off the other end.  But that's a premature optimisation
                // right now.  Also, this lock could be held for ages if I have a lot of ticks?
                synchronized(this) {
                    pulses.removeIf { pulse ->
                        !pulse.tick(canvas, paint, lastFrameDuration)
                    }
                }
            }

        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    fun addPulse(x: Int, y: Int) {
        Log.i(TAG, "addPulse $x $y")
        pulses.add(Pulse.randomPulse(x.toFloat(), y.toFloat(), initialPulseSize))
    }

    companion object {
        private const val PULSE_RATIO: Float = 0.1f
        private val TAG = RenderThread::class.java.name
    }
}