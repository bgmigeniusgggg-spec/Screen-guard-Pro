package com.guard.screen.domain.usecase

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Front/Back camera se photo capture karta hai aur upload karta hai.
 */
class CapturePhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {

    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Photo capture karo aur upload karo.
     * @param useFrontCamera true → front, false → back
     */
    suspend operator fun invoke(
        deviceKey: String,
        useFrontCamera: Boolean = true
    ): AppResult<MediaFile> = suspendCancellableCoroutine { continuation ->
        try {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    val provider = future.get()

                    val owner = DummyLifecycleOwner()
                    owner.start()

                    val selector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    provider.unbindAll()
                    provider.bindToLifecycle(owner, selector, capture)

                    // Camera ready hone ka wait
                    Thread.sleep(700)

                    val file = File(
                        context.cacheDir,
                        "photo_${System.currentTimeMillis()}.jpg"
                    )

                    val options = ImageCapture.OutputFileOptions.Builder(file).build()

                    capture.takePicture(
                        options,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(
                                outputFileResults: ImageCapture.OutputFileResults
                            ) {
                                Logger.d("CapturePhoto", "Saved: ${file.absolutePath}")
                                provider.unbindAll()
                                owner.stop()

                                // Upload karo
                                kotlinx.coroutines.CoroutineScope(
                                    kotlinx.coroutines.Dispatchers.IO
                                ).launch {
                                    val result = mediaRepository.uploadMedia(
                                        deviceKey = deviceKey,
                                        mediaType = Constants.MEDIA_TYPE_PHOTO,
                                        file = file
                                    )
                                    if (continuation.isActive) {
                                        continuation.resume(result)
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Logger.e("CapturePhoto", "Capture error", exception)
                                provider.unbindAll()
                                owner.stop()
                                try { file.delete() } catch (_: Exception) {}

                                if (continuation.isActive) {
                                    continuation.resume(
                                        AppResult.Error(
                                            ErrorType.STORAGE,
                                            exception.message ?: "Capture failed",
                                            exception
                                        )
                                    )
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    Logger.e("CapturePhoto", "Failed", e)
                    if (continuation.isActive) {
                        continuation.resume(
                            AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Error", e)
                        )
                    }
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Logger.e("CapturePhoto", "Outer failed", e)
            if (continuation.isActive) {
                continuation.resume(
                    AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Error", e)
                )
            }
        }
    }

    /**
     * Dummy lifecycle owner — CameraX ke liye.
     */
    private class DummyLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun start() { registry.currentState = Lifecycle.State.RESUMED }
        fun stop() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}

// Extension for launching coroutine inside callback
private fun kotlinx.coroutines.CoroutineScope.launch(block: suspend () -> Unit) {
    kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.IO) {
        block()
    }
}
