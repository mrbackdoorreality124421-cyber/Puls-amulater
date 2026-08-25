package com.example.engine

import com.example.data.model.GameGenre
import com.example.data.model.GameLoadAnalysis
import com.example.data.model.GameLoadScore
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.zip.ZipFile

object GameAnalyzer {

    fun analyzeDirectory(gameDirectory: File, userTitle: String? = null): GameLoadAnalysis {
        val files = mutableListOf<File>()
        collectFiles(gameDirectory, files)

        val totalFiles = files.size
        var totalSizeBytes = 0L
        var largeAssetsSize = 0L
        var shaderCount = 0
        var textureCount = 0
        var audioCount = 0
        var videoCount = 0
        val detectedDlls = mutableListOf<String>()
        val detectedExeCandidates = mutableListOf<File>()

        for (file in files) {
            val size = file.length()
            totalSizeBytes += size
            val nameLower = file.name.lowercase(Locale.US)

            if (size > 50 * 1024 * 1024) {
                largeAssetsSize += size
            }

            when {
                nameLower.endsWith(".exe") -> {
                    if (!isExcludedExe(nameLower)) {
                        detectedExeCandidates.add(file)
                    }
                }
                nameLower.endsWith(".dll") -> {
                    if (detectedDlls.size < 20) {
                        detectedDlls.add(file.name)
                    }
                }
                nameLower.endsWith(".hlsl") || nameLower.endsWith(".spv") || nameLower.endsWith(".cso") ||
                        nameLower.endsWith(".shader") || nameLower.endsWith(".fx") -> {
                    shaderCount++
                }
                nameLower.endsWith(".dds") || nameLower.endsWith(".tga") || nameLower.endsWith(".png") ||
                        nameLower.endsWith(".jpg") || nameLower.endsWith(".ktx") || nameLower.endsWith(".bmp") -> {
                    textureCount++
                }
                nameLower.endsWith(".ogg") || nameLower.endsWith(".wav") || nameLower.endsWith(".mp3") ||
                        nameLower.endsWith(".flac") || nameLower.endsWith(".bank") || nameLower.endsWith(".fev") -> {
                    audioCount++
                }
                nameLower.endsWith(".bik") || nameLower.endsWith(".bk2") || nameLower.endsWith(".mp4") ||
                        nameLower.endsWith(".avi") || nameLower.endsWith(".wmv") -> {
                    videoCount++
                }
            }
        }

        val primaryExe = selectPrimaryExecutable(detectedExeCandidates, gameDirectory)
        val relativeExePath = if (primaryExe != null) {
            primaryExe.relativeTo(gameDirectory).path
        } else {
            "game.exe"
        }

        val arch = if (primaryExe != null && primaryExe.exists()) {
            detectArchitectureFromPE(primaryExe)
        } else {
            "64-bit (x86_64)"
        }

        val engine = detectGameEngine(files, detectedDlls)
        val graphicsApi = detectGraphicsApi(detectedDlls, files)
        val detectedGenre = detectGenre(gameDirectory.name, files, detectedExeCandidates)

        val title = userTitle?.ifBlank { null }
            ?: formatGameTitle(primaryExe?.nameWithoutExtension ?: gameDirectory.name)

        // Calculate Game Heaviness Score
        val (loadScore, cpuWeight, gpuWeight, ramMb) = calculateLoadScore(
            totalSizeBytes = totalSizeBytes,
            largeAssetsSize = largeAssetsSize,
            shaderCount = shaderCount,
            textureCount = textureCount,
            videoCount = videoCount,
            graphicsApi = graphicsApi,
            engine = engine,
            arch = arch
        )

        val bottlenecks = mutableListOf<String>()
        if (ramMb > 4000) bottlenecks.add("High memory footprint (>4GB RAM required)")
        if (graphicsApi.contains("DirectX 12") || graphicsApi.contains("DirectX 11")) {
            bottlenecks.add("DXVK Translation overhead (Vulkan Vulkan pipeline recommended)")
        }
        if (largeAssetsSize > 500 * 1024 * 1024) {
            bottlenecks.add("Heavy disk streaming (Fast UFS/NVMe cache beneficial)")
        }
        if (bottlenecks.isEmpty()) {
            bottlenecks.add("No significant bottlenecks detected. High compatibility expected.")
        }

        val summary = "$title ($arch) detected with $engine engine using $graphicsApi. Load rating: ${loadScore.title}."

        return GameLoadAnalysis(
            gameTitle = title,
            totalFiles = totalFiles,
            totalSizeBytes = totalSizeBytes,
            extractedEstimateBytes = (totalSizeBytes * 1.05).toLong(),
            detectedExecutable = relativeExePath,
            detectedArchitecture = arch,
            detectedEngine = engine,
            detectedGraphicsApi = graphicsApi,
            detectedDlls = detectedDlls,
            shaderFilesCount = shaderCount,
            textureFilesCount = textureCount,
            audioFilesCount = audioCount,
            videoFilesCount = videoCount,
            largeAssetsSizeMb = largeAssetsSize / (1024 * 1024),
            loadScore = loadScore,
            estimatedCpuWeight = cpuWeight,
            estimatedGpuWeight = gpuWeight,
            estimatedRamMb = ramMb,
            detectedGenre = detectedGenre,
            compatibilitySummary = summary,
            potentialBottlenecks = bottlenecks
        )
    }

