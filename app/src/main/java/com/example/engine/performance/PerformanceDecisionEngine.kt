package com.example.engine.performance

import com.example.data.model.DeviceCapability
import com.example.data.model.DevicePerformanceTier
import com.example.data.model.GameConfig
import com.example.data.model.GameLoadAnalysis
import com.example.data.model.GameLoadScore
import com.example.data.model.PerformanceProfile

object PerformanceDecisionEngine {

    fun calculateRecommendedProfile(
        device: DeviceCapability,
        analysis: GameLoadAnalysis
    ): PerformanceProfile {
        val devTier = device.performanceTier
        val gameLoad = analysis.loadScore

        return when (devTier) {
            DevicePerformanceTier.ENTHUSIAST_EXTREME -> {
                when (gameLoad) {
                    GameLoadScore.LIGHT -> PerformanceProfile.SUPER_EXTREME
                    GameLoadScore.MODERATE -> PerformanceProfile.EXTREME
                    GameLoadScore.HEAVY -> PerformanceProfile.ULTRA
                    GameLoadScore.VERY_HEAVY -> PerformanceProfile.HIGH
                    GameLoadScore.EXTREME -> PerformanceProfile.BALANCE
                }
            }
            DevicePerformanceTier.ULTRA_FLAGSHIP -> {
                when (gameLoad) {
                    GameLoadScore.LIGHT -> PerformanceProfile.EXTREME
                    GameLoadScore.MODERATE -> PerformanceProfile.ULTRA
                    GameLoadScore.HEAVY -> PerformanceProfile.HIGH
                    GameLoadScore.VERY_HEAVY -> PerformanceProfile.BALANCE
                    GameLoadScore.EXTREME -> PerformanceProfile.SMOOTH
                }
            }
            DevicePerformanceTier.HIGH -> {
                when (gameLoad) {
                    GameLoadScore.LIGHT -> PerformanceProfile.HIGH
                    GameLoadScore.MODERATE -> PerformanceProfile.BALANCE
                    GameLoadScore.HEAVY -> PerformanceProfile.SMOOTH
                    GameLoadScore.VERY_HEAVY -> PerformanceProfile.SMOOTH
                    GameLoadScore.EXTREME -> PerformanceProfile.SUPER_SMOOTH
                }
            }
            DevicePerformanceTier.MID -> {
                when (gameLoad) {
                    GameLoadScore.LIGHT -> PerformanceProfile.BALANCE
                    GameLoadScore.MODERATE -> PerformanceProfile.SMOOTH
                    GameLoadScore.HEAVY -> PerformanceProfile.SUPER_SMOOTH
                    GameLoadScore.VERY_HEAVY -> PerformanceProfile.SUPER_SMOOTH
                    GameLoadScore.EXTREME -> PerformanceProfile.SUPER_SMOOTH
                }
            }
            DevicePerformanceTier.ENTRY -> {
                when (gameLoad) {
                    GameLoadScore.LIGHT -> PerformanceProfile.SMOOTH
                    else -> PerformanceProfile.SUPER_SMOOTH
                }
            }
        }
    }

