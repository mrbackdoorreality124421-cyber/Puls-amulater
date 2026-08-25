package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.example.data.model.DeviceCapability
import com.example.data.model.DevicePerformanceTier
import java.io.File
import java.io.RandomAccessFile

object DeviceAnalyzer {

    private const val TAG = "PulsePC_DeviceAnalyzer"

    fun analyzeDevice(context: Context): DeviceCapability {
        return try {
            val totalRamBytes = queryTotalRamBytes(context)
            val availRamBytes = queryAvailableRamBytes(context)
            val cpuArch = queryCpuArchitecture()
            val cpuCores = queryCpuCores()
            val maxFreq = queryMaxCpuFrequencyGhz()
            val (gpuVendor, gpuRenderer, glesVer) = queryGpuInfo(context)
            val isVulkan = queryVulkanSupport(context)
            val vulkanVer = if (isVulkan) queryVulkanVersion(context) else "Not Supported"
            val (freeStorage, totalStorage) = queryStorageBytes(context)
            val (thermalLevel, thermalStatusString) = queryThermalStatus(context)
            val refreshRate = queryRefreshRate(context)
            val resolution = queryDisplayResolution(context)
            val soc = querySocModel()
            val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

            val score = calculatePerformanceScore(
                totalRamBytes = totalRamBytes,
                cpuCores = cpuCores,
                cpuMaxFreqGhz = maxFreq,
                isVulkanSupported = isVulkan,
                gpuRenderer = gpuRenderer,
                socModel = soc
            )

            val tier = when {
                score >= 88 -> DevicePerformanceTier.ENTHUSIAST_EXTREME
                score >= 75 -> DevicePerformanceTier.ULTRA_FLAGSHIP
                score >= 60 -> DevicePerformanceTier.HIGH
                score >= 42 -> DevicePerformanceTier.MID
                else -> DevicePerformanceTier.ENTRY
            }

            CrashLogger.setSubsystemStatus("DeviceAnalyzer", SubsystemStatus.HEALTHY)

            DeviceCapability(
                socModel = soc,
                cpuArchitecture = cpuArch,
                cpuCores = cpuCores,
                cpuMaxFreqGhz = maxFreq,
                totalRamBytes = totalRamBytes,
                availableRamBytes = availRamBytes,
                totalStorageBytes = totalStorage,
                freeStorageBytes = freeStorage,
                gpuRenderer = gpuRenderer,
                gpuVendor = gpuVendor,
                openGlesVersion = glesVer,
                isVulkanSupported = isVulkan,
                vulkanVersion = vulkanVer,
                displayRefreshRateHz = refreshRate,
                displayResolution = resolution,
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                is64BitSupported = is64Bit,
                thermalLevel = thermalLevel,
                thermalStatusString = thermalStatusString,
                performanceScore = score,
                performanceTier = tier
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Exception during analyzeDevice, falling back to safe defaults", t)
            CrashLogger.recordCrash(context, t, "DeviceAnalyzer", isFatal = false)
            createSafeDefaultCapability()
        }
    }

    fun createSafeDefaultCapability(): DeviceCapability {
        return DeviceCapability(
            socModel = "Universal ARM64 Platform",
            cpuArchitecture = "arm64-v8a",
            cpuCores = 8,
            cpuMaxFreqGhz = 2.4f,
            totalRamBytes = 6L * 1024 * 1024 * 1024,
            availableRamBytes = 3L * 1024 * 1024 * 1024,
            totalStorageBytes = 64L * 1024 * 1024 * 1024,
            freeStorageBytes = 20L * 1024 * 1024 * 1024,
            gpuRenderer = "Adreno / Mali Graphics",
            gpuVendor = "Qualcomm / ARM",
            openGlesVersion = "3.2",
            isVulkanSupported = true,
            vulkanVersion = "Vulkan 1.2",
            displayRefreshRateHz = 60,
            displayResolution = "1080 x 2400",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            is64BitSupported = true,
            thermalLevel = 0,
            thermalStatusString = "Nominal",
            performanceScore = 65,
            performanceTier = DevicePerformanceTier.MID
        )
    }

    private fun queryTotalRamBytes(context: Context): Long {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            if (actManager != null) {
                actManager.getMemoryInfo(memInfo)
                if (memInfo.totalMem > 0) memInfo.totalMem else 6L * 1024 * 1024 * 1024
            } else {
                6L * 1024 * 1024 * 1024
            }
        } catch (e: Throwable) {
            6L * 1024 * 1024 * 1024
        }
    }

