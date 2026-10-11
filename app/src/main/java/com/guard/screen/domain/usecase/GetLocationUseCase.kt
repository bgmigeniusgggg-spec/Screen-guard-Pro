package com.guard.screen.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.guard.screen.core.AppResult
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.remote.SupabaseDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

class GetLocationUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseDS: SupabaseDataSource
) {

    @SuppressLint("MissingPermission")
    suspend operator fun invoke(deviceKey: String): AppResult<MediaFile> {
        Logger.d("GetLocation", "Fetching location for $deviceKey")

        val location = withTimeoutOrNull(15_000L) {
            getCurrentLocation()
        }

        if (location == null) {
            Logger.e("GetLocation", "Timeout or null")
            return AppResult.Error(ErrorType.TIMEOUT, "Location timeout")
        }

        return try {
            val success = supabaseDS.updateDeviceLocation(
                deviceKey = deviceKey,
                lat = location.latitude,
                lng = location.longitude,
                accuracy = location.accuracy
            )

            if (success) {
                Logger.i("GetLocation", "Location saved: ${location.latitude}, ${location.longitude}")
                AppResult.Success(
                    MediaFile(
                        deviceKey = deviceKey,
                        mediaType = "location",
                        storagePath = "",
                        durationSeconds = 0
                    )
                )
            } else {
                AppResult.Error(ErrorType.NETWORK, "Failed to save location")
            }
        } catch (e: Exception) {
            Logger.e("GetLocation", "Save failed", e)
            AppResult.Error(ErrorType.NETWORK, e.message ?: "Error", e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)

            // Pehle last location try
            client.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    if (continuation.isActive) continuation.resume(lastLoc)
                } else {
                    // Fresh location maango
                    requestFreshLocation(client, continuation)
                }
            }.addOnFailureListener {
                requestFreshLocation(client, continuation)
            }
        } catch (e: Exception) {
            Logger.e("GetLocation", "Failed", e)
            if (continuation.isActive) continuation.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(
        client: FusedLocationProviderClient,
        continuation: kotlin.coroutines.Continuation<Location?>
    ) {
        try {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            ).setMaxUpdates(1).build()

            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    client.removeLocationUpdates(this)
                    if (continuation is kotlinx.coroutines.CancellableContinuation && continuation.isActive) {
                        continuation.resume(result.lastLocation)
                    }
                }
            }, Looper.getMainLooper())
        } catch (e: Exception) {
            Logger.e("GetLocation", "Fresh request failed", e)
            if (continuation is kotlinx.coroutines.CancellableContinuation && continuation.isActive) {
                continuation.resume(null)
            }
        }
    }
}
