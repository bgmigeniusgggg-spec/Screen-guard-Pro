package com.guard.screen.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

class RecordAudioUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {

    private var currentRecorder: MediaRecorder? = null
    private var isRecording = false

    @SuppressLint("MissingPermission")
    suspend operator fun invoke(
        deviceKey: String,
        durationSeconds: Int = Constants.DEFAULT_MIC_DURATION
    ): AppResult<MediaFile> {

        if (isRecording) {
            return AppResult.Error(ErrorType.VALIDATION, "Already recording")
        }

        if (durationSeconds <= 0 || durationSeconds > Constants.MAX_DURATION) {
            return AppResult.Error(ErrorType.VALIDATION, "Invalid duration")
        }

        // ⭐ Total timeout = duration + 30 sec buffer
        val totalTimeout = (durationSeconds + 30) * 1000L

        val result = withTimeoutOrNull(totalTimeout) {
            recordInternal(deviceKey, durationSeconds)
        }

        return result ?: run {
            Logger.e("RecordAudio", "TIMEOUT after $totalTimeout ms")
            AppResult.Error(ErrorType.TIMEOUT, "Audio recording timed out")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun recordInternal(
        deviceKey: String,
        durationSeconds: Int
    ): AppResult<MediaFile> = withContext(Dispatchers.IO) {

        isRecording = true
        var recorder: MediaRecorder? = null
        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")

        try {
            recorder = createRecorder()
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            currentRecorder = recorder

            Logger.d("RecordAudio", "Recording for ${durationSeconds}s")
            delay(durationSeconds * 1000L)

            try {
                recorder.stop()
            } catch (e: Exception) {
                Logger.e("RecordAudio", "Stop failed", e)
            }
            try {
                recorder.release()
            } catch (_: Exception) {}
            currentRecorder = null

            if (!file.exists() || file.length() == 0L) {
                file.delete()
                return@withContext AppResult.Error(
                    ErrorType.STORAGE,
                    "Recording file empty"
                )
            }

            Logger.i("RecordAudio", "Recorded ${file.length()} bytes")

            mediaRepository.uploadMedia(
                deviceKey = deviceKey,
                mediaType = Constants.MEDIA_TYPE_AUDIO,
                file = file,
                durationSeconds = durationSeconds
            )
        } catch (e: Exception) {
            Logger.e("RecordAudio", "Failed", e)
            try { recorder?.release() } catch (_: Exception) {}
            currentRecorder = null
            if (file.exists()) file.delete()

            AppResult.Error(
                ErrorType.STORAGE,
                e.message ?: "Recording failed",
                e
            )
        } finally {
            isRecording = false
        }
    }

    fun stop() {
        try {
            currentRecorder?.stop()
            currentRecorder?.release()
        } catch (_: Exception) {}
        currentRecorder = null
        isRecording = false
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
