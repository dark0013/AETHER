# AETHER — Implementación faltante v0.1

**Documento:** `IMPLEMENTACION-FALTANTE-V01.md`  
**Fecha:** 12 de septiembre de 2026  
**Fuentes:** `AETHER_SPEC.md`, `AETHER_EVOLUTION_SPEC.md`, código actual en `app/src/main/java/com/example/aether/`  
**Propósito:** Inventario de lo que **aún no cumple** el spec v0.1. No es un backlog de ideas futuras.

El stack real es **Kotlin / Compose / Media3 / Room**, no Flutter. Eso es una decisión de evolución, no un pendiente.

**Criterio de calidad del spec:** en 20 minutos de uso el usuario casi no toca cromo, entiende dónde cambia la canción en la cinta, y al menos un cruce “cae” en un golpe o frase.

---

## 0. Resumen

El esqueleto de v0.1 **está montado**: escaneo, reproducción en segundo plano, Presence, cinta, gestos básicos, doble player, similitud, ritual, sesiones y hardening de archivos ilegibles.

Lo que falta para cerrar v0.1 con fidelidad al spec se concentra en **cuatro bloques**:

| Prioridad | Bloque | Por qué importa |
|---|---|---|
| P0 | Ritmo del análisis (BPM, beat grid, umbral de confianza) | Sin esto los cruces no “caen” en un golpe de forma fiable. |
| P0 | Usar ese ritmo en el cruce (`alignToBeat`) | El hueco se oye. `TransitEngine` tiene el `if` vacío. |
| P1 | Loudness + secciones según el algoritmo del spec | Embedding y mapa estructural mediocres. |
| P2 | UX Presence / Ritual / librería / sesión que el spec detalla y el código aproxima | Producto usable, pero no “spec-complete”. |

Fuera de v0.1 (no implementar aquí): galaxia 3D, ML de cara, clima, sync entre teléfonos, exportar video, karaoke, ecualizador en Home, JNI/FFT nativo.

---

## 1. Análisis de audio (`REQ` §4 / Evolution §12–15)

Pipeline existente: decode streaming → Hann 2048 / hop 2048 @ 22 050 Hz → RMS, FFT, centroid, flux, onset → embedding 16 → cinta uint8 percentil 2–98 → persistir `READY` / `ERROR`. Un análisis a la vez. Boost de la pista actual. Si falla, la pista sigue sonando.

Eso cubre el **esqueleto**. Falta el **algoritmo de ritmo y estructura** tal como está escrito.

### 1.1 BPM — incorrecto respecto al spec (P0)

**Spec §4.2.4**

- Autocorrelación de la **función de onset** en 60–180 BPM.
- `bpmConfidence = peak / secondPeak` clamp 0..1.
- Si `confidence < 0.35` → `bpm = 0` y **no usar grid para cruces**.

**Código actual** (`AudioAnalyzer.estimateBpm`)

- Mediana de intervalos entre onsets.
- Confianza = fracción de intervalos a ±5 BPM de esa mediana.
- Nunca pone `bpm = 0` por umbral 0.35.

**Qué implementar**

1. Construir la función de onset (serie 0/1 o flux rectificado) a hop ~93 ms.
2. Autocorrelación en el rango de lags que corresponde a 60–180 BPM.
3. `confidence = peak / secondPeak` (evitar división por 0).
4. Si `confidence < 0.35`, persistir `bpm = 0`, `bpmConfidence` real.
5. Tests unitarios con pulsos sintéticos a 90 / 120 / 140 BPM y un caso arrítmico.

**Archivos:** `analysis/AudioAnalyzer.kt`, tests en `app/src/test/.../analysis/`.

### 1.2 Beat grid — campo vacío (P0)

**Spec §3.2:** `beatGridOffsetMs` = fase del primer beat estimado.

**Código:** `ProfileEntity.beatGridOffsetMs` existe y **nunca se escribe**. Siempre `null`.

