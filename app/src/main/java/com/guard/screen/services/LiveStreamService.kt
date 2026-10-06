package com.guard.screen.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.guard.screen.core.Constants
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import com.guard.screen.domain.usecase.StreamLiveScreenUseCase
import com.guard.screen.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Live screen streaming service.
 * Har 3 sec pe screen frame capture karke Supabase pe upload karta hai.
 */
@AndroidEntryPoint
class LiveStreamService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_DEVICE_KEY = "device_key"
    }

    @Inject
    lateinit var streamUseCase: StreamLiveScreenUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val isStreaming = AtomicBoolean(false)
    private var lastFrameTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("LiveStreamService", "onStartCommand")

        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundSafely()

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        val deviceKey = intent.getStringExtra(EXTRA_DEVICE_KEY) ?: DeviceKey.get(this)

        if (resultCode == -1 || resultData == null) {
            Logger.e("LiveStreamService", "No consent data")
            stopSelf()
            return START_NOT_STICKY
        }

        startStreaming(resultCode, resultData, deviceKey)
        return START_NOT_STICKY
    }

    private fun startForegroundSafely() {
        val notification = notificationHelper.buildLiveNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.NOTIF_ID_LIVE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(Constants.NOTIF_ID_LIVE, notification)
            }
        } catch (e: Exception) {
            Logger.e("LiveStreamService", "Foreground failed", e)
        }
    }

    private fun startStreaming(
        resultCode: Int,
        resultData: Intent,
        deviceKey: String
    ) {
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(resultCode, resultData)

            val (width, height) = getScreenMetrics()
            val dpi = resources.displayMetrics.densityDpi

            // Handler thread for ImageReader
            handlerThread = HandlerThread("LiveStream").apply { start() }
            handler = Handler(handlerThread!!.looper)

            imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "LiveStream",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                handler
            )

            isStreaming.set(true)

            // Frame provider set karo
            streamUseCase.setFrameProvider {
                captureFrame()
            }

            // Stream start karo
            serviceScope.launch {
                try {
                    streamUseCase(deviceKey)
                    Logger.i("LiveStreamService", "Streaming started")
                } catch (e: Exception) {
                    Logger.e("LiveStreamService", "Stream failed", e)
                }
            }

            // ImageReader listener
            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    image.close()
                } catch (_: Exception) {}
            }, handler)

        } catch (e: Exception) {
            Logger.e("LiveStreamService", "Start failed", e)
            stopSelf()
        }
    }

    private suspend fun captureFrame(): ByteArray? {
        return try {
            val reader = imageReader ?: return null
            val image = reader.acquireLatestImage() ?: return null

            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * image.width

                val bitmap = android.graphics.Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // Crop
                val cropped = android.graphics.Bitmap.createBitmap(
                    bitmap, 0, 0, image.width, image.height
                )

                // Compress to JPEG
                val bos = ByteArrayOutputStream()
                cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, bos)

                bitmap.recycle()
                cropped.recycle()

                bos.toByteArray()
            } finally {
                image.close()
            }
        } catch (e: Exception) {
            Logger.e("LiveStreamService", "Frame capture failed", e)
            null
        }
    }

    private fun getScreenMetrics(): Pair<Int, Int> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width() and 0xFFFFFFFE.toInt(), bounds.height() and 0xFFFFFFFE.toInt())
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

    override fun onDestroy() {
        Logger.d("LiveStreamService", "onDestroy")

        isStreaming.set(false)
        streamUseCase.stop()
        streamUseCase.clearFrameProvider()

        try { imageReader?.close() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { mediaProjection?.stop() } catch (_: Exception) {}
        try { handlerThread?.quitSafely() } catch (_: Exception) {}
        try { serviceScope.cancel() } catch (_: Exception) {}

        super.onDestroy()
    }
}
