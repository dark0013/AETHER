# AETHER — Implementación faltante v0.1

**Documento:** `IMPLEMENTACION-FALTANTE-V01.md`  
**Fecha:** 13 de septiembre de 2026  
**Estado:** Bloque rítmico P0/P1 **implementado** en código + tests. Residual menor (P2).

## Cerrado en esta pasada

| Ítem | Dónde |
|---|---|
| BPM autocorrelación 60–180 + umbral 0.35 + octavas | `RhythmAnalysis.estimateBpm` |
| `beatGridOffsetMs` + nearest beat | `RhythmAnalysis` |
| `alignToBeat` ±120 ms si confianza ≥ 0.45 y BPM relativo 0.92–1.08 | `TransitEngine` |
| `loudnessApprox` dBFS + embedding[7] | `AudioAnalyzer` |
| Secciones 1 Hz / p80 / gap 12 s / máx. 8 | `RhythmAnalysis.detectSections` |
| `schemaVersion = 2` y reanálisis de perfiles viejos | `AnalysisScheduler.enqueueOutdatedProfiles` |
| Fade exponencial + fallback cut 80 ms | `PlaybackService` |
| Shader `uProgress`, decay onset 180 ms, wave × energy/flux | `PresenceShader` |
| `startReasons` en sesión | `SessionEntity` v7 |
| Badge ready/pending/error en biblioteca | `SongItem` |

Tests: `RhythmAnalysisTest` + snap de tránsito en `HardeningTest`.

## Residual (P2, no bloquea)

- Cola corta (actual + 1 prefetch) en vez de toda la librería.
- Fallback carpeta/álbum si nadie `READY` (hoy sigue la cola Media3).
- Agrupar biblioteca por álbum/carpeta (hace falta campo álbum).
- Long press en título para ritual; loop de sección.
- Residuo **visual** 400–900 ms al cruce (el audio ritual ya funde 1.6 s).
- Cerrar sesión en `PlaybackService.onDestroy`.
- PCM float/24-bit en el decoder; resample anti-alias.

## Definition of Done spec (código)

1. Autocorrelación + `bpm = 0` si confianza &lt; 0.35 — **sí**.
2. `beatGridOffsetMs` escrito en perfiles READY — **sí**.
3. Cruce snapea a beat ±120 ms — **sí** (test con 120 BPM).
4. Loudness y secciones según §4.2 — **sí**.
5. Shader casi muerto si energy/flux = 0 — **sí** (wave * drive).
6. Tests unitarios del bloque rítmico — **sí**.

Validación en dispositivo: dejar que se reanalicen temas (schema 2) y comprobar que un cruce entre temas con BPM similar cae cerca de un golpe.
