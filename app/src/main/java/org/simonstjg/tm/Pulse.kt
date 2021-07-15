package org.simonstjg.tm

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.SoundPool
import android.util.Log
import androidx.core.content.ContextCompat

class Pulse(private val drawable: Drawable, private val soundId: Int) {
    private var ready = false

    fun renderer(startingPosition: PulseRenderer.StartingPosition) =
        PulseRenderer(startingPosition, drawable)

    fun playSound(soundPool: SoundPool, rate: Float) {
        if (!ready) {
            Log.e(TAG, "Sound $soundId not ready")
        }
        if (soundPool.play(soundId, 1f, 1f, 1, 0, rate) != 0) {
            Log.e(TAG, "Failed to play sound $soundId")
        }
    }

    fun onLoadComplete(sampleId: Int) {
        if (sampleId == soundId) {
            ready = true
        }
    }

    companion object {
        private val TAG = Pulse::class.java.name
    }
}

class Pulses(context: Context, soundPool: SoundPool) : SoundPool.OnLoadCompleteListener {
    init {
        soundPool.setOnLoadCompleteListener(this)
    }

    private val pulses = listOf(
        Pulse(
            ContextCompat.getDrawable(context, R.drawable.holt)!!,
            soundPool.load(context, R.raw.holt_boost, 1)
        ),
        Pulse(
            ContextCompat.getDrawable(context, R.drawable.boyle)!!,
            soundPool.load(context, R.raw.boyle_gobble, 1)
        ),
        Pulse(
            ContextCompat.getDrawable(context, R.drawable.gina)!!,
            soundPool.load(context, R.raw.gina_spaghetti, 1)
        )
    )

    private val randomShuffle = RandomShuffle(0, pulses.size - 1, 2)

    fun random(): Pulse = pulses[randomShuffle.next()]

    override fun onLoadComplete(soundPool: SoundPool?, sampleId: Int, status: Int) {
        Log.i(TAG, "onLoadComplete $sampleId")
        assert(status == 0)
        pulses.forEach { it.onLoadComplete(sampleId) }
    }

    companion object {
        private val TAG = Pulses::class.java.name
    }
}