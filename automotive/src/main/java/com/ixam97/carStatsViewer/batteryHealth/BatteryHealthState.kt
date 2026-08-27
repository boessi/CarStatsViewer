package com.ixam97.carStatsViewer.batteryHealth

import com.ixam97.carStatsViewer.database.batteryHealth.BatteryHealthRecord

enum class BatteryHealthConfidence(val level: String) {
    INITIAL("INITIAL"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH")
}

data class BatteryHealthState(
    val stateOfHealthPercent: Double? = null,           // e.g. 96.8 (%)
    val usableCapacityKwh: Double? = null,              // e.g. 72.6 (kWh)
    val referenceCapacityKwh: Double = 75.0,            // e.g. 75.0 (kWh)
    val degradationPercent: Double? = null,             // e.g. 3.2 (%)
    val confidenceLevel: BatteryHealthConfidence = BatteryHealthConfidence.INITIAL,
    val confidenceScore: Int = 0,                       // 0 - 100 (%)
    val progressPercent: Int = 0,                       // 0 - 100 (%)
    val validCycleCount: Int = 0,                       // Number of valid samples
    val estimatedTolerancePercent: Double? = null,      // e.g. ± 1.1 (%)
    val lastCalculationEpoch: Long? = null,             // Timestamp of last update
    val recentRecords: List<BatteryHealthRecord> = emptyList()
)
