package com.example.data.model

enum class GameLoadScore(
    val title: String,
    val description: String,
    val cpuWeight: Int, // 1-10
    val gpuWeight: Int, // 1-10
    val ramEstimateMb: Int,
    val colorHex: Long
) {
    LIGHT(
        title = "Light",
        description = "2D retro, indie, or lightweight DirectX 8/9 titles. Runs smoothly on virtually all hardware.",
        cpuWeight = 2,
        gpuWeight = 2,
        ramEstimateMb = 1024,
        colorHex = 0xFF00E676
    ),
    MODERATE(
        title = "Moderate",
        description = "Early 3D DirectX 9/OpenGL games or lightweight Unity engines.",
        cpuWeight = 4,
        gpuWeight = 4,
        ramEstimateMb = 2048,
        colorHex = 0xFF00E5FF
    ),
    HEAVY(
        title = "Heavy",
        description = "DirectX 10/11 titles, mid-generation Unreal Engine 3/4, demanding physics and complex shaders.",
        cpuWeight = 7,
        gpuWeight = 7,
        ramEstimateMb = 3584,
        colorHex = 0xFFFFD600
    ),
    VERY_HEAVY(
        title = "Very Heavy",
        description = "Demanding modern DirectX 11/12 or Vulkan titles with dense geometry, high-res textures and volumetric lighting.",
        cpuWeight = 9,
        gpuWeight = 8,
        ramEstimateMb = 5120,
        colorHex = 0xFFFF9100
    ),
    EXTREME(
        title = "Extreme",
        description = "AAA open-world titles, Ray-tracing indicators, or uncompressed 4K asset pipelines.",
        cpuWeight = 10,
        gpuWeight = 10,
        ramEstimateMb = 7168,
        colorHex = 0xFFFF1744
    );

    companion object {
        fun fromScore(scoreValue: Int): GameLoadScore {
            return when {
                scoreValue <= 25 -> LIGHT
                scoreValue <= 50 -> MODERATE
                scoreValue <= 75 -> HEAVY
                scoreValue <= 90 -> VERY_HEAVY
                else -> EXTREME
            }
        }
    }
}
