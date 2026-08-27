package com.ixam97.carStatsViewer.database.tripData

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ixam97.carStatsViewer.database.batteryHealth.BatteryHealthDao
import com.ixam97.carStatsViewer.database.batteryHealth.BatteryHealthRecord

@Database(entities =
    [
        DrivingSession::class,
        DrivingPoint::class,
        ChargingSession::class,
        ChargingPoint::class,
        DrivingSessionPointCrossRef::class,
        DrivingChargingCrossRef::class,
        SessionMarker::class,
        BatteryHealthRecord::class
    ],
    version = 9
)
abstract class TripDataDatabase: RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun batteryHealthDao(): BatteryHealthDao
}