package org.simonstjg.tm

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

class RenderHandler(looper: Looper, renderThread: RenderThread) : Handler(looper) {
    private val renderThread = WeakReference(renderThread)

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            SHUTDOWN -> {
                Looper.myLooper()!!.quit()
            }
            SURFACE_CHANGED -> {
                val width = msg.arg1
                val height = msg.arg2
                renderThread.get()!!.surfaceChanged(width, height)
            }
            DO_FRAME -> {
                if (hasMessages(DO_FRAME)) {
                    // The number of do frame messages in the queue is backing up, drop them.
                    removeMessages(DO_FRAME, null)
                }
                renderThread.get()!!.doFrame(msg.obj as Long)
            }
            ADD_PULSE -> {
                renderThread.get()!!.addPulse(msg.obj as PulseRenderer)
            }
        }
    }

    fun shutdown() = sendMessage(obtainMessage(SHUTDOWN))
    fun surfaceChanged(width: Int, height: Int) =
        sendMessage(obtainMessage(SURFACE_CHANGED, width, height))

    fun doFrame(frameTimeNanos: Long) = sendMessage(obtainMessage(DO_FRAME, frameTimeNanos))
    fun addPulse(pulse: PulseRenderer) = sendMessage(obtainMessage(ADD_PULSE, pulse))

    companion object {
        private const val SHUTDOWN = 0
        private const val SURFACE_CHANGED = 1
        private const val DO_FRAME = 2
        private const val ADD_PULSE = 3
    }
}