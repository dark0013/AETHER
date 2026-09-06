# Aether — Spec de implementación v0.1

Documento para construir. Reproductor local inmersivo.
Sin catálogo externo, sin cuentas, sin equalizador en primer plano.

---

## 0. Objetivo de esta versión

Un APK Flutter que:

1. Escanea audio local (MP3 obligatorio; M4A/AAC/FLAC si el decoder lo permite).
2. Reproduce en segundo plano sin cortes tontos.
3. Genera un **perfil de pista** y una **cinta de densidad**.
4. Cruza temas **cerca de un onset**, no con fade a tiempo fijo.
5. Expone modo **Presencia** (gestos) y modo **Ritual** (una canción).
6. Elige “siguiente” por **similitud del perfil**, con fallback a cola / carpeta.

**Fuera de v0.1:** galaxia 3D, ML de cara, clima, sync entre teléfonos, exportar video, karaoke social.

**Criterio de calidad:** en 20 minutos de uso el usuario casi no toca cromo, entiende dónde cambia la canción en la cinta, y al menos un cruce “cae” en un golpe o frase.

---

## 1. Stack

| Capa | Elección |
|---|---|
| UI | Flutter 3.x |
| Estado | Riverpod |
| Audio | `just_audio` + `audio_service` |
| Metadata / librería | `on_audio_query` (Android) + tags embebidos |
| Persistencia | Isar |
| Análisis | Isolate dedicado. FFT / RMS en Dart o FFI ligero. Nunca en el UI isolate |
| Visual | CustomPainter (cinta) + un fragment shader (presencia) |
| Permisos | `permission_handler` + Storage Access Framework en Android 11+ |

Paquetes de referencia (ajustar versiones al crear el proyecto):

```yaml
dependencies:
  flutter:
    sdk: flutter
  flutter_riverpod: ^2.6.1
  just_audio: ^0.9.46
  audio_service: ^0.18.17
  on_audio_query: ^2.9.3
  isar: ^3.1.0+1
  isar_flutter_libs: ^3.1.0+1
  permission_handler: ^11.3.1
  path_provider: ^2.1.5
  sensors_plus: ^6.1.1
```

---

## 2. Arquitectura

```
lib/
  main.dart
  app.dart
  audio/
    aether_audio_handler.dart    # audio_service
    playback_controller.dart
    crossfade_engine.dart
  analysis/
    analysis_scheduler.dart
    track_analyzer.dart          # corre en isolate
    features.dart                # structs
    similarity.dart
  data/
    isar_service.dart
    models/
      track_record.dart
      track_profile.dart
      density_tape.dart
      mark.dart
      session_record.dart
  library/
    scanner.dart
    library_repository.dart
  session/
    session_controller.dart
    transit_engine.dart
  ui/
    presence/
      presence_page.dart
      density_tape_widget.dart
      presence_gestures.dart
      presence_shader.dart
    ritual/
      ritual_page.dart
    library/
      library_page.dart          # secundaria, no home
    widgets/
      mini_residue.dart
  theme/
    aether_theme.dart
```

Capas y reglas:

- UI no decodifica audio ni calcula FFT.
- El handler de `audio_service` es la única autoridad de play/pause/seek/cola.
- El análisis escribe en Isar; la UI solo lee perfiles listos o estado `pending`.
- Si no hay perfil aún, se reproduce igual. La cinta muestra waveform placeholder hasta que el análisis termine.

---

## 3. Modelo de datos

### 3.1 TrackRecord

Identidad del archivo.

```
id                IsarId
uri               String          # content:// o file path
filePath          String?
fileSize          int
mtimeMs           int             # para invalidar perfil
durationMs        int
title             String
artist            String
album             String
albumArtHash      String?         # opcional
dateAddedMs       int
```

Clave de invalidación de perfil: `(uri, fileSize, mtimeMs, durationMs)`.
Si cambia cualquiera, se regenera el perfil.

### 3.2 TrackProfile

Resumen de toda la pista. Un documento por track.

```
trackId           int             # Isar link
schemaVersion     int             # = 1
analyzedAtMs      int
sampleRateUsed    int
hopMs             int             # 46 o 93
bpm               double          # 0 si no confiable
bpmConfidence     double          # 0..1
beatGridOffsetMs  int             # fase del primer beat estimado
loudnessApprox    double          # RMS medio en dBFS aprox
energyMean        double
energyStd         double
centroidMean      double          # Hz normalizado 0..1 también guardado
centroidNorm      double          # 0..1
fluxMean          double
silenceRatio      double          # frames bajo umbral / total
sectionBoundsMs   List<int>       # cortes A/B aproximados, max 8
embedding         List<float>     # 16 floats
ready             bool
error             String?
```

