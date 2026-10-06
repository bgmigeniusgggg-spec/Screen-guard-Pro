package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.model.DeviceInfo

interface DeviceRepository {

    /** Device ko register karo (pehli baar) */
    suspend fun registerDevice(device: DeviceInfo): AppResult<Boolean>

    /** Heartbeat bhejo — har 5 min */
    suspend fun sendHeartbeat(
        deviceKey: String,
        batteryLevel: Int,
        isCharging: Boolean
    ): AppResult<Boolean>

    /** Battery level lo */
    suspend fun getBatteryLevel(): Int

    /** Charging status lo */
    suspend fun isCharging(): Boolean
}
