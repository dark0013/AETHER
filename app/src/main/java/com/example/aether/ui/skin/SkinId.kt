package com.example.aether.ui.skin

enum class SkinId {
    DEFAULT,
    NEON,
    XP,
    WINAMP,
    GLASS,
    MINIMAL,
    CYBERPUNK;

    companion object {
        fun fromStorage(raw: String): SkinId =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DEFAULT
    }
}