`embedding` v1 (16 dims, todo normalizado 0..1 salvo bpm):

```
0  bpm / 200 (clamp)
1  bpmConfidence
2  energyMean
3  energyStd
4  centroidNorm
5  fluxMean
6  silenceRatio
7  loudnessApprox normalizado
8  energy del primer 15%
9  energy del 15–50%
10 energy del 50–85%
11 energy del último 15%
12 densidad de onsets (onsets/s normalizado)
13 ratio frames “brillantes” (centroid alto)
14 número de secciones / 8
15 reserved = 0
```

### 3.3 DensityTape

Serie temporal compacta. No guardar FFT completa.

```
trackId           int
frameCount        int
hopMs             int
energy            List<uint8>     # 0..255
centroid          List<uint8>     # 0..255
flux              List<uint8>     # 0..255
onsetFlags        List<uint8>     # bit0 = onset
```

Presupuesto: una canción de 4 min con hop 93 ms ≈ 2580 frames × 4 bytes ≈ 10 KB. Aceptable.

Resolución objetivo: **hopMs = 93** (v1). Si el análisis va rápido en mid-range, permitir 46.

### 3.4 Mark (marca de cuerpo)

```
id                IsarId
trackId           int
positionMs        int
createdAtMs       int
notePath          String?         # audio corto opcional, fuera de v0.1 UI
photoPath         String?         # fuera de v0.1 UI si no da tiempo
```

En v0.1 solo `positionMs`. Cápsula foto/voz queda como campo reservado.

### 3.5 SessionRecord

```
id                IsarId
startedAtMs       int
endedAtMs         int?
mode              String          # presence | ritual | transit
trackIds          List<int>
startReasons      List<String>    # similarity | user | folder | ritual
```

---

## 4. Pipeline de análisis

### 4.1 Cuándo corre

- Al escanear la librería: encola tracks nuevos o invalidados.
- Al abrir una pista sin perfil: prioridad máxima (head-of-line).
- Nunca más de **un** análisis pesado a la vez. Cola FIFO + boost del track actual.
- Abortable si el archivo desaparece.

### 4.2 Pasos

1. Abrir archivo y decodificar a PCM mono 22_050 Hz (basta para features).
   Si no se puede decodificar PCM desde Dart, estrategia v0.1:
   - usar un isolate que lea samples vía plugin nativo mínimo, **o**
   - calcular RMS/onset sobre un downsample obtenido con `ffmpeg_kit` solo si ya está en el proyecto.
   Preferencia: plugin/FFI propio pequeño. No bloquear el player (`just_audio` sigue en nativo).

2. Ventana Hann, N = 2048, hop = 2048 @ 22050 ≈ 93 ms.

3. Por frame:
   - `rms = sqrt(mean(x^2))`
   - magnitud FFT
   - `centroid = sum(f * mag) / sum(mag)`
   - `flux = sum(max(0, mag - magPrev))`
   - onset si `flux > median(flux, 1.5s) * k` con `k = 1.8` y cooldown 120 ms

4. BPM:
   - autocorrelación de la función de onset en rango 60–180 BPM
   - `bpmConfidence = peak / secondPeak` clamp 0..1
   - si confidence < 0.35 → `bpm = 0`, no usar grid para cruces

5. Secciones (barato):
   - downsample energy a 1 Hz
   - self-distance en ventanas de 8 s
   - cortes donde la distancia local supera percentil 80 y hay gap mínimo de 12 s
   - máximo 8 bounds

6. Cuantizar energy/centroid/flux a uint8 con percentiles 2–98 del propio track (no globales) para que la cinta tenga contraste.

7. Persistir perfil + cinta. `ready = true`.

### 4.3 Fallos

Si el análisis falla: `ready = false`, `error` texto corto, la pista es reproducible. Cinta placeholder = energía constante media + seek normal.

---

## 5. Motor de reproducción

### 5.1 Handler

`AetherAudioHandler extends BaseAudioHandler with QueueHandler, SeekHandler`.

Estados que publica:

```
playing
position
duration
currentTrackId
pendingCrossfade   bool
sessionMode
```

### 5.2 Cola

Cola corta: actual + 1 prefetch (siguiente probable).
No precargar 50 temas.

### 5.3 Crossfade / cruce (obligatorio)

Parámetros:

```
crossfadeMinMs        400
crossfadeMaxMs        1800
searchWindowEndMs     2200   # busca onset en los últimos 2.2 s
searchWindowStartMs   1800   # busca onset en los primeros 1.8 s del siguiente
alignToBeat           true si ambos bpmConfidence >= 0.45
```

Algoritmo `planCrossfade(current, next)`:

1. `tEnd` = último onset de `current` dentro de `[duration - searchWindowEndMs, duration - 80ms]`.
   Si no hay, `tEnd = duration - 700ms`.
