package org.simonstjg.tm

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.random.Random

sealed class Pulse(protected val x: Float, protected val y: Float, protected val initialPulseSize: Double) {
    protected var size: Double = initialPulseSize

    /**
     * I want the time to shrink from initialPulseSize to 0 to be TIME_TO_SHRINK_SECS, and we're
     * going to shrink linearly.  The unit of this is pixels per nanosecond.
     */
    private val shrinkSpeed: Double = initialPulseSize / (1_000_000_000.0 * TIME_TO_SHRINK_SECS)

    fun tick(canvas: Canvas, paint: Paint, frameDuration: Long): Boolean {
        draw(canvas, paint)
        size -= shrinkSpeed * frameDuration
        return size >= 0
    }

    protected abstract fun draw(canvas: Canvas, paint: Paint)

    companion object {
        private const val TIME_TO_SHRINK_SECS = 3

        fun randomPulse(x: Float, y: Float, initialPulseSize: Double): Pulse =
            when (Random.nextInt(0, 2)) {
                0 -> SolidPulse(x, y, initialPulseSize)
                1 -> RadialPulse(x, y, initialPulseSize)
                else -> throw IllegalArgumentException()
            }
    }
}

class SolidPulse(x: Float, y: Float, size: Double) : Pulse(x, y, size) {
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawCircle(x, y, size.toFloat(), paint)
    }
}

class RadialPulse(x: Float, y: Float, size: Double) : Pulse(x, y, size) {
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawArc(
            (x - (size / 2)).toFloat(),
            (y - (size / 2)).toFloat(),
            (x + (size / 2)).toFloat(),
            (y + (size / 2)).toFloat(),
            0f,
            60f,
            true,
            paint
        )
    }
}
