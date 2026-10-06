package com.guard.screen.data.remote

import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.data.model.Command
import com.guard.screen.data.model.CommandUpdate
import com.guard.screen.data.model.DeviceHeartbeat
import com.guard.screen.data.model.DeviceInfo
import com.guard.screen.data.model.LiveFrame
import com.guard.screen.data.model.LiveFrameUpload
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.model.MediaUploadRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {

    // ============================================
    // COMMANDS
    // ============================================

    /**
     * Pending commands fetch karo is device ke liye.
     */
    suspend fun getPendingCommands(deviceKey: String): List<Command> = withContext(Dispatchers.IO) {
        try {
            supabase.from(Constants.TABLE_COMMANDS)
                .select {
                    filter {
                        eq("device_key", deviceKey)
                        eq("status", Constants.STATUS_PENDING)
                    }
                }
                .decodeList<Command>()
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "getPendingCommands failed", e)
            emptyList()
        }
    }

    /**
     * Command status update karo.
     */
    suspend fun updateCommandStatus(
        commandId: String,
        status: String,
        errorMessage: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.from(Constants.TABLE_COMMANDS).update(
                CommandUpdate(
                    status = status,
                    executedAt = getCurrentTimestamp(),
                    errorMessage = errorMessage
                )
            ) {
                filter {
                    eq("id", commandId)
                }
            }
            true
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "updateCommandStatus failed", e)
            false
        }
    }

    // ============================================
    // DEVICES
    // ============================================

    /**
     * Device ko register karo ya update karo.
     */
    suspend fun upsertDevice(device: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.from(Constants.TABLE_DEVICES).upsert(device) {
                onConflict = "device_key"
            }
            true
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "upsertDevice failed", e)
            false
        }
    }

    /**
     * Heartbeat bhejo.
     */
    suspend fun sendHeartbeat(heartbeat: DeviceHeartbeat): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.from(Constants.TABLE_DEVICES).update(
                mapOf(
                    "last_seen" to heartbeat.lastSeen,
                    "battery_level" to heartbeat.batteryLevel,
                    "is_charging" to heartbeat.isCharging
                )
            ) {
                filter {
                    eq("device_key", heartbeat.deviceKey)
                }
            }
            true
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "sendHeartbeat failed", e)
            false
        }
    }

    // ============================================
    // MEDIA
    // ============================================

    /**
     * File ko Supabase Storage mein upload karo.
     */
    suspend fun uploadFile(
        bucket: String,
        storagePath: String,
        file: File
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = file.readBytes()
            supabase.storage.from(bucket).upload(
                path = storagePath,
                data = bytes
            ) {
                upsert = true
            }

            // Public URL lo
            val publicUrl = supabase.storage.from(bucket)
                .publicUrl(storagePath)

            Logger.d("SupabaseDS", "Uploaded: $storagePath (${bytes.size} bytes)")
            publicUrl
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "uploadFile failed", e)
            null
        }
    }

    /**
     * Media entry database mein insert karo.
     */
    suspend fun insertMedia(request: MediaUploadRequest): MediaFile? = withContext(Dispatchers.IO) {
        try {
            supabase.from(Constants.TABLE_MEDIA)
                .insert(request) {
                    select()
                }
                .decodeSingle<MediaFile>()
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "insertMedia failed", e)
            null
        }
    }

    // ============================================
    // LIVE FRAMES
    // ============================================

    /**
     * Live frame upload karo.
     */
    suspend fun uploadLiveFrame(
        deviceKey: String,
        frameBytes: ByteArray,
        frameIndex: Long
    ): String? = withContext(Dispatchers.IO) {
        try {
            val path = "$deviceKey/frame_$frameIndex.jpg"

            supabase.storage.from(Constants.BUCKET_LIVE_FRAMES).upload(
                path = path,
                data = frameBytes
            ) {
                upsert = true
            }

            val publicUrl = supabase.storage.from(Constants.BUCKET_LIVE_FRAMES)
                .publicUrl(path)

            // DB mein entry
            val upload = LiveFrameUpload(
                deviceKey = deviceKey,
                framePath = path,
                frameIndex = frameIndex,
                createdAt = getCurrentTimestamp()
            )

            supabase.from(Constants.TABLE_LIVE_FRAMES)
                .insert(upload)

            publicUrl
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "uploadLiveFrame failed", e)
            null
        }
    }

    /**
     * Live frame delete karo (purane frames).
     */
    suspend fun deleteOldLiveFrames(deviceKey: String, keepCount: Int = 10): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val frames = supabase.from(Constants.TABLE_LIVE_FRAMES)
                    .select {
                        filter {
                            eq("device_key", deviceKey)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<LiveFrame>()

                // Purane frames delete karo
                frames.drop(keepCount).forEach { frame ->
                    try {
                        supabase.storage.from(Constants.BUCKET_LIVE_FRAMES)
                            .delete(frame.framePath)
                        supabase.from(Constants.TABLE_LIVE_FRAMES).delete {
                            filter { eq("id", frame.id ?: "") }
                        }
                    } catch (_: Exception) {}
                }
                true
            } catch (e: Exception) {
                Logger.e("SupabaseDS", "deleteOldLiveFrames failed", e)
                false
            }
        }

    // ============================================
    // HELPERS
    // ============================================

    private fun getCurrentTimestamp(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())
    }
}
