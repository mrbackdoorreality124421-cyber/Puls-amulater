package com.example.data.model

enum class ControlElementType {
    JOYSTICK_LEFT,
    JOYSTICK_RIGHT,
    DPAD,
    BUTTON,
    TRIGGER_BUTTON,
    MOUSE_TRACKPAD,
    CAMERA_SWIPE_ZONE,
    KEYBOARD_KEY
}

data class ControlElement(
    val id: String,
    val label: String,
    val type: ControlElementType,
    val mappedKey: String, // e.g. "W", "A", "S", "D", "SPACE", "LCLICK", "RCLICK", "GAMEPAD_A", "ESC", "SHIFT"
    val xPercent: Float, // 0.0 to 1.0 (relative to screen width)
    val yPercent: Float, // 0.0 to 1.0 (relative to screen height)
    val sizeDp: Float = 56f, // element diameter / dimension
    val opacity: Float = 0.75f,
    val isToggle: Boolean = false,
    val deadZone: Float = 0.15f,
    val sensitivity: Float = 1.0f,
    val customColorHex: Long? = null
)

data class ControlLayout(
    val id: String = "default_layout",
    val preset: GameGenre = GameGenre.DEFAULT,
    val globalOpacity: Float = 0.75f,
    val globalSensitivity: Float = 1.0f,
    val hapticFeedbackEnabled: Boolean = true,
    val gyroAimingEnabled: Boolean = false,
    val elements: List<ControlElement> = emptyList()
)
