package com.example.engine.controls

import android.content.Context
import android.util.Log
import com.example.data.model.ControlElement
import com.example.data.model.ControlElementType
import com.example.data.model.GameGenre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class ControlsIntelligenceEngine(
    private val context: Context
) {
    private val profileCache = ConcurrentHashMap<String, GameControlProfile>()

    private val _activeProfile = MutableStateFlow<GameControlProfile?>(null)
    val activeProfile: StateFlow<GameControlProfile?> = _activeProfile.asStateFlow()

    fun getOrGenerateProfile(gameId: String, genre: GameGenre): GameControlProfile {
        return profileCache.getOrPut(gameId) {
            val generated = ControlLayoutGenerator.generateLayoutForGenre(genre).copy(gameId = gameId)
            _activeProfile.value = generated
            generated
        }
    }

    fun updateProfile(gameId: String, profile: GameControlProfile) {
        profileCache[gameId] = profile
        if (_activeProfile.value?.gameId == gameId) {
            _activeProfile.value = profile
        }
    }

    /**
     * AUTO OPTIMIZE CONTROLS:
     * Analyzes screen aspect ratio, display width/height, thumb reach arc,
     * and action frequency to create a pristine mobile-native layout.
     */
    fun autoOptimizeControls(
        gameId: String,
        genre: GameGenre,
        screenWidthPx: Int,
        screenHeightPx: Int,
        densityDpi: Int
    ): GameControlProfile {
        val baseProfile = getOrGenerateProfile(gameId, genre)
        val aspectRatio = max(screenWidthPx, screenHeightPx).toFloat() / min(screenWidthPx, screenHeightPx).toFloat()

        // Thumb reach ergonomics calculations
        // Ultra-wide screens (e.g. 20:9 or 21:9) need buttons shifted further inward from edges
        val edgeInsetX = if (aspectRatio > 2.1f) 0.04f else if (aspectRatio > 1.9f) 0.02f else 0.0f
        val bottomInsetY = 0.02f

        // Reposition and scale joystick for optimal left-thumb arc
        val optimizedJoystick = baseProfile.joystick.copy(
            xPercent = 0.14f + edgeInsetX,
            yPercent = 0.68f - bottomInsetY,
            radiusDp = if (densityDpi > 440) 68f else 62f,
            deadZone = 0.10f
        )

        // Reposition action buttons according to natural right thumb sweep arc
        val optimizedButtons = baseProfile.buttons.map { button ->
            when (button.id) {
                "fire_btn", "attack_btn", "gas_pedal" -> {
                    button.copy(
                        xPercent = 0.88f - edgeInsetX,
                        yPercent = 0.60f - bottomInsetY,
                        sizeDp = 74f
                    )
                }
                "aim_btn", "heavy_attack", "brake_pedal" -> {
                    button.copy(
                        xPercent = 0.76f - edgeInsetX,
                        yPercent = 0.72f - bottomInsetY,
                        sizeDp = 62f
                    )
                }
                "jump_btn", "dodge_btn", "handbrake_btn" -> {
                    button.copy(
                        xPercent = 0.90f - edgeInsetX,
                        yPercent = 0.40f - bottomInsetY,
                        sizeDp = 58f
                    )
                }
                "crouch_btn", "interact_btn", "nitro_btn" -> {
                    button.copy(
                        xPercent = 0.84f - edgeInsetX,
                        yPercent = 0.82f - bottomInsetY,
                        sizeDp = 54f
                    )
                }
                "reload_btn", "skill1_btn" -> {
                    button.copy(
                        xPercent = 0.72f - edgeInsetX,
                        yPercent = 0.54f - bottomInsetY,
                        sizeDp = 50f
                    )
                }
                "sprint_btn" -> {
                    button.copy(
                        xPercent = 0.12f + edgeInsetX,
                        yPercent = 0.38f - bottomInsetY,
                        sizeDp = 52f
                    )
                }
                else -> button
            }
        }

        // Calibrate camera zone
        val optimizedCamera = baseProfile.camera.copy(
            xPercent = 0.50f,
            yPercent = 0.15f,
            widthPercent = 0.48f - edgeInsetX,
            heightPercent = 0.70f,
            acceleration = 1.30f
        )

        val optimizedProfile = baseProfile.copy(
            joystick = optimizedJoystick,
            camera = optimizedCamera,
            buttons = optimizedButtons,
            opacity = 0.80f,
            haptics = true,
            layoutVersion = baseProfile.layoutVersion + 1
        )

        profileCache[gameId] = optimizedProfile
        _activeProfile.value = optimizedProfile

        Log.d(TAG, "Auto-optimized controls layout generated for $gameId (Aspect Ratio: ${String.format("%.2f", aspectRatio)}:1)")
        return optimizedProfile
    }

    companion object {
        private const val TAG = "ControlsIntelEngine"
    }
}
