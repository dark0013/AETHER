# AETHER - Auditoría de Proyecto v0.1

## A. Resumen ejecutivo

El proyecto **AETHER** es un reproductor local inmersivo (Presence) con análisis DSP, tránsito onset-aware, ritual, sesiones y hardening de errores. Conserva la base Media3 / Compose / MediaStore.

*   **Sólido**: La arquitectura de reproducción basada en Media3, el manejo de permisos dinámicos y la UI básica.
*   **Sólido (2026-09)**: persistencia Room, análisis DSP, Presence, tránsito onset-aware, ritual, sesiones y hardening de errores.
*   **Residual**: alineación fina de beat-grid en cruces; BPM sigue siendo una estimación simple.

---

## B. Arquitectura actual

La arquitectura real encontrada es un patrón **MVVM simplificado**:

```text
UI (Jetpack Compose)
   ↓ 
ViewModel (MusicViewModel / StateFlow)
   ↓ 
Repository (MusicRepository / MediaStore)
   ↓ 
Audio Engine (Media3 / ExoPlayer / MediaSessionService)
```

Persistencia Room (`AetherDatabase`) y DSP (`AudioAnalyzer` / `AnalysisScheduler`) integrados sobre el MVVM existente.

---

## C. Matriz de requisitos

| ID | Requisito | Estado | Evidencia | Observaciones |
| :--- | :--- | :--- | :--- | :--- |
| **CORE-001** | Escaneo MediaStore | **IMPLEMENTADO** | `MusicRepository` | Incluye filtros por duración y carpetas. |
| **CORE-002** | Reproducción Background | **IMPLEMENTADO** | `PlaybackService` | Usa MediaSessionService de Media3. |
| **DATA-001** | TrackRecord | **IMPLEMENTADO** | `SongEntity` | Persistente en Room con invalidation key. |
| **DATA-002** | TrackProfile | **IMPLEMENTADO** | `ProfileEntity` | BPM, energy, centroid, embedding 16. |
| **DATA-003** | DensityTape | **IMPLEMENTADO** | `DensityTapeEntity` | Energy/centroid/flux/onsets uint8. |
| **ANA-001** | Pipeline de Análisis | **IMPLEMENTADO** | `AudioAnalyzer` | FFT, RMS, onset, BPM; un análisis a la vez. |
| **ENG-001** | TransitEngine | **IMPLEMENTADO** | `TransitEngine.kt` | Plan de cruce por onsets. |
| **ENG-002** | Crossfade Onset-aware | **IMPLEMENTADO** | `PlaybackService` | Doble ExoPlayer + fade. |
| **UX-001** | Presence Mode | **IMPLEMENTADO** | `PresenceScreen` | Home full-bleed. |
| **UX-002** | DensityTape Widget | **IMPLEMENTADO** | `DensityTapeWidget` | Seek + placeholder si no hay perfil. |
| **UX-003** | Presence Shader | **IMPLEMENTADO** | `PresenceShader` | AGSL API 33+; fallback Canvas. |
| **UX-004** | Gestos Presence | **IMPLEMENTADO** | `PresenceScreen` | Tap, flick, volumen, pinch, mark. |
| **UX-005** | Ritual Mode | **IMPLEMENTADO** | `PlaybackService` / Presence | Sin auto-next + residuo. |
| **SYS-001** | Sesiones | **IMPLEMENTADO** | `SessionEntity` / `SessionMapScreen` | Historial + pinch. |
| **SYS-002** | Archivo ilegible | **IMPLEMENTADO** | `PlaybackService` + snack | Skip al siguiente + aviso silencioso. |
| **SYS-003** | Permiso denegado | **IMPLEMENTADO** | `PermissionScreen` | Pantalla única de acceso a audio. |
| **SYS-004** | Biblioteca vacía | **IMPLEMENTADO** | `EmptyState` | Texto corto + botón escanear. |
| **SYS-005** | Análisis fallido | **IMPLEMENTADO** | `AnalysisStatus.ERROR` | Pista reproducible; cinta placeholder. |
| **PERF-001** | Shader API < 33 | **IMPLEMENTADO** | `PresenceShader` fallback Canvas | AGSL solo en API 33+. |
| **PERF-002** | Visualización en pausa | **IMPLEMENTADO** | `PresenceVisualizer(isPlaying)` | No anima ni hace polling de cruce en pausa. |

---

## D. Funcionalidades existentes

*   **Escaneo inteligente**: Filtra audios de WhatsApp/Telegram y archivos cortos (>30s).
*   **Gestión de Carátulas**: Carga mediante Coil con fallback a logo de la app.
*   **Búsqueda**: Filtrado reactivo de la biblioteca por título y artista.
*   **Reproducción Completa**: Play/Pause, Next/Prev, Shuffle, Repeat y Seek.
*   **UI v0.2**: Tema oscuro "AETHER Look" con acentos lavanda.
*   **Media3 integration**: Notificaciones y controles de medios en el sistema.

---

## E. Funcionalidades faltantes

*   **Motor de DSP**: Procesamiento de audio para extraer features (Isolate/Worker).
*   **Base de Datos (Room)**: Necesaria para persistir los `TrackProfile` y `DensityTape`.
*   **Visualizador de Densidad**: El componente visual interactivo para la Home.
*   **Lógica de Similitud**: Selección de canción basada en vectores de embedding.
*   **TransitEngine**: Control coordinado de dos instancias de ExoPlayer para crossfade perfecto.

---

## F. Refactorizaciones necesarias

*   **Doble Player**: El `MusicViewModel` y `PlaybackService` deben evolucionar para manejar dos instancias de ExoPlayer simultáneamente para permitir el crossfade basado en onsets.
*   **Persistencia**: Migrar de una lista en memoria a una fuente de datos basada en Room que sincronice con MediaStore.

---

## G. Riesgos

*   **Rendimiento DSP**: El análisis de audio (FFT/BPM) en Android puede ser costoso. Se requerirá un manejo cuidadoso de `Coroutines` o `WorkManager` para no afectar el audio ni la UI.
*   **Memoria**: Almacenar `DensityTape` (series temporales) para miles de canciones requiere una estrategia de almacenamiento eficiente.
*   **Compatibilidad de Shaders**: AGSL (Android Graphics Shading Language) requiere API 33+. Se necesita fallback para versiones anteriores.

---

## H. Dependencias

```text
TrackProfile (Análisis)
    ↓
Similarity (Cálculo de embeddings)
    ↓
TransitEngine (Selección inteligente)
    ↓
Crossfade (Reproducción coordinada)
```

---

## I. Recomendación

La **Fase 1** después de esta auditoría debe centrarse en la **Persistencia y el Análisis Básico**. 
Sin un `TrackProfile` persistente, no se pueden implementar las funcionalidades core de AETHER (Similitud, Tránsito, Presencia).

1.  Implementar **Room** para persistir `Song`, `TrackProfile` y `DensityTape`.
2.  Crear el `AnalysisScheduler` y un primer `TrackAnalyzer` que extraiga al menos **RMS (Energía)** para poder pintar una `DensityTape` básica.
