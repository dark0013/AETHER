# Informe de Avance — Proyecto AETHER

**Fecha:** 13 de septiembre de 2026  
**Estado Actual:** Spec v0.1 rítmico cerrado en código (BPM / grid / alignToBeat / loudness / secciones / fade exp / shader). Residual P2 menor.

## 1. Trabajo realizado

### Fases 1–7 — Infraestructura y motor (COMPLETADO)
- Persistencia Room: canciones, perfiles, cintas, marcas, sesiones, **playlists**.
- DSP: FFT, RMS, onset, BPM (estimación simple), embedding 16.
- Similarity + TransitEngine + doble ExoPlayer con crossfade por onsets.

### Fases 8–9 — Presence y gestos (COMPLETADO, pulido 13-sep)
- Home full-bleed, DensityTape, chrome de título/play que se oculta.
- **Barra superior fija:** biblioteca, engrane (ajustes), ancla, BPM.
- Gestos unificados (`PresenceGestures.kt`): flick, volumen, pellizco, borde izquierdo, mark 420 ms.
- Flick **derecha→izquierda = siguiente**; izquierda→derecha = anterior (o reinicio si > 4 s).
- Play sin cola arranca la **primera canción** de la biblioteca.

### Fase 10 — Shader (COMPLETADO)
- AGSL API 33+; fallback Canvas. Uniform `uRitual` (ámbar, −20 % brillo).

### Fases 11–12 — Ritual y sesión (COMPLETADO, pulido 13-sep)
- Ritual visible: chip `RITUAL · Esta canción no avanza`, marco, snack, háptico, botón **Salir**.
- Flick desactivado en ritual; al terminar: fundido y stop (el flag no se pierde).
- Session Map por pellizco; idle 30 min cierra sesión.

### Fase 13 — Hardening (COMPLETADO)
- Skip + snack en archivos ilegibles; análisis single-flight; perfil `ERROR`.
- Permiso, biblioteca vacía, logs debug gated.

### Producto añadido (fuera del spec original, pedido del usuario)

**Playlists**
- Flujo: nombre → marcar canciones → OK → lista ordenable.
- Arrastre continuo (long press) para el orden de reproducción.
- Agregar más canciones sin perder las ya elegidas.
- Nombres únicos (case-insensitive), máximo **100** caracteres.
- Borrado múltiple (Seleccionar / Todas / Eliminar N).
- Al reproducir una playlist **no** interviene la similitud.

**Importación manual (SAF)**
- Engrane → módulo **Importar música**: carpeta o archivos.
- Permisos persistentes; no se borran en el rescan de MediaStore (`Song.source = import`).
- No aplica el filtro de 30 s / WhatsApp del escaneo automático.

**Volumen**
- El gesto vertical cambia el **volumen de medios del sistema** (`AudioManager`), no el gain interno de ExoPlayer.

**Ajustes**
- `AetherSettings.kt`: sheet con engrane, no iconos sueltos de importar.

Base de datos Room **versión 6** (migraciones 4→5 playlists, 5→6 `songs.source`).

---

## 2. Cierre rítmico (13-sep, tarde)

- `RhythmAnalysis`: autocorrelación BPM, beat grid, secciones, loudness.
- `TransitEngine.alignToBeat`.
- Fade exponencial + fallback 80 ms.
- Shader `uProgress` + decay 180 ms.
- Reanálisis schemaVersion 2; `startReasons`; badge de análisis en la lista.

Pendiente menor: cola corta, agrupar por álbum, residuo visual al cruce. Ver `IMPLEMENTACION-FALTANTE-V01.md`.

---

## 3. Archivos clave

| Área | Ruta |
|---|---|
| Playback | `service/PlaybackService.kt` |
| Análisis | `analysis/AudioAnalyzer.kt`, `AnalysisScheduler.kt`, `TransitEngine.kt` |
| Presence | `ui/presence/PresenceScreen.kt`, `PresenceGestures.kt`, `PresenceShader.kt` |
| Playlists | `ui/playlist/PlaylistScreens.kt`, `data/db/entities/PlaylistEntity.kt` |
| Import | `data/MusicImport.kt`, `ui/AetherSettings.kt` |
| Estado | `ui/MusicViewModel.kt`, `data/MusicRepository.kt` |

---
*AETHER es un reproductor local inmersivo con playlists propias, importación SAF y ritual visible. El ADN rítmico del cruce aún no está a spec.*
