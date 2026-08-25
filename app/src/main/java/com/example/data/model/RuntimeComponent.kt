package com.example.data.model

enum class ComponentCategory(val title: String, val icon: String) {
    WINDOWS_COMPATIBILITY("Windows Compatibility", "wine"),
    CPU_TRANSLATION("CPU Translation Layer", "cpu"),
    GRAPHICS_TRANSLATION("Graphics Translation (Vulkan)", "gpu"),
    GRAPHICS_DEVICE_SUPPORT("GPU & Driver Infrastructure", "driver"),
    INPUT_LAYER("Input & Controller Bridge", "gamepad"),
    RUNTIME_LIBRARIES("Runtime C++ & DirectShow", "library"),
    SHADER_INFRASTRUCTURE("Async Shader Pipeline", "shader"),
    FONTS_DATA("Fonts & System Locale Data", "font")
}

enum class ComponentScope {
    CORE,           // Normally required for standard PC games on this device
    OPTIONAL,       // DirectX 12 / 32-bit legacy or specialized enhancement
    GAME_SPECIFIC   // Required only when imported game explicitly needs it
}

enum class DownloadStatus {
    NOT_INSTALLED,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    INSTALLING,
    INSTALLED,
    FAILED
}

data class ComponentErrorInfo(
    val componentId: String,
    val componentName: String,
    val reason: String,
    val isOptional: Boolean = false
)

data class RuntimeComponent(
    val id: String,
    val name: String,
    val version: String,
    val category: ComponentCategory,
    val scope: ComponentScope,
    val sizeBytes: Long,
    val formattedSize: String,
    val sha256Checksum: String,
    val targetSubdir: String,
    val description: String,
    val dependencies: List<String> = emptyList(),
    val requiredForArch: List<String> = listOf("arm64-v8a", "universal"),
    val minRamGb: Float = 3.0f,
    val requiresVulkan: Boolean = false,
    val requiresVulkan13: Boolean = false,
    val gpuSpecificVendor: String? = null // e.g. "Qualcomm", "ARM", null for universal
)

data class ComponentDownloadItem(
    val component: RuntimeComponent,
    val status: DownloadStatus = DownloadStatus.NOT_INSTALLED,
    val bytesDownloaded: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val isSelectedForDownload: Boolean = true
)

data class GamingEnvironmentState(
    val isEnvironmentReady: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val installedComponentIds: Set<String> = emptySet(),
    val activeDownloads: List<ComponentDownloadItem> = emptyList(),
    val isDownloadingInProgress: Boolean = false,
    val isPaused: Boolean = false,
    val totalBytesToDownload: Long = 0L,
    val totalBytesDownloaded: Long = 0L,
    val overallProgress: Float = 0f,
    val overallSpeedBytesPerSec: Long = 0L,
    val overallEtaSeconds: Long = 0L,
    val activeComponentName: String? = null,
    val currentStepText: String? = null,
    val failedComponentError: ComponentErrorInfo? = null,
    val updateAvailable: Boolean = false,
    val latestEnvironmentVersion: String = "2.4.0",
    val installedEnvironmentVersion: String? = null
)
