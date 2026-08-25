package com.example.engine.controls

import com.example.data.model.ControlElement
import com.example.data.model.ControlElementType
import com.example.data.model.GameGenre

enum class ResponseCurveType {
    LINEAR,
    EXPONENTIAL,
    S_CURVE,
    AGGRESSIVE
}

data class JoystickConfig(
    val id: String = "primary_joystick",
    val label: String = "Movement",
    val isDynamicFloating: Boolean = false,
    val xPercent: Float = 0.16f,
    val yPercent: Float = 0.68f,
    val radiusDp: Float = 65f,
    val deadZone: Float = 0.12f,
    val responseCurve: ResponseCurveType = ResponseCurveType.S_CURVE,
    val mappedKeys: List<String> = listOf("W", "A", "S", "D")
)

data class CameraTouchZoneConfig(
    val id: String = "camera_swipe_zone",
    val xPercent: Float = 0.60f,
    val yPercent: Float = 0.20f,
    val widthPercent: Float = 0.38f,
    val heightPercent: Float = 0.70f,
    val sensitivityX: Float = 1.0f,
    val sensitivityY: Float = 0.85f,
    val acceleration: Float = 1.25f,
    val friction: Float = 0.90f,
    val invertY: Boolean = false
)

data class GameControlProfile(
    val gameId: String,
    val orientation: String = "Landscape",
    val joystick: JoystickConfig = JoystickConfig(),
    val camera: CameraTouchZoneConfig = CameraTouchZoneConfig(),
    val buttons: List<ControlElement> = emptyList(),
    val mappings: Map<String, String> = emptyMap(),
    val sensitivity: Float = 1.0f,
    val deadZones: Map<String, Float> = mapOf("joystick" to 0.12f, "camera" to 0.05f),
    val responseCurves: Map<String, String> = mapOf("joystick" to "S_CURVE", "camera" to "EXPONENTIAL"),
    val opacity: Float = 0.75f,
    val buttonSizes: Map<String, Float> = emptyMap(),
    val haptics: Boolean = true,
    val autoHideWhenInactive: Boolean = false,
    val layoutVersion: Int = 2
)
