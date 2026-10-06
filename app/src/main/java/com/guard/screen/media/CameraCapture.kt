package com.guard.screen.media

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.guard.screen.core.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CameraCapturer @Inject constructor(
    private val context: Context
) {

    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Photo capture karo.
     */
    @SuppressLint("RestrictedApi")
    suspend fun capture(useFrontCamera: Boolean = true): File? =
        suspendCancellableCoroutine { continuation ->
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
                            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                            .build()

                        provider.unbindAll()
                        provider.bindToLifecycle(owner, selector, capture)

                        // Camera ready hone ka wait
                        Thread.sleep(700)

                        val file = File(
                            context.cacheDir,
                            "photo_${if (useFrontCamera) "front" else "back"}_${System.currentTimeMillis()}.jpg"
                        )

                        val options = ImageCapture.OutputFileOptions.Builder(file).build()

                        capture.takePicture(
                            options,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(
                                    results: ImageCapture.OutputFileResults
                                ) {
                                    Logger.d("CameraCapturer", "Saved: ${file.name}")
                                    provider.unbindAll()
                                    owner.stop()

                                    if (continuation.isActive) {
                                        continuation.resume(file)
                                    }
                                }

                                override fun onError(e: ImageCaptureException) {
                                    Logger.e("CameraCapturer", "Capture error", e)
                                    provider.unbindAll()
                                    owner.stop()
                                    try { file.delete() } catch (_: Exception) {}

                                    if (continuation.isActive) {
                                        continuation.resume(null)
                                    }
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Logger.e("CameraCapturer", "Failed", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {
                Logger.e("CameraCapturer", "Outer failed", e)
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private class DummyLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun start() { registry.currentState = Lifecycle.State.RESUMED }
        fun stop() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}
