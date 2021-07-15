package org.simonstjg.tm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.min
import kotlin.random.Random

sealed class Pulse(protected val startingPosition: StartingPosition) {
    protected var size: Float = 0f
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

    protected abstract fun draw(canvas: Canvas, paint: Paint)

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

    class Factory(context: Context) {
        private val solidPulseFactory = SolidPulse.Factory()
        private val holtFactory = BitmapPulse.Factory(context, R.raw.holt)

        fun randomPulse(startingPosition: StartingPosition): Pulse {
            val factory = when (Random.nextInt(0, 2)) {
                0 -> solidPulseFactory
                1-> holtFactory
                else -> {
                    throw IllegalArgumentException()
                }
            }
            return factory.build(startingPosition)
        }
    }

    interface PulseFactory {
        fun build(startingPosition: StartingPosition): Pulse
    }
}

class SolidPulse(startingPosition: StartingPosition) : Pulse(startingPosition) {
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawCircle(startingPosition.x, startingPosition.y, size / 2, paint)
    }

    class Factory : PulseFactory {
        override fun build(startingPosition: StartingPosition) = SolidPulse(startingPosition)
    }
}

class BitmapPulse(startingPosition: StartingPosition, private val drawable: Drawable) : Pulse(startingPosition) {
    override fun draw(canvas: Canvas, paint: Paint) {
        val x = startingPosition.x - size /2
        val y = startingPosition.y - size /2

        canvas.drawBitmap(
            drawable.toBitmap(size.toInt(), size.toInt()),
            x,
            y,
            paint
        )
    }

    class Factory(context: Context, drawableId: Int): PulseFactory {
        private val drawable = ContextCompat.getDrawable(context, drawableId)!!

        override fun build(startingPosition: StartingPosition): Pulse = BitmapPulse(startingPosition, drawable)
    }
}
