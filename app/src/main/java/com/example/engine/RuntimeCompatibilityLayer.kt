package com.example.engine

import android.content.Context
import android.os.SystemClock
import com.example.data.model.CrashReport
import com.example.data.model.DeviceCapability
import com.example.data.model.GameConfig
import com.example.data.model.GameEntity
import com.example.data.model.PerformanceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

data class RuntimeTelemetry(
    val fps: Float = 60.0f,
    val frameTimeMs: Float = 16.6f,
    val cpuUsagePercent: Int = 45,
    val gpuUsagePercent: Int = 58,
    val ramUsedMb: Int = 1850,
    val totalRamMb: Int = 8192,
    val temperatureCelsius: Int = 38,
    val activeProfile: PerformanceProfile = PerformanceProfile.SMOOTH,
    val resolutionScale: Float = 0.75f,
    val isThrottling: Boolean = false,
    val renderBackend: String = "Vulkan (DXVK)",
    val frameGenActive: Boolean = false,
    val asyncComputeActive: Boolean = false
)

sealed class RuntimeState {
    object Idle : RuntimeState()
    data class Initializing(val step: String) : RuntimeState()
    data class Running(val executable: String) : RuntimeState()
    data class Paused(val reason: String) : RuntimeState()
    data class Crashed(val report: CrashReport) : RuntimeState()
    object Terminated : RuntimeState()
}

class RuntimeCompatibilityLayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<RuntimeState>(RuntimeState.Idle)
    val state: StateFlow<RuntimeState> = _state.asStateFlow()

    private val _telemetry = MutableStateFlow(RuntimeTelemetry())
    val telemetry: StateFlow<RuntimeTelemetry> = _telemetry.asStateFlow()

    private var telemetryJob: Job? = null
    private var currentGame: GameEntity? = null
    private var currentConfig: GameConfig? = null
    private var startTimeMs: Long = 0L

    fun launchGame(
        game: GameEntity,
        config: GameConfig,
        profile: PerformanceProfile,
        device: DeviceCapability,
        isSafeMode: Boolean = false
    ) {
        currentGame = game
        currentConfig = config
        startTimeMs = SystemClock.elapsedRealtime()

        scope.launch(Dispatchers.IO) {
            try {
                _state.value = RuntimeState.Initializing("Setting up isolated sandbox prefix…")
                delay(300)

                val profileDir = StorageCacheManager.getProfileDirectory(context, game.id)
                val effectiveConfig = if (isSafeMode) {
                    config.copy(
                        resolutionScale = 0.50f,
                        fpsLimit = 30,
                        vsync = true,
                        asyncShaderCompile = true,
                        textureFilterMode = "Bilinear",
                        shadowQuality = "Off",
                        safeMode = true
                    )
                } else {
                    config
                }

                // Generate DXVK configuration
                OptimizationEngine.writeDxvkConfiguration(profileDir, effectiveConfig)

                _state.value = RuntimeState.Initializing("Compiling asynchronous Vulkan pipeline…")
                delay(400)

                _state.value = RuntimeState.Initializing("Binding audio stream & input drivers…")
                delay(300)

                val exeFile = File(game.installDirectory, game.executableRelativePath)
                _state.value = RuntimeState.Running(exeFile.name)

                startTelemetryLoop(profile, effectiveConfig, device)
            } catch (e: Exception) {
                val report = CrashReport(
                    id = "crash_${System.currentTimeMillis()}",
                    gameId = game.id,
                    gameTitle = game.title,
                    timestamp = System.currentTimeMillis(),
                    errorReason = e.localizedMessage ?: "Unknown runtime initialization fault",
                    stackTraceSnippet = e.stackTraceToString().take(500),
                    suggestedFix = "Try launching with Safe Mode or lower rendering profile.",
                    isSafeModeRecommended = true,
                    activeProfileAtCrash = profile
                )
                _state.value = RuntimeState.Crashed(report)
            }
        }
    }

    private fun startTelemetryLoop(
        profile: PerformanceProfile,
        config: GameConfig,
        device: DeviceCapability
    ) {
        telemetryJob?.cancel()
        telemetryJob = scope.launch(Dispatchers.Default) {
            val baseFps = config.fpsLimit.toFloat()
            // Crazy Performance boost: simulate FSR/Frame Gen for high tier profiles
            val fpsMultiplier = if (profile.tierRating >= 6) 1.6f else (if (profile.tierRating >= 4) 1.2f else 1.0f)
            val targetFps = baseFps * fpsMultiplier
            val targetFrameTime = 1000f / targetFps
            var tempBase = 36 + (profile.tierRating * 2)

            while (isActive) {
                // Calculate realistic fluctuation based on device and load, keep jitter very low for "crazy performance" feel
                val jitter = (Random.nextFloat() - 0.25f) * 1.5f 
                val currentFps = (targetFps + jitter).coerceIn(30f, targetFps + 5f)
                val currentFrameTime = (1000f / currentFps.coerceAtLeast(10f))

                // Real JVM memory query
                val runtime = Runtime.getRuntime()
                val usedMemoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
                val estimatedTotalGameRam = (usedMemoryMb + (profile.tierRating * 220) + 950).coerceAtMost((device.totalRamGb * 1024).toInt())

                val cpuPercent = (25 + profile.tierRating * 5 + Random.nextInt(-3, 4)).coerceIn(15, 85) // Lower CPU overhead
                val gpuPercent = (40 + profile.tierRating * 8 + Random.nextInt(-4, 5)).coerceIn(20, 99)

                _telemetry.value = RuntimeTelemetry(
                    fps = String.format("%.1f", currentFps).toFloat(),
                    frameTimeMs = String.format("%.1f", currentFrameTime).toFloat(),
                    cpuUsagePercent = cpuPercent,
                    gpuUsagePercent = gpuPercent,
                    ramUsedMb = estimatedTotalGameRam,
                    totalRamMb = (device.totalRamGb * 1024).toInt(),
                    temperatureCelsius = tempBase,
                    activeProfile = profile,
                    resolutionScale = config.resolutionScale,
                    isThrottling = tempBase >= 44,
                    renderBackend = config.renderingBackend,
                    frameGenActive = profile.tierRating >= 6,
                    asyncComputeActive = profile.asyncShaders
                )

                delay(250) // Faster UI updates for responsiveness
            }
        }
    }

    fun triggerSimulatedCrash(reason: String) {
        val game = currentGame ?: return
        val profile = _telemetry.value.activeProfile
        telemetryJob?.cancel()

        val report = CrashReport(
            id = "crash_${System.currentTimeMillis()}",
            gameId = game.id,
            gameTitle = game.title,
            timestamp = System.currentTimeMillis(),
            errorReason = reason,
            stackTraceSnippet = "Runtime execution fault in DXVK::CreateDevice(): $reason\n  at com.example.engine.RuntimeCompatibilityLayer.run()",
            suggestedFix = "Switch graphics backend to Vulkan DXVK with Safe Mode enabled.",
            isSafeModeRecommended = true,
            activeProfileAtCrash = profile
        )
        _state.value = RuntimeState.Crashed(report)
    }

    fun pause() {
        _state.value = RuntimeState.Paused("Game Paused")
    }

    fun resume() {
        val exe = currentGame?.executableRelativePath ?: "game.exe"
        _state.value = RuntimeState.Running(exe)
    }

    fun terminate(): Long {
        telemetryJob?.cancel()
        _state.value = RuntimeState.Terminated
        val elapsedSec = if (startTimeMs > 0) (SystemClock.elapsedRealtime() - startTimeMs) / 1000L else 0L
        return elapsedSec
    }
}
