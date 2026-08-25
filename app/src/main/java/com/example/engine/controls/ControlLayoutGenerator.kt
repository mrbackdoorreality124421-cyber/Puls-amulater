package com.example.engine.controls

import com.example.data.model.ControlElement
import com.example.data.model.ControlElementType
import com.example.data.model.GameGenre

object ControlLayoutGenerator {

    fun generateLayoutForGenre(genre: GameGenre): GameControlProfile {
        return when (genre) {
            GameGenre.SHOOTER -> createShooterProfile()
            GameGenre.TPS -> createTpsProfile()
            GameGenre.RACING -> createRacingProfile()
            GameGenre.RPG -> createRpgProfile()
            GameGenre.FIGHTING -> createFightingProfile()
            GameGenre.PLATFORMER -> createPlatformerProfile()
            GameGenre.STRATEGY -> createStrategyProfile()
            GameGenre.DEFAULT -> createShooterProfile()
        }
    }

    private fun createShooterProfile(): GameControlProfile {
        val buttons = listOf(
            // Fire button (Primary Trigger - Large, Right thumb sweet spot)
            ControlElement(
                id = "fire_btn",
                label = "FIRE",
                type = ControlElementType.BUTTON,
                mappedKey = "LCLICK",
                xPercent = 0.88f,
                yPercent = 0.60f,
                sizeDp = 72f,
                customColorHex = 0xFFFF1744
            ),
            // Aim Zoom (ADS)
            ControlElement(
                id = "aim_btn",
                label = "AIM",
                type = ControlElementType.BUTTON,
                mappedKey = "RCLICK",
                xPercent = 0.76f,
                yPercent = 0.72f,
                sizeDp = 62f,
                customColorHex = 0xFF2979FF
            ),
            // Jump
            ControlElement(
                id = "jump_btn",
                label = "JUMP",
                type = ControlElementType.BUTTON,
                mappedKey = "SPACE",
                xPercent = 0.91f,
                yPercent = 0.40f,
                sizeDp = 56f,
                customColorHex = 0xFF00E676
            ),
            // Crouch / Slide
            ControlElement(
                id = "crouch_btn",
                label = "CROUCH",
                type = ControlElementType.BUTTON,
                mappedKey = "CTRL",
                xPercent = 0.86f,
                yPercent = 0.82f,
                sizeDp = 52f
            ),
            // Reload
            ControlElement(
                id = "reload_btn",
                label = "RELOAD",
                type = ControlElementType.BUTTON,
                mappedKey = "R",
                xPercent = 0.72f,
                yPercent = 0.54f,
                sizeDp = 50f
            ),
            // Sprint Toggle
            ControlElement(
                id = "sprint_btn",
                label = "SPRINT",
                type = ControlElementType.BUTTON,
                mappedKey = "SHIFT",
                xPercent = 0.12f,
                yPercent = 0.38f,
                sizeDp = 52f,
                isToggle = true
            ),
            // Interact / Use
            ControlElement(
                id = "interact_btn",
                label = "USE / E",
                type = ControlElementType.BUTTON,
                mappedKey = "E",
                xPercent = 0.80f,
                yPercent = 0.38f,
                sizeDp = 48f
            ),
            // Pause / Menu
            ControlElement(
                id = "pause_btn",
                label = "ESC",
                type = ControlElementType.KEYBOARD_KEY,
                mappedKey = "ESC",
                xPercent = 0.05f,
                yPercent = 0.08f,
                sizeDp = 44f
            ),
            // Map
            ControlElement(
                id = "map_btn",
                label = "MAP",
                type = ControlElementType.KEYBOARD_KEY,
                mappedKey = "TAB",
                xPercent = 0.93f,
                yPercent = 0.08f,
                sizeDp = 44f
            )
        )

        return GameControlProfile(
            gameId = "fps_shooter",
            orientation = "Landscape",
            joystick = JoystickConfig(
                id = "left_stick",
                label = "Movement (WASD)",
                isDynamicFloating = false,
                xPercent = 0.16f,
                yPercent = 0.68f,
                radiusDp = 65f,
                deadZone = 0.12f,
                responseCurve = ResponseCurveType.S_CURVE
            ),
            camera = CameraTouchZoneConfig(
                id = "aim_camera_zone",
                xPercent = 0.55f,
                yPercent = 0.15f,
                widthPercent = 0.42f,
                heightPercent = 0.70f,
                sensitivityX = 1.0f,
                sensitivityY = 0.85f,
                acceleration = 1.3f
            ),
            buttons = buttons,
            mappings = mapOf(
                "LCLICK" to "Shoot / Attack",
                "RCLICK" to "Aim Down Sights",
                "WASD" to "Move",
                "SPACE" to "Jump",
                "CTRL" to "Crouch",
                "R" to "Reload",
                "SHIFT" to "Sprint",
                "E" to "Interact"
            ),
            sensitivity = 1.0f,
            opacity = 0.78f,
            haptics = true
        )
    }

