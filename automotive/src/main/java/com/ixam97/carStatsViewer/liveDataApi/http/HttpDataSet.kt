package com.ixam97.carStatsViewer.liveDataApi.http

import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint

data class HttpDataSet(
    val apiVersion: String = "2.1",
    val appVersion: String,

    val timestamp: Long,

    val staticBatteryCapacity: Float?,
    val staticVehicleMake: String?,
    val staticModelName: String?,

    val realTimeSpeed: Float?,
    val realTimePower: Float?,
    val realTimeGear: String?,
    val realTimeIgnitionState: String?,
    val realTimeChargePortConnected: Boolean?,
    val realTimeBatteryLevel: Float?,
    val realTimeStateOfCharge: Float?,
    val realTimeAmbientTemperature: Float?,
    val realTimeLat: Float?,
    val realTimeLon: Float?,
    val realTimeAlt: Float?,

    val drivingSessionId: Long?,
    val drivingSessionType: Int?,
    val drivingSessionStartTime: Long?,
    val drivingSessionEndTime: Long?,
    val drivingSessionDriveTime: Long?,
    val drivingSessionTripTime: Long?,
    val drivingSessionUsedEnergy: Double?,
    val drivingSessionUsedStateOfCharge: Double?,
    val drivingSessionTraveledDistance: Double?,

    // Delta Values
    val deltaUsedEnergy: Float?,
    val deltaTraveledDistance: Float?,
    val deltaTimeSpan: Long?,

    val drivingPoints: List<DrivingPoint>? = null,
    val chargingSessions: List<ChargingSession>? = null
)