**Qué implementar**

1. A partir del BPM fiable, estimar el offset del primer beat (fase que maximiza alineación con onsets).
2. Persistir `beatGridOffsetMs`.
3. Exponer un helper `beatTimes(durationMs, bpm, offsetMs) → List<Long>` o lookup “beat más cercano a t”.

Sin grid, el paso 4 de `planCrossfade` no puede existir.

**Archivos:** `analysis/AudioAnalyzer.kt`, posiblemente `analysis/BeatGrid.kt` (nuevo, testable).

### 1.3 Loudness — campo vacío (P1)

**Spec §3.2:** `loudnessApprox` = RMS medio en dBFS aprox.  
Embedding dim 7 = loudness normalizado.

**Código:** el campo no se rellena. `embedding[7] = energyMean * 1.5`.

**Qué implementar**

```
loudnessApprox = 20 * log10(energyMean.coerceAtLeast(1e-8))
embedding[7] = normalizar a 0..1 (p.ej. mapear dBFS típico −60..0)
```

**Archivos:** `analysis/AudioAnalyzer.kt`.

### 1.4 Secciones — algoritmo distinto (P1)

**Spec §4.2.5**

1. Downsample de energy a 1 Hz.
2. Self-distance en ventanas de 8 s.
3. Cortes donde la distancia local supera **percentil 80**.
4. Gap mínimo **12 s**.
5. Máximo **8 bounds**.

**Código** (`detectSections`): diferencia de medias de energía `> 0.05`, muestreo cada ~11 s. No hay 1 Hz, ni self-distance, ni percentil, ni gap de 12 s.

**Impacto:** `embedding[14]` y cualquier loop de sección ritual no representan intro / estrofa / estribillo.

**Archivos:** `analysis/AudioAnalyzer.kt` (extraer a función pura testeable).

### 1.5 Decode / resample — calidad (P2)

| Spec | Actual | Falta |
|---|---|---|
| PCM mono 22 050 Hz | MediaCodec + **decimación** (tirar samples) | Resample anti-alias (al menos promedio de bloque; ideal interpolación) |
| Stream por bloques, no PCM entero | Ya es streaming por callback | OK |
| Abortable si el archivo desaparece | Check al **inicio** | Cancelar el decode a mitad si el URI deja de ser legible |
| Hop 46 ms si el dispositivo va rápido | Fijo 93 ms | Opcional; no bloquea v0.1 |
| Un worker | Mutex + WorkManager | OK, ver 1.6 |

`MediaCodec` puede entregar PCM float / 24-bit. Hoy se asume **16-bit signed**. Añadir rama por `MediaFormat.KEY_PCM_ENCODING`.

**Archivos:** `analysis/AudioDecoder.kt`.

### 1.6 Orquestación del scheduler (P2)

| Spec | Actual | Falta |
|---|---|---|
| Cola FIFO + boost HEAD de la pista actual | Boost cancela el Work de **esa** canción y corre ya | Si hay otro análisis en el mutex, el boost **espera**; no cancela ni reordena |
| Invalidación `(uri, fileSize, mtimeMs, durationMs)` | Reanaliza si cambian size o mtime | No guarda clave en el perfil; un cambio solo de `durationMs` no invalida |
| `schemaVersion` | Siempre `1` | Subir versión al cambiar BPM/secciones y reanalizar perfiles viejos |

**Archivos:** `analysis/AnalysisScheduler.kt`, `data/db/entities/ProfileEntity.kt`, `data/MusicRepository.kt`.

### 1.7 Qué del análisis NO falta

- RMS, FFT, centroid, flux, onset con `k = 1.8` y cooldown 120 ms.
- Cinta energy / centroid / flux / onsetFlags uint8 con percentiles 2–98 del propio track.
- Embedding de 16 dims (estructura; dims 0, 1 y 7 están mal alimentadas hasta P0/P1).
- Estados `PENDING / READY / ERROR` y pista reproducible con error.
- Un análisis pesado a la vez.

