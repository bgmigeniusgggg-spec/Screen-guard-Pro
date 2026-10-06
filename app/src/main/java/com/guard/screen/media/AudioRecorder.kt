package com.guard.screen.media

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.guard.screen.core.Logger
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    private val context: Context
) {

    private var recorder: MediaRecorder? = null
    private val isRecording = AtomicBoolean(false)
    private var currentFile: File? = null

    /**
     * Audio record karo aur file return karo.
     */
    @SuppressLint("MissingPermission")
    suspend fun record(durationSeconds: Int): File? {
        if (isRecording.get()) {
            Logger.w("AudioRecorder", "Already recording")
            return null
        }

        isRecording.set(true)

        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        currentFile = file

        return try {
            val rec = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = rec
            Logger.d("AudioRecorder", "Recording for ${durationSeconds}s")

            delay(durationSeconds * 1000L)

            try {
                rec.stop()
                rec.release()
            } catch (e: Exception) {
                Logger.e("AudioRecorder", "Stop failed", e)
            }
            recorder = null

            if (file.exists() && file.length() > 0) {
                Logger.i("AudioRecorder", "Recorded ${file.length()} bytes")
                file
            } else {
                file.delete()
                null
            }
        } catch (e: Exception) {
            Logger.e("AudioRecorder", "Recording failed", e)
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            if (file.exists()) file.delete()
            null
        } finally {
            isRecording.set(false)
            currentFile = null
        }
    }

    /**
     * Manual stop (agar beech mein band karna ho).
     */
    fun stop() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null
        isRecording.set(false)
    }

    fun isRecording(): Boolean = isRecording.get()

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }
}
