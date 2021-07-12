package org.simonstjg.tm

import android.graphics.Canvas
import android.graphics.Paint
import java.lang.IllegalArgumentException
import kotlin.random.Random

sealed class Pulse(protected var x: Float, protected var y: Float, protected var size: Float) {
    fun tick(canvas: Canvas, paint: Paint): Boolean {
        draw(canvas, paint)
        size = (size * .8).toFloat()
        return size >= MIN_SIZE
    }

    protected abstract fun draw(canvas: Canvas, paint: Paint)

    companion object {
        const val MIN_SIZE: Float = 1f

        fun randomPulse(x: Float, y: Float, size: Float): Pulse =
            when (Random.nextInt(0, 2)) {
                0 -> SolidPulse(x, y, size)
                1 -> RadialPulse(x, y, size)
                else -> throw IllegalArgumentException()
            }
    }
}

class SolidPulse(x: Float, y: Float, size: Float) : Pulse(x, y, size) {
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawCircle(x, y, size, paint)
    }
}

class RadialPulse(x: Float, y: Float, size: Float) : Pulse(x, y, size) {
    override fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawArc(
            x - (size / 2),
            y - (size / 2),
            x + (size / 2),
            y + (size / 2),
            0f,
            60f,
            true,
            paint
        )
    }
}