2. `tStart` = primer onset de `next` dentro de `[80ms, searchWindowStartMs]`.
   Si no hay, `tStart = 0`.
3. Duración del fade = clamp(`tEnd` residual, min, max).
4. Si hay grid en ambos y BPM relativo está entre 0.92 y 1.08:
   - ajustar `tEnd` al beat más cercano (máx ±120 ms).
5. Ejecutar:
   - player A fade out exponencial suave
   - player B start en `tStart` con fade in
   - gapless si el plan falla (fallback hard cut + 80 ms fade)

Implementación: dos `AudioPlayer` en el handler (A/B) o `ConcatenatingAudioSource` no basta para seek de entrada. **Usar dos players.**

### 5.4 Seek “que encaja”

Seek de usuario en Presencia:

- objetivo = posición del gesto
- snap al onset más cercano en ±180 ms
- si no hay onset, seek exacto

### 5.5 Volumen

Curva de gesto: `gain = pow(fingerYNormalized, 1.6)` clamp 0..1.
No saltar de 0 a 1.

---

## 6. Similitud y tránsito

### 6.1 Distancia

```
d = sqrt(sum_i w[i] * (eA[i] - eB[i])^2)
```

Pesos v1:

```
bpm            1.2   (si alguno tiene bpm=0, peso 0)
energyMean     1.0
energyStd      0.6
centroidNorm   1.0
fluxMean       0.7
silenceRatio   0.4
onsetDensity   0.8
shape 8..11    0.5 cada uno
resto          0.3
```

### 6.2 Elegir siguiente (modo tránsito / siguiente automático)

Candidatos: perfiles `ready` de la librería, excluir:

- el actual
- los últimos 8 reproducidos
- mismo `artist` si hay ≥1 alternativa a distancia ≤ 1.15 × mejor distancia de otro artista

Elegir argmin `d`. Si nadie `ready`, siguiente de la carpeta/álbum.

### 6.3 Modo ritual

No hay siguiente automático. Al terminar: residuo 1.6 s y stop (o loop de la pista si el usuario activó loop).

Loop de sección (v0.1 si da tiempo): si hay `sectionBounds` y el usuario sostiene ancla, loop entre bound i e i+1 con cruce de 250 ms.

---

## 7. UX — modo Presencia (home)

Presencia es la ruta `/` tras el primer escaneo. La librería es un drawer o página secundaria.

### 7.1 Layout

```
[ shader de presencia, full bleed ]
[ cinta de densidad, 28–36 dp, borde inferior + safe area ]
[ título + artista, opacity 0.0 → 1.0 solo 2.2 s al cambiar de tema o al toque breve ]
sin FAB, sin barra de 5 iconos
```

Toque breve en el centro: muestra chrome 2.2 s (título, tiempo, icono ritual, icono librería).

### 7.2 Gestos

| Gesto | Acción |
|---|---|
| Tap centro | toggle chrome |
| Tap doble | play / pause |
| Vertical (1 dedo, zona central) | volumen |
| Horizontal arrastre | seek con snap a onset; la cinta sigue el dedo |
| Horizontal flick rápido derecha | siguiente (plan de cruce) |
| Horizontal flick rápido izquierda | anterior desde 0 o tema previo si position < 4 s |
| Long press 420 ms | crea Mark en position actual; feedback háptico |
| Pellizco | abre “mapa de sesión” (lista mínima de lo escuchado hoy + energía) |
| Deslizar desde borde izquierdo | librería |

No usar botones de skip permanentes.

### 7.3 Cinta de densidad

- Ancho = duración.
- Altura del área rellena = `energy[i]`.
- Tono / opacidad = `centroid[i]` (más brillante = más alto).
- Micro-marcas en `onsetFlags`.
- Playhead = línea de 1.5 dp.
- Marks = ticks de otro color, no pins grandes.

La cinta es el seekbar. No hay seekbar rectangular clásico.

### 7.4 Shader de presencia (regla)

Uniforms mínimos:

```
uTime
uEnergy        // frame actual 0..1
uCentroid      // 0..1
uFlux          // 0..1
uOnset         // 1.0 un frame, decae en 180 ms en CPU
uProgress      // 0..1
```

Si se alimenta un seno constante, el visual debe verse casi muerto. Prohibido ruido autónomo fuerte.

### 7.5 Residuo de fin de tema

Al plan de cruce o al stop:

- shader baja contraste en 400–900 ms
- no saltar a negro
- título no parpadea

---

## 8. UX — modo Ritual

Entrada: long press en título cuando el chrome está visible, o icono de ancla.

Comportamiento:

- skip por flick desactivado
- para salir: arrastre desde arriba o botón “salir” en chrome largo
- keep screen on
- brillo de shader un 20 % más bajo
- al terminar: stop + residuo, no auto-next

---

## 9. Librería (secundaria)

- Grid/lista por álbum o carpeta.
- Indicador de perfil: punto si `ready`, hilo si `pending`.
- Filtro “listos para tránsito” opcional.
- Escaneo manual + escaneo al arrancar si hay permiso.

Permisos Android:

- `READ_MEDIA_AUDIO` (33+)
- fallback `READ_EXTERNAL_STORAGE` (≤32)
- notificación para `audio_service`

No pedir ubicación ni cámara en v0.1.

---

## 10. Sesión

Al primer play del proceso se abre `SessionRecord(mode)`.

Cada cambio de track append `trackId` + razón.

Al 30 min idle o al kill controlado: `endedAtMs`.

Mapa de sesión (pellizco): vertical list, 64 dp por tema, mini-cinta, sin menús.

---

## 11. Estados y errores

| Caso | UI |
|---|---|
| Sin permiso | pantalla única “permitir acceso a audio” |
| Librería vacía | texto corto + botón escanear |
| Archivo ilegible | skip + snack silencioso en chrome |
| Análisis lento | cinta placeholder, play normal |
| Interrupción llamada | pause vía audio_service |
| Audio focus perdido | pause |

---

## 12. Performance

- UI 60 fps en un dispositivo de gama media 2022.
- Análisis de un MP3 de 4 min < 8 s en ese dispositivo (objetivo; 15 s aceptable).
- Memoria: no cargar PCM entero de discos de 20 min si se puede streamer por bloques de 30 s para features.
- Isolates: 1 worker.
- Shader: un pass, sin texturas enormes.

---

## 13. Orden de construcción (sprints)

### Sprint A — esqueleto que suena

- Proyecto Flutter, permisos, escaneo, lista cruda.
- `audio_service` + play/pause/seek/next/prev.
- Lock screen y notificación.

**Done:** se oye un MP3 con la pantalla apagada.

### Sprint B — datos

- Isar models.
- Worker de análisis: al menos energy + centroid + flux + onsets + embedding 16.
- Persistencia e invalidación por mtime.

**Done:** abrir un tema ya analizado pinta una cinta real.

### Sprint C — Presencia

- Página full-screen, gestos de la tabla 7.2.
- Cinta como seekbar con snap.
- Chrome efímero.
- Marks.

**Done:** se puede usar 10 minutos sin ver una ListView.

### Sprint D — cruce y tránsito

- Doble player + `planCrossfade`.
- Siguiente por distancia.
- Residuo visual.

**Done:** un cambio de tema no se siente como “app de archivos”.

### Sprint E — Ritual + sesión

- Modo ritual.
- SessionRecord + mapa pellizco.
- Pulido hápticos, keep-awake, temas de color desde artwork mezclado con centroid.

**Done:** v0.1 instalable.

---

## 14. Contratos de API interna

### AnalysisScheduler

```
enqueue(trackId, {priority: normal|now})
cancel(trackId)
Stream<AnalysisProgress> watch()
```

### PlaybackController

```
playTrack(trackId, {reason})
togglePause()
seekMs(ms, {snapOnset: true})
next({forceImmediate: false})
previous()
setMode(presence|ritual|transit)
addMark()
```

### TransitEngine

```
TrackId? suggestNext(currentId, historyIds)
```

---

## 15. Constantes (un solo archivo `aether_constants.dart`)

```dart
const kProfileSchema = 1;
const kHopMs = 93;
const kFftN = 2048;
const kAnalysisSr = 22050;
const kOnsetK = 1.8;
const kOnsetCooldownMs = 120;
const kBpmMin = 60.0;
const kBpmMax = 180.0;
const kBpmUseThreshold = 0.45;
const kFadeMinMs = 400;
const kFadeMaxMs = 1800;
const kSeekSnapMs = 180;
const kChromeMs = 2200;
const kLongPressMs = 420;
const kHistoryExclude = 8;
const kEmbeddingDims = 16;
```

---

## 16. Lo que no se implementa “por si acaso”

- Equalizador de 10 bandas en home.
- Login.
- Lyrics network obligatorias (LRC local se puede añadir después).
- Recomendación por género etiquetado ID3 como fuente principal.
- Animaciones que no leen `uEnergy/uFlux`.
- Segunda piel visual en v0.1.

---

## 17. Definición de “listo para crear”

El repo arranca cuando existen:

1. Este spec en `/AETHER_SPEC.md`
2. `flutter create aether`
3. Sprint A mergeable

Cualquier feature nueva se evalúa con: **¿muere si apago el análisis?** Si sigue igual de “bonita”, no entra.
