package com.guard.screen.domain.usecase

import android.content.Context
import android.os.Build
import com.guard.screen.BuildConfig
import com.guard.screen.core.AppResult
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import com.guard.screen.data.model.DeviceInfo
import com.guard.screen.data.repository.DeviceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Device ko Supabase mein register karta hai.
 * Ye app start hone pe ek baar call hota hai.
 */
class RegisterDeviceUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository
) {

    suspend operator fun invoke(): AppResult<Boolean> {
        Logger.i("RegisterDevice", "Registering device")

        val deviceKey = DeviceKey.get(context)
        val batteryLevel = deviceRepository.getBatteryLevel()
        val isCharging = deviceRepository.isCharging()

        val device = DeviceInfo(
            deviceKey = deviceKey,
            brand = Build.BRAND ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            appVersion = BuildConfig.VERSION_NAME,
            lastSeen = getCurrentTimestamp(),
            batteryLevel = batteryLevel,
            isCharging = isCharging
        )

        return deviceRepository.registerDevice(device)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
    }
}
