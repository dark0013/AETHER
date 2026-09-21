# AETHER — Reproductor local inmersivo

AETHER es un reproductor de música **local** para Android (Kotlin / Jetpack Compose / Media3 / Room).  
Estética oscura (#121212) con acento lavanda (#C4B5FD). Home = **Presence** (gestos), no una lista clásica.

## Qué hace hoy

- Escanea MediaStore (música ≥ 30 s; excluye WhatsApp, ringtones, etc.).
- **Importación manual** (engrane → Importar música): carpeta o archivos vía SAF.
- **Skins** (engrane → Apariencia): Default, Neon, XP, Winamp, Glass, Minimal, Cyberpunk. Solo cambian la vista; los gestos no.
- Presence: visualizador según skin (barras, onda, Winamp clásico, anillo neón), cinta de densidad, play/pause, seek con snap a onsets.
- Gestos: flick siguiente/anterior, volumen del **teléfono**, pellizco = mapa de sesión, borde izquierdo = biblioteca.
- Play al arrancar sin tema: primera canción de la biblioteca.
- **Ritual** (ancla): una sola canción, visual ámbar, no auto-next.
- **Playlists**: nombre único (máx. 100), marcar, ordenar arrastrando, borrar varias.
- Siguiente por similitud (si no hay playlist activa) y crossfade entre dos ExoPlayers.
- Análisis DSP en segundo plano (perfil + cinta); si falla, la pista igual suena.
- Reproducción en segundo plano (MediaSession / notificación).

## Formatos

No filtra por extensión. Reproduce lo que Media3 + códecs del dispositivo permitan (MP3, M4A/AAC, OGG/Opus, FLAC, WAV en la mayoría de Android). WMA/APE/DSD no.

## Arquitectura

```text
UI Compose (Presence / Biblioteca / Playlists / Sesión)
        ↓  LocalSkin
Skin Engine (7 skins, DataStore)     Gesture Engine     Playback Engine
        ↓
MusicViewModel (StateFlow)
        ↓
MusicRepository → Room + MediaStore + SAF
        ↓
PlaybackService (Media3, dos ExoPlayers)
```

## Skins

Ajustes (engrane) → **Apariencia**. Elige una de las 7 skins de la maqueta. Se guarda en DataStore (`selected_skin_id`) y se aplica al instante en Presence, lista, mini player, Now Playing y settings. Gestos y Media3 no cambian.

| Skin | Identidad visual |
|---|---|
| Default | Noche púrpura, anillo lavanda `#C4B5FD`, barras suaves |
| Neon | Magenta `#B85FFF` / `#FF80AA`, glow, barras neón |
| XP | Cielo `#5BA3E8` → `#B8D4F0`, barras azules |
| Winamp | Chrome gris, verdes `#39FF14` / `#7CFF00`, barras clásicas |
| Glass | Fondo claro, onda fina |
| Minimal | Negro puro, línea mínima |
| Cyberpunk | Cyan `#00F0FF` + magenta `#FF2BD6`, scanlines |

Código: `app/src/main/java/com/example/aether/ui/skin/` (Skin Engine). Gesture Engine y Playback Engine no dependen de las skins.

## Documentos

| Archivo | Contenido |
|---|---|
| `AETHER_SPEC.md` | Qué debe ser AETHER (spec conceptual v0.1). |
| `AETHER_EVOLUTION_SPEC.md` | Cómo evolucionar el código existente. |
| `PROGRESS_REPORT.md` | Estado real a 13-sep-2026. |
| `IMPLEMENTACION-FALTANTE-V01.md` | Huecos vs spec (BPM / beat grid / cruce). |
| `ROADMAP.md` | Fases de implementación. |
| `AUDIT.md` | Matriz de requisitos vs código. |

---
*Stack nativo Android. El spec original menciona Flutter; no aplica a este repo.*
