package com.guard.screen.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.services.ScreenRecordService

class MediaProjectionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_DEVICE_KEY = "device_key"

        const val MODE_RECORD = "record"
        const val MODE_LIVE = "live"

        private const val REQ_CODE = 5001

        private var resultCallback: ((Int, Intent?) -> Unit)? = null

        fun setResultCallback(callback: (Int, Intent?) -> Unit) {
            resultCallback = callback
        }
    }

    private var mode: String = MODE_RECORD
    private var duration: Int = Constants.DEFAULT_SCREEN_DURATION
    private var deviceKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_RECORD
        duration = intent.getIntExtra(EXTRA_DURATION, Constants.DEFAULT_SCREEN_DURATION)
        deviceKey = intent.getStringExtra(EXTRA_DEVICE_KEY) ?: ""

        requestConsent()
    }

    private fun requestConsent() {
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CODE)
        } catch (e: Exception) {
            Logger.e("MediaProjectionAct", "Failed", e)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Logger.d("MediaProjectionAct", "Consent granted")

                // Save token
                saveToken(resultCode, data)

                // Notify callback (agar set hai)
                resultCallback?.invoke(resultCode, data)

                // Start screen recording service
                when (mode) {
                    MODE_RECORD -> {
                        startScreenRecordService(resultCode, data)
                    }
                    MODE_LIVE -> {
                        startLiveService(resultCode, data)
                    }
                }
            } else {
                Logger.w("MediaProjectionAct", "Consent denied")
            }
            finish()
        }
    }

    private fun saveToken(resultCode: Int, data: Intent) {
        try {
            val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            prefs.edit()
                .putInt(Constants.KEY_MEDIA_PROJECTION_TOKEN, resultCode)
                .putLong(Constants.KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {}
    }

    private fun startScreenRecordService(resultCode: Int, data: Intent) {
        try {
            val intent = Intent(this, ScreenRecordService::class.java).apply {
                putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
                putExtra(ScreenRecordService.EXTRA_DURATION, duration)
                putExtra(ScreenRecordService.EXTRA_DEVICE_KEY, deviceKey)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Logger.e("MediaProjectionAct", "Service start failed", e)
        }
    }

    private fun startLiveService(resultCode: Int, data: Intent) {
        // TODO: Part 8 mein LiveStreamService ke saath
        Logger.d("MediaProjectionAct", "Live mode — service later")
    }
}
