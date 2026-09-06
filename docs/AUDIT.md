# AETHER - Auditoría de Proyecto v0.1

## A. Resumen ejecutivo

El proyecto **AETHER** se encuentra actualmente en una fase de **reproductor convencional sólido**. Tiene una base funcional robusta en cuanto a escaneo de archivos, manejo de permisos, integración con el sistema (MediaSession) e interfaz de usuario moderna (Material 3 / Compose).

*   **Sólido**: La arquitectura de reproducción basada en Media3, el manejo de permisos dinámicos y la UI básica.
*   **Incompleto**: No existe el motor de análisis de audio, ni la persistencia de perfiles musicales, ni el motor de transición inteligente.
*   **Mayor trabajo pendiente**: La implementación del pipeline de análisis (FFT/RMS) para generar los `TrackProfile` y `DensityTape`, y la transformación de la UI hacia el modo **Presence**.

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

No hay persistencia local (DB) ni lógica de procesamiento de señales de audio (DSP) implementada.

---

## C. Matriz de requisitos

| ID | Requisito | Estado | Evidencia | Observaciones |
| :--- | :--- | :--- | :--- | :--- |
| **CORE-001** | Escaneo MediaStore | **IMPLEMENTADO** | `MusicRepository` | Incluye filtros por duración y carpetas. |
| **CORE-002** | Reproducción Background | **IMPLEMENTADO** | `PlaybackService` | Usa MediaSessionService de Media3. |
| **DATA-001** | TrackRecord | **PARCIAL** | `Song.kt` | Existe el modelo de datos pero no es persistente. |
| **DATA-002** | TrackProfile | **NO IMPLEMENTADO** | - | No hay estructura para BPM, Energy, Centroid, etc. |
| **DATA-003** | DensityTape | **NO IMPLEMENTADO** | - | Falta la serie temporal de audio. |
| **ANA-001** | Pipeline de Análisis | **NO IMPLEMENTADO** | - | No existe lógica de FFT, RMS u Onset detection. |
| **ENG-001** | TransitEngine | **NO IMPLEMENTADO** | - | La transición es la nativa de ExoPlayer. |
| **ENG-002** | Crossfade Onset-aware | **NO IMPLEMENTADO** | - | Requiere implementación de doble player. |
| **UX-001** | Presence Mode | **NO IMPLEMENTADO** | - | La UI actual es de tipo lista estándar. |
| **UX-002** | DensityTape Widget | **NO IMPLEMENTADO** | - | Falta el visualizador de onda interactivo. |
| **UX-003** | Presence Shader | **NO IMPLEMENTADO** | - | No hay integración de shaders AGSL/GLSL. |
| **UX-004** | Gestos Presence | **NO IMPLEMENTADO** | - | Solo gestos estándar de Compose/Android. |
| **UX-005** | Ritual Mode | **NO IMPLEMENTADO** | - | Falta la lógica de terminación de pista específica. |
| **SYS-001** | Sesiones | **NO IMPLEMENTADO** | - | No se registran SessionRecords. |

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
