package com.example.aether.ui.skin

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinEngineTest {

    @Test
    fun sevenBuiltinSkinsMatchEnum() {
        assertEquals(7, BuiltinSkins.all.size)
        assertEquals(SkinId.entries.toSet(), BuiltinSkins.all.map { it.id }.toSet())
    }

    @Test
    fun resolveReturnsTheRequestedSkin() {
        SkinId.entries.forEach { id ->
            assertEquals(id, SkinEngine.resolve(id).id)
        }
    }

    @Test
    fun fromStorageIsCaseInsensitiveAndFallsBack() {
        assertEquals(SkinId.NEON, SkinId.fromStorage("neon"))
        assertEquals(SkinId.CYBERPUNK, SkinId.fromStorage("Cyberpunk"))
        assertEquals(SkinId.DEFAULT, SkinId.fromStorage(""))
        assertEquals(SkinId.DEFAULT, SkinId.fromStorage("unknown"))
    }

    @Test
    fun palettesMatchMockup() {
        assertEquals(Color(0xFFC4B5FD), BuiltinSkins.Default.colors.primary)
        assertEquals(Color(0xFF121212), BuiltinSkins.Default.colors.background)
        assertEquals(Color(0xFFB85FFF), BuiltinSkins.Neon.colors.primary)
        assertEquals(Color(0xFFFF80AA), BuiltinSkins.Neon.colors.secondary)
        assertEquals(Color(0xFF5BA3E8), BuiltinSkins.Xp.background.top)
        assertEquals(Color(0xFFB8D4F0), BuiltinSkins.Xp.background.bottom)
        assertEquals(Color(0xFF39FF14), BuiltinSkins.Winamp.colors.primary)
        assertEquals(Color(0xFF7CFF00), BuiltinSkins.Winamp.colors.secondary)
        assertEquals(Color(0xFF000000), BuiltinSkins.Minimal.colors.background)
        assertEquals(Color(0xFF00F0FF), BuiltinSkins.Cyberpunk.colors.primary)
        assertEquals(Color(0xFFFF2BD6), BuiltinSkins.Cyberpunk.colors.secondary)
    }

    @Test
    fun visualizerStylesMatchMockup() {
        assertEquals(VisualizerStyle.BARS, BuiltinSkins.Default.visualizer)
        assertEquals(VisualizerStyle.BARS, BuiltinSkins.Neon.visualizer)
        assertEquals(VisualizerStyle.BARS, BuiltinSkins.Xp.visualizer)
        assertEquals(VisualizerStyle.CLASSIC_WINAMP, BuiltinSkins.Winamp.visualizer)
        assertEquals(VisualizerStyle.WAVE, BuiltinSkins.Glass.visualizer)
        assertEquals(VisualizerStyle.NONE, BuiltinSkins.Minimal.visualizer)
        assertEquals(VisualizerStyle.BARS, BuiltinSkins.Cyberpunk.visualizer)
        assertTrue(BuiltinSkins.Winamp.cardRadius.value < 4f)
        assertEquals(18f, BuiltinSkins.Neon.buttonRadius.value)
        assertEquals(24f, BuiltinSkins.Neon.cardRadius.value)
        assertTrue(BuiltinSkins.Cyberpunk.showScanlines)
        assertTrue(BuiltinSkins.Winamp.showWinampChrome)
        assertTrue(BuiltinSkins.Glass.glassmorphism)
        assertTrue(BuiltinSkins.Neon.visualizerGlow)
        assertFalse(BuiltinSkins.Minimal.showMountains)
    }

    @Test
    fun typographyNamesMatchSpec() {
        assertEquals("Space Grotesk", BuiltinSkins.Default.fontName)
        assertEquals("Space Grotesk", BuiltinSkins.Neon.fontName)
        assertEquals("Roboto", BuiltinSkins.Xp.fontName)
        assertEquals("Inter", BuiltinSkins.Glass.fontName)
        assertEquals("Inter", BuiltinSkins.Minimal.fontName)
        assertEquals("Orbitron", BuiltinSkins.Cyberpunk.fontName)
    }

    @Test
    fun neonJsonFromMockup() {
        val neon = BuiltinSkins.Neon
        assertEquals(PlayButtonAnim.PULSE, neon.playButton)
        assertEquals(ScreenTransition.FADE, neon.screenTransition)
        assertEquals(SkinBackgroundKind.VERTICAL_GRADIENT, neon.background.kind)
    }
}
