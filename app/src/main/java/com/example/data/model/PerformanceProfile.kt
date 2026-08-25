package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PerformanceProfile(
    val title: String,
    val description: String,
    val resolutionScale: Float,
    val targetFps: Int,
    val vsync: Boolean,
    val shaderCache: Boolean,
    val asyncShaders: Boolean,
    val textureQuality: String,
    val shadowQuality: String,
    val effectsQuality: String,
    val framePacing: String,
    val tierRating: Int, // 1 to 7
    val accentColorHex: Long
) {
    SUPER_SMOOTH(
        title = "Super Smooth",
        description = "Maximum stability and lowest possible workload. Prioritizes zero thermal throttling.",
        resolutionScale = 0.50f,
        targetFps = 30,
        vsync = true,
        shaderCache = true,
        asyncShaders = true,
        textureQuality = "Low",
        shadowQuality = "Off",
        effectsQuality = "Low",
        framePacing = "Ultra-Consistent (33.3ms)",
        tierRating = 1,
        accentColorHex = 0xFF00E676 // Bright Emerald Green
    ),
    SMOOTH(
        title = "Smooth",
        description = "Prioritizes stable 60 FPS and low input latency with moderate graphical details.",
        resolutionScale = 0.65f,
        targetFps = 60,
        vsync = true,
        shaderCache = true,
        asyncShaders = true,
        textureQuality = "Medium",
        shadowQuality = "Low",
        effectsQuality = "Medium",
        framePacing = "Optimized Low Latency",
        tierRating = 2,
        accentColorHex = 0xFF00E5FF // Neon Cyan
    ),
    BALANCE(
        title = "Balance",
        description = "Balanced visual fidelity and frame rate. Recommended sweet spot for most mid-to-high devices.",
        resolutionScale = 0.75f,
        targetFps = 60,
        vsync = true,
        shaderCache = true,
        asyncShaders = true,
        textureQuality = "Medium-High",
        shadowQuality = "Medium",
        effectsQuality = "Medium",
        framePacing = "Adaptive Balanced",
        tierRating = 3,
        accentColorHex = 0xFF2979FF // Electric Blue
    ),
    HIGH(
        title = "High",
        description = "Higher visual quality, sharp rendering, and enhanced effects while maintaining good frame pacing.",
        resolutionScale = 0.85f,
        targetFps = 60,
        vsync = true,
        shaderCache = true,
        asyncShaders = false,
        textureQuality = "High",
        shadowQuality = "High",
        effectsQuality = "High",
        framePacing = "Standard Precise",
        tierRating = 4,
        accentColorHex = 0xFF7C4DFF // Deep Neon Violet
    ),
    ULTRA(
        title = "Ultra",
        description = "High visual fidelity with advanced shaders and native-like resolution for flagship chipsets.",
        resolutionScale = 1.00f,
        targetFps = 60,
        vsync = true,
        shaderCache = true,
        asyncShaders = false,
        textureQuality = "Ultra",
        shadowQuality = "Ultra",
        effectsQuality = "Ultra",
        framePacing = "Standard Precise",
        tierRating = 5,
        accentColorHex = 0xFFFF9100 // Amber Glow
    ),
    EXTREME(
        title = "Extreme",
        description = "Maximum quality and high refresh rates (90/120 FPS) intended for actively cooled devices.",
        resolutionScale = 1.15f,
        targetFps = 90,
        vsync = false,
        shaderCache = true,
        asyncShaders = false,
        textureQuality = "Ultra HD",
        shadowQuality = "Ultra",
        effectsQuality = "Ultra Max",
        framePacing = "High Refresh Sync",
        tierRating = 6,
        accentColorHex = 0xFFFF3D00 // Crimson Flame
    ),
    SUPER_EXTREME(
        title = "Super Extreme",
        description = "Aggressive maximum-performance supersampling configuration for enthusiast flagship hardware.",
        resolutionScale = 1.30f,
        targetFps = 120,
        vsync = false,
        shaderCache = true,
        asyncShaders = false,
        textureQuality = "Maximum (4K textures)",
        shadowQuality = "Contact Hardening (Max)",
        effectsQuality = "Extreme Cinematic",
        framePacing = "Uncapped Mailbox",
        tierRating = 7,
        accentColorHex = 0xFFFF007F // Neon Magenta Red
    );

    companion object {
        fun fromName(name: String?): PerformanceProfile {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.title.equals(name, ignoreCase = true) }
                ?: BALANCE
        }
    }
}
