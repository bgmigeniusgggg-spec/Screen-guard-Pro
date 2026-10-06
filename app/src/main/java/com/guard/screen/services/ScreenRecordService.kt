package com.guard.screen.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import com.guard.screen.data.repository.MediaRepository
import com.guard.screen.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Screen recording service.
 * MediaProjection consent ke baad screen record karta hai aur upload karta hai.
 */
@AndroidEntryPoint
class ScreenRecordService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_DEVICE_KEY = "device_key"
    }

    @Inject
    lateinit var mediaRepository: MediaRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("ScreenRecordService", "onStartCommand")

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
        val duration = intent.getIntExtra(EXTRA_DURATION, Constants.DEFAULT_SCREEN_DURATION)
        val deviceKey = intent.getStringExtra(EXTRA_DEVICE_KEY) ?: DeviceKey.get(this)

        if (resultCode == -1 || resultData == null) {
            Logger.e("ScreenRecordService", "No MediaProjection data")
            stopSelf()
            return START_NOT_STICKY
        }

        startRecording(resultCode, resultData, duration, deviceKey)
        return START_NOT_STICKY
    }

    private fun startForegroundSafely() {
        val notification = notificationHelper.buildRecordingNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    Constants.NOTIF_ID_RECORDING,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(Constants.NOTIF_ID_RECORDING, notification)
            }
        } catch (e: Exception) {
            Logger.e("ScreenRecordService", "Foreground failed", e)
        }
    }

    private fun startRecording(
        resultCode: Int,
        resultData: Intent,
        duration: Int,
        deviceKey: String
    ) {
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(resultCode, resultData)

            val (width, height) = getScreenMetrics()
            val dpi = resources.displayMetrics.densityDpi

            val file = File(cacheDir, "screen_${System.currentTimeMillis()}.mp4")
            currentFile = file

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

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenGuard",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface,
                null, null
            )

            mediaRecorder?.start()
            Logger.d("ScreenRecordService", "Recording started — ${duration}s")

            // Duration ke baad stop
            Handler(Looper.getMainLooper()).postDelayed({
                stopRecording(deviceKey)
            }, duration * 1000L)

        } catch (e: Exception) {
            Logger.e("ScreenRecordService", "Start failed", e)
            stopSelf()
        }
    }

    private fun stopRecording(deviceKey: String) {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Logger.e("ScreenRecordService", "Stop failed", e)
        }

        try { mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null

        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null

        try { mediaProjection?.stop() } catch (_: Exception) {}
        mediaProjection = null

        val file = currentFile
        if (file != null && file.exists() && file.length() > 0) {
            Logger.i("ScreenRecordService", "Uploading: ${file.length()} bytes")

            serviceScope.launch {
                try {
                    val result = mediaRepository.uploadMedia(
                        deviceKey = deviceKey,
                        mediaType = Constants.MEDIA_TYPE_SCREEN,
                        file = file,
                        durationSeconds = 0
                    )
                    when (result) {
                        is AppResult.Success -> Logger.i("ScreenRecordService", "Upload OK")
                        is AppResult.Error -> Logger.e("ScreenRecordService", "Upload failed: ${result.message}")
                        else -> {}
                    }
                } catch (e: Exception) {
                    Logger.e("ScreenRecordService", "Upload error", e)
                }
            }
        }

        stopSelf()
    }

    private fun getScreenMetrics(): Pair<Int, Int> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            val w = bounds.width() and 0xFFFFFFFE.toInt()
            val h = bounds.height() and 0xFFFFFFFE.toInt()
            Pair(w, h)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            val w = metrics.widthPixels and 0xFFFFFFFE.toInt()
            val h = metrics.heightPixels and 0xFFFFFFFE.toInt()
            Pair(w, h)
        }
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
    }

    override fun onDestroy() {
        try { mediaRecorder?.release() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { mediaProjection?.stop() } catch (_: Exception) {}
        try { serviceScope.cancel() } catch (_: Exception) {}
        super.onDestroy()
    }
}
