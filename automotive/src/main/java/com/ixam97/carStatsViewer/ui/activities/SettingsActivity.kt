package com.ixam97.carStatsViewer.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.plot.objects.PlotGlobalConfiguration
import com.ixam97.carStatsViewer.utils.setContentViewAndTheme
import kotlinx.android.synthetic.main.activity_settings.settings_about_widget
import kotlinx.android.synthetic.main.activity_settings.settings_apis_widget
import kotlinx.android.synthetic.main.activity_settings.settings_button_back
import kotlinx.android.synthetic.main.activity_settings.settings_main_view_widget
import kotlinx.android.synthetic.main.activity_settings.settings_switch_alt_layout
import kotlinx.android.synthetic.main.activity_settings.settings_switch_autostart
import kotlinx.android.synthetic.main.activity_settings.settings_switch_consumption_unit
import kotlinx.android.synthetic.main.activity_settings.settings_switch_notifications
import kotlinx.android.synthetic.main.activity_settings.settings_switch_phone_reminder
import kotlinx.android.synthetic.main.activity_settings.settings_switch_theme
import kotlinx.android.synthetic.main.activity_settings.settings_switch_theme_container
import kotlinx.android.synthetic.main.activity_settings.settings_switch_use_location
import kotlinx.android.synthetic.main.activity_settings.settings_vehicle_widget
import kotlinx.android.synthetic.main.activity_settings.settings_version_text
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsActivity : FragmentActivity() {

    private lateinit var context : Context
    private val appPreferences = CarStatsViewer.appPreferences

    private var versionClickCounter = 0

    private var moving = false

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        if (intent?.hasExtra("noTransition") == false)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onResume() {
        super.onResume()
        settings_switch_consumption_unit.text = getString(R.string.settings_consumption_unit, appPreferences.distanceUnit.unit())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.dataProcessor.realTimeDataFlow.collectLatest {
                    //setDistractionOptimization((it.speed?:0f) > 0)
                    setDistractionOptimization(false)
                }
            }
        }
        setContentViewAndTheme(this, R.layout.activity_settings)

        setupSettingsMaster()
    }

    private fun setDistractionOptimization(doOptimize: Boolean) {
        if (moving == doOptimize) return
        settings_main_view_widget.isEnabled = !doOptimize
        settings_vehicle_widget.isEnabled = !doOptimize
        settings_apis_widget.isEnabled = !doOptimize
        settings_about_widget.isEnabled = !doOptimize
        moving = doOptimize
    }

    private fun setupSettingsMaster() {
        settings_switch_notifications.isChecked = appPreferences.notifications
        settings_switch_consumption_unit.isChecked = appPreferences.consumptionUnit
        settings_switch_use_location.isChecked = appPreferences.useLocation
        settings_switch_autostart.isChecked = appPreferences.autostart
        settings_switch_phone_reminder.isChecked = appPreferences.phoneNotification
        settings_switch_alt_layout.isChecked = appPreferences.altLayout
        settings_switch_theme.isChecked = when (appPreferences.colorTheme) {
            1 -> true
            else -> false
        }

        settings_version_text.text = "Car Stats Viewer %s (%s)".format(BuildConfig.VERSION_NAME, BuildConfig.APPLICATION_ID)

        settings_button_back.setOnClickListener() {
            finish()
            overridePendingTransition(R.anim.stay_still, R.anim.slide_out_right)
        }

        settings_switch_notifications.setSwitchClickListener {
            appPreferences.notifications = settings_switch_notifications.isChecked
        }

        settings_switch_consumption_unit.setSwitchClickListener {
            appPreferences.consumptionUnit = settings_switch_consumption_unit.isChecked
            PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit, appPreferences.consumptionUnit)
        }

        settings_switch_use_location.setSwitchClickListener {
            appPreferences.useLocation = settings_switch_use_location.isChecked
            CarStatsViewer.watchdog.triggerWatchdog()
        }

        settings_switch_autostart.setSwitchClickListener {
            appPreferences.autostart = settings_switch_autostart.isChecked
            CarStatsViewer.setupRestartAlarm(CarStatsViewer.appContext, "termination", 10_000, !appPreferences.autostart, extendedLogging = true)
        }

        settings_switch_phone_reminder.setSwitchClickListener {
            appPreferences.phoneNotification = settings_switch_phone_reminder.isChecked
        }

        // if (emulatorMode) settings_switch_distance_unit.visibility = View.VISIBLE
        // settings_switch_distance_unit.setOnClickListener {
        //     appPreferences.distanceUnit = when (settings_switch_distance_unit.isChecked) {
        //         true -> DistanceUnitEnum.MILES
        //         else -> DistanceUnitEnum.KM
        //     }
        //     PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)
        // }

        settings_switch_alt_layout.setSwitchClickListener {
            appPreferences.altLayout = settings_switch_alt_layout.isChecked
        }

        settings_main_view_widget.setOnRowClickListener {
            startActivity(Intent(this, SettingsMainViewActivity::class.java))
        }

        settings_vehicle_widget.setOnRowClickListener {
            startActivity(Intent(this, SettingsVehicleActivity::class.java))
        }

        settings_apis_widget.setOnRowClickListener {
            startActivity(Intent(this, SettingsApisActivity::class.java))
        }

        settings_about_widget.setOnRowClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        settings_version_text.setOnClickListener {
            startActivity(Intent(this, DebugActivity::class.java))
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still)
        }

        settings_switch_theme_container.setOnClickListener {
            settings_switch_theme.isChecked = !settings_switch_theme.isChecked
            appPreferences.colorTheme = if (settings_switch_theme.isChecked) 1 else 0
            finish()
        }

        settings_switch_theme.setOnClickListener {
            appPreferences.colorTheme = if (settings_switch_theme.isChecked) 1 else 0
            finish()
        }
    }
}
