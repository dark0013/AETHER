# AETHER — Implementación faltante v0.1

**Documento:** `IMPLEMENTACION-FALTANTE-V01.md`  
**Fecha:** 13 de septiembre de 2026  
**Fuentes:** `AETHER_SPEC.md`, código en `app/src/main/java/com/example/aether/`  
**Propósito:** Inventario de lo que **aún no cumple** el spec v0.1. Lo ya entregado (playlists, SAF, ritual visible, gestos, volumen de sistema) está en `PROGRESS_REPORT.md` y **no** es pendiente.

El stack real es **Kotlin / Compose / Media3 / Room**.

**Criterio de calidad del spec:** en 20 minutos de uso el usuario casi no toca cromo, entiende dónde cambia la canción en la cinta, y al menos un cruce “cae” en un golpe o frase.

---

## 0. Resumen

El producto **se usa**: Presence, ritual, playlists, import, hardening.

Para **cerrar el spec v0.1** falta el **ritmo** (se oye en los cruces), no más pantallas.

| Prioridad | Bloque | Estado |
|---|---|---|
| P0 | BPM autocorrelación + umbral 0.35 | **Pendiente** |
| P0 | `beatGridOffsetMs` | **Pendiente** (siempre `null`) |
| P0 | `TransitEngine.alignToBeat` | **Pendiente** (`if` vacío) |
| P1 | Loudness dBFS + secciones 1 Hz / p80 | **Pendiente** |
| P1 | Fade exponencial + cut 80 ms | **Pendiente** |
| P1 | Shader `uProgress`, decay 180 ms, sin seno autónomo | **Pendiente** |
| P2 | `startReasons`, badge ready/pending, librería por álbum | **Pendiente** |

**Hecho desde el inventario anterior (ya no falta):**

- Gestos unificados (flick, pellizco, volumen, mark 420 ms, borde izquierdo → biblioteca).
- Flick izquierda = siguiente; prev respeta 4 s.
- Volumen = stream del teléfono.
- Ritual visible (chip, Salir, ámbar, skip off, residuo).
- Play = primera canción; barra superior fija; ajustes/import SAF; playlists.

Fuera de v0.1: galaxia 3D, cuentas, karaoke, EQ en Home, JNI.

---

## 1. Análisis de audio (`REQ` §4)

Pipeline existente: decode streaming → Hann 2048 / hop ~93 ms → RMS, FFT, centroid, flux, onset → embedding 16 → cinta uint8 percentil 2–98 → `READY` / `ERROR`. Un análisis a la vez.

### 1.1 BPM — incorrecto (P0)

Spec: autocorrelación de onset 60–180 BPM; `confidence = peak / secondPeak`; si &lt; 0.35 → `bpm = 0`.

Código (`estimateBpm`): mediana de intervalos; nunca pone `bpm = 0`.

**Hacer:** autocorrelación + tests sintéticos 90/120/140 y un caso arrítmico.  
**Archivos:** `analysis/AudioAnalyzer.kt`.

### 1.2 Beat grid — vacío (P0)

`ProfileEntity.beatGridOffsetMs` no se escribe. Sin esto no hay `alignToBeat`.

**Hacer:** offset del primer beat + helper nearest-beat.  
**Archivos:** `analysis/AudioAnalyzer.kt`, `analysis/BeatGrid.kt` (nuevo).

### 1.3 Loudness (P1)

Campo `loudnessApprox` vacío; `embedding[7] = energyMean * 1.5`.

**Hacer:** `20 * log10(rms)` y normalizar a 0..1.

### 1.4 Secciones (P1)

Spec: energy 1 Hz, self-distance 8 s, corte &gt; p80, gap 12 s, máx. 8.

Código: diff de medias `> 0.05`.

### 1.5 Decode (P2)

Decimación en vez de resample; PCM asumido 16-bit; no aborta a mitad si desaparece el archivo.

### 1.6 Scheduler (P2)

Boost espera al mutex (no cancela el análisis anterior). `schemaVersion` no invalida perfiles viejos.

---

## 2. Cruce (`REQ` §5)

### 2.1 `alignToBeat` (P0)

Si ambos `bpmConfidence ≥ 0.45` y BPM relativo ∈ [0.92, 1.08], snap `tEnd` al beat (±120 ms). Stub vacío. Depende de 1.1 y 1.2.

### 2.2 Fade (P1)

Hoy lineal 10 pasos. Spec: exponencial; si el plan falla, cut + 80 ms.

### 2.3 Cola corta (P2)

`playSong` / playlists cargan la cola completa. Spec: actual + 1 prefetch.

Volumen de gesto y skip ilegible: **OK** (sistema + snack).

---

## 3. Similitud (`REQ` §6)

Implementado (pesos, historial 8, regla artista 1.15×). Desactivada si hay playlist o ritual.

Pendiente: fallback carpeta/álbum si nadie `READY`; `startReasons` en sesión.

---

## 4. Presence / Ritual / librería / sesión

Casi cerrado a nivel producto.

Pendiente menor vs spec:

- Arrastre horizontal como **seek** (hoy es flick; seek está en la cinta).
- Shader: `uProgress`, decay 180 ms real, cero movimiento si energy=0.
- Residuo **visual** 400–900 ms al cruce (el audio de ritual sí funde 1.6 s).
- Long press en **título** para entrar a ritual (hoy: ancla).
- Loop de sección (opcional spec).
- Badge ready/pending en filas de biblioteca; agrupar álbum.
- Cerrar sesión en `PlaybackService.onDestroy`.

---

## 5. Orden de cierre spec

```text
1. BPM autocorrelación + umbral 0.35 + tests
        ↓
2. beatGridOffsetMs + nearest beat
        ↓
3. TransitEngine.alignToBeat
        ↓
4. loudnessApprox + embedding[7]
        ↓
5. Secciones 1 Hz / p80 / gap 12 s
        ↓
6. schemaVersion++ y reanálisis
        ↓
7. Fade exponencial + fallback 80 ms
        ↓
8. Shader uProgress / decay / sin seno autónomo
```

1–3 son el camino crítico del criterio de calidad.

---

## 6. Definition of Done (spec)

1. Tema con beats claros: `bpm > 0` y `bpmConfidence ≥ 0.35`; arrítmico: `bpm = 0`.
2. `beatGridOffsetMs` no null si BPM válido.
3. Cruce entre BPMs similares ajusta `tEnd` a un beat (±120 ms).
4. Loudness y secciones según §4.2.
5. Shader casi muerto si energy/flux/onset = 0.
6. Compila; tests rítmicos en verde; no rompe playlists ni import.

Hasta entonces AETHER **suena y se usa**, pero el cruce no está “imantado” al golpe como pide el spec.
