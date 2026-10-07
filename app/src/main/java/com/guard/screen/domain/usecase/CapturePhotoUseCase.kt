package com.guard.screen.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume

class CapturePhotoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend operator fun invoke(
        deviceKey: String,
        useFrontCamera: Boolean = true
    ): AppResult<MediaFile> = withContext(Dispatchers.IO) {

        // 25 sec timeout
        val result = withTimeoutOrNull(25_000L) {
            captureWithCamera2(deviceKey, useFrontCamera)
        }

        result ?: run {
            Logger.e("CapturePhoto", "TIMEOUT")
            AppResult.Error(ErrorType.TIMEOUT, "Camera timed out")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun captureWithCamera2(
        deviceKey: String,
        useFrontCamera: Boolean
    ): AppResult<MediaFile> = suspendCancellableCoroutine { continuation ->

        var cameraDevice: CameraDevice? = null
        var session: CameraCaptureSession? = null
        var imageReader: ImageReader? = null
        var handlerThread: HandlerThread? = null

        fun cleanup() {
            try { session?.close() } catch (_: Exception) {}
            try { cameraDevice?.close() } catch (_: Exception) {}
            try { imageReader?.close() } catch (_: Exception) {}
            try { handlerThread?.quitSafely() } catch (_: Exception) {}
        }

        fun resumeError(msg: String) {
            if (continuation.isActive) {
                continuation.resume(AppResult.Error(ErrorType.STORAGE, msg))
            }
        }

        try {
            val thread = HandlerThread("Camera2BG").apply { start() }
            handlerThread = thread
            val handler = Handler(thread.looper)

            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val facing = if (useFrontCamera) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }

            var cameraId: String? = null
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.LENS_FACING) == facing) {
                    cameraId = id
                    break
                }
            }

            if (cameraId == null) {
                resumeError("Camera not found")
                cleanup()
                return@suspendCancellableCoroutine
            }

            val reader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 2)
            imageReader = reader

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    try {
                        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        request.addTarget(reader.surface)

                        camera.createCaptureSession(
                            listOf(reader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(s: CameraCaptureSession) {
                                    session = s
                                    try {
                                        s.capture(request.build(), null, handler)

                                        reader.setOnImageAvailableListener({ r ->
                                            val image = r.acquireLatestImage()
                                            if (image == null) {
                                                resumeError("No image data")
                                                cleanup()
                                                return@setOnImageAvailableListener
                                            }

                                            try {
                                                val buffer = image.planes[0].buffer
                                                val bytes = ByteArray(buffer.remaining())
                                                buffer.get(bytes)
                                                image.close()

                                                val file = File(
                                                    context.cacheDir,
                                                    "photo_${System.currentTimeMillis()}.jpg"
                                                )
                                                FileOutputStream(file).use { it.write(bytes) }
                                                Logger.d("CapturePhoto", "Saved: ${file.length()} bytes")

                                                scope.launch {
                                                    val uploadResult = mediaRepository.uploadMedia(
                                                        deviceKey = deviceKey,
                                                        mediaType = Constants.MEDIA_TYPE_PHOTO,
                                                        file = file
                                                    )
                                                    if (continuation.isActive) {
                                                        continuation.resume(uploadResult)
                                                    }
                                                    cleanup()
                                                }
                                            } catch (e: Exception) {
                                                Logger.e("CapturePhoto", "Read failed", e)
                                                try { image.close() } catch (_: Exception) {}
                                                resumeError(e.message ?: "Read failed")
                                                cleanup()
                                            }
                                        }, handler)

                                    } catch (e: Exception) {
                                        Logger.e("CapturePhoto", "Capture failed", e)
                                        resumeError(e.message ?: "Capture failed")
                                        cleanup()
                                    }
                                }

                                override fun onConfigureFailed(s: CameraCaptureSession) {
                                    resumeError("Session config failed")
                                    cleanup()
                                }
                            },
                            handler
                        )
                    } catch (e: Exception) {
                        Logger.e("CapturePhoto", "Request failed", e)
                        resumeError(e.message ?: "Request failed")
                        cleanup()
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    resumeError("Camera disconnected")
                    cleanup()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    resumeError("Camera error code: $error")
                    cleanup()
                }
            }, handler)

        } catch (e: Exception) {
            Logger.e("CapturePhoto", "Setup failed", e)
            resumeError(e.message ?: "Setup failed")
            cleanup()
        }
    }
}
