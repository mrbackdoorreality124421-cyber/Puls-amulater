package com.example.data.model

enum class DevicePerformanceTier(val title: String, val ratingStars: Int, val badgeColorHex: Long) {
    ENTRY("Entry Mobile", 2, 0xFF78909C),
    MID("Mid-Range Gaming", 3, 0xFF00E5FF),
    HIGH("High Performance", 4, 0xFF2979FF),
    ULTRA_FLAGSHIP("Flagship Elite", 5, 0xFF7C4DFF),
    ENTHUSIAST_EXTREME("Enthusiast Extreme", 5, 0xFFFF007F)
}

data class DeviceCapability(
    val socModel: String,
    val cpuArchitecture: String,
    val cpuCores: Int,
    val cpuMaxFreqGhz: Float,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val totalStorageBytes: Long,
    val freeStorageBytes: Long,
    val gpuRenderer: String,
    val gpuVendor: String,
    val openGlesVersion: String,
    val isVulkanSupported: Boolean,
    val vulkanVersion: String,
    val displayRefreshRateHz: Int,
    val displayResolution: String,
    val androidVersion: String,
    val is64BitSupported: Boolean,
    val thermalLevel: Int, // 0 = NONE, 1 = LIGHT, 2 = MODERATE, 3 = SEVERE, 4 = CRITICAL
    val thermalStatusString: String,
    val performanceScore: Int, // 0 to 100
    val performanceTier: DevicePerformanceTier
) {
    val totalRamGb: Float get() = totalRamBytes / (1024f * 1024f * 1024f)
    val availableRamGb: Float get() = availableRamBytes / (1024f * 1024f * 1024f)
    val freeStorageGb: Float get() = freeStorageBytes / (1024f * 1024f * 1024f)
}
