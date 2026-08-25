package com.example.data.model

enum class GameGenre(val displayName: String, val iconName: String) {
    SHOOTER("FPS / First-Person Shooter", "crosshairs"),
    TPS("TPS / Third-Person Action", "target"),
    RACING("Racing / Driving", "steering_wheel"),
    FIGHTING("Fighting / Brawler", "boxing_glove"),
    RPG("RPG / Adventure", "shield"),
    STRATEGY("Strategy / RTS / City", "chess"),
    PLATFORMER("Platformer / 2D Arcade", "gamepad"),
    DEFAULT("General PC Game", "mouse_keyboard");

    companion object {
        fun fromString(str: String?): GameGenre {
            return entries.firstOrNull { it.name.equals(str, ignoreCase = true) || it.displayName.contains(str ?: "", ignoreCase = true) }
                ?: DEFAULT
        }
    }
}
