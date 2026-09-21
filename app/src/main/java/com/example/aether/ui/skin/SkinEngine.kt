package com.example.aether.ui.skin

object SkinEngine {
    fun resolve(id: SkinId): Skin = when (id) {
        SkinId.DEFAULT -> BuiltinSkins.Default
        SkinId.NEON -> BuiltinSkins.Neon
        SkinId.XP -> BuiltinSkins.Xp
        SkinId.WINAMP -> BuiltinSkins.Winamp
        SkinId.GLASS -> BuiltinSkins.Glass
        SkinId.MINIMAL -> BuiltinSkins.Minimal
        SkinId.CYBERPUNK -> BuiltinSkins.Cyberpunk
    }
}