    private fun createTpsProfile(): GameControlProfile {
        val shooter = createShooterProfile()
        val tpsButtons = shooter.buttons.map { btn ->
            if (btn.id == "jump_btn") {
                btn.copy(label = "ROLL / DODGE", mappedKey = "SPACE")
            } else btn
        }
        return shooter.copy(
            gameId = "tps_action",
            buttons = tpsButtons
        )
    }

    private fun createRacingProfile(): GameControlProfile {
        val buttons = listOf(
            ControlElement(
                id = "gas_pedal",
                label = "ACCEL",
                type = ControlElementType.TRIGGER_BUTTON,
                mappedKey = "W",
                xPercent = 0.88f,
                yPercent = 0.65f,
                sizeDp = 86f,
                customColorHex = 0xFF00E676
            ),
            ControlElement(
                id = "brake_pedal",
                label = "BRAKE",
                type = ControlElementType.TRIGGER_BUTTON,
                mappedKey = "S",
                xPercent = 0.74f,
                yPercent = 0.75f,
                sizeDp = 76f,
                customColorHex = 0xFFFF1744
            ),
            ControlElement(
                id = "handbrake_btn",
                label = "E-BRAKE",
                type = ControlElementType.BUTTON,
                mappedKey = "SPACE",
                xPercent = 0.90f,
                yPercent = 0.38f,
                sizeDp = 60f,
                customColorHex = 0xFFFF9100
            ),
            ControlElement(
                id = "nitro_btn",
                label = "NITRO",
                type = ControlElementType.BUTTON,
                mappedKey = "SHIFT",
                xPercent = 0.75f,
                yPercent = 0.50f,
                sizeDp = 58f,
                customColorHex = 0xFF00E5FF
            ),
            ControlElement(
                id = "steer_left",
                label = "◀ STEER",
                type = ControlElementType.BUTTON,
                mappedKey = "A",
                xPercent = 0.12f,
                yPercent = 0.70f,
                sizeDp = 80f
            ),
            ControlElement(
                id = "steer_right",
                label = "STEER ▶",
                type = ControlElementType.BUTTON,
                mappedKey = "D",
                xPercent = 0.28f,
                yPercent = 0.70f,
                sizeDp = 80f
            ),
            ControlElement(
                id = "camera_btn",
                label = "CAM",
                type = ControlElementType.BUTTON,
                mappedKey = "C",
                xPercent = 0.50f,
                yPercent = 0.10f,
                sizeDp = 48f
            ),
            ControlElement(
                id = "pause_btn",
                label = "ESC",
                type = ControlElementType.KEYBOARD_KEY,
                mappedKey = "ESC",
                xPercent = 0.05f,
                yPercent = 0.08f,
                sizeDp = 44f
            )
        )

        return GameControlProfile(
            gameId = "racing_game",
            orientation = "Landscape",
            joystick = JoystickConfig(
                id = "steering_stick",
                label = "Analog Steering",
                xPercent = 0.18f,
                yPercent = 0.70f,
                radiusDp = 60f,
                deadZone = 0.08f,
                responseCurve = ResponseCurveType.LINEAR,
                mappedKeys = listOf("W", "A", "S", "D")
            ),
            camera = CameraTouchZoneConfig(xPercent = 0.40f, yPercent = 0.20f, widthPercent = 0.20f, heightPercent = 0.30f),
            buttons = buttons,
            mappings = mapOf(
                "W" to "Accelerate",
                "S" to "Brake / Reverse",
                "A" to "Steer Left",
                "D" to "Steer Right",
                "SPACE" to "Handbrake",
                "SHIFT" to "Nitro Boost",
                "C" to "Change Camera"
            ),
            sensitivity = 1.0f,
            opacity = 0.80f,
            haptics = true
        )
    }