    fun buildGamePerformanceProfile(
        gameId: String,
        device: DeviceCapability,
        analysis: GameLoadAnalysis,
        selectedProfile: PerformanceProfile? = null
    ): GamePerformanceProfile {
        val profile = selectedProfile ?: calculateRecommendedProfile(device, analysis)

        val isSnapdragon = device.socModel.contains("Snapdragon", ignoreCase = true)
        val isMali = device.gpuVendor.contains("ARM", ignoreCase = true) || device.gpuRenderer.contains("Mali", ignoreCase = true)
        val is64BitGame = analysis.detectedArchitecture.contains("64")

        val renderingBackend = when {
            isSnapdragon && device.isVulkanSupported -> "Vulkan (Turnip Adreno Direct)"
            device.isVulkanSupported -> "Vulkan (DXVK 2.3 Async)"
            else -> "OpenGL ES 3.2 (VirGL Accelerated)"
        }

        val boxTranslator = if (is64BitGame) "Box64 v0.2.8 Dynarec" else "Box86 v0.3.2 Dynarec"

        val graphicsBridge = when {
            analysis.detectedGraphicsApi.contains("DirectX 12") && device.isVulkanSupported -> "VKD3D-Proton 2.11"
            analysis.detectedGraphicsApi.contains("DirectX 9") -> "DXVK D3D9 / WineD3D"
            device.isVulkanSupported -> "DXVK 2.3.1 Async"
            else -> "WineD3D OpenGL Bridge"
        }

        val availableRamMb = (device.availableRamBytes / (1024 * 1024)).toInt()
        val heapLimitMb = (availableRamMb - 768).coerceIn(1024, 6144)

        val renderScalePercent = when (profile) {
            PerformanceProfile.SUPER_SMOOTH -> 50
            PerformanceProfile.SMOOTH -> 65
            PerformanceProfile.BALANCE -> 75
            PerformanceProfile.HIGH -> 85
            PerformanceProfile.ULTRA -> 100
            PerformanceProfile.EXTREME -> 110
            PerformanceProfile.SUPER_EXTREME -> 120
        }

        val textureQuality = profile.textureQuality
        val shadowQuality = profile.shadowQuality
        val effectsQuality = profile.effectsQuality

        val antiAliasing = when (profile) {
            PerformanceProfile.SUPER_SMOOTH -> "None"
            PerformanceProfile.SMOOTH -> "FXAA"
            PerformanceProfile.BALANCE -> "FXAA"
            PerformanceProfile.HIGH -> "SMAA"
            PerformanceProfile.ULTRA -> "SMAA 2x"
            PerformanceProfile.EXTREME, PerformanceProfile.SUPER_EXTREME -> "TAA Ultra"
        }

        val anisotropic = when (profile) {
            PerformanceProfile.SUPER_SMOOTH -> "Bilinear"
            PerformanceProfile.SMOOTH -> "2x"
            PerformanceProfile.BALANCE -> "4x"
            PerformanceProfile.HIGH -> "8x"
            PerformanceProfile.ULTRA, PerformanceProfile.EXTREME, PerformanceProfile.SUPER_EXTREME -> "16x"
        }

        val compilerThreads = (device.cpuCores - 2).coerceIn(2, 8)

        val fpsTarget = profile.targetFps

        val cpuAffinity = when {
            device.cpuCores >= 8 -> "Performance Big+MID Cores"
            else -> "All CPU Cores"
        }

        return GamePerformanceProfile(
            gameId = gameId,
            deviceProfile = DevicePerformanceSnapshot(
                socModel = device.socModel,
                cpuArch = device.cpuArchitecture,
                cpuCores = device.cpuCores,
                gpuRenderer = device.gpuRenderer,
                totalRamMb = (device.totalRamBytes / (1024 * 1024)).toInt(),
                availableRamMb = availableRamMb,
                isVulkanSupported = device.isVulkanSupported,
                vulkanVersion = device.vulkanVersion,
                thermalStatus = device.thermalStatusString,
                performanceTier = device.performanceTier.name
            ),
            runtimeProfile = RuntimeProfileConfig(
                wineVersion = "Wine 9.2 Staging (PulsePC Prefix)",
                boxTranslator = boxTranslator,
                graphicsBridge = graphicsBridge,
                renderingBackend = renderingBackend,
                winArch = if (is64BitGame) "Win64" else "Win32",
                heapLimitMb = heapLimitMb
            ),
            graphicsProfile = GraphicsProfileConfig(
                textureQuality = textureQuality,
                shadowQuality = shadowQuality,
                effectsQuality = effectsQuality,
                antiAliasing = antiAliasing,
                anisotropicFiltering = anisotropic,
                postProcessing = profile.tierRating >= 3
            ),
            resolutionProfile = ResolutionProfileConfig(
                renderScalePercent = renderScalePercent,
                internalResolution = calculateInternalResolution(device.displayResolution, renderScalePercent),
                displayMode = "Fullscreen Aspect-Ratio Fit",
                frameRateLimiter = fpsTarget
            ),
            shaderProfile = ShaderProfileConfig(
                shaderCacheEnabled = profile.shaderCache,
                asyncShaderCompile = profile.asyncShaders,
                compilerThreads = compilerThreads,
                spirvOptimization = true,
                maxTessellationFactor = if (profile.tierRating >= 4) 16 else 8
            ),
            cpuProfile = CpuProfileConfig(
                cpuAffinity = cpuAffinity,
                dynarecBigBlock = true,
                dynarecFastRound = true,
                dynarecStrongMem = false,
                threadConcurrency = (device.cpuCores - 1).coerceAtLeast(2)
            ),
            memoryProfile = MemoryProfileConfig(
                heapLimitMb = heapLimitMb,
                aggressiveGarbageCollection = (availableRamMb < 2500),
                textureCompressionEnabled = true,
                swapFileMb = 1024
            ),
            thermalProfile = ThermalProfileConfig(
                thermalProtectionEnabled = true,
                aggressiveFramePacingOnThrottle = true,
                autoReduceResolutionOnOverheat = true,
                targetMaxTempCelsius = 43.0f
            ),
            fpsTarget = fpsTarget,
            framePacing = profile.framePacing,
            optimizationLevel = "Deep Hardware Adaptive",
            recommendedProfile = profile,
            stabilityScore = 1.0f,
            lastAdaptedTimestamp = System.currentTimeMillis()
        )
    }

    private fun calculateInternalResolution(displayRes: String, scalePercent: Int): String {
        return try {
            val parts = displayRes.split("x")
            if (parts.size == 2) {
                val width = parts[0].trim().toInt()
                val height = parts[1].trim().toInt()
                val scale = scalePercent / 100f
                val scaledW = (width * scale).toInt()
                val scaledH = (height * scale).toInt()
                "${scaledW}x${scaledH}"
            } else {
                "1280x720"
            }
        } catch (e: Exception) {
            "1280x720"
        }
    }

    fun convertToGameConfig(profile: GamePerformanceProfile): GameConfig {
        return GameConfig(
            renderingBackend = profile.runtimeProfile.renderingBackend,
            resolutionScale = profile.resolutionProfile.renderScalePercent / 100f,
            fpsLimit = profile.fpsTarget,
            vsync = profile.recommendedProfile.vsync,
            shaderCache = profile.shaderProfile.shaderCacheEnabled,
            asyncShaderCompile = profile.shaderProfile.asyncShaderCompile,
            framePacingMode = profile.framePacing,
            textureFilterMode = "Anisotropic ${profile.graphicsProfile.anisotropicFiltering}",
            shadowQuality = profile.graphicsProfile.shadowQuality,
            effectsQuality = profile.graphicsProfile.effectsQuality,
            antiAliasing = profile.graphicsProfile.antiAliasing,
            audioLatencyMs = 25,
            cpuAffinity = profile.cpuProfile.cpuAffinity,
            memoryLimitMb = profile.memoryProfile.heapLimitMb,
            dxvkAsync = profile.shaderProfile.asyncShaderCompile,
            wineVersion = profile.runtimeProfile.wineVersion,
            winArch = profile.runtimeProfile.winArch,
            audioDriver = "AAudio Low Latency",
            safeMode = false
        )
    }
}
