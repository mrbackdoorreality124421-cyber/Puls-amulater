package com.example.engine

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.example.data.model.PerformanceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThermalEvent(
    val thermalStatus: Int,
    val statusText: String,
    val requiresThrottle: Boolean,
    val suggestedFallbackProfile: PerformanceProfile?,
    val timestamp: Long = System.currentTimeMillis()
)

class ThermalProtectionManager(private val context: Context) {

    private val _thermalState = MutableStateFlow(
        ThermalEvent(
            thermalStatus = 0,
            statusText = "Nominal",
            requiresThrottle = false,
            suggestedFallbackProfile = null
        )
    )
    val thermalState: StateFlow<ThermalEvent> = _thermalState.asStateFlow()

    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
    var autoThrottleEnabled: Boolean = true

    fun startMonitoring(currentProfile: PerformanceProfile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return

            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                handleThermalStatus(status, currentProfile)
            }
            thermalListener?.let { pm.addThermalStatusListener(it) }

            // Initial check
            handleThermalStatus(pm.currentThermalStatus, currentProfile)
        }
    }

    private fun handleThermalStatus(status: Int, currentProfile: PerformanceProfile) {
        val (text, requiresThrottle, fallback) = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> Triple("Cool & Nominal", false, null)
            PowerManager.THERMAL_STATUS_LIGHT -> Triple("Light Warmth", false, null)
            PowerManager.THERMAL_STATUS_MODERATE -> {
                val fallback = if (currentProfile.tierRating >= 5) PerformanceProfile.BALANCE else null
                Triple("Moderate Heat", fallback != null, fallback)
            }
            PowerManager.THERMAL_STATUS_SEVERE -> {
                val fallback = when {
                    currentProfile.tierRating >= 4 -> PerformanceProfile.SMOOTH
                    else -> PerformanceProfile.SUPER_SMOOTH
                }
                Triple("Severe Throttling", true, fallback)
            }
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                Triple("Critical Thermal Limit", true, PerformanceProfile.SUPER_SMOOTH)
            }
            else -> Triple("Normal", false, null)
        }

        _thermalState.value = ThermalEvent(
            thermalStatus = status,
            statusText = text,
            requiresThrottle = requiresThrottle,
            suggestedFallbackProfile = if (autoThrottleEnabled) fallback else null
        )
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener != null) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            thermalListener?.let { pm?.removeThermalStatusListener(it) }
            thermalListener = null
        }
    }
}
