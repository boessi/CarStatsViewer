package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.database.tripData.ChargingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.ui.plot.enums.PlotMarkerType
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineItem
import com.ixam97.carStatsViewer.ui.plot.objects.PlotMarker
import com.ixam97.carStatsViewer.ui.plot.objects.PlotMarkers

object DataConverters {

    private data class DrivingPointWithDistanceSum(
        val drivingPoint: DrivingPoint,
        val distanceSum: Float,
    )

    fun consumptionPlotLineFromDrivingPoints(drivingPoints: List<DrivingPoint>, maxDistance: Float? = null): List<PlotLineItem> {
        val plotLine = mutableListOf<PlotLineItem>()
        var distanceSum = 0f
        var startIndex = 0

        if (maxDistance != null ) run distanceLimit@ {
            drivingPoints.reversed().forEachIndexed { index, drivingPoint ->
                distanceSum += drivingPoint.distance_delta
                if (distanceSum > maxDistance + 1_000) {
                    startIndex = drivingPoints.size - index
                    return@distanceLimit
                }
            }
        }

        drivingPoints.forEachIndexed { index, drivingPoint ->
            if (index < startIndex) return@forEachIndexed
            if (index - startIndex == 0) plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, null))
            else {
                if ((drivingPoint.point_marker_type == 2 && plotLine[index - startIndex - 1].Marker == PlotLineMarkerType.END_SESSION))
                    plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint.copy(point_marker_type = 0), plotLine[index - startIndex - 1]))
                else
                    plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, plotLine[index - startIndex - 1]))
            }
        }
        return plotLine
    }

    fun consumptionPlotLineItemFromDrivingPoint(drivingPoint: DrivingPoint, prevPlotLineItem: PlotLineItem? = null): PlotLineItem {
        val markerType = PlotLineMarkerType.getType(drivingPoint.point_marker_type)

        return PlotLineItem(
            Value = drivingPoint.energy_delta,
            EpochTime = drivingPoint.driving_point_epoch_time,
            TimeDelta = drivingPoint.time_delta ?: ((drivingPoint.driving_point_epoch_time - (prevPlotLineItem?.EpochTime ?: drivingPoint.driving_point_epoch_time)) * 1_000_000),
            Distance = drivingPoint.distance_delta + (prevPlotLineItem?.Distance ?: 0f),
            DistanceDelta = drivingPoint.distance_delta,
            StateOfCharge = drivingPoint.state_of_charge * 100,
            StateOfChargeDelta = (drivingPoint.state_of_charge * 100) - (prevPlotLineItem?.StateOfCharge?: (drivingPoint.state_of_charge * 100)),
            Altitude = drivingPoint.alt?.let {
                when (it) {
                    0f -> null
                    else -> it
                }
            },
            Marker = markerType
        )
    }

    fun chargePlotLineFromChargingPoints(chargingPoints: List<ChargingPoint>): List<PlotLineItem> {
        val plotLine = mutableListOf<PlotLineItem>()

        chargingPoints.forEachIndexed() { index, chargingPoint ->
            if (index == 0) plotLine.add(chargePlotLineItemFromChargingPoint(chargingPoint, null))
            else plotLine.add(chargePlotLineItemFromChargingPoint(chargingPoint, plotLine[index - 1]))
        }

        return plotLine
    }

    fun chargePlotLineItemFromChargingPoint(chargingPoint: ChargingPoint, prevPlotLineItem: PlotLineItem? = null): PlotLineItem {
        val markerType = PlotLineMarkerType.getType(chargingPoint.point_marker_type)

        return PlotLineItem(
            Value = -chargingPoint.power / 1_000_000,
            EpochTime = chargingPoint.charging_point_epoch_time,
            TimeDelta = 1,
            Distance = 0f,
            DistanceDelta = 0f,
            StateOfCharge = chargingPoint.state_of_charge * 100,
            StateOfChargeDelta = (chargingPoint.state_of_charge * 100) - (prevPlotLineItem?.StateOfCharge ?: (chargingPoint.state_of_charge * 100)),
            Altitude = null,
            Marker = markerType
        )
    }

    fun plotMarkersFromSession(session: DrivingSession): PlotMarkers {
        val plotMarkers = mutableListOf<PlotMarker>()
        var distanceSum = 0f

        val markedDrivingPoints = mutableListOf<DrivingPointWithDistanceSum>()

        /** Create a list of driving points with markers and calculate distance sums. */
        session.drivingPoints?.forEach { drivingPoint ->
            distanceSum += drivingPoint.distance_delta
            if ((drivingPoint.point_marker_type?:0) != 0) {
                markedDrivingPoints.add(DrivingPointWithDistanceSum(drivingPoint, distanceSum))
            }
        }

        /** Create markers for charging sessions, making use of split sessions. */
        session.chargingSessions?.forEachIndexed { index, chargingSession ->
            if (chargingSession.end_epoch_time != null) {

                if (chargingSession.start_epoch_time <= (markedDrivingPoints.firstOrNull()?.drivingPoint?.driving_point_epoch_time?:0L) && index == 0) {
                    plotMarkers.add(PlotMarker(
                        MarkerType = PlotMarkerType.CHARGE,
                        MarkerVersion = 1,
                        StartTime = chargingSession.start_epoch_time,
                        EndTime = chargingSession.end_epoch_time,
                        StartDistance = 0f,
                        EndDistance = 0f
                    ))
                } else {
                    val startDrivingPoint = markedDrivingPoints.lastOrNull { it.drivingPoint.driving_point_epoch_time <= chargingSession.start_epoch_time }
                    val endDrivingPoint = markedDrivingPoints.firstOrNull { it.drivingPoint.driving_point_epoch_time >= chargingSession.end_epoch_time }

                    if (startDrivingPoint != null && endDrivingPoint != null) {
                        plotMarkers.add(PlotMarker(
                            MarkerType = PlotMarkerType.CHARGE,
                            MarkerVersion = 1,
                            StartTime = startDrivingPoint.drivingPoint.driving_point_epoch_time,
                            EndTime = endDrivingPoint.drivingPoint.driving_point_epoch_time,
                            StartDistance = startDrivingPoint.distanceSum,
                            EndDistance = endDrivingPoint.distanceSum
                        ))
                    }
                }
            }
        }

        /** Get regular park markers. */
        markedDrivingPoints.forEachIndexed { index, markedDrivingPoint ->
            if (markedDrivingPoint.drivingPoint.point_marker_type == 1 && index >= 1) {
                plotMarkers.add(PlotMarker(
                    MarkerType = PlotMarkerType.PARK,
                    MarkerVersion = 1,
                    StartTime = markedDrivingPoints[index-1].drivingPoint.driving_point_epoch_time,
                    EndTime = markedDrivingPoint.drivingPoint.driving_point_epoch_time,
                    StartDistance = markedDrivingPoint.distanceSum,
                    EndDistance = markedDrivingPoint.distanceSum
                ))
            }
        }

        /** Filter out park markers withing charge markers. */
        plotMarkers.filter { it.MarkerType == PlotMarkerType.CHARGE }.forEach { chargeMarker ->
            if (chargeMarker.EndTime != null)
                plotMarkers.removeIf {
                    it.MarkerType == PlotMarkerType.PARK
                    && it.StartTime >= chargeMarker.StartTime
                    && it.EndTime != null
                    && it.EndTime!! <= chargeMarker.EndTime!!
                }
        }

        return PlotMarkers().apply{ addMarkers(plotMarkers) }
    }
}