---

## 2. Cruce y reproducción (`REQ` §5)

### 2.1 Alineación a beat — stub (P0)

**Spec §5.3 paso 4:** si hay grid en ambos y BPM relativo ∈ [0.92, 1.08], ajustar `tEnd` al beat más cercano (máx ±120 ms). `alignToBeat` si ambos `bpmConfidence >= 0.45`.

**Código** (`TransitEngine.planCrossfade`): el `if` de confianza está vacío.

Depende de 1.1 y 1.2. Implementar **después** del beat grid.

**Archivos:** `analysis/TransitEngine.kt`.

### 2.2 Fade exponencial y fallback gapless (P1)

**Spec**

- Fade-out / fade-in **exponencial** suave.
- Si el plan falla: hard cut + 80 ms fade.

**Actual:** fade **lineal** en 10 pasos. No hay fallback explícito “plan falló → cut + 80 ms”. `getMediaItemAt` inválido aborta el cruce y deja el tema actual.

**Archivos:** `service/PlaybackService.kt`.

### 2.3 Cola corta: actual + 1 prefetch (P2)

**Spec §5.2:** no precargar 50 temas.

**Actual:** `playSong` carga **toda** la librería en el `MediaController` si la cola está vacía.

**Falta:** cola = actual + siguiente probable (similitud). Reduce memoria y errores de URI masivos.

**Archivos:** `ui/MusicViewModel.kt`.

### 2.4 Volumen del gesto (P2)

**Spec §5.5:** `gain = pow(fingerYNormalized, 1.6)` clamp 0..1.

**Actual:** `volume - dragAmount.y * 0.005` lineal.

**Archivos:** `ui/presence/PresenceScreen.kt`.

### 2.5 Seek snap (OK con matices)

Snap ±180 ms a onset: **implementado**.  
Arrastre horizontal en Presence hoy es **flick skip**, no seek-con-cinta-sigue-el-dedo (el seek está en la cinta). Ver §4.2.

### 2.6 Qué de playback NO falta

- Dos ExoPlayer A/B.
- Ventanas de onset 2.2 s / 1.8 s, fade clamp 400–1800 ms, fallback `duration - 700` / `tStart = 0`.
- Audio focus + `handleAudioBecomingNoisy` (pausa en llamada / auriculares).
- Skip de archivo ilegible + snack.

---

## 3. Similitud (`REQ` §6)

**Implementado:** distancia euclidiana ponderada, exclusión de actual + 8 últimos, regla de artista 1.15×, pesos del spec (bpm 1.2, etc.).

**Falta**

| Item | Prioridad | Detalle |
|---|---|---|
| Peso BPM = 0 si `bpm = 0` | P0 colateral | La regla ya está en código; no dispara hasta que 1.1 ponga `bpm = 0` cuando la confianza es baja. |
| Fallback carpeta / álbum | P1 | Spec: si nadie `ready`, siguiente de la carpeta/álbum. Hoy, si no hay sugerencia, no se sustituye el siguiente de la cola Media3 (orden de librería). |
| `startReasons` | P2 | Spec: `similarity \| user \| folder \| ritual`. `SessionEntity` solo guarda `trackIds`, no la razón. |

**Archivos:** `analysis/SimilarityEngine.kt`, `ui/MusicViewModel.kt`, `data/db/entities/SessionEntity.kt`.

---

## 4. Presence UX (`REQ` §7)

### 4.1 Shader (P1)

**Spec uniforms:** `uTime, uEnergy, uCentroid, uFlux, uOnset, uProgress`.  
Onset: 1.0 un frame, **decae 180 ms en CPU**.  
Si se alimenta un seno constante, el visual debe verse casi muerto.

**Actual**

