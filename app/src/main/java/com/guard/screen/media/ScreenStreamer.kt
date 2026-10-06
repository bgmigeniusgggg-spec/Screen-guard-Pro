package com.guard.screen.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import com.guard.screen.core.Logger
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenStreamer @Inject constructor(
    private val context: Context
) {

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val isStreaming = AtomicBoolean(false)

    /**
     * Streaming start karo.
     */
    fun start(mediaProjection: MediaProjection): Boolean {
        if (isStreaming.get()) {
            Logger.w("ScreenStreamer", "Already streaming")
            return false
        }

        return try {
            val (width, height) = getScreenMetrics()
            val dpi = context.resources.displayMetrics.densityDpi

            handlerThread = HandlerThread("ScreenStreamer").apply { start() }
            handler = Handler(handlerThread!!.looper)

            imageReader = ImageReader.newInstance(
                width, height,
                PixelFormat.RGBA_8888,
                2
            )

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenStreamer",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                handler
            )

            isStreaming.set(true)
            Logger.d("ScreenStreamer", "Started — ${width}x${height}")
            true
        } catch (e: Exception) {
            Logger.e("ScreenStreamer", "Start failed", e)
            cleanup()
            false
        }
    }

    /**
     * Ek frame capture karo (JPEG bytes).
     */
    fun captureFrame(quality: Int = 60): ByteArray? {
        if (!isStreaming.get()) return null

        return try {
            val reader = imageReader ?: return null
            val image = reader.acquireLatestImage() ?: return null

            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * image.width

                val bitmap = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // Crop padding
                val cropped = Bitmap.createBitmap(
                    bitmap, 0, 0, image.width, image.height
                )

                // Scale down (bandwidth bachane ke liye)
                val scaled = scaleBitmap(cropped, 720)

                // Compress to JPEG
                val bos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, bos)

                bitmap.recycle()
                if (cropped != scaled) cropped.recycle()
                scaled.recycle()

                bos.toByteArray()
            } finally {
                image.close()
            }
        } catch (e: Exception) {
            Logger.e("ScreenStreamer", "Frame capture failed", e)
            null
        }
    }

    /**
     * Streaming stop karo.
     */
    fun stop() {
        isStreaming.set(false)
        cleanup()
        Logger.d("ScreenStreamer", "Stopped")
    }

    fun isStreaming(): Boolean = isStreaming.get()

    private fun cleanup() {
        try { imageReader?.close() } catch (_: Exception) {}
        imageReader = null
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { handlerThread?.quitSafely() } catch (_: Exception) {}
        handlerThread = null
        handler = null
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
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
}
