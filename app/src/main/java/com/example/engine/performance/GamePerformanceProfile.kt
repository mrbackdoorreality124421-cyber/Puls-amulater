package com.example.engine.performance

import com.example.data.model.DeviceCapability
import com.example.data.model.PerformanceProfile

data class DevicePerformanceSnapshot(
    val socModel: String,
    val cpuArch: String,
    val cpuCores: Int,
    val gpuRenderer: String,
    val totalRamMb: Int,
    val availableRamMb: Int,
    val isVulkanSupported: Boolean,
    val vulkanVersion: String,
    val thermalStatus: String,
    val performanceTier: String
)

data class RuntimeProfileConfig(
    val wineVersion: String = "Wine 9.2 Staging",
    val boxTranslator: String = "Box64 v0.2.8 Dynarec",
    val graphicsBridge: String = "DXVK 2.3.1 Async",
    val renderingBackend: String = "Vulkan Direct",
    val winArch: String = "Win64",
    val heapLimitMb: Int = 4096
)

data class GraphicsProfileConfig(
    val textureQuality: String = "Medium", // Low, Medium, High, Ultra
    val shadowQuality: String = "Low", // Off, Low, Medium, High, Ultra
    val effectsQuality: String = "Medium", // Low, Medium, High, Ultra
    val antiAliasing: String = "FXAA", // None, FXAA, SMAA, TAA
    val anisotropicFiltering: String = "4x", // Bilinear, 2x, 4x, 8x, 16x
    val postProcessing: Boolean = true
)

data class ResolutionProfileConfig(
    val renderScalePercent: Int = 75, // 50% to 120%
    val internalResolution: String = "1440x810",
    val displayMode: String = "Fullscreen Stretch",
    val frameRateLimiter: Int = 60
)

data class ShaderProfileConfig(
    val shaderCacheEnabled: Boolean = true,
    val asyncShaderCompile: Boolean = true,
    val compilerThreads: Int = 4,
    val spirvOptimization: Boolean = true,
    val maxTessellationFactor: Int = 8
)

data class CpuProfileConfig(
    val cpuAffinity: String = "All Performance Cores",
    val dynarecBigBlock: Boolean = true,
    val dynarecFastRound: Boolean = true,
    val dynarecStrongMem: Boolean = false,
    val threadConcurrency: Int = 6
)

data class MemoryProfileConfig(
    val heapLimitMb: Int = 3072,
    val aggressiveGarbageCollection: Boolean = false,
    val textureCompressionEnabled: Boolean = true,
    val swapFileMb: Int = 1024
)

data class ThermalProfileConfig(
    val thermalProtectionEnabled: Boolean = true,
    val aggressiveFramePacingOnThrottle: Boolean = true,
    val autoReduceResolutionOnOverheat: Boolean = true,
    val targetMaxTempCelsius: Float = 42.0f
)

data class GamePerformanceProfile(
    val gameId: String,
    val deviceProfile: DevicePerformanceSnapshot,
    val runtimeProfile: RuntimeProfileConfig,
    val graphicsProfile: GraphicsProfileConfig,
    val resolutionProfile: ResolutionProfileConfig,
    val shaderProfile: ShaderProfileConfig,
    val cpuProfile: CpuProfileConfig,
    val memoryProfile: MemoryProfileConfig,
    val thermalProfile: ThermalProfileConfig,
    val fpsTarget: Int = 60,
    val framePacing: String = "Optimized Low Latency",
    val optimizationLevel: String = "Dynamic Tailored",
    val recommendedProfile: PerformanceProfile = PerformanceProfile.BALANCE,
    val stabilityScore: Float = 1.0f,
    val lastAdaptedTimestamp: Long = 0L
)
