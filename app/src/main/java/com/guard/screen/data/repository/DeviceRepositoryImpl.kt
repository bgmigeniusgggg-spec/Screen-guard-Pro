package com.guard.screen.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.guard.screen.BuildConfig
import com.guard.screen.core.AppResult
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.DeviceHeartbeat
import com.guard.screen.data.model.DeviceInfo
import com.guard.screen.data.remote.SupabaseDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val heartbeat = DeviceHeartbeat(
                deviceKey = deviceKey,
                lastSeen = getCurrentTimestamp(),
                batteryLevel = batteryLevel,
                isCharging = isCharging
            )
            val success = supabaseDS.sendHeartbeat(heartbeat)
            if (success) {
                AppResult.Success(true)
            } else {
                AppResult.Error(ErrorType.NETWORK, "Heartbeat failed")
            }
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

    // ============================================
    // HELPERS
    // ============================================

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
    }
}
