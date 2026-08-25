package com.example.engine.controls

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class TouchPointerState(
    val pointerId: Int,
    val x: Float,
    val y: Float,
    val activeElementId: String? = null
)

data class VirtualJoystickState(
    val normalizedX: Float = 0f, // -1.0 to +1.0
    val normalizedY: Float = 0f, // -1.0 to +1.0
    val angleDegrees: Float = 0f,
    val magnitude: Float = 0f, // 0.0 to 1.0
    val isActive: Boolean = false
)

data class CameraLookDelta(
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val isSwiping: Boolean = false
)

class TouchInputManager(
    private val context: Context
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _joystickState = MutableStateFlow(VirtualJoystickState())
    val joystickState: StateFlow<VirtualJoystickState> = _joystickState.asStateFlow()

    private val _cameraLookDelta = MutableStateFlow(CameraLookDelta())
    val cameraLookDelta: StateFlow<CameraLookDelta> = _cameraLookDelta.asStateFlow()

    private val _activePressedKeys = MutableStateFlow<Set<String>>(emptySet())
    val activePressedKeys: StateFlow<Set<String>> = _activePressedKeys.asStateFlow()

    fun handleJoystickInput(
        touchX: Float,
        touchY: Float,
        centerX: Float,
        centerY: Float,
        radiusPx: Float,
        deadZoneFraction: Float = 0.12f,
        curve: ResponseCurveType = ResponseCurveType.S_CURVE
    ) {
        val dx = touchX - centerX
        val dy = touchY - centerY
        val dist = sqrt(dx * dx + dy * dy)
        val rawMagnitude = (dist / radiusPx).coerceIn(0f, 1f)

        if (rawMagnitude < deadZoneFraction) {
            _joystickState.value = VirtualJoystickState(isActive = true)
            return
        }

        // Apply deadzone rescaling: (raw - deadZone) / (1 - deadZone)
        val scaledMagnitude = ((rawMagnitude - deadZoneFraction) / (1f - deadZoneFraction)).coerceIn(0f, 1f)

        // Apply response curve
        val finalMagnitude = when (curve) {
            ResponseCurveType.LINEAR -> scaledMagnitude
            ResponseCurveType.EXPONENTIAL -> scaledMagnitude.pow(2.0f)
            ResponseCurveType.S_CURVE -> {
                // Smooth cubic S-curve
                3 * scaledMagnitude.pow(2f) - 2 * scaledMagnitude.pow(3f)
            }
            ResponseCurveType.AGGRESSIVE -> sqrt(scaledMagnitude)
        }

        val angle = atan2(dy, dx)
        val normX = (cos(angle) * finalMagnitude).coerceIn(-1f, 1f)
        val normY = (sin(angle) * finalMagnitude).coerceIn(-1f, 1f)
        val deg = Math.toDegrees(angle.toDouble()).toFloat()

        _joystickState.value = VirtualJoystickState(
            normalizedX = normX,
            normalizedY = normY,
            angleDegrees = deg,
            magnitude = finalMagnitude,
            isActive = true
        )
    }

    fun releaseJoystick() {
        _joystickState.value = VirtualJoystickState(isActive = false)
    }

    fun handleCameraSwipe(
        deltaX: Float,
        deltaY: Float,
        sensitivityX: Float = 1.0f,
        sensitivityY: Float = 0.85f,
        acceleration: Float = 1.25f
    ) {
        val speed = sqrt(deltaX * deltaX + deltaY * deltaY)
        val accelFactor = if (speed > 25f) acceleration else 1.0f

        val finalDx = deltaX * sensitivityX * accelFactor
        val finalDy = deltaY * sensitivityY * accelFactor

        _cameraLookDelta.value = CameraLookDelta(
            deltaX = finalDx,
            deltaY = finalDy,
            isSwiping = true
        )
    }

    fun releaseCameraSwipe() {
        _cameraLookDelta.value = CameraLookDelta(isSwiping = false)
    }

    fun onButtonDown(key: String, hapticsEnabled: Boolean = true) {
        val current = _activePressedKeys.value.toMutableSet()
        current.add(key)
        _activePressedKeys.value = current

        if (hapticsEnabled) {
            triggerLightHaptic()
        }
    }

    fun onButtonUp(key: String) {
        val current = _activePressedKeys.value.toMutableSet()
        current.remove(key)
        _activePressedKeys.value = current
    }

    fun triggerLightHaptic() {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(15)
                }
            }
        } catch (e: Exception) {
            // Non-critical vibration fallback
        }
    }
}
