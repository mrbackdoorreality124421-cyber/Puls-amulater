package com.example.engine.performance

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AdaptiveAdjustmentLog(
    val timestamp: Long,
    val bottleneck: DetectedBottleneck,
    val parameterChanged: String,
    val oldValue: String,
    val newValue: String,
    val reason: String
)

class AdaptiveOptimizationEngine(
    private val scope: CoroutineScope,
    private val optimizationEngine: PerformanceOptimizationEngine,
    private val performanceMonitor: PerformanceMonitor
) {
    private val _adaptationHistory = MutableStateFlow<List<AdaptiveAdjustmentLog>>(emptyList())
    val adaptationHistory: StateFlow<List<AdaptiveAdjustmentLog>> = _adaptationHistory.asStateFlow()

    private val _isAdaptiveRunning = MutableStateFlow(false)
    val isAdaptiveRunning: StateFlow<Boolean> = _isAdaptiveRunning.asStateFlow()

    private var loopJob: Job? = null
    private var lastAdjustmentTime = 0L
    private val COOLDOWN_MS = 6000L // 6 seconds stability threshold between changes
    private var consecutiveBottleneckCount = 0
    private var lastObservedBottleneck = DetectedBottleneck.NONE

    fun startAdaptiveOptimization(gameId: String) {
        if (_isAdaptiveRunning.value) return
        _isAdaptiveRunning.value = true
        lastAdjustmentTime = System.currentTimeMillis()
        consecutiveBottleneckCount = 0

        loopJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isAdaptiveRunning.value) {
                delay(1000)
                val metrics = performanceMonitor.metrics.value
                val now = System.currentTimeMillis()

                if (metrics.activeBottleneck != DetectedBottleneck.NONE) {
                    if (metrics.activeBottleneck == lastObservedBottleneck) {
                        consecutiveBottleneckCount++
                    } else {
                        lastObservedBottleneck = metrics.activeBottleneck
                        consecutiveBottleneckCount = 1
                    }

                    // Stability threshold: Require at least 3 consecutive seconds of the same bottleneck
                    // and obey cooldown period to prevent oscillation!
                    if (consecutiveBottleneckCount >= 3 && (now - lastAdjustmentTime) >= COOLDOWN_MS) {
                        applyTargetedAdjustment(gameId, metrics.activeBottleneck)
                        lastAdjustmentTime = now
                        consecutiveBottleneckCount = 0
                    }
                } else {
                    consecutiveBottleneckCount = 0
                }
            }
        }
    }

    fun stopAdaptiveOptimization() {
        _isAdaptiveRunning.value = false
        loopJob?.cancel()
        loopJob = null
    }

    private fun applyTargetedAdjustment(gameId: String, bottleneck: DetectedBottleneck) {
        val currentProfile = optimizationEngine.activeProfile.value ?: return
        var updated = currentProfile
        var log: AdaptiveAdjustmentLog? = null

        when (bottleneck) {
            DetectedBottleneck.GPU_BOTTLENECK, DetectedBottleneck.EXCESSIVE_RESOLUTION -> {
                // Adjust resolution scale down by 5% (safe lower bound 50%)
                val currentScale = currentProfile.resolutionProfile.renderScalePercent
                if (currentScale > 50) {
                    val newScale = (currentScale - 5).coerceAtLeast(50)
                    updated = currentProfile.copy(
                        resolutionProfile = currentProfile.resolutionProfile.copy(renderScalePercent = newScale)
                    )
                    log = AdaptiveAdjustmentLog(
                        timestamp = System.currentTimeMillis(),
                        bottleneck = bottleneck,
                        parameterChanged = "Resolution Scale",
                        oldValue = "$currentScale%",
                        newValue = "$newScale%",
                        reason = "GPU fill-rate saturation detected. Scaled down internal rendering buffer to maintain 60 FPS."
                    )
                }
            }

            DetectedBottleneck.CPU_BOTTLENECK -> {
                // Adjust Dynarec thread concurrency and affinity
                val oldAffinity = currentProfile.cpuProfile.cpuAffinity
                val newAffinity = "Pinned Performance Big Cores"
                updated = currentProfile.copy(
                    cpuProfile = currentProfile.cpuProfile.copy(
                        cpuAffinity = newAffinity,
                        dynarecBigBlock = true
                    )
                )
                log = AdaptiveAdjustmentLog(
                    timestamp = System.currentTimeMillis(),
                    bottleneck = bottleneck,
                    parameterChanged = "CPU Affinity & Dynarec",
                    oldValue = oldAffinity,
                    newValue = newAffinity,
                    reason = "CPU translation bottleneck detected. Pinned Box64 translation loops to high-performance ARM cores."
                )
            }

            DetectedBottleneck.SHADER_COMPILATION_STUTTER -> {
                // Increase compiler thread count and enable async pipeline
                val oldThreads = currentProfile.shaderProfile.compilerThreads
                val newThreads = (oldThreads + 2).coerceAtMost(8)
                updated = currentProfile.copy(
                    shaderProfile = currentProfile.shaderProfile.copy(
                        asyncShaderCompile = true,
                        compilerThreads = newThreads
                    )
                )
                log = AdaptiveAdjustmentLog(
                    timestamp = System.currentTimeMillis(),
                    bottleneck = bottleneck,
                    parameterChanged = "Shader Compiler Threads",
                    oldValue = "$oldThreads threads",
                    newValue = "$newThreads threads",
                    reason = "Pipeline compilation stutter detected. Elevated DXVK background compiler worker threads."
                )
            }

            DetectedBottleneck.THERMAL_THROTTLING -> {
                // Enforce frame rate cap and frame pacing to cool down SoC
                val oldFps = currentProfile.fpsTarget
                val newFps = 45
                updated = currentProfile.copy(
                    fpsTarget = newFps,
                    framePacing = "Thermal Safe Adaptive (22.2ms)"
                )
                log = AdaptiveAdjustmentLog(
                    timestamp = System.currentTimeMillis(),
                    bottleneck = bottleneck,
                    parameterChanged = "Target FPS & Pacing",
                    oldValue = "$oldFps FPS",
                    newValue = "$newFps FPS",
                    reason = "Device thermal ceiling reached. Locked framerate to 45 FPS to prevent severe hardware thermal throttling."
                )
            }

            DetectedBottleneck.EXPENSIVE_EFFECTS -> {
                val oldShadows = currentProfile.graphicsProfile.shadowQuality
                val newShadows = if (oldShadows == "High" || oldShadows == "Ultra") "Medium" else "Low"
                updated = currentProfile.copy(
                    graphicsProfile = currentProfile.graphicsProfile.copy(
                        shadowQuality = newShadows,
                        effectsQuality = "Low"
                    )
                )
                log = AdaptiveAdjustmentLog(
                    timestamp = System.currentTimeMillis(),
                    bottleneck = bottleneck,
                    parameterChanged = "Shadow & Effect Precision",
                    oldValue = oldShadows,
                    newValue = newShadows,
                    reason = "Post-process rendering bottleneck detected. Decreased shadow map resolution."
                )
            }

            DetectedBottleneck.RAM_EXHAUSTION -> {
                updated = currentProfile.copy(
                    memoryProfile = currentProfile.memoryProfile.copy(
                        aggressiveGarbageCollection = true,
                        textureCompressionEnabled = true
                    )
                )
                log = AdaptiveAdjustmentLog(
                    timestamp = System.currentTimeMillis(),
                    bottleneck = bottleneck,
                    parameterChanged = "Memory Compaction",
                    oldValue = "Standard GC",
                    newValue = "Aggressive Heap Compaction",
                    reason = "High memory usage detected. Enabled texture compression and runtime GC compaction."
                )
            }

            DetectedBottleneck.NONE -> Unit
        }

        if (log != null) {
            optimizationEngine.updateProfile(gameId, updated)
            _adaptationHistory.value = listOf(log) + _adaptationHistory.value.take(19)
            Log.d(TAG, "Adaptive adjustment applied: ${log.parameterChanged} -> ${log.newValue}")
        }
    }

    companion object {
        private const val TAG = "AdaptiveOptEngine"
    }
}
