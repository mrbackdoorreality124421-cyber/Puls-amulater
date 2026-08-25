package com.example.engine.performance

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

data class LivePerformanceMetrics(
    val fps: Float = 60.0f,
    val targetFps: Int = 60,
    val frameTimeMs: Float = 16.6f,
    val frameTimeJitterMs: Float = 1.2f,
    val cpuUsagePercent: Int = 42,
    val gpuUsagePercent: Int = 55,
    val ramUsedMb: Int = 1850,
    val ramTotalMb: Int = 8192,
    val batteryTempCelsius: Float = 36.5f,
    val thermalStatus: String = "Nominal",
    val shaderPipelineHits: Int = 94,
    val drawCallsPerFrame: Int = 850,
    val activeBottleneck: DetectedBottleneck = DetectedBottleneck.NONE,
    val statusText: String = "Pipeline Running Smoothly"
)

enum class DetectedBottleneck {
    NONE,
    CPU_BOTTLENECK,
    GPU_BOTTLENECK,
    RAM_EXHAUSTION,
    SHADER_COMPILATION_STUTTER,
    THERMAL_THROTTLING,
    EXCESSIVE_RESOLUTION,
    EXPENSIVE_EFFECTS
}

class PerformanceMonitor(
    private val scope: CoroutineScope
) {
    private val _metrics = MutableStateFlow(LivePerformanceMetrics())
    val metrics: StateFlow<LivePerformanceMetrics> = _metrics.asStateFlow()

    private var monitorJob: Job? = null
    private val frameTimeSamples = mutableListOf<Float>()

    fun startMonitoring(targetFps: Int = 60) {
        monitorJob?.cancel()
        frameTimeSamples.clear()

        monitorJob = scope.launch(Dispatchers.Default) {
            val random = Random(System.currentTimeMillis())
            var simulatedTemp = 36.5f

            while (isActive) {
                val currentFps = (targetFps.toFloat() - (random.nextFloat() * 2.5f)).coerceAtLeast(15f)
                val baseFrameTime = 1000f / currentFps
                val jitter = (random.nextFloat() * 2.8f)

                frameTimeSamples.add(baseFrameTime)
                if (frameTimeSamples.size > 30) {
                    frameTimeSamples.removeAt(0)
                }

                simulatedTemp += (random.nextFloat() * 0.05f - 0.02f)
                simulatedTemp = simulatedTemp.coerceIn(34.0f, 44.0f)

                val cpuPercent = (35 + random.nextInt(30)).coerceIn(10, 99)
                val gpuPercent = (45 + random.nextInt(35)).coerceIn(10, 99)

                val bottleneck = detectLiveBottleneck(
                    fps = currentFps,
                    targetFps = targetFps,
                    jitter = jitter,
                    cpu = cpuPercent,
                    gpu = gpuPercent,
                    temp = simulatedTemp
                )

                val status = when (bottleneck) {
                    DetectedBottleneck.NONE -> "Frame pacing optimal (16.6ms avg)"
                    DetectedBottleneck.CPU_BOTTLENECK -> "CPU saturation detected in translation thread"
                    DetectedBottleneck.GPU_BOTTLENECK -> "GPU fill-rate bound — rendering pipeline under load"
                    DetectedBottleneck.RAM_EXHAUSTION -> "High memory pressure — low system heap"
                    DetectedBottleneck.SHADER_COMPILATION_STUTTER -> "Vulkan pipeline compilation spike"
                    DetectedBottleneck.THERMAL_THROTTLING -> "Device thermal ceiling reached (${String.format("%.1f", simulatedTemp)}°C)"
                    DetectedBottleneck.EXCESSIVE_RESOLUTION -> "High pixel fill rate on current resolution scale"
                    DetectedBottleneck.EXPENSIVE_EFFECTS -> "Heavy post-processing / particle pass load"
                }

                _metrics.value = LivePerformanceMetrics(
                    fps = currentFps,
                    targetFps = targetFps,
                    frameTimeMs = baseFrameTime,
                    frameTimeJitterMs = jitter,
                    cpuUsagePercent = cpuPercent,
                    gpuUsagePercent = gpuPercent,
                    ramUsedMb = 1920 + random.nextInt(200),
                    ramTotalMb = 8192,
                    batteryTempCelsius = simulatedTemp,
                    thermalStatus = if (simulatedTemp > 42.0f) "Throttled" else if (simulatedTemp > 39.0f) "Warm" else "Nominal",
                    shaderPipelineHits = (92 + random.nextInt(7)).coerceIn(80, 100),
                    drawCallsPerFrame = 700 + random.nextInt(300),
                    activeBottleneck = bottleneck,
                    statusText = status
                )

                delay(500)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun detectLiveBottleneck(
        fps: Float,
        targetFps: Int,
        jitter: Float,
        cpu: Int,
        gpu: Int,
        temp: Float
    ): DetectedBottleneck {
        return when {
            temp >= 42.5f -> DetectedBottleneck.THERMAL_THROTTLING
            jitter > 8.0f -> DetectedBottleneck.SHADER_COMPILATION_STUTTER
            fps < (targetFps * 0.75f) && gpu > 90 -> DetectedBottleneck.GPU_BOTTLENECK
            fps < (targetFps * 0.75f) && cpu > 85 -> DetectedBottleneck.CPU_BOTTLENECK
            fps < (targetFps * 0.85f) && gpu > 80 -> DetectedBottleneck.EXCESSIVE_RESOLUTION
            else -> DetectedBottleneck.NONE
        }
    }
}
