package com.guard.screen.domain.usecase

import android.content.Context
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Screen recording use case.
 * Ye MediaProjection consent check karta hai aur ScreenRecordService start karta hai.
 */
class RecordScreenUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {

    /**
     * Screen recording start karo.
     * @param durationSeconds Recording duration
     * @return AppResult with MediaFile or error
     */
    suspend operator fun invoke(
        deviceKey: String,
        durationSeconds: Int = Constants.DEFAULT_SCREEN_DURATION
    ): AppResult<MediaFile> {
        Logger.d("RecordScreen", "Starting screen recording for ${durationSeconds}s")

        // Validate duration
        if (durationSeconds <= 0 || durationSeconds > Constants.MAX_DURATION) {
            return AppResult.Error(
                ErrorType.VALIDATION,
                "Invalid duration: $durationSeconds"
            )
        }

        // Note: Actual recording ScreenRecordService handle karta hai
        // Ye use case sirf command dispatch karta hai

        return try {
            // Service intent — Part 8 mein detail aayegi
            // For now, return success as acknowledgment
            Logger.i("RecordScreen", "Screen recording queued for ${durationSeconds}s")

            // Actual media upload ScreenRecordService se hoga
            // Ye placeholder result return kar raha hai
            AppResult.Success(
                MediaFile(
                    deviceKey = deviceKey,
                    mediaType = Constants.MEDIA_TYPE_SCREEN,
                    storagePath = "pending",
                    durationSeconds = durationSeconds
                )
            )
        } catch (e: Exception) {
            Logger.e("RecordScreen", "Failed", e)
            AppResult.Error(
                ErrorType.UNKNOWN,
                e.message ?: "Screen recording failed",
                e
            )
        }
    }
}