    private fun queryAvailableRamBytes(context: Context): Long {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            if (actManager != null) {
                actManager.getMemoryInfo(memInfo)
                if (memInfo.availMem > 0) memInfo.availMem else 3L * 1024 * 1024 * 1024
            } else {
                3L * 1024 * 1024 * 1024
            }
        } catch (e: Throwable) {
            3L * 1024 * 1024 * 1024
        }
    }

    private fun queryCpuArchitecture(): String {
        return try {
            if (Build.SUPPORTED_ABIS.isNotEmpty()) {
                Build.SUPPORTED_ABIS[0]
            } else {
                @Suppress("DEPRECATION")
                Build.CPU_ABI ?: "arm64-v8a"
            }
        } catch (e: Throwable) {
            "arm64-v8a"
        }
    }

    private fun queryCpuCores(): Int {
        return try {
            Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        } catch (e: Throwable) {
            8
        }
    }

    private fun queryMaxCpuFrequencyGhz(): Float {
        return try {
            val freqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (freqFile.exists() && freqFile.canRead()) {
                val reader = RandomAccessFile(freqFile, "r")
                val line = reader.readLine()
                reader.close()
                val khz = line?.trim()?.toLongOrNull() ?: 2400000L
                (khz / 1_000_000.0f).coerceIn(1.0f, 4.5f)
            } else {
                2.4f
            }
        } catch (e: Throwable) {
            2.4f
        }
    }

    private fun queryGpuInfo(context: Context): Triple<String, String, String> {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val confInfo = actManager?.deviceConfigurationInfo
            val glesVersion = confInfo?.glEsVersion ?: "3.2"

            val hardware = Build.HARDWARE.lowercase()
            val board = Build.BOARD.lowercase()

            val (vendor, renderer) = when {
                hardware.contains("qcom") || board.contains("qcom") || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MANUFACTURER.contains("Qualcomm", ignoreCase = true)) -> {
                    Pair("Qualcomm Technologies", "Adreno (TM) High Performance GPU")
                }
                hardware.contains("exynos") || hardware.contains("mali") || board.contains("mali") -> {
                    Pair("ARM", "Mali-G Series Multi-Core GPU")
                }
                hardware.contains("mt") || hardware.contains("dimensity") -> {
                    Pair("MediaTek / ARM", "Mali / Immortalis GPU")
                }
                hardware.contains("bcm") || hardware.contains("kirin") -> {
                    Pair("HiSilicon", "Mali GPU")
                }
                else -> {
                    Pair("Standard Vendor", "DirectX-to-Vulkan Emulation GPU")
                }
            }

            Triple(vendor, renderer, glesVersion)
        } catch (e: Throwable) {
            Triple("Qualcomm Technologies", "Adreno (TM) Graphics", "3.2")
        }
    }

    private fun queryVulkanSupport(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) ||
                        pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
            } else {
                false
            }
        } catch (e: Throwable) {
            true
        }
    }

    private fun queryVulkanVersion(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val pm = context.packageManager
                val has13 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x403000)
                } else false
                if (has13) "Vulkan 1.3 (VKD3D / DXVK Full)" else "Vulkan 1.1 / 1.2"
            } else {
                "Vulkan 1.0"
            }
        } catch (e: Throwable) {
            "Vulkan 1.2"
        }
    }

    private fun queryStorageBytes(context: Context): Pair<Long, Long> {
        return try {
            val internalDir = context.filesDir ?: Environment.getDataDirectory()
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            val stat = StatFs(internalDir.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong

            val freeBytes = blockSize * availableBlocks
            val totalBytes = blockSize * totalBlocks
            Pair(freeBytes, totalBytes)
        } catch (e: Throwable) {
            Pair(20L * 1024 * 1024 * 1024, 64L * 1024 * 1024 * 1024)
        }
    }

    private fun queryThermalStatus(context: Context): Pair<Int, String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val level = pm?.currentThermalStatus ?: 0
                val str = when (level) {
                    PowerManager.THERMAL_STATUS_NONE -> "Cool (Nominal)"
                    PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
                    PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Warmth"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Throttling Active"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Heat"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Cooldown"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown"
                    else -> "Nominal"
                }
                Pair(level, str)
            } else {
                Pair(0, "Nominal")
            }
        } catch (e: Throwable) {
            Pair(0, "Nominal")
        }
    }

    private fun queryRefreshRate(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
                display?.refreshRate?.toInt() ?: 60
            } else {
                @Suppress("DEPRECATION")
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                wm?.defaultDisplay?.refreshRate?.toInt() ?: 60
            }
        } catch (e: Throwable) {
            60
        }
    }

    private fun queryDisplayResolution(context: Context): String {
        return try {
            val metrics = context.resources.displayMetrics
            "${metrics.widthPixels} x ${metrics.heightPixels}"
        } catch (e: Throwable) {
            "1080 x 2400"
        }
    }

    private fun querySocModel(): String {
        return try {
            val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MODEL
            } else {
                Build.HARDWARE
            }
            if (soc.isNotBlank() && soc != "unknown") {
                "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} $soc"
            } else {
                "${Build.MANUFACTURER} ${Build.MODEL}"
            }
        } catch (e: Throwable) {
            "ARM64 Universal SoC"
        }
    }

    private fun calculatePerformanceScore(
        totalRamBytes: Long,
        cpuCores: Int,
        cpuMaxFreqGhz: Float,
        isVulkanSupported: Boolean,
        gpuRenderer: String,
        socModel: String
    ): Int {
        var score = 0
        val totalRamGb = totalRamBytes / (1024.0 * 1024.0 * 1024.0)

        // RAM scoring (up to 30 points)
        score += when {
            totalRamGb >= 12.0 -> 30
            totalRamGb >= 8.0 -> 26
            totalRamGb >= 6.0 -> 20
            totalRamGb >= 4.0 -> 12
            else -> 6
        }

        // CPU scoring (up to 30 points)
        val corePoints = (cpuCores * 2).coerceAtMost(16)
        val freqPoints = (cpuMaxFreqGhz * 6).toInt().coerceAtMost(14)
        score += (corePoints + freqPoints)

        // GPU / Vulkan scoring (up to 30 points)
        if (isVulkanSupported) score += 18
        if (gpuRenderer.contains("Adreno", ignoreCase = true) || gpuRenderer.contains("Mali-G7", ignoreCase = true) || gpuRenderer.contains("Immortalis", ignoreCase = true)) {
            score += 12
        } else {
            score += 6
        }

        // SoC flagship bonus (up to 10 points)
        val lowerSoc = socModel.lowercase()
        if (lowerSoc.contains("8 gen") || lowerSoc.contains("888") || lowerSoc.contains("865") || lowerSoc.contains("dimensity 9") || lowerSoc.contains("tensor g")) {
            score += 10
        } else if (lowerSoc.contains("7 gen") || lowerSoc.contains("778") || lowerSoc.contains("dimensity 8")) {
            score += 6
        } else {
            score += 3
        }

        return score.coerceIn(10, 99)
    }
}
