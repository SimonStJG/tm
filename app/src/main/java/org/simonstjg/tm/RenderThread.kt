package org.simonstjg.tm

import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import kotlin.math.min


class RenderThread(
    private val surfaceHolder: SurfaceHolder,
    private val backgroundString: String
) : Thread() {
    lateinit var handler: RenderHandler

    private val startLock = Object()
    private var ready: Boolean = false

    private var initialPulseSize: Double = 0.0
    private var pulses: MutableList<Pulse> = mutableListOf()
    private var lastFrameTimeNanos: Long? = null
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
        synchronized(startLock) {
            ready = true;
            startLock.notify();    // signal waitUntilReady()
        }
        Looper.loop()
    }

    /**
     * Waits until the render thread is ready to receive messages.  Supposed to be called from the
     * UI thread which needs to post a message almost immediately after the render thread starts.
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

    /**
     * Calculate useful constants which will be needed often, e.g. when we draw a frame or create a
     * pulse.
     */
    private fun precalculateUsefulConstants(width: Int, height: Int) {
        Log.i(TAG, "precalculateUsefulConstants")
        initialPulseSize = (min(width, height) * PULSE_RATIO).toDouble()

        // Set the font size to something in the right ballpark, then scale it until it's correct.
        // We want to end up with `textWidth / canvasWidth = .8`.
        val textSizeGuess = 300f
        backgroundTextPaint.textSize = textSizeGuess
        val actualSizeOfGuess = backgroundTextPaint.measureText(backgroundString)
        backgroundTextPaint.textSize = (textSizeGuess / actualSizeOfGuess) * .8f * width
    }

    /**
     * Called to render each frame.  Try not to do anything even slightly heavy here, this will be
     * running every 16ms or so.
     */
    fun doFrame(frameTimeNanos: Long) {
        val lastFrameDuration = lastFrameTimeNanos?.let { frameTimeNanos - it }
        lastFrameTimeNanos = frameTimeNanos

        val canvas = surfaceHolder.lockCanvas()

        try {
            canvas.drawColor(Color.BLACK)
            canvas.drawText(
                backgroundString,
                canvas.width / 2f,
                canvas.height / 2f,
                backgroundTextPaint
            )

            if (lastFrameDuration != null) {
                // Could be more performant, e.g. because we could always push onto the end of
                // the queue and pop off the other end?  OTOH we remove relatively rarely so maybe
                // that's a premature optimisation.
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
        /**
         * The size of the pulse as a ratio of the min(width, height) of the canvas.
         */
        private const val PULSE_RATIO: Float = 0.1f
        private val TAG = RenderThread::class.java.name
    }
}