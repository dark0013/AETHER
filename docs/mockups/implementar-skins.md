Implementa el sistema de Skins de AETHER según la maqueta de producto.

CONTEXTO DEL REPO (NO ROMPER)
- App Android Kotlin + Jetpack Compose + Material 3 + Media3.
- Tema actual forzado en app/src/main/java/com/example/aether/ui/theme/Theme.kt
  (Lavender #C4B5FD, Background #121212).
- Settings actuales en app/src/main/java/com/example/aether/ui/AetherSettings.kt
  SOLO importan carpeta/archivos. Extiéndelo, no lo elimines.
- UI principal: PresenceScreen, MusicScreens, MusicViewModel, MainActivity.
- Gestos y Playback Engine ya existen. Las skins NO cambian gestos ni Media3.
  Solo cambian apariencia: colores, tipografía, fondos, formas, visualizador,
  iconos, animaciones.

OBJETIVO
1. Motor de skins (Skin Engine) separado del Gesture Engine y Playback Engine.
2. 7 skins de ejemplo de la maqueta, seleccionables.
3. Pantalla Configuración → Apariencia → elegir skin, con persistencia.
4. Cambio inmediato en toda la UI (Presence, lista, mini player, Now Playing,
   settings, playlists) sin reiniciar la Activity si es posible.

ARQUITECTURA
Crea paquetes nuevos, no mezcles lógica de playback:

app/src/main/java/com/example/aether/ui/skin/
  Skin.kt                 // data class inmutable
  SkinId.kt               // enum: DEFAULT, NEON, XP, WINAMP, GLASS, MINIMAL, CYBERPUNK
  BuiltinSkins.kt         // las 7 skins hardcodeadas
  SkinEngine.kt           // resuelve Skin activa
  SkinRepository.kt       // DataStore Preferences, key "selected_skin_id"
  LocalSkin.kt            // CompositionLocal
  VisualizerStyle.kt      // BARS, WAVE, CIRCLE, PARTICLES, CLASSIC_WINAMP, NONE

Modelo Skin (alineado a la maqueta JSON):
- id, name, tagline
- colors: primary, secondary, background, surface, onBackground, onSurface,
  accent, visualizerStart, visualizerEnd
- typography: fontFamily name (Inter / Space Grotesk / Roboto / Orbitron / Montserrat)
  Usa google-fonts via compose si hace falta; si no, mapear a FontFamily.SansSerif
  + letterSpacing distinto por skin para que se note.
- shapes: buttonRadius, cardRadius (dp)
- icons: style hint (Material / rounded / sharp)
- background: Solid / VerticalGradient / ImageAsset (nullable)
- animations: screenTransition (fade|slide), playButton (pulse|scale|none)
- visualizer: style + barCount

SKINS OBLIGATORIAS (imita la maqueta)

1. AETHER Default — "Moderna y elegante (Material 3)"
   bg #121212, primary #C4B5FD, visualizer barras lavanda suaves, Space Grotesk/Inter.

2. AETHER Neon — "Futurista y vibrante"
   bg casi negro, primary #B85FFF, secondary #FF80AA, glow, Orbitron o Space Grotesk,
   visualizer barras neón magenta/violeta.

3. AETHER XP — "Clásica y nostálgica"
   cielo azul claro #5BA3E8 → #B8D4F0, acentos azules Windows XP,
   tipografía más redonda (Roboto), visualizador tipo barras azules.

4. AETHER Winamp — "Retro y clásica"
   chrome gris/negro tipo Winamp 2, verdes #39FF14 / #7CFF00,
   visualizer clásico de barras verdes, radios casi 0, look "player de escritorio".
   Puede mostrar un chrome extra (botones pequeños) SOLO visual; los gestos siguen iguales.

5. AETHER Glass — "Transparente y minimalista"
   fondo claro azulado, glassmorphism (blur + alpha), primary suave,
   visualizer onda fina, tipografía ligera.

6. AETHER Minimal — "Simple y enfocada"
   negro puro, un solo acento tenue, poco chrome, visualizer línea mínima, Inter.

7. AETHER Cyberpunk — "Oscura y poderosa"
   negro + cyan #00F0FF + magenta #FF2BD6, grid/scanlines sutiles,
   Orbitron, visualizer barras + glow.

PERSISTENCIA
- androidx.datastore:datastore-preferences.
- SkinRepository.selectedSkinId: Flow<SkinId>
- Default = DEFAULT.
- MusicViewModel o un SkinViewModel expone selectedSkin y setSkin(id).
- Al arrancar, leer DataStore y aplicar antes del primer frame si se puede.

INTEGRACIÓN UI
1. CompositionLocalProvider(LocalSkin provides skin) debajo de setContent.
2. Sustituye AETHERTheme para que reciba Skin y construya ColorScheme +
   MaterialTheme.shapes desde la skin.
3. PresenceScreen, DensityTapeWidget, mini player y Now Playing deben leer
   LocalSkin.current para:
   - colores del visualizador
   - fondo (gradiente / sólido)
   - radio de cards
   - animación del botón play
4. NO toques la lógica de PresenceGestures ni PlaybackService.

PANTALLA DE AJUSTES
Extiende AetherSettings.kt:

- El IconButton de settings abre un sheet o navega a SettingsScreen.
- Secciones:
  A. Biblioteca (lo que ya existe: carpeta / archivos)
  B. Apariencia
     - Título "Apariencia"
     - Subtítulo "Las skins cambian la vista. Los gestos no cambian."
     - Grid o lista horizontal de SkinCard:
         preview circular con paleta (3-4 círculos de color)
         nombre + tagline
         check en la seleccionada
     - Al tocar: setSkin + snackbar "Skin aplicada: {name}"
- Si ya hay navegación, añade ruta "settings" / "appearance".
  Si no, ModalBottomSheet con scroll está bien.

VISUALIZADOR
Crea ui/skin/SkinVisualizer.kt usado por PresenceScreen:
- BARS, WAVE, CIRCLE, CLASSIC_WINAMP, NONE
- Usa amplitude real si MusicViewModel ya expone energy/density tape;
  si no, anima con un placeholder suave ligado a isPlaying para no bloquear.

REGLAS
- No cambies applicationId ni namespace.
- No pidas rediseñar cada botón: la skin define identidad, Compose aplica.
- Código idiomático Kotlin, preview @Preview por cada skin de Presence.
- Añade las dependencias DataStore (y fonts si las usas) en
  gradle/libs.versions.toml + app/build.gradle.kts.
- Actualiza docs/README.md con la sección Skins y cómo cambiarlas.
- Compila mentalmente contra compileSdk 34 / minSdk 26.

CRITERIOS DE ACEPTACIÓN
- Puedo abrir Ajustes → Apariencia y ver las 7 skins.
- Al elegir Neon/Winamp/XP/etc. Presence, lista y mini player cambian color/fondo.
- Al matar la app y reabrir, sigue la última skin.
- Gestos (swipe, tap, long press) intactos.
- Importar carpeta/archivos sigue funcionando.

Empieza por el modelo + DataStore + Theme dinámico, luego Settings UI,
luego cablea Presence y el visualizador. Implementa el código completo.