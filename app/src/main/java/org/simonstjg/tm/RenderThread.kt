package org.simonstjg.tm

import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder


class RenderThread(
    private val surfaceHolder: SurfaceHolder,
    private val backgroundString: String,
    private val pulseFactory: Pulse.Factory
) : Thread() {
    lateinit var handler: RenderHandler

    private val startLock = Object()
    private var ready: Boolean = false

    private var pulses: MutableList<Pulse> = mutableListOf()
    private var lastFrameTimeNanos: Long? = null
    private var paint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
    }
    private var backgroundTextPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
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
    private fun precalculateUsefulConstants(width: Int, @Suppress("UNUSED_PARAMETER") height: Int) {
        Log.i(TAG, "precalculateUsefulConstants")

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
        pulses.add(pulseFactory.randomPulse(Pulse.StartingPosition(x.toFloat(), y.toFloat())))
    }

    companion object {
        private val TAG = RenderThread::class.java.name
    }
}