    private fun isExcludedExe(name: String): Boolean {
        val excluded = listOf(
            "unins", "setup", "installer", "crash", "reporter", "redist", "vcredist",
            "dxsetup", "directx", "patch", "updater", "config", "launcher_updater",
            "easyanticheat", "battleye", "unitycrashhandler", "unrealcefsubprocess"
        )
        return excluded.any { name.contains(it) }
    }

    fun selectPrimaryExecutable(candidates: List<File>, baseDir: File): File? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        // 1. Prefer executable that matches folder name
        val dirNameLower = baseDir.name.lowercase(Locale.US).replace(" ", "")
        val matched = candidates.firstOrNull {
            it.nameWithoutExtension.lowercase(Locale.US).replace(" ", "").contains(dirNameLower) ||
                    dirNameLower.contains(it.nameWithoutExtension.lowercase(Locale.US).replace(" ", ""))
        }
        if (matched != null) return matched

        // 2. Prefer executable in root directory or Binaries/Win64
        val rootCandidate = candidates.firstOrNull { it.parentFile == baseDir }
        if (rootCandidate != null) return rootCandidate

        val win64Candidate = candidates.firstOrNull { it.parentFile?.name?.equals("Win64", ignoreCase = true) == true }
        if (win64Candidate != null) return win64Candidate

