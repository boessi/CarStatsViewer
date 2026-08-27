package com.ixam97.carStatsViewer.batteryHealth

import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.database.batteryHealth.BatteryHealthCycleType
import com.ixam97.carStatsViewer.database.batteryHealth.BatteryHealthDao
import com.ixam97.carStatsViewer.database.batteryHealth.BatteryHealthRecord
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

class BatteryHealthManager(
    private val batteryHealthDao: BatteryHealthDao,
    private val appPreferences: AppPreferences
) {
    private val _batteryHealthState = MutableStateFlow(
        BatteryHealthState(referenceCapacityKwh = appPreferences.referenceBatteryCapacity.toDouble())
    )
    val batteryHealthState = _batteryHealthState.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            recalculate()
        }
    }

    suspend fun processChargingSession(chargingSession: ChargingSession) = withContext(Dispatchers.IO) {
        try {
            val chargingPoints = chargingSession.chargingPoints
            if (chargingPoints == null || chargingPoints.size < 3) {
                InAppLogger.d("[SoH] Charging session skipped: insufficient data points")
                return@withContext
            }

            val startSoc = chargingPoints.first().state_of_charge
            val endSoc = chargingPoints.last().state_of_charge
            val socGain = endSoc - startSoc

            val sessionDurationMs = (chargingSession.end_epoch_time ?: System.currentTimeMillis()) - chargingSession.start_epoch_time
            val chargeTimeMs = chargingSession.chargeTime ?: 0L

            // Verify continuous charging to avoid invalid calculations when AAOS sleeps during AC charge
            val isContinuous = chargeTimeMs >= (sessionDurationMs * 0.85) || sessionDurationMs < 60_000L
            val chargedEnergyWh = chargingSession.charged_energy

            InAppLogger.i("[SoH] Evaluating charge: socGain=${(socGain * 100).toInt()}%, continuous=$isContinuous, energy=${chargedEnergyWh / 1000.0} kWh")

            if (socGain >= 0.20f && isContinuous && chargedEnergyWh > 5_000.0) {
                val capacityKwh = (chargedEnergyWh / socGain) / 1000.0

                // Plausibility check (40 kWh to 130 kWh covers current EV batteries)
                if (capacityKwh in 40.0..130.0) {
                    val record = BatteryHealthRecord(
                        epoch_time = System.currentTimeMillis(),
                        cycle_type = BatteryHealthCycleType.CHARGE,
                        start_soc = roundToOneDecimal(startSoc * 100f),
                        end_soc = roundToOneDecimal(endSoc * 100f),
                        soc_delta = roundToOneDecimal(socGain * 100f),
                        energy_wh = chargedEnergyWh,
                        calculated_capacity_kwh = roundToOneDecimal(capacityKwh),
                        ambient_temperature = chargingSession.outside_temp,
                        is_valid = true
                    )
                    batteryHealthDao.insertRecord(record)
                    InAppLogger.i("[SoH] Inserted valid charge cycle: ${record.calculated_capacity_kwh} kWh (ΔSoC: ${record.soc_delta}%)")
                    recalculate()
                } else {
                    InAppLogger.w("[SoH] Calculated capacity outside plausible bounds: $capacityKwh kWh")
                }
            }
        } catch (e: Exception) {
            InAppLogger.e("[SoH] Error processing charging session: ${e.message}")
        }
    }

    suspend fun processDrivingSession(drivingSession: DrivingSession) = withContext(Dispatchers.IO) {
        try {
            val drivingPoints = drivingSession.drivingPoints
            if (drivingPoints == null || drivingPoints.size < 10) {
                return@withContext
            }

            val startSoc = drivingPoints.first().state_of_charge
            val endSoc = drivingPoints.last().state_of_charge
            val socLoss = startSoc - endSoc
            val usedEnergyWh = drivingSession.used_energy

            InAppLogger.i("[SoH] Evaluating drive: socLoss=${(socLoss * 100).toInt()}%, energy=${usedEnergyWh / 1000.0} kWh")

            if (socLoss >= 0.25f && usedEnergyWh > 8_000.0) {
                val capacityKwh = (usedEnergyWh / socLoss) / 1000.0

                if (capacityKwh in 40.0..130.0) {
                    val record = BatteryHealthRecord(
                        epoch_time = System.currentTimeMillis(),
                        cycle_type = BatteryHealthCycleType.DRIVE,
                        start_soc = roundToOneDecimal(startSoc * 100f),
                        end_soc = roundToOneDecimal(endSoc * 100f),
                        soc_delta = roundToOneDecimal(socLoss * 100f),
                        energy_wh = usedEnergyWh,
                        calculated_capacity_kwh = roundToOneDecimal(capacityKwh),
                        ambient_temperature = CarStatsViewer.dataProcessor.realTimeData.ambientTemperature,
                        is_valid = true
                    )
                    batteryHealthDao.insertRecord(record)
                    InAppLogger.i("[SoH] Inserted valid drive cycle: ${record.calculated_capacity_kwh} kWh (ΔSoC: ${record.soc_delta}%)")
                    recalculate()
                } else {
                    InAppLogger.w("[SoH] Calculated drive capacity outside plausible bounds: $capacityKwh kWh")
                }
            }
        } catch (e: Exception) {
            InAppLogger.e("[SoH] Error processing driving session: ${e.message}")
        }
    }

    suspend fun recalculate() = withContext(Dispatchers.IO) {
        try {
            val records = batteryHealthDao.getAllValidRecords()
            val refCapacity = appPreferences.referenceBatteryCapacity.toDouble()

            if (records.isEmpty()) {
                _batteryHealthState.value = BatteryHealthState(
                    referenceCapacityKwh = refCapacity,
                    confidenceLevel = BatteryHealthConfidence.INITIAL,
                    confidenceScore = 0,
                    progressPercent = 0,
                    validCycleCount = 0,
                    lastCalculationEpoch = System.currentTimeMillis()
                )
                return@withContext
            }

            val capacities = records.map { it.calculated_capacity_kwh }.sorted()
            val medianCapacity = if (capacities.size % 2 == 1) {
                capacities[capacities.size / 2]
            } else {
                (capacities[capacities.size / 2 - 1] + capacities[capacities.size / 2]) / 2.0
            }

            val mean = capacities.average()
            val variance = capacities.map { (it - mean) * (it - mean) }.average()
            val stdDev = sqrt(variance)
            val relativeSpread = if (medianCapacity > 0) (stdDev / medianCapacity) * 100.0 else 0.0

            val count = records.size
            val progressPercent = min(100, count * 10) // 10 cycles = 100% learning progress

            val confidenceLevel = when {
                count < 3 -> BatteryHealthConfidence.INITIAL
                count in 3..5 -> BatteryHealthConfidence.LOW
                count in 6..8 -> BatteryHealthConfidence.MEDIUM
                count >= 9 && stdDev < 2.5 -> BatteryHealthConfidence.HIGH
                else -> BatteryHealthConfidence.MEDIUM
            }

            val confidenceScore = when (confidenceLevel) {
                BatteryHealthConfidence.INITIAL -> (count * 10).coerceAtMost(25)
                BatteryHealthConfidence.LOW -> (30 + (count - 3) * 10 - relativeSpread.toInt()).coerceIn(26, 55)
                BatteryHealthConfidence.MEDIUM -> (60 + (count - 6) * 5 - (relativeSpread * 1.5).toInt()).coerceIn(56, 80)
                BatteryHealthConfidence.HIGH -> (85 + (count - 9) * 3 - relativeSpread.toInt()).coerceIn(81, 100)
            }

            val tolerancePercent = when (confidenceLevel) {
                BatteryHealthConfidence.INITIAL -> 5.0
                BatteryHealthConfidence.LOW -> 3.0
                BatteryHealthConfidence.MEDIUM -> 1.8
                BatteryHealthConfidence.HIGH -> 0.9
            }

            val rawSoh = (medianCapacity / refCapacity) * 100.0
            val sohPercent = roundToOneDecimal(rawSoh.coerceIn(50.0, 105.0))
            val degradationPercent = roundToOneDecimal((100.0 - sohPercent).coerceAtLeast(0.0))

            val recentRecords = batteryHealthDao.getLatestRecords(10)

            val newState = BatteryHealthState(
                stateOfHealthPercent = sohPercent,
                usableCapacityKwh = roundToOneDecimal(medianCapacity),
                referenceCapacityKwh = refCapacity,
                degradationPercent = degradationPercent,
                confidenceLevel = confidenceLevel,
                confidenceScore = confidenceScore,
                progressPercent = progressPercent,
                validCycleCount = count,
                estimatedTolerancePercent = tolerancePercent,
                lastCalculationEpoch = System.currentTimeMillis(),
                recentRecords = recentRecords
            )

            _batteryHealthState.value = newState
            InAppLogger.i("[SoH] Recalculated: SoH=$sohPercent%, Capacity=$medianCapacity kWh (Ref: $refCapacity kWh), Confidence=$confidenceLevel ($confidenceScore%), Cycles=$count")
        } catch (e: Exception) {
            InAppLogger.e("[SoH] Error during recalculation: ${e.message}")
        }
    }

    private fun roundToOneDecimal(value: Double): Double {
        return round(value * 10.0) / 10.0
    }

    private fun roundToOneDecimal(value: Float): Float {
        return round(value * 10.0f) / 10.0f
    }
}
