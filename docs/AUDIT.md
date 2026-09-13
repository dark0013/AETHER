# AETHER - Auditoría de Proyecto

**Fecha:** 13 de septiembre de 2026  
**Código auditado:** `app/src/main/java/com/example/aether/`  
**DB:** Room `AetherDatabase` v6

## A. Resumen ejecutivo

AETHER es un **reproductor local inmersivo** (Presence) con análisis DSP, tránsito por onsets, ritual visible, sesiones, **playlists** e **importación SAF**.

* **Sólido:** Media3, permisos, Room, Presence, gestos, ritual, playlists, import, hardening de archivos ilegibles.
* **Residual (spec):** BPM por autocorrelación, beat grid, `alignToBeat`, loudness, secciones 1 Hz.

## B. Arquitectura

```text
UI (Compose: Presence, Library, Playlist, Session, Settings)
   ↓
ViewModel (MusicViewModel / StateFlow)
   ↓
Repository (MediaStore + SAF + Room)
   ↓
PlaybackService (2× ExoPlayer + MediaSession)
AnalysisScheduler (WorkManager, un análisis a la vez)
```

## C. Matriz de requisitos

| ID | Requisito | Estado | Evidencia |
| :--- | :--- | :--- | :--- |
| **CORE-001** | Escaneo MediaStore | **IMPLEMENTADO** | `MusicRepository.syncWithMediaStore` |
| **CORE-003** | Importación manual SAF | **IMPLEMENTADO** | `MusicImport` + engrane Ajustes |
| **CORE-002** | Reproducción background | **IMPLEMENTADO** | `PlaybackService` |
| **DATA-001** | TrackRecord | **IMPLEMENTADO** | `SongEntity` + `source` mediastore/import |
| **DATA-002** | TrackProfile | **PARCIAL** | Falta loudness, beatGrid, BPM spec |
| **DATA-003** | DensityTape | **IMPLEMENTADO** | uint8 percentil 2–98 |
| **DATA-004** | Playlists | **IMPLEMENTADO** | `PlaylistEntity` / `PlaylistScreens` |
| **ANA-001** | Pipeline análisis | **PARCIAL** | RMS/FFT/onset OK; BPM/secciones no a spec |
| **ENG-001** | TransitEngine | **PARCIAL** | Onsets sí; `alignToBeat` stub |
| **ENG-002** | Crossfade doble player | **IMPLEMENTADO** | Fade lineal 10 pasos |
| **UX-001** | Presence | **IMPLEMENTADO** | `PresenceScreen` |
| **UX-002** | DensityTape widget | **IMPLEMENTADO** | Seek + placeholder |
| **UX-003** | Shader | **PARCIAL** | AGSL + ritual; falta `uProgress` |
| **UX-004** | Gestos | **IMPLEMENTADO** | `PresenceGestures` (flick, vol sistema, pinch) |
| **UX-005** | Ritual | **IMPLEMENTADO** | Chip, ámbar, Salir, no skip |
| **UX-006** | Ajustes / import | **IMPLEMENTADO** | `AetherSettings.kt` |
| **SYS-001** | Sesiones | **PARCIAL** | Hay trackIds; no `startReasons` |
| **SYS-002** | Archivo ilegible | **IMPLEMENTADO** | Skip + snack |
| **SYS-003** | Permiso | **IMPLEMENTADO** | `PermissionScreen` |
| **SYS-004** | Biblioteca vacía | **IMPLEMENTADO** | Escanear + importar |
| **PERF-001** | Shader API &lt; 33 | **IMPLEMENTADO** | Fallback Canvas |

## D. Funcionalidades existentes (código)

* Presence inmersiva; barra superior **fija** (biblioteca, engrane, ancla, BPM).
* Chrome de título/play efímero (2.2 s).
* Volumen de **sistema** (STREAM_MUSIC).
* Play inicial = primera pista.
* Playlists con nombre único ≤ 100, pick, drag reorder, bulk delete.
* Import carpeta/archivos persistente.
* Similarity solo si **no** hay playlist activa ni ritual.
* Análisis background; pista reproducible con `ERROR`.

## E. Funcionalidades faltantes vs spec v0.1

* BPM autocorrelación + umbral 0.35 + `bpm = 0`.
* `beatGridOffsetMs` y `alignToBeat`.
* `loudnessApprox` dBFS; secciones 1 Hz / p80 / gap 12 s.
* Fade exponencial + fallback cut 80 ms.
* Shader: `uProgress`, decay 180 ms, cero seno autónomo.
* `startReasons` de sesión; badge ready/pending en lista.
* Agrupar librería por álbum/carpeta (opcional spec).

## F. Riesgos

* DSP costoso en bibliotecas grandes (un worker; OK).
* Import SAF: URIs persistentes; si el usuario revoca el permiso, esas pistas dejan de leerse.
* Migración Room destructiva solo como fallback; 4→5 y 5→6 son aditivas.

## G. Recomendación

El producto **ya se usa**. El siguiente trabajo de spec es el **bloque rítmico** (BPM → grid → cruce), no más superficie de UI.

Ver `IMPLEMENTACION-FALTANTE-V01.md` y `PROGRESS_REPORT.md`.