- No hay `uProgress`.
- Decaimiento de onset es un decremento por frame (`−0.08`), no 180 ms reales.
- `uTime` sigue avanzando con el reloj (aunque se pausa al no reproducir). El shader usa `sin(dist * 15 - uTime * …)`: hay **movimiento autónomo** aunque energy/flux sean 0. Viola la regla del seno constante.

**Archivos:** `ui/presence/PresenceShader.kt`.

### 4.2 Gestos incompletos (P1–P2)

| Gesto spec | Estado | Falta |
|---|---|---|
| Tap centro → chrome 2.2 s | OK | — |
| Double tap play/pause | OK | — |
| Vertical volumen | Parcial | Curva `pow(_, 1.6)` |
| Horizontal arrastre = seek snap, cinta sigue el dedo | No | El arrastre horizontal es flick skip; el seek vive solo en la cinta |
| Flick derecha = siguiente **con plan de cruce** | Parcial | `skipNext()` nativo; no fuerza `planCrossfade` |
| Flick izquierda = anterior desde 0, o tema previo si `position < 4 s` | No | Siempre `seekToPrevious()` |
| Long press 420 ms → Mark + háptico | Parcial | Long press de Compose (duración no fijada a 420 ms) |
| Pellizco → mapa de sesión | OK | — |
| Deslizar desde borde izquierdo → librería | No | Solo icono en chrome |
| Sin botones de skip permanentes | Parcial | Play/pause grande en chrome (aceptable); Now Playing sheet sigue siendo reproductor clásico |

### 4.3 Residuo visual (P2)

**Spec §7.5:** al cruce o stop, shader baja contraste 400–900 ms; no saltar a negro; título no parpadea.

**Actual:** residuo de **audio** 1.6 s en ritual. El shader no tiene transición de contraste. Ritual baja alpha a 0.7 de forma constante, no animada.

### 4.4 Cinta (casi OK)

Energy / centroid / onsets / playhead / marks: implementado. Altura spec 28–36 dp; el widget usa **48 dp**. Ajuste visual menor.

### 4.5 Qué de Presence NO falta

- Home full-bleed, chrome efímero, fallback Canvas API &lt; 33, keep-screen-on mientras suena, placeholder de cinta si no hay perfil.

---

## 5. Ritual (`REQ` §8)

| Spec | Actual | Falta |
|---|---|---|
| Entrada: long press en **título** o icono ancla | Solo icono ancla | Long press en título |
| Flick skip desactivado | No | Flick sigue llamando next/prev |
| Salir: arrastre desde arriba o “salir” en chrome largo | Toggle del mismo icono | Gesto de salida dedicado |
| Keep screen on | OK (Presence + play) | — |
| Shader −20 % brillo | Alpha 0.7 (aprox.) | Uniform de brillo, no solo alpha de artwork |
| Fin: stop + residuo, no auto-next | OK | — |
| Loop de sección si sostiene ancla (v0.1 *si da tiempo*) | No | Depende de secciones P1; **opcional** |

---

## 6. Librería (`REQ` §9)

| Spec | Actual | Falta |
|---|---|---|
| Secundaria, no home | OK | — |
| Grid/lista por álbum o carpeta | Lista plana por título | Agrupar álbum/carpeta |
| Indicador perfil: punto `ready`, hilo `pending` | No | Badge por fila |
| Filtro “listos para tránsito” | No | Opcional spec |
| Escanear al arrancar + botón manual | OK | — |

---

## 7. Sesión (`REQ` §10)

| Spec | Actual | Falta |
|---|---|---|
| Abrir `SessionRecord` al primer play | OK | — |
| Append `trackId` + **razón** | Solo ids | `startReasons` |
| Idle 30 min → `endedAtMs` | OK | — |
| Kill controlado → `endedAtMs` | No fiable (`onCleared` cancela el scope) | Cerrar sesión en `PlaybackService.onDestroy` / `onTaskRemoved` |
| Mapa: 64 dp/tema, mini-cinta, sin menús | Lista con mini-cinta | Altura 64 dp exacta (menor) |

