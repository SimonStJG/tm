package org.simonstjg.tm

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import kotlin.math.pow
import kotlin.random.Random

sealed class Pulse(protected var x: Float, protected var y: Float, protected var size: Double) {
    fun tick(canvas: Canvas, paint: Paint, frameDuration: Long): Boolean {
        draw(canvas, paint)

        if(frameDuration >= 1006666985) {
            // A silly hack because this is an eulator
            return true
        }
        val shrinkage = PULSE_SHRINKING_SPEED.pow((frameDuration / 100_000_000).toDouble())
        val newSize = (size * shrinkage)
        Log.i("Pulse", "$frameDuration $size $newSize $shrinkage")
        size = newSize
        return size >= MIN_SIZE
    }

    protected abstract fun draw(canvas: Canvas, paint: Paint)

    companion object {
        const val PULSE_SHRINKING_SPEED = .8
        const val MIN_SIZE: Float = 1f

        fun randomPulse(x: Float, y: Float, size: Double): Pulse =
            when (Random.nextInt(0, 2)) {
                0 -> SolidPulse(x, y, size)
                1 -> RadialPulse(x, y, size)
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
