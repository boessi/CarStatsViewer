package com.ixam97.carStatsViewer.liveDataApi.http

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import com.google.gson.Gson
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataProcessor.DeltaData
import com.ixam97.carStatsViewer.dataProcessor.IgnitionState
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.ui.views.FixedSwitchWidget
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64


class HttpLiveData (
    detailedLog : Boolean = true
): LiveDataApi("Webhook", R.string.settings_apis_http, detailedLog) {
    private fun addBasicAuth(connection: HttpURLConnection, username: String, password: String) {
        if (username == ""  && password == "") {
            return
        }

        val encoded: String = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8)) //Java 8

        connection.setRequestProperty("Authorization", "Basic $encoded")
    }

    private fun getConnection(url: URL, username: String, password: String) : HttpURLConnection {
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
        con.setRequestProperty("Accept","application/json")
        con.setRequestProperty("User-Agent", "CarStatsViewer %s".format(BuildConfig.VERSION_NAME))
        con.connectTimeout = timeout
        con.readTimeout = timeout
        con.doOutput = true
        con.doInput = true

        addBasicAuth(con, username, password)

        return con
    }

    private fun isValidURL(possibleURL: CharSequence?): Boolean {
        if (possibleURL == null) {
            return false
        }

        if (!possibleURL.contains("https://"))
            return false

        return android.util.Patterns.WEB_URL.matcher(possibleURL).matches()
    }

    override fun showSettingsDialog(context: Context) {
        super.showSettingsDialog(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_http_live_data, null)
        val url = layout.findViewById<EditText>(R.id.http_live_data_url)
        val username = layout.findViewById<EditText>(R.id.http_live_data_username)
        val password = layout.findViewById<EditText>(R.id.http_live_data_password)
        val httpLiveDataEnabled = layout.findViewById<FixedSwitchWidget>(R.id.http_live_data_enabled)
        val httpLiveDataLocation = layout.findViewById<FixedSwitchWidget>(R.id.http_live_data_location)

        val httpLiveDataSettingsDialog = AlertDialog.Builder(context).apply {
            setTitle(R.string.settings_apis_title)
            // setMessage(R.string.http_description)
            setPositiveButton("OK") {dialog, _ ->
                AppPreferences(context).httpLiveDataURL = url.text.toString()
                AppPreferences(context).httpLiveDataUsername = username.text.toString()
                AppPreferences(context).httpLiveDataPassword = password.text.toString()
                dialog.cancel()
            }
            setView(layout)
            setCancelable(true)
            create()
        }

        val dialog = httpLiveDataSettingsDialog.show()

        httpLiveDataEnabled.isChecked = AppPreferences(context).httpLiveDataEnabled
        httpLiveDataLocation.isChecked = AppPreferences(context).httpLiveDataLocation

        httpLiveDataEnabled.setSwitchClickListener {
            AppPreferences(context).httpLiveDataEnabled = httpLiveDataEnabled.isChecked
            if (!httpLiveDataEnabled.isChecked) connectionStatus = ConnectionStatus.UNUSED
            updateWatchdog()
        }
        httpLiveDataLocation.setSwitchClickListener {
            AppPreferences(context).httpLiveDataLocation = httpLiveDataLocation.isChecked
        }

        url.setText(AppPreferences(context).httpLiveDataURL)
        username.setText(AppPreferences(context).httpLiveDataUsername)
        password.setText(AppPreferences(context).httpLiveDataPassword)

        // Enable the Ok button initially only in case the user already entered a valid URL
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isValidURL(url.text.toString())

        url.addTextChangedListener(object : TextValidator(url) {
            override fun validate(textView: TextView?, text: String?) {
                if (text == null || textView == null) {
                    return
                }
                if (!isValidURL(text) && text.isNotEmpty()) {
                    textView.error = context.getString(R.string.http_invalid_url)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    return
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        })
    }

    override fun sendNow(realTimeData: RealTimeData, drivingSession: DrivingSession?, deltaData: DeltaData?) {

        if (!AppPreferences(CarStatsViewer.appContext).httpLiveDataEnabled) {
            connectionStatus = ConnectionStatus.UNUSED
            return
        }

        if (!realTimeData.isInitialized()) {
            InAppLogger.w("Real time data is not entirely initialized: ${realTimeData}")
            connectionStatus = ConnectionStatus.ERROR
            return
        }

        connectionStatus = try {
            val useLocation = AppPreferences(CarStatsViewer.appContext).httpLiveDataLocation
            send(
                HttpDataSet(
                    apiVersion = "2.1",
                    appVersion = BuildConfig.VERSION_NAME,

                    timestamp = System.currentTimeMillis(),

                    CarStatsViewer.dataProcessor.staticVehicleData.batteryCapacity,
                    CarStatsViewer.dataProcessor.staticVehicleData.vehicleMake,
                    CarStatsViewer.dataProcessor.staticVehicleData.modelName,

                    realTimeSpeed = realTimeData.speed!!,
                    realTimePower = realTimeData.power!!,
                    realTimeGear = StringFormatters.getGearString(realTimeData.selectedGear!!),
                    realTimeIgnitionState = IgnitionState.nameMap[realTimeData.ignitionState!!]?:"UNKNOWN",
                    realTimeChargePortConnected = realTimeData.chargePortConnected!!,
                    realTimeBatteryLevel = realTimeData.batteryLevel!!,
                    realTimeStateOfCharge = realTimeData.stateOfCharge!!,
                    realTimeAmbientTemperature = realTimeData.ambientTemperature!!,

                    realTimeLat = if (useLocation) realTimeData.lat else null,
                    realTimeLon = if (useLocation) realTimeData.lon else null,
                    realTimeAlt = if (useLocation) realTimeData.alt else null,

                    // Session Data
                    drivingSession?.driving_session_id,
                    drivingSession?.session_type,
                    drivingSession?.start_epoch_time,
                    drivingSession?.end_epoch_time,
                    drivingSession?.drive_time,
                    drivingSession?.trip_time,
                    drivingSession?.used_energy,
                    drivingSession?.used_soc,
                    drivingSession?.driven_distance,

                    // DeltaValues
                    deltaData?.usedEnergy,
                    deltaData?.traveledDistance,
                    deltaData?.timeSpan,

                    deltaData?.drivingPoints
                )
            )
        } catch (e: java.lang.Exception) {
            InAppLogger.e("[HTTP] Dataset error")
            ConnectionStatus.ERROR
        }
    }

    override fun isEnabled(): Boolean {
        return AppPreferences(CarStatsViewer.appContext).httpLiveDataEnabled
    }

    private fun send(dataSet: HttpDataSet, context: Context = CarStatsViewer.appContext): ConnectionStatus {
        val username = AppPreferences(context).httpLiveDataUsername
        val password = AppPreferences(context).httpLiveDataPassword
        val responseCode: Int

        val gson = Gson()
        val liveDataJson = gson.toJson(dataSet)

        try {
            val url = URL(AppPreferences(context).httpLiveDataURL) // + "?json=$jsonObject")
            val connection = getConnection(url, username, password)

            DataOutputStream(connection.outputStream).apply {
                writeBytes(liveDataJson)
                flush()
                close()
            }

            responseCode = connection.responseCode

            if (detailedLog) {
                var logString = "[HTTP] Status: ${connection.responseCode}, Msg: ${connection.responseMessage}, Content:"
                logString += try {
                    if (connection.responseCode in 100..399) {
                        connection.inputStream.bufferedReader().use {it.readText()}
                    } else {
                        connection.errorStream.bufferedReader().use {it.readText()}
                    }
                } catch (e: java.lang.Exception) {
                    "No response content"
                }
                InAppLogger.d(logString)
            }

            if (connection.responseCode in 100..399) {
                connection.inputStream.close()
            } else {
                connection.errorStream.close()
            }
            connection.disconnect()
        } catch (e: java.net.SocketTimeoutException) {
            InAppLogger.e("[HTTP] Network timeout error")
            if (timeout < originalInterval * 5) {
                timeout += originalInterval
                InAppLogger.w("[HTTP] Interval increased to $timeout ms")
            }
            return ConnectionStatus.ERROR
        } catch (e: java.lang.Exception) {
            InAppLogger.e("[HTTP] Connection error ${e.message}")
            return ConnectionStatus.ERROR
        }

        if (responseCode >= 400) {
            InAppLogger.e("[HTTP] Transmission failed. Status code $responseCode")
            return ConnectionStatus.ERROR
        }

        if (timeout > originalInterval) {
            timeout -= originalInterval
            InAppLogger.i("[HTTP] Interval decreased to $timeout ms")
            return ConnectionStatus.LIMITED
        }

        return ConnectionStatus.CONNECTED
    }
}

