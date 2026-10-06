package com.guard.screen.domain.usecase

import android.content.Context
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamLiveScreenUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {

    private var streamJob: Job? = null
    private val isStreaming = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var frameIndex = 0L

    // ⭐ Private — external setter nahi
    private var frameProvider: (suspend () -> ByteArray?)? = null

    suspend operator fun invoke(deviceKey: String): AppResult<Boolean> {
        if (isStreaming.get()) {
            return AppResult.Error(ErrorType.VALIDATION, "Already streaming")
        }

        if (frameProvider == null) {
            return AppResult.Error(
                ErrorType.CONSENT,
                "Frame provider not set — MediaProjection consent needed"
            )
        }

        isStreaming.set(true)
        frameIndex = 0L

        Logger.i("StreamLive", "Starting live stream")

        streamJob = scope.launch {
            while (isActive && isStreaming.get()) {
                try {
                    val frameBytes = frameProvider?.invoke()

                    if (frameBytes != null && frameBytes.isNotEmpty()) {
                        frameIndex++

                        val result = mediaRepository.uploadLiveFrame(
                            deviceKey = deviceKey,
                            frameBytes = frameBytes,
                            frameIndex = frameIndex
                        )

                        when (result) {
                            is AppResult.Success -> {
                                Logger.d("StreamLive", "Frame $frameIndex uploaded")
                            }
                            is AppResult.Error -> {
                                Logger.w("StreamLive", "Frame $frameIndex failed: ${result.message}")
                            }
                            else -> {}
                        }

                        if (frameIndex % 10 == 0L) {
                            try {
                                mediaRepository.cleanOldLiveFrames(deviceKey)
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("StreamLive", "Loop iteration failed", e)
                }

                delay(Constants.LIVE_FRAME_INTERVAL_MS)
            }
            Logger.i("StreamLive", "Live stream ended")
        }

        return AppResult.Success(true)
    }

    fun stop() {
        if (!isStreaming.get()) return

        isStreaming.set(false)
        streamJob?.cancel()
        streamJob = null
        frameIndex = 0L
        Logger.i("StreamLive", "Stream stopped")
    }

    fun isActive(): Boolean = isStreaming.get()

    // ⭐ Sirf ye method public hai — koi var setter nahi
    fun setFrameProvider(provider: suspend () -> ByteArray?) {
        this.frameProvider = provider
    }

    fun clearFrameProvider() {
        this.frameProvider = null
    }
}
