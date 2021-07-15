package org.simonstjg.tm

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.min

class PulseRenderer(
    private val startingPosition: StartingPosition,
    private val drawable: Drawable
) {
    private var size: Float = 0f
    private var shrinkSpeed: Double = 0.0

    fun tick(canvas: Canvas, paint: Paint, frameDuration: Long): Boolean {
        if (shrinkSpeed == 0.0) {
            // First tick, we need to initialize the size and shrinkSpeed
            val initialSize = min(canvas.height, canvas.width) * PULSE_RATIO

            //  I want the time to shrink from initialPulseSize to 0 to be TIME_TO_SHRINK_SECS, and
            //  we're going to shrink linearly.  The unit of this is pixels per nanosecond.
            shrinkSpeed = initialSize / (1_000_000_000.0 * TIME_TO_SHRINK_SECS)
            size = initialSize
        }

        draw(canvas, paint)

        size -= (shrinkSpeed * frameDuration).toFloat()
        return size >= MIN_WIDTH
    }

    private fun draw(canvas: Canvas, paint: Paint) {
        val x = startingPosition.x - size / 2
        val y = startingPosition.y - size / 2

        canvas.drawBitmap(
            drawable.toBitmap(size.toInt(), size.toInt()),
            x,
            y,
            paint
        )
    }

    companion object {
        private const val TIME_TO_SHRINK_SECS = 3

        /**
         * The size of the pulse as a ratio of the min(width, height) of the canvas.
         */
        private const val PULSE_RATIO: Float = 0.15f

        /**
         * The minimum width of a pulse before it's removed
         */
        private const val MIN_WIDTH = 5
    }

    data class StartingPosition(val x: Float, val y: Float)
}