    private fun createRpgProfile(): GameControlProfile {
        val buttons = listOf(
            ControlElement("attack_btn", "ATTACK", ControlElementType.BUTTON, "LCLICK", 0.88f, 0.62f, 72f, customColorHex = 0xFFFF5252),
            ControlElement("heavy_attack", "HEAVY", ControlElementType.BUTTON, "RCLICK", 0.76f, 0.74f, 60f, customColorHex = 0xFFFF9100),
            ControlElement("dodge_btn", "DODGE", ControlElementType.BUTTON, "SPACE", 0.88f, 0.84f, 56f, customColorHex = 0xFF00E676),
            ControlElement("interact_btn", "TALK/E", ControlElementType.BUTTON, "E", 0.75f, 0.58f, 52f),
            ControlElement("skill1_btn", "SKILL 1", ControlElementType.BUTTON, "1", 0.72f, 0.40f, 48f, customColorHex = 0xFF7C4DFF),
            ControlElement("skill2_btn", "SKILL 2", ControlElementType.BUTTON, "2", 0.82f, 0.34f, 48f, customColorHex = 0xFF00E5FF),
            ControlElement("skill3_btn", "ULT", ControlElementType.BUTTON, "Q", 0.92f, 0.34f, 50f, customColorHex = 0xFFFFD600),
            ControlElement("inventory_btn", "BAG", ControlElementType.KEYBOARD_KEY, "I", 0.92f, 0.08f, 44f),
            ControlElement("pause_btn", "ESC", ControlElementType.KEYBOARD_KEY, "ESC", 0.05f, 0.08f, 44f)
        )

        return GameControlProfile(
            gameId = "rpg_adventure",
            orientation = "Landscape",
            joystick = JoystickConfig(
                id = "rpg_movement",
                label = "Movement",
                xPercent = 0.16f,
                yPercent = 0.68f,
                radiusDp = 65f,
                responseCurve = ResponseCurveType.S_CURVE
            ),
            camera = CameraTouchZoneConfig(xPercent = 0.55f, yPercent = 0.15f, widthPercent = 0.40f, heightPercent = 0.65f),
            buttons = buttons,
            mappings = mapOf(
                "LCLICK" to "Primary Attack",
                "RCLICK" to "Special Attack",
                "SPACE" to "Dodge / Dash",
                "E" to "Interact / Loot",
                "1" to "Skill 1",
                "2" to "Skill 2",
                "Q" to "Ultimate Skill",
                "I" to "Inventory"
            ),
            opacity = 0.76f,
            haptics = true
        )
    }

