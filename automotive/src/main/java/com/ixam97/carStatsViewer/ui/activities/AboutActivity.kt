package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import android.os.Build
import android.content.Context
import android.app.ActivityManager
import com.ixam97.carStatsViewer.databinding.ActivityAboutBinding

class AboutActivity : FragmentActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)



        binding.aboutButtonBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.aboutVersionWidget.setOnRowClickListener {
            CarStatsViewer.getChangelogDialog(this, isChangelog = true).show()
        }
        binding.aboutSupportWidget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.readme_link))))
        }

        binding.aboutForumsWidget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.polestar_forum_link))))
        }

        binding.aboutClubWidget.setOnRowClickListener() {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.polestar_fans_link))))
        }

        binding.aboutGithubIssuesWidget.setOnRowClickListener() {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_issues_link))))
        }

        binding.aboutLibsWidget.setOnRowClickListener {
            startActivity(Intent(this, LibsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.aboutPrivacyWidget.setOnRowClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_link))))
        }

        binding.aboutVersionWidget.bottomText = "%s (%s)".format(BuildConfig.VERSION_NAME, BuildConfig.APPLICATION_ID)

        var contributors = ""
        val contributorsArray = resources.getStringArray(R.array.contributors)
        for ((index, contributor) in contributorsArray.withIndex()) {
            contributors += contributor
            if (index < contributorsArray.size -1) contributors += ", "
        }
        binding.aboutContributorsWidget.bottomText = contributors

        binding.aboutTranslatorsWidget.setOnRowClickListener {
            val translatorsDialog = AlertDialog.Builder(this).apply {
                setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                    dialog.cancel()
                }
                setTitle(getString(R.string.about_translators))
                val translatorsArray = resources.getStringArray(R.array.translators)
                var translators = ""
                for ((index, translator) in translatorsArray.withIndex()) {
                    translators += translator
                    if (index < translatorsArray.size - 1) translators += ", "
                }
                setMessage(translators)
                setCancelable(true)
                create()
            }
            translatorsDialog.show()
        }

        binding.aboutSupportersWidget.setOnRowClickListener {
            val supportersDialog = AlertDialog.Builder(this).apply {
                setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                    dialog.cancel()
                }
                setTitle(getString(R.string.about_thank_you))
                val supportersArray = resources.getStringArray(R.array.supporters)
                var supporters = getString(R.string.about_supporters_message) + "\n\n"
                for ((index, supporter) in supportersArray.withIndex()) {
                    supporters += supporter
                    if (index < supportersArray.size - 1) supporters += ", "
                }
                setMessage(supporters)
                setCancelable(true)
                create()
            }
            supportersDialog.show()
        }

        binding.aboutSystemInfoWidget.setOnRowClickListener {
            showSystemInfoDialog()
        }
    }

    private fun showSystemInfoDialog() {
        val systemInfo = getSystemInfo()
        AlertDialog.Builder(this).apply {
            setPositiveButton(getString(R.string.dialog_close)) { dialog, _ ->
                dialog.cancel()
            }
            setTitle(getString(R.string.about_system_info_title))
            setMessage(systemInfo)
            setCancelable(true)
            create()
        }.show()
    }

    private fun getSystemInfo(): String {
        val sb = StringBuilder()
        
        // Software
        sb.append("--- SOFTWARE ---\n")
        sb.append("Android Ver: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        sb.append("Build ID: ${Build.ID}\n")
        sb.append("Incremental: ${Build.VERSION.INCREMENTAL}\n")
        sb.append("Codename: ${Build.VERSION.CODENAME}\n")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sb.append("Sec. Patch: ${Build.VERSION.SECURITY_PATCH}\n")
        }
        sb.append("Tags: ${Build.TAGS}\n")
        sb.append("Type: ${Build.TYPE}\n")
        // Hardware
        sb.append("\n--- HARDWARE ---\n")
        sb.append("Manufacturer: ${Build.MANUFACTURER}\n")
        sb.append("Brand: ${Build.BRAND}\n")
        sb.append("Model: ${Build.MODEL}\n")
        sb.append("Product: ${Build.PRODUCT}\n")
        sb.append("Device: ${Build.DEVICE}\n")
        sb.append("Board: ${Build.BOARD}\n")
        sb.append("Hardware: ${Build.HARDWARE}\n")
        sb.append("Architecture: ${System.getProperty("os.arch")}\n")
        
        // Memory
        sb.append("\n--- MEMORY ---\n")
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalMem = memInfo.totalMem / (1024 * 1024)
        val availMem = memInfo.availMem / (1024 * 1024)
        sb.append("RAM: ${availMem}MB free / ${totalMem}MB total\n")

        // Storage
        val dataPath = android.os.Environment.getDataDirectory()
        val stat = android.os.StatFs(dataPath.path)
        val blockSize = stat.blockSizeLong
        val totalStorage = (stat.blockCountLong * blockSize) / (1024 * 1024)
        val freeStorage = (stat.availableBlocksLong * blockSize) / (1024 * 1024)
        sb.append("Int. Storage: ${freeStorage}MB free / ${totalStorage}MB total\n")
        
        // Display
        sb.append("\n--- DISPLAY ---\n")
        val dm = resources.displayMetrics
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val display = windowManager.defaultDisplay
        sb.append("Resolution: ${dm.widthPixels}x${dm.heightPixels}\n")
        sb.append("Density: ${dm.densityDpi} dpi\n")
        sb.append("Refresh Rate: ${display.refreshRate} Hz\n")

        // Graphics
        val configInfo = actManager.deviceConfigurationInfo
        sb.append("GLES Ver: ${configInfo.getGlEsVersion()}\n")

        if (packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUTOMOTIVE)) {
            sb.append("\nSystem Feature: Automotive\n")
        }

        return sb.toString()
    }
}