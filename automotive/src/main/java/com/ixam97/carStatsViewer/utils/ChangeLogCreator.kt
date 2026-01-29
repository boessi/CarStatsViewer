package com.ixam97.carStatsViewer.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R

object ChangeLogCreator {

    private val versionRegex = Regex("\\[V]\\((.*)\\)")

    fun createChangelog(context: Context): Map<String, String> {

        val changesList = context.resources.getStringArray(R.array.changes).toMutableList()
        val pendingChangesList = context.resources.getStringArray(R.array.pending_changes).toMutableList()

        pendingChangesList.toList().forEachIndexed { index, change ->
            if (change.contains(versionRegex)) {
                val match = versionRegex.find(change)!!.destructured.toList()[0]
                pendingChangesList[index] = "[V]($match (Pre-Release))"
            }
        }

        val versionsMap = mutableMapOf<String, String>()

        var currentTitle = ""
        var currentChanges = ""

        changesList.addAll(pendingChangesList)

        versionsMap["System"] = getSystemInfo()

        changesList.reversed().forEachIndexed { index, change ->
            if (change.contains(versionRegex)) {
                if (index > 0) {
                    versionsMap[currentTitle] = currentChanges
                }
                versionRegex.find(change)?.let {
                    val destructedChange = it.destructured.toList()
                    currentTitle = if (destructedChange.isNotEmpty()) it.destructured.toList()[0] else "UNKNOWN"
                }
                currentChanges = ""
            } else {
                if (currentChanges.isNotEmpty()) currentChanges += "\n"
                currentChanges += "●  $change"
                if (index >= changesList.size -1) {
                    versionsMap[currentTitle] = currentChanges
                }
            }
        }

        return versionsMap
    }

    private fun String.toPreReleaseVersion(): String {
        val original = this

        var preReleaseVersion: String = ""

        return preReleaseVersion
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
        val actManager = CarStatsViewer.appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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

        // Graphics
        val configInfo = actManager.deviceConfigurationInfo
        sb.append("GLES Ver: ${configInfo.getGlEsVersion()}\n")


        return sb.toString()
    }
}