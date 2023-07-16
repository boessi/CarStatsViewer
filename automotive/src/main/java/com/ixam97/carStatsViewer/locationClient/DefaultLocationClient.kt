package com.ixam97.carStatsViewer.locationClient

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.matthiaszimmermann.location.egm96.Geoid

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {

    var locationNotAvailable = true
    var lastTimeStamp = 0L

    init {
        Geoid.init()

    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location?> {
        return callbackFlow {
            InAppLogger.i("[LOC] Setting up location client")
            if (!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permissions")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            locationNotAvailable = !isGpsEnabled && !isNetworkEnabled

            val request = LocationRequest.create()
                .setInterval(interval)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            if (locationNotAvailable) {
                throw LocationClient.LocationException("GPS is not enabled!")
            }

            val locationCallback = object: LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    InAppLogger.v("[LOC] LocationCallback ${locationResult.locations?.size ?: 0} locations")
                    locationResult.locations.lastOrNull()?.let { location ->
                        if (CarStatsViewer.appPreferences.useLocation) {
                            if (location.altitude > 0 || location.altitude < 0 || emulatorMode) {
                                location.altitude -= Geoid.getOffset(
                                    org.matthiaszimmermann.location.Location(location.latitude, location.longitude)
                                )
                                if (location.time > lastTimeStamp + 5_000) {
                                    InAppLogger.w("[LOC] LocationClient: Interval exceeded!")
                                }
                                lastTimeStamp = location.time
                                launch { send(location) }
                            } else {
                                InAppLogger.w("[LOC] LocationClient has returned altitude of 0m")
                            }
                        } else {
                            launch { send(null) }
                        }
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            InAppLogger.i("[LOC] Location tracking started. isGpsEnabled: $isGpsEnabled, isNetworkEnabled: $isNetworkEnabled")

            awaitClose {
                InAppLogger.i("[LOC] Canceling location tracking")
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}