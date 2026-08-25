package com.example.engine

import com.example.data.model.ControlElement
import com.example.data.model.ControlElementType
import com.example.data.model.ControlLayout
import com.example.data.model.GameGenre
import com.example.engine.controls.ControlLayoutGenerator
import com.example.engine.controls.GameControlProfile
import org.json.JSONArray
import org.json.JSONObject

object ControlEngine {

    fun generateLayoutForGenre(genre: GameGenre): ControlLayout {
        val profile: GameControlProfile = ControlLayoutGenerator.generateLayoutForGenre(genre)

        return ControlLayout(
            id = "layout_${genre.name.lowercase()}",
            preset = genre,
            globalOpacity = profile.opacity,
            globalSensitivity = profile.sensitivity,
            hapticFeedbackEnabled = profile.haptics,
            gyroAimingEnabled = (genre == GameGenre.SHOOTER || genre == GameGenre.TPS),
            elements = profile.buttons
        )
    }

    fun autoOptimizeControlsForErgonomics(layout: ControlLayout): ControlLayout {
        val optimized = layout.elements.map { elem ->
            when (elem.type) {
                ControlElementType.JOYSTICK_LEFT -> {
                    elem.copy(xPercent = 0.16f, yPercent = 0.68f, sizeDp = 136f)
                }
                ControlElementType.BUTTON -> {
                    when (elem.id) {
                        "fire_btn", "attack_btn", "gas_pedal" -> elem.copy(xPercent = 0.86f, yPercent = 0.62f, sizeDp = 72f)
                        "aim_btn", "heavy_attack", "brake_pedal" -> elem.copy(xPercent = 0.74f, yPercent = 0.74f, sizeDp = 60f)
                        "jump_btn", "dodge_btn", "handbrake_btn" -> elem.copy(xPercent = 0.90f, yPercent = 0.42f, sizeDp = 58f)
                        else -> elem
                    }
                }
                ControlElementType.CAMERA_SWIPE_ZONE -> {
                    elem.copy(xPercent = 0.50f, yPercent = 0.15f)
                }
                else -> elem
            }
        }
        return layout.copy(elements = optimized)
    }

    fun serializeToJson(layout: ControlLayout): String {
        return try {
            val root = JSONObject()
            root.put("id", layout.id)
            root.put("preset", layout.preset.name)
            root.put("globalOpacity", layout.globalOpacity.toDouble())
            root.put("globalSensitivity", layout.globalSensitivity.toDouble())
            root.put("hapticFeedbackEnabled", layout.hapticFeedbackEnabled)
            root.put("gyroAimingEnabled", layout.gyroAimingEnabled)

            val array = JSONArray()
            layout.elements.forEach { elem ->
                val obj = JSONObject()
                obj.put("id", elem.id)
                obj.put("label", elem.label)
                obj.put("type", elem.type.name)
                obj.put("mappedKey", elem.mappedKey)
                obj.put("xPercent", elem.xPercent.toDouble())
                obj.put("yPercent", elem.yPercent.toDouble())
                obj.put("sizeDp", elem.sizeDp.toDouble())
                obj.put("opacity", elem.opacity.toDouble())
                obj.put("isToggle", elem.isToggle)
                obj.put("deadZone", elem.deadZone.toDouble())
                obj.put("sensitivity", elem.sensitivity.toDouble())
                elem.customColorHex?.let { obj.put("customColorHex", it) }
                array.put(obj)
            }
            root.put("elements", array)
            root.toString()
        } catch (e: Exception) {
            ""
        }
    }

    fun deserializeFromJson(json: String?, fallbackGenre: GameGenre = GameGenre.DEFAULT): ControlLayout {
        if (json.isNullOrBlank()) {
            return generateLayoutForGenre(fallbackGenre)
        }

        return try {
            val root = JSONObject(json)
            val id = root.optString("id", "layout_${fallbackGenre.name.lowercase()}")
            val presetName = root.optString("preset", fallbackGenre.name)
            val preset = try { GameGenre.valueOf(presetName) } catch (e: Exception) { fallbackGenre }
            val opacity = root.optDouble("globalOpacity", 0.75).toFloat()
            val sens = root.optDouble("globalSensitivity", 1.0).toFloat()
            val haptics = root.optBoolean("hapticFeedbackEnabled", true)
            val gyro = root.optBoolean("gyroAimingEnabled", false)

            val array = root.optJSONArray("elements") ?: JSONArray()
            val elements = mutableListOf<ControlElement>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeName = obj.optString("type", ControlElementType.BUTTON.name)
                val type = try { ControlElementType.valueOf(typeName) } catch (e: Exception) { ControlElementType.BUTTON }

                elements.add(
                    ControlElement(
                        id = obj.optString("id", "elem_$i"),
                        label = obj.optString("label", "BTN"),
                        type = type,
                        mappedKey = obj.optString("mappedKey", "SPACE"),
                        xPercent = obj.optDouble("xPercent", 0.5).toFloat(),
                        yPercent = obj.optDouble("yPercent", 0.5).toFloat(),
                        sizeDp = obj.optDouble("sizeDp", 56.0).toFloat(),
                        opacity = obj.optDouble("opacity", 0.75).toFloat(),
                        isToggle = obj.optBoolean("isToggle", false),
                        deadZone = obj.optDouble("deadZone", 0.15).toFloat(),
                        sensitivity = obj.optDouble("sensitivity", 1.0).toFloat(),
                        customColorHex = if (obj.has("customColorHex")) obj.optLong("customColorHex") else null
                    )
                )
            }

            if (elements.isEmpty()) {
                generateLayoutForGenre(preset)
            } else {
                ControlLayout(
                    id = id,
                    preset = preset,
                    globalOpacity = opacity,
                    globalSensitivity = sens,
                    hapticFeedbackEnabled = haptics,
                    gyroAimingEnabled = gyro,
                    elements = elements
                )
            }
        } catch (e: Exception) {
            generateLayoutForGenre(fallbackGenre)
        }
    }
}
