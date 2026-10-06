package com.guard.screen.media

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.guard.screen.core.Logger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenRecorder @Inject constructor(
    private val context: Context
) {

    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    private var isRecording = false

    /**
     * Recording start karo.
     */
    fun start(mediaProjection: MediaProjection, file: File): Boolean {
        if (isRecording) {
            Logger.w("ScreenRecorder", "Already recording")
            return false
        }

        return try {
            val (width, height) = getScreenMetrics()
            val dpi = context.resources.displayMetrics.densityDpi

            outputFile = file

            mediaRecorder = createRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(4_000_000)
                setOutputFile(file.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface,
                null, null
            )

            mediaRecorder?.start()
            isRecording = true

            Logger.d("ScreenRecorder", "Started: ${file.name}")
            true
        } catch (e: Exception) {
            Logger.e("ScreenRecorder", "Start failed", e)
            cleanup()
            false
        }
    }

    /**
     * Recording stop karo.
     */
    fun stop(): File? {
        if (!isRecording) return null

        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            virtualDisplay?.release()
            virtualDisplay = null

            isRecording = false

            val file = outputFile
            Logger.d("ScreenRecorder", "Stopped: ${file?.name}")

            if (file != null && file.exists() && file.length() > 0) {
                file
            } else {
                file?.delete()
                null
            }
        } catch (e: Exception) {
            Logger.e("ScreenRecorder", "Stop failed", e)
            cleanup()
            null
        }
    }

    fun isRecording(): Boolean = isRecording

    private fun cleanup() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        isRecording = false
    }

    private fun getScreenMetrics(): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(
                bounds.width() and 0xFFFFFFFE.toInt(),
                bounds.height() and 0xFFFFFFFE.toInt()
            )
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            Pair(
                metrics.widthPixels and 0xFFFFFFFE.toInt(),
                metrics.heightPixels and 0xFFFFFFFE.toInt()
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }
}