---

## 8. Fuera de v0.1 (no implementar)

Confirmado por spec §0 y Evolution §30:

- Login, backend, sync nube, letras online
- Galaxia 3D, reconocimiento facial, clima, karaoke, exportar video
- Ecualizador de 10 bandas en Home, segunda skin
- JNI/FFT nativo salvo que el análisis de 4 min no baje de 15 s en gama media
- Hop 46 ms (permitido, no obligatorio)
- Marks con foto/nota de voz

---

## 9. Orden recomendado de cierre v0.1

Cada paso debe compilar, no romper playback, y dejar la pista reproducible si el análisis falla.

```text
1. BPM autocorrelación + umbral 0.35 + tests sintéticos
        ↓
2. beatGridOffsetMs + helper de beat más cercano
        ↓
3. TransitEngine.alignToBeat (±120 ms, ratio BPM 0.92–1.08)
        ↓
4. loudnessApprox dBFS + embedding[7]
        ↓
5. Secciones 1 Hz / self-distance / p80 / gap 12 s
        ↓
6. schemaVersion++ y reanálisis de perfiles READY viejos
        ↓
7. Fade exponencial + fallback cut 80 ms
        ↓
8. Gestos: flick < 4 s, seek horizontal, borde izquierdo, ritual sin skip
        ↓
9. Shader: uProgress, decay 180 ms, cero movimiento autónomo
        ↓
10. Fallback carpeta/álbum + startReasons + indicador ready/pending
```

Los pasos 1–3 son el **camino crítico** del criterio de calidad (“un cruce cae en un golpe”). 4–6 cierran el análisis. 7–10 cierran producto.

---

## 10. Archivos previstos

| Archivo | Cambios |
|---|---|
| `analysis/AudioAnalyzer.kt` | BPM, grid, loudness, secciones |
| `analysis/BeatGrid.kt` | **Crear** — offset + nearest beat (testeable, sin Android) |
| `analysis/AudioDecoder.kt` | Resample, encoding PCM, abort mid-decode |
| `analysis/AnalysisScheduler.kt` | Prioridad HEAD real, invalidación por schema |
| `analysis/TransitEngine.kt` | `alignToBeat` |
| `analysis/SimilarityEngine.kt` | Fallback carpeta (o en ViewModel) |
| `service/PlaybackService.kt` | Fade exp, fallback 80 ms, end session al kill |
| `ui/MusicViewModel.kt` | Cola corta, razones de cambio, flick prev &lt; 4 s |
| `ui/presence/PresenceScreen.kt` | Gestos, volumen, ritual |
| `ui/presence/PresenceShader.kt` | `uProgress`, decay, sin seno autónomo |
| `ui/MusicScreens.kt` | Badge ready/pending |
| `data/db/entities/SessionEntity.kt` | `startReasons` |
| `app/src/test/.../analysis/` | Pulso sintético BPM, secciones, beat snap |

---

## 11. Definition of Done de este documento

v0.1 se considera **cerrado por spec** cuando:

1. Un tema con beats claros persiste `bpm > 0` y `bpmConfidence ≥ 0.35`; uno arrítmico persiste `bpm = 0`.
2. `beatGridOffsetMs` no es null en perfiles READY con BPM válido.
3. Un cruce entre dos temas con BPM similar ajusta `tEnd` a un beat (±120 ms).
4. `loudnessApprox` y `sectionBoundsMs` se rellenan con los algoritmos del §4.2.
5. El shader casi no se mueve si energy/flux/onset son 0.
6. Ritual no hace skip por flick; Presence flick-izquierda respeta la regla de 4 s.
7. Compila, unit tests del bloque rítmico en verde, reproducción básica sin regresión.

Hasta entonces, AETHER **suena y se ve** como el concepto, pero el análisis rítmico —el núcleo que diferencia v0.1 de un reproductor de archivos— sigue incompleto.
