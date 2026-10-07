package com.guard.screen.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.DeviceInfo
import com.guard.screen.data.remote.SupabaseDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseDS: SupabaseDataSource
) : DeviceRepository {

    override suspend fun registerDevice(device: DeviceInfo): AppResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val success = supabaseDS.upsertDevice(device)
                if (success) {
                    Logger.i("DeviceRepo", "Device registered: ${device.deviceKey}")
                    AppResult.Success(true)
                } else {
                    AppResult.Error(ErrorType.UNKNOWN, "Registration failed")
                }
            } catch (e: Exception) {
                Logger.e("DeviceRepo", "registerDevice failed", e)
                AppResult.Error(ErrorType.NETWORK, e.message ?: "Error", e)
            }
        }

    override suspend fun sendHeartbeat(
        deviceKey: String,
        batteryLevel: Int,
        isCharging: Boolean
    ): AppResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            val url = URL(
                "${Constants.SUPABASE_URL}/rest/v1/devices?device_key=eq.$deviceKey"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.setRequestProperty("apikey", Constants.SUPABASE_ANON_KEY)
            conn.setRequestProperty("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val body = """{"last_seen":"$timestamp","battery_level":$batteryLevel,"is_charging":$isCharging}"""
            conn.outputStream.write(body.toByteArray())
            conn.outputStream.flush()

            val code = conn.responseCode
            Logger.d("DeviceRepo", "Heartbeat: HTTP $code")

            conn.disconnect()

            if (code in 200..299) AppResult.Success(true)
            else AppResult.Error(ErrorType.NETWORK, "HTTP $code")
        } catch (e: Exception) {
            Logger.e("DeviceRepo", "sendHeartbeat failed", e)
            AppResult.Error(ErrorType.NETWORK, e.message ?: "Error", e)
        }
    }

    override suspend fun getBatteryLevel(): Int = withContext(Dispatchers.IO) {
        try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            ) ?: return@withContext -1

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (scale > 0) (level * 100) / scale else -1
        } catch (e: Exception) {
            Logger.e("DeviceRepo", "getBatteryLevel failed", e)
            -1
        }
    }

    override suspend fun isCharging(): Boolean = withContext(Dispatchers.IO) {
        try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            ) ?: return@withContext false

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }
}
