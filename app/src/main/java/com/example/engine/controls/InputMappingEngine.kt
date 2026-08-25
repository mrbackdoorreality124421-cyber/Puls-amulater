package com.example.engine.controls

data class PcInputEvent(
    val scancode: Int,
    val isPressed: Boolean,
    val mouseDeltaX: Int = 0,
    val mouseDeltaY: Int = 0,
    val isLeftClick: Boolean = false,
    val isRightClick: Boolean = false,
    val actionName: String
)

object InputMappingEngine {

    // Virtual key to DirectInput / PC Key Code mapping
    private val KEY_MAP = mapOf(
        "W" to 0x11,
        "A" to 0x1E,
        "S" to 0x1F,
        "D" to 0x20,
        "SPACE" to 0x39,
        "CTRL" to 0x1D,
        "SHIFT" to 0x2A,
        "R" to 0x13,
        "E" to 0x12,
        "F" to 0x21,
        "Q" to 0x10,
        "C" to 0x2E,
        "Z" to 0x2C,
        "X" to 0x2D,
        "I" to 0x17,
        "M" to 0x32,
        "TAB" to 0x0F,
        "ESC" to 0x01,
        "ENTER" to 0x1C,
        "1" to 0x02,
        "2" to 0x03,
        "3" to 0x04,
        "4" to 0x05,
        "U" to 0x16,
        "J" to 0x24,
        "K" to 0x25,
        "L" to 0x26,
        "O" to 0x18
    )

    fun translateKey(key: String, isPressed: Boolean): PcInputEvent {
        val scancode = KEY_MAP[key.uppercase()] ?: 0x00
        val isLeft = key.equals("LCLICK", ignoreCase = true)
        val isRight = key.equals("RCLICK", ignoreCase = true)

        return PcInputEvent(
            scancode = scancode,
            isPressed = isPressed,
            isLeftClick = isLeft && isPressed,
            isRightClick = isRight && isPressed,
            actionName = key
        )
    }

    fun translateJoystickToDirectionalKeys(normX: Float, normY: Float): List<String> {
        val activeKeys = mutableListOf<String>()
        val threshold = 0.35f

        if (normY < -threshold) activeKeys.add("W")
        if (normY > threshold) activeKeys.add("S")
        if (normX < -threshold) activeKeys.add("A")
        if (normX > threshold) activeKeys.add("D")

        return activeKeys
    }
}
