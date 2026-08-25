package com.example.data.model

data class GameLoadAnalysis(
    val gameTitle: String,
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val extractedEstimateBytes: Long,
    val detectedExecutable: String,
    val detectedArchitecture: String, // 32-bit (x86), 64-bit (x86_64)
    val detectedEngine: String, // Unreal Engine 4/5, Unity, Source, Godot, DirectX 9/11/12, Custom C++
    val detectedGraphicsApi: String, // DirectX 9, DirectX 11, DirectX 12, Vulkan, OpenGL
    val detectedDlls: List<String>,
    val shaderFilesCount: Int,
    val textureFilesCount: Int,
    val audioFilesCount: Int,
    val videoFilesCount: Int,
    val largeAssetsSizeMb: Long,
    val loadScore: GameLoadScore,
    val estimatedCpuWeight: Int,
    val estimatedGpuWeight: Int,
    val estimatedRamMb: Int,
    val detectedGenre: GameGenre,
    val compatibilitySummary: String,
    val potentialBottlenecks: List<String>
) {
    val sizeGbString: String
        get() = String.format("%.1f GB", totalSizeBytes / (1024.0 * 1024.0 * 1024.0))
}