    private fun createFightingProfile(): GameControlProfile {
        val buttons = listOf(
            ControlElement("btn_lp", "LP", ControlElementType.BUTTON, "U", 0.74f, 0.48f, 58f),
            ControlElement("btn_hp", "HP", ControlElementType.BUTTON, "I", 0.84f, 0.42f, 58f),
            ControlElement("btn_special", "EX", ControlElementType.BUTTON, "O", 0.94f, 0.40f, 58f, customColorHex = 0xFFFFD600),
            ControlElement("btn_lk", "LK", ControlElementType.BUTTON, "J", 0.74f, 0.68f, 58f),
            ControlElement("btn_hk", "HK", ControlElementType.BUTTON, "K", 0.84f, 0.62f, 58f),
            ControlElement("btn_block", "BLOCK", ControlElementType.BUTTON, "L", 0.94f, 0.60f, 58f, customColorHex = 0xFF2979FF),
            ControlElement("start_btn", "START", ControlElementType.KEYBOARD_KEY, "ENTER", 0.55f, 0.08f, 46f),
            ControlElement("select_btn", "SELECT", ControlElementType.KEYBOARD_KEY, "ESC", 0.45f, 0.08f, 46f)
        )

        return GameControlProfile(
            gameId = "fighting_game",
            orientation = "Landscape",
            joystick = JoystickConfig(
                id = "dpad_stick",
                label = "8-Way Arcade Stick",
                xPercent = 0.16f,
                yPercent = 0.68f,
                radiusDp = 70f,
                responseCurve = ResponseCurveType.AGGRESSIVE
            ),
            buttons = buttons,
            opacity = 0.82f,
            haptics = true
        )
    }

    private fun createPlatformerProfile(): GameControlProfile {
        val buttons = listOf(
            ControlElement("jump_btn", "JUMP", ControlElementType.BUTTON, "SPACE", 0.90f, 0.65f, 72f, customColorHex = 0xFF00E676),
            ControlElement("action1_btn", "ATTACK", ControlElementType.BUTTON, "Z", 0.78f, 0.75f, 62f, customColorHex = 0xFFFF1744),
            ControlElement("dash_btn", "DASH", ControlElementType.BUTTON, "SHIFT", 0.88f, 0.45f, 54f, customColorHex = 0xFF00E5FF),
            ControlElement("pause_btn", "PAUSE", ControlElementType.KEYBOARD_KEY, "ESC", 0.05f, 0.08f, 44f)
        )

        return GameControlProfile(
            gameId = "platformer_game",
            orientation = "Landscape",
            joystick = JoystickConfig(
                id = "platformer_stick",
                label = "D-Pad / Stick",
                xPercent = 0.16f,
                yPercent = 0.68f,
                radiusDp = 65f,
                deadZone = 0.10f
            ),
            buttons = buttons,
            opacity = 0.78f,
            haptics = true
        )
    }

    private fun createStrategyProfile(): GameControlProfile {
        val buttons = listOf(
            ControlElement("lclick_btn", "L-CLICK / SELECT", ControlElementType.BUTTON, "LCLICK", 0.88f, 0.70f, 68f, customColorHex = 0xFF2979FF),
            ControlElement("rclick_btn", "R-CLICK / COMMAND", ControlElementType.BUTTON, "RCLICK", 0.76f, 0.78f, 62f, customColorHex = 0xFFFF9100),
            ControlElement("box_select", "DRAG SELECT", ControlElementType.BUTTON, "SHIFT", 0.88f, 0.50f, 54f, isToggle = true),
            ControlElement("pause_game", "PAUSE / SPACE", ControlElementType.BUTTON, "SPACE", 0.08f, 0.38f, 50f),
            ControlElement("menu_btn", "MENU", ControlElementType.KEYBOARD_KEY, "ESC", 0.05f, 0.08f, 44f)
        )

        return GameControlProfile(
            gameId = "rts_strategy",
            orientation = "Landscape",
            joystick = JoystickConfig(
                id = "pan_map_stick",
                label = "Pan Map",
                xPercent = 0.14f,
                yPercent = 0.70f,
                radiusDp = 55f
            ),
            camera = CameraTouchZoneConfig(
                id = "trackpad_zone",
                xPercent = 0.35f,
                yPercent = 0.20f,
                widthPercent = 0.40f,
                heightPercent = 0.65f,
                sensitivityX = 1.1f,
                sensitivityY = 1.1f
            ),
            buttons = buttons,
            opacity = 0.70f,
            haptics = true
        )
    }
}