        // 3. Fallback to largest executable
        return candidates.maxByOrNull { it.length() } ?: candidates.first()
    }

    private fun detectArchitectureFromPE(file: File): String {
        return try {
            FileInputStream(file).use { stream ->
                val buffer = ByteArray(1024)
                val read = stream.read(buffer)
                if (read < 64) return "64-bit (x86_64)"

                // Check MZ signature
                if (buffer[0] != 0x4D.toByte() || buffer[1] != 0x5A.toByte()) {
                    return "64-bit (x86_64)"
                }

                // PE Header Offset at 0x3C
                val peOffset = (buffer[0x3C].toInt() and 0xFF) or
                        ((buffer[0x3D].toInt() and 0xFF) shl 8) or
                        ((buffer[0x3E].toInt() and 0xFF) shl 16) or
                        ((buffer[0x3F].toInt() and 0xFF) shl 24)

                if (peOffset in 0..(read - 6)) {
                    // Check 'PE\0\0'
                    if (buffer[peOffset] == 0x50.toByte() && buffer[peOffset + 1] == 0x45.toByte()) {
                        val machine = (buffer[peOffset + 4].toInt() and 0xFF) or
                                ((buffer[peOffset + 5].toInt() and 0xFF) shl 8)
                        return when (machine) {
                            0x014c -> "32-bit (x86)"
                            0x8664 -> "64-bit (x86_64)"
                            0xAA64 -> "ARM64 (Native Windows)"
                            else -> "64-bit (x86_64)"
                        }
                    }
                }
                "64-bit (x86_64)"
            }
        } catch (e: Exception) {
            "64-bit (x86_64)"
        }
    }

    private fun detectGameEngine(files: List<File>, dlls: List<String>): String {
        val allNames = files.map { it.name.lowercase(Locale.US) }
        val dllNames = dlls.map { it.lowercase(Locale.US) }

        return when {
            allNames.any { it.contains("unityplayer") || it.contains("unitydefaultresources") } ||
                    dllNames.any { it.contains("unityplayer") } -> "Unity Engine"
            allNames.any { it.contains("unrealengine") || it.contains("ue4") || it.contains("ue5") } ||
                    files.any { it.path.contains("Engine/Binaries", ignoreCase = true) } -> "Unreal Engine"
            allNames.any { it.contains("project.godot") || it.contains("godot") } -> "Godot Engine"
            allNames.any { it.contains("tier0.dll") || it.contains("vstdlib.dll") || it.contains("hl2.exe") } -> "Source Engine (Valve)"
            allNames.any { it.contains("game.rgss3a") || it.contains("game.rgss2a") || it.contains("rgss") } -> "RPG Maker Engine"
            allNames.any { it.contains("love.dll") || it.endsWith(".love") } -> "LÖVE2D Engine"
            allNames.any { it.contains("nw.exe") || it.contains("nw.dll") || it.contains("electron.exe") } -> "Chromium / Web Game"
            else -> "DirectX / Native C++"
        }
    }

    private fun detectGraphicsApi(dlls: List<String>, files: List<File>): String {
        val dllNames = dlls.map { it.lowercase(Locale.US) }
        val fileNames = files.map { it.name.lowercase(Locale.US) }

        return when {
            dllNames.any { it.contains("d3d12") } -> "DirectX 12 (VKD3D-Proton)"
            dllNames.any { it.contains("d3d11") || it.contains("dxgi") } -> "DirectX 11 (DXVK 2.3)"
            dllNames.any { it.contains("vulkan-1") } -> "Native Vulkan"
            dllNames.any { it.contains("d3d9") || it.contains("d3d8") } -> "DirectX 9 (D9VK / DXVK)"
            dllNames.any { it.contains("opengl") } || fileNames.any { it.contains("glad") || it.contains("glew") } -> "OpenGL (Zink/VirGL)"
            else -> "DirectX 11 (DXVK Auto)"
        }
    }

    private fun detectGenre(folderName: String, files: List<File>, exes: List<File>): GameGenre {
        val combinedText = (folderName + " " + exes.joinToString(" ") { it.name } + " " +
                files.take(50).joinToString(" ") { it.name }).lowercase(Locale.US)

        return when {
            combinedText.contains("race") || combinedText.contains("drift") || combinedText.contains("kart") ||
                    combinedText.contains("speed") || combinedText.contains("rally") || combinedText.contains("auto") ||
                    combinedText.contains("car") || combinedText.contains("track") -> GameGenre.RACING

            combinedText.contains("shoot") || combinedText.contains("fps") || combinedText.contains("gun") ||
                    combinedText.contains("war") || combinedText.contains("sniper") || combinedText.contains("doom") ||
                    combinedText.contains("strike") || combinedText.contains("call") || combinedText.contains("dead") -> GameGenre.SHOOTER

            combinedText.contains("fight") || combinedText.contains("kombat") || combinedText.contains("street") ||
                    combinedText.contains("brawl") || combinedText.contains("smash") || combinedText.contains("arena") -> GameGenre.FIGHTING

            combinedText.contains("rpg") || combinedText.contains("quest") || combinedText.contains("fantasy") ||
                    combinedText.contains("sword") || combinedText.contains("scroll") || combinedText.contains("souls") ||
                    combinedText.contains("witcher") || combinedText.contains("elder") -> GameGenre.RPG

            combinedText.contains("strategy") || combinedText.contains("rts") || combinedText.contains("civ") ||
                    combinedText.contains("command") || combinedText.contains("empire") || combinedText.contains("craft") ||
                    combinedText.contains("tycoon") || combinedText.contains("city") -> GameGenre.STRATEGY

            combinedText.contains("mario") || combinedText.contains("sonic") || combinedText.contains("jump") ||
                    combinedText.contains("hollow") || combinedText.contains("celeste") || combinedText.contains("platform") ||
                    combinedText.contains("mega") -> GameGenre.PLATFORMER

            else -> GameGenre.DEFAULT
        }
    }

    private fun calculateLoadScore(
        totalSizeBytes: Long,
        largeAssetsSize: Long,
        shaderCount: Int,
        textureCount: Int,
        videoCount: Int,
        graphicsApi: String,
        engine: String,
        arch: String
    ): ScoreResult {
        var points = 0
        val sizeGb = totalSizeBytes / (1024.0 * 1024.0 * 1024.0)

        // Size weighting
        points += when {
            sizeGb >= 20.0 -> 35
            sizeGb >= 10.0 -> 28
            sizeGb >= 4.0 -> 20
            sizeGb >= 1.0 -> 12
            sizeGb >= 0.3 -> 6
            else -> 2
        }

        // Graphics API weighting
        points += when {
            graphicsApi.contains("DirectX 12") -> 30
            graphicsApi.contains("DirectX 11") -> 22
            graphicsApi.contains("Vulkan") -> 15
            graphicsApi.contains("DirectX 9") -> 8
            else -> 10
        }

        // Engine weighting
        points += when {
            engine.contains("Unreal Engine") -> 22
            engine.contains("Unity") -> 14
            engine.contains("Source") -> 10
            engine.contains("Godot") -> 6
            engine.contains("RPG Maker") || engine.contains("LÖVE2D") -> 2
            else -> 12
        }

        // Asset density weighting
        if (shaderCount > 50 || largeAssetsSize > 2000L * 1024 * 1024) points += 10
        if (textureCount > 200) points += 5
        if (arch.contains("64-bit")) points += 4

        val finalScore = points.coerceIn(5, 100)
        val loadScore = GameLoadScore.fromScore(finalScore)

        val cpuWeight = ((finalScore / 10).coerceIn(1, 10))
        val gpuWeight = when (loadScore) {
            GameLoadScore.LIGHT -> 2
            GameLoadScore.MODERATE -> 4
            GameLoadScore.HEAVY -> 7
            GameLoadScore.VERY_HEAVY -> 9
            GameLoadScore.EXTREME -> 10
        }

        return ScoreResult(loadScore, cpuWeight, gpuWeight, loadScore.ramEstimateMb)
    }

    private data class ScoreResult(
        val score: GameLoadScore,
        val cpuWeight: Int,
        val gpuWeight: Int,
        val ramMb: Int
    )

    private fun formatGameTitle(raw: String): String {
        return raw.replace(Regex("[-_.]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
    }

    private fun collectFiles(dir: File, result: MutableList<File>) {
        val list = dir.listFiles() ?: return
        for (file in list) {
            if (file.isDirectory) {
                collectFiles(file, result)
            } else {
                result.add(file)
            }
        }
    }
}
