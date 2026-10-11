package com.guard.screen.data.remote

import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.data.model.Command
import com.guard.screen.data.model.CommandUpdate
import com.guard.screen.data.model.DeviceHeartbeat
import com.guard.screen.data.model.DeviceInfo
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.model.MediaUploadRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {

    // ============================================
    // COMMANDS
    // ============================================

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
                filter { eq("id", commandId) }
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

    suspend fun upsertDevice(device: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.from(Constants.TABLE_DEVICES).upsert(device) {
                select()
            }
            true
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "upsertDevice failed", e)
            false
        }
    }

    suspend fun sendHeartbeat(heartbeat: DeviceHeartbeat): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.from(Constants.TABLE_DEVICES).update(
                mapOf(
                    "last_seen" to heartbeat.lastSeen,
                    "battery_level" to heartbeat.batteryLevel,
                    "is_charging" to heartbeat.isCharging
                )
            ) {
                filter { eq("device_key", heartbeat.deviceKey) }
            }
            true
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "sendHeartbeat failed", e)
            false
        }
    }

    // ============================================
    // ⭐ LOCATION UPDATE
    // ============================================

    suspend fun updateDeviceLocation(
        deviceKey: String,
        lat: Double,
        lng: Double,
        accuracy: Float
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val locationData = mapOf(
                "lat" to lat,
                "lng" to lng,
                "accuracy" to accuracy.toDouble(),
                "time" to System.currentTimeMillis()
            )

            supabase.from(Constants.TABLE_DEVICES).update(
                mapOf(
                    "location" to locationData,
                    "last_seen" to getCurrentTimestamp()
                )
            ) {
                filter { eq("device_key", deviceKey) }
            }
            true
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "updateDeviceLocation failed", e)
            false
        }
    }

    // ============================================
    // MEDIA UPLOAD
    // ============================================

    suspend fun uploadFile(
        bucket: String,
        storagePath: String,
        file: File
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = file.readBytes()

            supabase.storage.from(bucket).upload(
                path = storagePath,
                data = bytes,
                upsert = true
            )

            val publicUrl = "${Constants.SUPABASE_URL}/storage/v1/object/public/$bucket/$storagePath"

            Logger.d("SupabaseDS", "Uploaded: $storagePath -> $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "uploadFile failed", e)
            null
        }
    }

    suspend fun insertMedia(
        request: MediaUploadRequest,
        publicUrl: String?
    ): MediaFile? = withContext(Dispatchers.IO) {
        try {
            val finalRequest = request.copy(publicUrl = publicUrl)

            supabase.from(Constants.TABLE_MEDIA)
                .insert(finalRequest) {
                    select()
                }
                .decodeSingle<MediaFile>()
        } catch (e: Exception) {
            Logger.e("SupabaseDS", "insertMedia failed", e)
            null
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
    }
}
