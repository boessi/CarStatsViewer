package com.ixam97.carStatsViewer.database.batteryHealth

import androidx.room.Entity
import androidx.room.PrimaryKey

object BatteryHealthCycleType {
    const val CHARGE = 1
    const val DRIVE = 2
}

@Entity(tableName = "BatteryHealthRecord")
data class BatteryHealthRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val epoch_time: Long,
    val cycle_type: Int,                 // 1 = Charge, 2 = Drive
    val start_soc: Float,                // e.g. 20.0 (%)
    val end_soc: Float,                  // e.g. 80.0 (%)
    val soc_delta: Float,                // e.g. 60.0 (%)
    val energy_wh: Double,               // e.g. 43500.0 (Wh)
    val calculated_capacity_kwh: Double, // e.g. 72.5 (kWh)
    val ambient_temperature: Float? = null,
    val is_valid: Boolean = true
)
