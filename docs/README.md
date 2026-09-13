# AETHER — Reproductor local inmersivo

AETHER es un reproductor de música **local** para Android (Kotlin / Jetpack Compose / Media3 / Room).  
Estética oscura (#121212) con acento lavanda (#C4B5FD). Home = **Presence** (gestos), no una lista clásica.

## Qué hace hoy

- Escanea MediaStore (música ≥ 30 s; excluye WhatsApp, ringtones, etc.).
- **Importación manual** (engrane → Importar música): carpeta o archivos vía SAF.
- Presence: visual shader, cinta de densidad, play/pause, seek con snap a onsets.
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
        ↓
MusicViewModel (StateFlow)
        ↓
MusicRepository → Room + MediaStore + SAF
        ↓
PlaybackService (Media3, dos ExoPlayers)
AnalysisScheduler (WorkManager + mutex)
```

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
