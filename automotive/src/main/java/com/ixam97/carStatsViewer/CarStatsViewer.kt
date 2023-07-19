package com.ixam97.carStatsViewer

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.fonts.SystemFonts
import android.util.TypedValue
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.database.log.LogDao
import com.ixam97.carStatsViewer.database.log.LogDatabase
import com.ixam97.carStatsViewer.database.tripData.*
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.liveDataApi.http.HttpLiveData
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.Watchdog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.system.exitProcess

var emulatorMode = false
var emulatorPowerSign = -1

object Defines {
    const val PLOT_ENERGY_INTERVAL = 100L
    const val AUTO_RESET_TIME = 14_400_000L // 4h
    const val PLOT_DISTANCE_INTERVAL = 100.0
}

class CarStatsViewer : Application() {

    companion object {
        const val RESTART_CHANNEL_ID = "RestartChannel"
        const val RESTART_NOTIFICATION_ID = 1
        const val FOREGROUND_CHANNEL_ID = "ForegroundChannel"
        const val FOREGROUND_NOTIFICATION_ID = 2

        var screenshotBitmap: Bitmap? = null

        lateinit var appContext: Context
        lateinit var liveDataApis: ArrayList<LiveDataApi>
        lateinit var appPreferences: AppPreferences
        lateinit var notificationManager: NotificationManager
        var primaryColor by Delegates.notNull<Int>()
        var disabledAlpha by Delegates.notNull<Float>()

        var foregroundServiceStarted = false
        var restartNotificationDismissed = false
        var restartReason: String? = null

        lateinit var tripDatabase: TripDataDatabase
        lateinit var tripDataSource: LocalTripDataSource
        lateinit var dataProcessor: DataProcessor
        lateinit var watchdog: Watchdog

        lateinit var logDao: LogDao

        var typefaceRegular: Typeface? = null
        var typefaceMedium: Typeface? = null
        var isPolestarTypeface = false

        val appContextIsInitialized: Boolean get() = this::appContext.isInitialized

        fun setupRestartAlarm(context: Context, reason: String, delay: Long, cancel: Boolean = false, extendedLogging: Boolean = false) {
            val serviceIntent = Intent(context, AutoStartReceiver::class.java)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            serviceIntent.action = "com.ixam97.carStatsViewer.RestartAction"
            serviceIntent.putExtra("reason", reason)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
            if (cancel) return
            if (delay < 10_000) {
                alarmManager.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + delay,
                    pendingIntent
                )
                InAppLogger.i("[ASR] Setup single shot alarm")
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + delay,
                    delay,
                    pendingIntent
                )
                if (extendedLogging) InAppLogger.i("[ASR] Setup repeating alarm")
            }
        }
    }



    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
        appPreferences = AppPreferences(applicationContext)
        watchdog = Watchdog()

        val logDatabase = Room.databaseBuilder(
            applicationContext,
            LogDatabase::class.java,
            "LogDatabase"
        ).build()
        logDao = logDatabase.logDao()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->

            try {
                setupRestartAlarm(applicationContext, "crash", 2_000, extendedLogging = true)
                InAppLogger.i("setup crash alarm")
            } catch (e: Exception) {
                InAppLogger.e(e.stackTraceToString())
            }

            InAppLogger.e("[NEO] Car Stats Viewer has crashed!\n ${e.stackTraceToString()}")

            exitProcess(0)
        }

        val MIGRATION_5_6 = object: Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE DrivingSession ADD COLUMN last_edited_epoch_time INTEGER NOT NULL DEFAULT 0")
            }
        }

        tripDatabase = Room.databaseBuilder(
            applicationContext,
            TripDataDatabase::class.java,
            "TripDatabase"
        )
            //.fallbackToDestructiveMigration()
            .addMigrations(MIGRATION_5_6)
            .build()

        tripDataSource = LocalTripDataSource(tripDatabase.tripDao())

        CoroutineScope(Dispatchers.IO).launch {

            // tripDatabase.clearAllTables()

            val drivingSessionIds = tripDataSource.getActiveDrivingSessionsIdsMap()
            InAppLogger.d("Trip Database: $drivingSessionIds")
        }

        dataProcessor = DataProcessor()

        val typedValue = TypedValue()
        applicationContext.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        primaryColor = typedValue.data

        applicationContext.theme.resolveAttribute(android.R.attr.disabledAlpha, typedValue, true)
        disabledAlpha = typedValue.float

        // StrictMode.setVmPolicy(
        //     VmPolicy.Builder(StrictMode.getVmPolicy())
        //         .detectLeakedClosableObjects()
        //         .build()
        // )

        InAppLogger.i("${appContext.getString(R.string.app_name)} v${BuildConfig.VERSION_NAME} started")

        val abrpApiKey = if (resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName) != 0) {
            getString(resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName))
        } else ""

        liveDataApis = arrayListOf(
            AbrpLiveData(abrpApiKey),
            HttpLiveData()
        )

        notificationManager = createNotificationManager()

        InAppLogger.i("Available OEM fonts:")
        SystemFonts.getAvailableFonts().filter{ it.file?.name?.contains("volvo", true) == true }.forEach {
            InAppLogger.i("    ${it.file?.name}")
            when {
                it.file?.name?.contains("light", true) == true -> typefaceRegular = Typeface.Builder(it.file!!).build()
                it.file?.name?.contains("medium", true) == true -> typefaceMedium = Typeface.Builder(it.file!!).build()
            }
        }
        SystemFonts.getAvailableFonts().filter{ it.file?.name?.contains("polestar", true) == true }.forEach {
            InAppLogger.i("    ${it.file?.name}")
            isPolestarTypeface = true
            when {
                it.file?.name?.contains("regular", true) == true -> typefaceRegular = Typeface.Builder(it.file!!).build()
                it.file?.name?.contains("medium", true) == true -> typefaceMedium = Typeface.Builder(it.file!!).build()
            }
        }

        SystemFonts.getAvailableFonts().filter { it.file?.name?.contains("honda", true) == true }.forEach {
            InAppLogger.i("    ${it.file?.name}")
            when {
                it.file?.name?.contains("regular", true) == true -> {
                    typefaceRegular = Typeface.Builder(it.file!!).build()
                    typefaceMedium = Typeface.Builder(it.file!!).build()
                }
            }
        }

    }

    private fun createNotificationManager(): NotificationManager {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val restartChannel = NotificationChannel(
            RESTART_CHANNEL_ID,
            RESTART_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = RESTART_CHANNEL_ID
        }

        val foregroundChannel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            FOREGROUND_CHANNEL_ID,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = FOREGROUND_CHANNEL_ID
        }

        notificationManager.createNotificationChannels(listOf(
            restartChannel,
            foregroundChannel
        ))
        return notificationManager
    }
}