package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.database.tripData.DrivingPoint

data class DeltaData(
    val usedEnergy: Float? = null,
    val traveledDistance: Float? = null,
    val timeSpan: Long? = null,
    val refEpoch: Long? = null,
    val drivingPoints: List<DrivingPoint>? = null
)
