package com.guard.screen.domain.usecase

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.repository.MediaRepository
import com.guard.screen.ui.MediaProjectionActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RecordScreenUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {

    suspend operator fun invoke(
        deviceKey: String,
        durationSeconds: Int = Constants.DEFAULT_SCREEN_DURATION
    ): AppResult<MediaFile> {
        Logger.d("RecordScreen", "Starting screen recording for ${durationSeconds}s")

        if (durationSeconds <= 0 || durationSeconds > Constants.MAX_DURATION) {
            return AppResult.Error(
                ErrorType.VALIDATION,
                "Invalid duration: $durationSeconds"
            )
        }

        return try {
            // ⭐ MediaProjectionActivity kholo — consent dialog aayega
            val intent = Intent(context, MediaProjectionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MediaProjectionActivity.EXTRA_MODE, MediaProjectionActivity.MODE_RECORD)
                putExtra(MediaProjectionActivity.EXTRA_DURATION, durationSeconds)
                putExtra(MediaProjectionActivity.EXTRA_DEVICE_KEY, deviceKey)
            }
            context.startActivity(intent)
            Logger.i("RecordScreen", "Consent activity launched")

            // Placeholder success — actual upload ScreenRecordService se hoga
            AppResult.Success(
                MediaFile(
                    deviceKey = deviceKey,
                    mediaType = Constants.MEDIA_TYPE_SCREEN,
                    storagePath = "pending",
                    durationSeconds = durationSeconds
                )
            )
        } catch (e: Exception) {
            Logger.e("RecordScreen", "Failed to launch consent", e)
            AppResult.Error(
                ErrorType.CONSENT,
                e.message ?: "Failed to launch consent",
                e
            )
        }
    }
}
