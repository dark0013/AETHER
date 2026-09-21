package com.example.aether.ui.skin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.aether.ui.RealtimeAudioFeatures
import com.example.aether.ui.theme.AETHERTheme

@Preview(widthDp = 220, heightDp = 400, name = "Default")
@Composable
private fun PreviewDefault() = SkinPreview(BuiltinSkins.Default)

@Preview(widthDp = 220, heightDp = 400, name = "Neon")
@Composable
private fun PreviewNeon() = SkinPreview(BuiltinSkins.Neon)

@Preview(widthDp = 220, heightDp = 400, name = "XP")
@Composable
private fun PreviewXp() = SkinPreview(BuiltinSkins.Xp)

@Preview(widthDp = 220, heightDp = 400, name = "Winamp")
@Composable
private fun PreviewWinamp() = SkinPreview(BuiltinSkins.Winamp)

@Preview(widthDp = 220, heightDp = 400, name = "Glass")
@Composable
private fun PreviewGlass() = SkinPreview(BuiltinSkins.Glass)

@Preview(widthDp = 220, heightDp = 400, name = "Minimal")
@Composable
private fun PreviewMinimal() = SkinPreview(BuiltinSkins.Minimal)

@Preview(widthDp = 220, heightDp = 400, name = "Cyberpunk")
@Composable
private fun PreviewCyberpunk() = SkinPreview(BuiltinSkins.Cyberpunk)

@Composable
private fun SkinPreview(skin: Skin) {
    CompositionLocalProvider(LocalSkin provides skin) {
        AETHERTheme(skin) {
            Box(Modifier.fillMaxSize()) {
                SkinVisualizer(
                    features = RealtimeAudioFeatures(energy = 0.55f, flux = 0.3f),
                    isPlaying = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
