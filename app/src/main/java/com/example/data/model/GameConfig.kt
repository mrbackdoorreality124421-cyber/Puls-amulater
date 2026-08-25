package com.example.data.model

data class GameConfig(
    val renderingBackend: String = "Vulkan (DXVK 2.3)", // Vulkan (DXVK), Turnip Adreno, VirGL, OpenGL ES
    val resolutionScale: Float = 0.75f,
    val fpsLimit: Int = 60,
    val vsync: Boolean = true,
    val shaderCache: Boolean = true,
    val asyncShaderCompile: Boolean = true,
    val framePacingMode: String = "Adaptive Low Latency", // Standard, Adaptive, Low Latency, High Refresh
    val textureFilterMode: String = "Anisotropic 4x", // Bilinear, Trilinear, Anisotropic 2x/4x/8x/16x
    val shadowQuality: String = "Medium", // Off, Low, Medium, High, Ultra
    val effectsQuality: String = "Medium",
    val antiAliasing: String = "FXAA", // None, FXAA, SMAA, TAA
    val audioLatencyMs: Int = 30, // 20ms, 30ms, 50ms
    val cpuAffinity: String = "All Performance Cores", // All Cores, Big Cores Only, Power Saver
    val memoryLimitMb: Int = 4096,
    val dxvkAsync: Boolean = true,
    val wineVersion: String = "Proton-GE-8.26",
    val winArch: String = "Win64", // Win32, Win64
    val audioDriver: String = "AAudio Low Latency",
    val safeMode: Boolean = false
)
