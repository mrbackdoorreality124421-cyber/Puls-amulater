package com.example.engine.performance

import android.content.Context
import android.util.Log
import com.example.data.model.DeviceCapability
import com.example.data.model.GameConfig
import com.example.data.model.GameLoadAnalysis
import com.example.data.model.PerformanceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class OptimizationRecommendationResult(
    val recommendedProfile: PerformanceProfile,
    val generatedConfig: GameConfig,
    val performanceProfile: GamePerformanceProfile,
    val rationale: String,
    val deviceCapabilitySummary: String,
    val gameLoadSummary: String
)

class PerformanceOptimizationEngine(
    private val context: Context
) {
    private val profileCache = ConcurrentHashMap<String, GamePerformanceProfile>()

    private val _activeProfile = MutableStateFlow<GamePerformanceProfile?>(null)
    val activeProfile: StateFlow<GamePerformanceProfile?> = _activeProfile.asStateFlow()

    fun generateRecommendation(
        gameAnalysis: GameLoadAnalysis,
        deviceCapability: DeviceCapability,
        overrideProfile: PerformanceProfile? = null
    ): OptimizationRecommendationResult {
        val gameId = gameAnalysis.gameTitle.lowercase().replace(" ", "_")
        val perfProfile = PerformanceDecisionEngine.buildGamePerformanceProfile(
            gameId = gameId,
            device = deviceCapability,
            analysis = gameAnalysis,
            selectedProfile = overrideProfile
        )

        profileCache[gameId] = perfProfile
        _activeProfile.value = perfProfile

        val gameConfig = PerformanceDecisionEngine.convertToGameConfig(perfProfile)

        val rationale = buildRationale(perfProfile, deviceCapability, gameAnalysis)
        val devSummary = "${deviceCapability.socModel} (${deviceCapability.cpuCores} cores, ${deviceCapability.gpuRenderer}, ${deviceCapability.performanceTier.name})"
        val gameSummary = "${gameAnalysis.gameTitle} [${gameAnalysis.detectedGraphicsApi}, ${gameAnalysis.detectedArchitecture}, ${gameAnalysis.loadScore.name} Load]"

        return OptimizationRecommendationResult(
            recommendedProfile = perfProfile.recommendedProfile,
            generatedConfig = gameConfig,
            performanceProfile = perfProfile,
            rationale = rationale,
            deviceCapabilitySummary = devSummary,
            gameLoadSummary = gameSummary
        )
    }

    fun getOrGenerateProfile(
        gameId: String,
        deviceCapability: DeviceCapability,
        gameAnalysis: GameLoadAnalysis
    ): GamePerformanceProfile {
        return profileCache.getOrPut(gameId) {
            PerformanceDecisionEngine.buildGamePerformanceProfile(
                gameId = gameId,
                device = deviceCapability,
                analysis = gameAnalysis
            )
        }
    }

    fun updateProfile(gameId: String, updatedProfile: GamePerformanceProfile) {
        profileCache[gameId] = updatedProfile
        if (_activeProfile.value?.gameId == gameId) {
            _activeProfile.value = updatedProfile
        }
    }

    private fun buildRationale(
        profile: GamePerformanceProfile,
        device: DeviceCapability,
        analysis: GameLoadAnalysis
    ): String {
        return when (profile.recommendedProfile) {
            PerformanceProfile.SUPER_SMOOTH ->
                "Configured for fail-safe thermal stability. Resolution scaled to ${profile.resolutionProfile.renderScalePercent}% with light shaders to guarantee zero device overheating."
            PerformanceProfile.SMOOTH ->
                "Optimized for reliable 60 FPS gameplay with low input latency. Vulkan translation & async shader compilation enabled."
            PerformanceProfile.BALANCE ->
                "Balanced profile giving crisp visual details (${profile.resolutionProfile.renderScalePercent}% scale, ${profile.graphicsProfile.textureQuality} textures) while maintaining 60 FPS target."
            PerformanceProfile.HIGH ->
                "High-fidelity visual configuration with SMAA anti-aliasing and 8x anisotropic filtering, tailored for ${device.gpuRenderer}."
            PerformanceProfile.ULTRA ->
                "Near-native resolution and Ultra textures enabled. Utilizes multi-threaded DXVK shader compilation with performance CPU core pinning."
            PerformanceProfile.EXTREME, PerformanceProfile.SUPER_EXTREME ->
                "Maximum unlocked graphical performance with high refresh rates and ultra shader precision for enthusiast hardware."
        }
    }

    companion object {
        private const val TAG = "PerformanceOptEngine"
    }
}
