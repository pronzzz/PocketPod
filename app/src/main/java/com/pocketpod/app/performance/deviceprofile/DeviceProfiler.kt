package com.pocketpod.app.performance.deviceprofile

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

class DeviceProfiler(private val context: Context) {

    fun getPerformanceProfile(): DevicePerformanceProfile {
        val ramMb = getTotalRamMb()
        val cores = Runtime.getRuntime().availableProcessors()
        
        // Samsung Galaxy J2 Pro (2018) specs typically: 1.5GB to 2GB RAM, 4 cores
        // Let's define LOW as <= 2GB RAM or <= 4 cores
        return when {
            ramMb <= 2048L || cores <= 4 -> DevicePerformanceProfile.LOW
            ramMb <= 4096L -> DevicePerformanceProfile.STANDARD
            else -> DevicePerformanceProfile.HIGH
        }
    }

    fun getTotalRamMb(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }

    fun getAvailableStorageMb(): Long {
        val path = context.filesDir
        return path.usableSpace / (1024 * 1024)
    }
    
    fun getBatteryPercentage(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }
    }
    
    fun getDisplayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        return metrics
    }
    
    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE
    }
    
    fun getCpuCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }
}
