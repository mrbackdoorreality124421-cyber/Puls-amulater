package com.example.engine

import com.example.data.model.DeviceCapability
import com.example.data.model.GameConfig
import com.example.data.model.GameLoadAnalysis
import com.example.data.model.PerformanceProfile
import com.example.engine.performance.GamePerformanceProfile
import com.example.engine.performance.PerformanceDecisionEngine
import java.io.File

data class OptimizationRecommendation(
    val recommendedProfile: PerformanceProfile,
    val generatedConfig: GameConfig,
    val rationale: String,
    val deviceCapabilitySummary: String,
    val gameLoadSummary: String,
    val performanceProfile: GamePerformanceProfile? = null
)

object OptimizationEngine {

    fun generateRecommendation(
        gameAnalysis: GameLoadAnalysis,
        deviceCapability: DeviceCapability
    ): OptimizationRecommendation {
        val gameId = gameAnalysis.gameTitle.lowercase().replace(" ", "_")
        val perfProfile = PerformanceDecisionEngine.buildGamePerformanceProfile(
            gameId = gameId,
            device = deviceCapability,
            analysis = gameAnalysis
        )

        val config = PerformanceDecisionEngine.convertToGameConfig(perfProfile)

        val rationale = "Dynamically calculated for ${deviceCapability.socModel} & ${gameAnalysis.gameTitle} (${gameAnalysis.detectedGraphicsApi}). Target: ${perfProfile.fpsTarget} FPS with ${perfProfile.runtimeProfile.renderingBackend} (${perfProfile.resolutionProfile.renderScalePercent}% resolution scale)."
        val devSummary = "${deviceCapability.socModel} (${deviceCapability.cpuCores} Cores, ${deviceCapability.gpuRenderer}, ${deviceCapability.performanceTier.name})"
        val gameSummary = "${gameAnalysis.gameTitle} [${gameAnalysis.detectedGraphicsApi}, ${gameAnalysis.detectedArchitecture}, ${gameAnalysis.loadScore.name} Load]"

        return OptimizationRecommendation(
            recommendedProfile = perfProfile.recommendedProfile,
            generatedConfig = config,
            rationale = rationale,
            deviceCapabilitySummary = devSummary,
            gameLoadSummary = gameSummary,
            performanceProfile = perfProfile
        )
    }

    fun applyProfileToConfig(baseConfig: GameConfig, profile: PerformanceProfile): GameConfig {
        return when (profile) {
            PerformanceProfile.SUPER_SMOOTH -> baseConfig.copy(
                resolutionScale = 0.50f,
                fpsLimit = 30,
                vsync = false,
                asyncShaderCompile = true,
                textureFilterMode = "Bilinear",
                shadowQuality = "Off"
            )
            PerformanceProfile.SMOOTH -> baseConfig.copy(
                resolutionScale = 0.65f,
                fpsLimit = 45,
                vsync = false,
                asyncShaderCompile = true,
                textureFilterMode = "Bilinear",
                shadowQuality = "Low"
            )
            PerformanceProfile.BALANCE -> baseConfig.copy(
                resolutionScale = 0.75f,
                fpsLimit = 60,
                vsync = false,
                asyncShaderCompile = true,
                textureFilterMode = "Trilinear",
                shadowQuality = "Medium"
            )
            PerformanceProfile.HIGH -> baseConfig.copy(
                resolutionScale = 0.85f,
                fpsLimit = 60,
                vsync = true,
                asyncShaderCompile = true,
                textureFilterMode = "Anisotropic 4x",
                shadowQuality = "High"
            )
            PerformanceProfile.ULTRA -> baseConfig.copy(
                resolutionScale = 1.0f,
                fpsLimit = 90,
                vsync = true,
                asyncShaderCompile = true,
                textureFilterMode = "Anisotropic 8x",
                shadowQuality = "Ultra"
            )
            PerformanceProfile.EXTREME -> baseConfig.copy(
                resolutionScale = 1.0f,
                fpsLimit = 120,
                vsync = true,
                asyncShaderCompile = true,
                textureFilterMode = "Anisotropic 16x",
                shadowQuality = "Ultra"
            )
            PerformanceProfile.SUPER_EXTREME -> baseConfig.copy(
                resolutionScale = 1.0f,
                fpsLimit = 144,
                vsync = false,
                asyncShaderCompile = true,
                textureFilterMode = "Anisotropic 16x",
                shadowQuality = "Ultra"
            )
        }
    }

    fun writeDxvkConfiguration(profileDir: File, config: GameConfig) {
        try {
            if (!profileDir.exists()) profileDir.mkdirs()
            val confFile = File(profileDir, "dxvk.conf")
            val content = buildString {
                appendLine("# PulsePC Dynamic DXVK Optimization Profile")
                appendLine("dxvk.enableAsync = ${config.asyncShaderCompile}")
                appendLine("dxvk.numCompilerThreads = 6")
                appendLine("dxvk.maxChunkSize = 256")
                appendLine("d3d11.maxTessFactor = 8")
                appendLine("d3d9.maxAvailableMemory = 4096")
                appendLine("dxgi.syncInterval = ${if (config.vsync) 1 else 0}")
                appendLine("dxgi.maxFrameRate = ${config.fpsLimit}")
            }
            confFile.writeText(content)
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
