# AETHER — Evolution Specification
## Especificación de evolución del proyecto existente

**Documento:** `AETHER_EVOLUTION_SPEC.md`  
**Propósito:** Guiar a un agente de desarrollo (Gemini/Codex u otro) para evolucionar el proyecto AETHER existente hasta cumplir `AETHER_SPEC.md`, sin reconstruirlo desde cero.

---

## 1. Propósito de este documento

AETHER ya es un proyecto iniciado y contiene funcionalidades implementadas. Este documento define las reglas que deben seguirse para **evolucionar el código existente**, incorporando progresivamente las capacidades descritas en `AETHER_SPEC.md`.

Este documento **no reemplaza** a `AETHER_SPEC.md`.

- `AETHER_SPEC.md` define **qué debe ser AETHER y cómo debe comportarse**.
- `AETHER_EVOLUTION_SPEC.md` define **cómo debe trabajar el agente sobre el proyecto existente**.

El objetivo es llegar a una implementación funcional del concepto AETHER aprovechando todo lo que ya existe.

---

## 2. Regla principal

> **NO reconstruir AETHER desde cero. Evolucionar el proyecto actual.**

Antes de crear, eliminar, migrar o reemplazar componentes, el agente debe inspeccionar el código existente y determinar qué puede reutilizarse.

El estado actual del proyecto es la fuente de verdad sobre lo que ya está implementado.

El agente NO debe asumir que una funcionalidad no existe simplemente porque no aparece en una conversación o documento.

---

## 3. Documentos de referencia

El agente debe considerar como fuentes principales:

1. `docs/AETHER_SPEC.md`
2. `docs/AETHER_EVOLUTION_SPEC.md`
3. Código fuente actual del proyecto
4. Configuración actual de Gradle/Android
5. Dependencias actualmente utilizadas

Orden de prioridad:

```text
Código existente
        +
AETHER_SPEC.md
        +
AETHER_EVOLUTION_SPEC.md
```

Si existe una diferencia entre el código y la especificación, no se debe resolver destruyendo automáticamente lo existente.

Primero se debe identificar la diferencia y proponer la estrategia de integración.

---

# 4. Estado inicial del proyecto

AETHER ya cuenta con una base funcional de reproductor musical.

Según el estado conocido del proyecto, existen o se han implementado capacidades como:

- Interfaz visual AETHER.
- Tema oscuro.
- Color/acento lavanda.
- Biblioteca musical.
- Escaneo de archivos de audio.
- Filtrado de archivos no deseados.
- Detección de duración válida.
- Metadata musical.
- Carátulas mediante MediaStore/Coil.
- Búsqueda por título/artista.
- Reproducción mediante Media3/ExoPlayer.
- Shuffle.
- Repeat.
- Mini player.
- Barra de progreso.
- Pantalla Now Playing.
- Reproducción en segundo plano.
- MediaSession/servicio de reproducción.

**IMPORTANTE:** esta lista es únicamente un punto de partida conocido. El agente debe verificar el estado real del repositorio antes de modificar cualquier cosa.

---

# 5. Stack tecnológico

La evolución debe respetar el stack tecnológico que ya utiliza el proyecto.

Si el proyecto actual utiliza:

- Android nativo
- Kotlin
- Jetpack Compose
- Material 3
- Media3 / ExoPlayer
- Coil
- ViewModel
- StateFlow

debe mantenerse ese enfoque.

## No realizar migraciones de framework

No migrar el proyecto a:

- Flutter
- React Native
- Ionic
- Angular
- otro framework

salvo que el usuario lo solicite explícitamente.

El `AETHER_SPEC.md` original contiene referencias a Flutter porque representa una especificación conceptual/original. Para el proyecto actual se deben implementar equivalentes nativos Android cuando corresponda.

Ejemplos:

| Concepto original | Implementación objetivo |
|---|---|
| Flutter UI | Jetpack Compose |
| Riverpod | ViewModel + StateFlow |
| just_audio | Media3 / ExoPlayer |
| audio_service | MediaSession / Media3 |
| Isar | Room u otra persistencia Android apropiada |
| CustomPainter | Compose Canvas |
| Fragment shader | RuntimeShader / AGSL cuando sea viable |
| permission_handler | APIs de permisos Android |
| SAF | Storage Access Framework |

No introducir una tecnología nueva solamente porque aparezca en el documento original.

---

# 6. Principios de evolución

## 6.1 Preservar funcionalidades existentes

Las funcionalidades que ya funcionan deben conservarse.

Una nueva implementación debe integrarse con ellas siempre que sea razonable.

Ejemplo:

Si ya existe un `PlayerViewModel`, no crear automáticamente otro sistema paralelo de reproducción.

Primero investigar cómo funciona el existente.

---

## 6.2 Evitar duplicación

No crear:

- dos reproductores independientes;
- dos fuentes de verdad para la canción actual;
- dos sistemas de cola;
- dos sistemas de permisos;
- dos sistemas de escaneo;
- dos bases de datos para la misma información.

AETHER debe evolucionar hacia una arquitectura coherente.

---

## 6.3 Una sola fuente de verdad

Para cada dominio debe existir una autoridad clara.

Ejemplo:

```text
Playback
    ↓
Media3 / PlaybackController
    ↓
UI observa estado
```

La UI no debe convertirse en responsable de:

- decodificar audio;
- ejecutar análisis FFT pesado;
- controlar directamente la cola;
- calcular perfiles musicales.

---

## 6.4 Cambios incrementales

Las modificaciones deben realizarse por fases.

Cada fase debe:

1. Tener un objetivo concreto.
2. Modificar únicamente lo necesario.
3. Compilar.
4. Ejecutar las pruebas disponibles.
5. Verificar que no rompe funcionalidades existentes.
6. Documentar los cambios.

No implementar todo AETHER en una sola operación gigantesca.

---

# 7. Fase 0 — Auditoría obligatoria

Antes de implementar nuevas funcionalidades, el agente debe realizar una auditoría completa.

Debe inspeccionar:

### Proyecto

- estructura de carpetas;
- módulos;
- Gradle;
- versiones;
- manifest;
- permisos;
- dependencias;
- recursos;
- configuración Android.

### UI

- pantallas;
- navegación;
- componentes Compose;
- tema;
- estados;
- mini player;
- Now Playing;
- biblioteca;
- búsqueda.

### Audio

- ExoPlayer/Media3;
- MediaSession;
- servicio de reproducción;
- cola;
- shuffle;
- repeat;
- seek;
- volumen;
- audio focus;
- interrupciones.

### Biblioteca

- escaneo;
- MediaStore;
- filtros;
- metadata;
- album art;
- actualización de biblioteca.

### Persistencia

- Room;
- SQLite;
- DataStore;
- repositorios;
- cualquier mecanismo existente.

### Arquitectura

- ViewModels;
- StateFlow;
- repositorios;
- servicios;
- casos de uso;
- clases relacionadas con audio.

### Rendimiento

- operaciones en main thread;
- coroutines;
- workers;
- memoria;
- carga de audio;
- procesamiento de imágenes.

---

# 8. Resultado obligatorio de la auditoría

La auditoría debe producir un archivo:

```text
docs/AUDIT.md
```

Cada requisito relevante de `AETHER_SPEC.md` debe clasificarse como:

- `IMPLEMENTADO`
- `PARCIAL`
- `NO IMPLEMENTADO`
- `DIFERENTE PERO EQUIVALENTE`
- `REQUIERE REFACTORIZACIÓN`

Ejemplo:

```text
REQ-PLAYBACK-001
Estado: IMPLEMENTADO

Evidencia:
Media3/ExoPlayer ya controla la reproducción.

Integración:
El futuro TransitEngine deberá integrarse sobre el controlador actual.
```

El agente no debe empezar una implementación grande antes de completar esta auditoría.

---

# 9. Fase de planificación

Después de `AUDIT.md`, crear:

```text
docs/ROADMAP.md
```

El roadmap debe determinar:

- qué falta;
- dependencias entre funcionalidades;
- orden de implementación;
- riesgos;
- archivos que probablemente serán modificados;
- pruebas necesarias;
- criterios de aceptación.

No asumir que el orden original del `AETHER_SPEC.md` debe ejecutarse literalmente si el código actual requiere otro orden.

---

# 10. Arquitectura objetivo

La arquitectura final debe aproximarse conceptualmente a:

```text
                 ┌─────────────────────┐
                 │     AETHER UI       │
                 │     Compose         │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │    ViewModels       │
                 │   StateFlow         │
                 └──────────┬──────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        Biblioteca       Playback      Analysis
              │             │             │
              ▼             ▼             ▼
          MediaStore      Media3       Analyzer
                            │             │
                            ▼             ▼
                       Session       TrackProfile
                       Service       DensityTape
```

La arquitectura exacta debe adaptarse al código existente.

No crear esta estructura literalmente si el proyecto actual ya posee una equivalente funcional.

---

# 11. Evolución funcional

La evolución deberá cubrir progresivamente los siguientes dominios.

## 11.1 TrackRecord

AETHER debe disponer de una representación persistente de cada pista con información equivalente a:

- URI
- ruta cuando esté disponible
- tamaño
- fecha/modificación
- duración
- título
- artista
- álbum
- identificador/hash de carátula
- fecha de incorporación

La implementación debe respetar el almacenamiento actualmente utilizado o introducir Room si realmente es necesario.

Debe existir una estrategia de invalidación cuando cambie el archivo.

Clave conceptual:

```text
(uri, fileSize, mtimeMs, durationMs)
```

---

# 12. TrackProfile

AETHER debe evolucionar desde un simple reproductor hacia un reproductor que entiende características musicales.

Cada pista analizada podrá generar:

- BPM
- confianza del BPM
- offset de beat
- loudness aproximado
- energía media
- desviación de energía
- spectral centroid
- spectral flux
- ratio de silencio
- secciones
- embedding
- estado de análisis

Estados mínimos:

```text
PENDING
READY
ERROR
```

Una pista con análisis pendiente o fallido **debe seguir pudiendo reproducirse**.

---

# 13. DensityTape

Cada pista analizada debe poder tener una representación compacta de densidad temporal.

Debe contener información equivalente a:

- energía;
- centroid;
- flux;
- onsets;
- número de frames;
- hop temporal.

La DensityTape debe utilizarse posteriormente para:

- visualización;
- seek;
- comprensión de la transición;
- representación del ritmo/densidad.

No cargar innecesariamente audio PCM completo en memoria.

---

# 14. Analysis Engine

El análisis debe ejecutarse fuera del hilo principal.

Conceptualmente:

```text
Scan
  ↓
Track discovered
  ↓
Check invalidation key
  ↓
AnalysisScheduler
  ↓
Audio decode
  ↓
PCM mono
  ↓
Feature extraction
  ↓
TrackProfile + DensityTape
  ↓
Persist
```

Debe existir como máximo una operación pesada de análisis simultánea salvo que la arquitectura real demuestre que otra estrategia es segura.

Debe priorizarse la pista que está reproduciéndose o que está a punto de reproducirse cuando sea posible.

Si el archivo desaparece, el análisis debe cancelarse de forma segura.

---

# 15. Características de audio

El sistema de análisis debe evolucionar hacia características equivalentes a las definidas en `AETHER_SPEC.md`:

- RMS;
- FFT;
- spectral centroid;
- spectral flux;
- detección de onset;
- BPM;
- confianza;
- beat grid;
- secciones;
- cuantización;
- embedding de 16 dimensiones.

Los valores y constantes deben tomarse de `AETHER_SPEC.md`.

No inventar algoritmos incompatibles si el spec ya define un comportamiento.

---

# 16. Similaridad musical

AETHER debe poder seleccionar la siguiente pista basándose en similitud entre perfiles.

El sistema debe:

1. Excluir la pista actual.
2. Evitar repetir las últimas pistas según las reglas del spec.
3. Considerar perfiles `READY`.
4. Calcular distancia entre embeddings.
5. Seleccionar la mejor candidata.
6. Aplicar fallback cuando no exista suficiente información.

Fallback:

```text
Perfil similar
      ↓
si no existe
      ↓
álbum/carpeta/cola
      ↓
si tampoco existe
      ↓
comportamiento seguro del reproductor
```

---

# 17. TransitEngine

Este es uno de los componentes centrales de la evolución.

Debe encargarse de decidir y preparar transiciones entre pistas.

Conceptualmente:

```text
Current Track
      ↓
Current TrackProfile
      ↓
Similarity
      ↓
Next Candidate
      ↓
Next TrackProfile
      ↓
Find transition point
      ↓
Beat/Onset alignment
      ↓
Crossfade
```

Debe utilizar los parámetros definidos en `AETHER_SPEC.md`.

No debe convertirse en responsabilidad de la UI.

---

# 18. Crossfade

La reproducción debe evolucionar hacia un sistema de transición entre dos pistas.

Cuando la plataforma lo permita, utilizar dos instancias/reproductores coordinados.

El sistema debe intentar:

- localizar el último onset de la pista actual;
- localizar el primer onset de la siguiente;
- considerar BPM;
- alinear beats cuando exista suficiente confianza;
- aplicar crossfade;
- utilizar fallback seguro.

El crossfade no debe generar:

- silencios inesperados;
- doble reproducción;
- saltos audibles injustificados;
- flashes de UI;
- pérdida del estado de reproducción.

---

# 19. Presence Mode

Presence debe convertirse en la experiencia principal de AETHER.

La interfaz debe evolucionar hacia una experiencia visual inmersiva.

Debe contemplar:

- visualización full-bleed;
- DensityTape;
- playhead;
- información mínima de canción;
- controles que aparecen temporalmente;
- gestos;
- navegación hacia biblioteca.

La UI debe evitar convertirse en un reproductor tradicional lleno de botones.

---

# 20. Gestos de Presence

Implementar progresivamente:

- tap central → mostrar chrome;
- double tap → play/pause;
- gesto vertical → volumen;
- gesto horizontal → seek;
- flick derecho → siguiente;
- flick izquierdo → anterior;
- long press → Mark;
- pinch → Session Map;
- gesto desde borde izquierdo → biblioteca.

Los gestos deben introducirse de manera incremental y no deben romper el scroll o interacción existente.

---

# 21. Shader / visualización

El visualizador debe evolucionar para responder a:

- tiempo;
- energía;
- centroid;
- flux;
- onset;
- progreso.

Debe evitar animaciones autónomas que parezcan independientes de la música.

El objetivo es que la visualización comunique el comportamiento musical.

Si el dispositivo no soporta adecuadamente el efecto visual avanzado, debe existir un fallback visual.

---

# 22. Marks

AETHER debe permitir marcar momentos de una pista.

Mínimo:

```text
trackId
positionMs
createdAtMs
```

La interacción principal será el long press definido en `AETHER_SPEC.md`.

No implementar funcionalidades futuras como fotos/notas si no forman parte de la fase actual.

---

# 23. Ritual Mode

Ritual debe implementarse después de que Presence y Transit sean estables.

Características principales:

- no avanzar automáticamente;
- comportamiento de final de pista definido por el spec;
- residue visual;
- salida controlada;
- pantalla activa cuando corresponda.

No introducir Ritual antes de tener una base estable de reproducción y estado.

---

# 24. Sessions

AETHER debe registrar sesiones de escucha.

Una sesión debe almacenar conceptualmente:

- inicio;
- final;
- modo;
- pistas;
- razón del cambio.

Razones:

```text
similarity
user
folder
ritual
```

La sesión comienza con la primera reproducción según las reglas del spec.

---

# 25. Session Map

El pinch de Presence puede abrir una representación de la sesión.

Debe mostrar:

- pistas reproducidas;
- orden;
- mini DensityTape;
- información mínima;
- navegación hacia el punto correspondiente.

Debe respetar la estética AETHER.

---

# 26. Estados especiales y errores

El proyecto debe manejar correctamente:

- permisos no concedidos;
- biblioteca vacía;
- archivo ilegible;
- análisis lento;
- archivo eliminado;
- pérdida de audio focus;
- llamada/interrupción;
- servicio detenido;
- fallo de reproducción.

Los errores deben ser tratados como estados de producto y no únicamente como excepciones técnicas.

---

# 27. Rendimiento

La evolución debe respetar los objetivos del `AETHER_SPEC.md`.

Prioridades:

1. UI fluida.
2. No bloquear Main Thread.
3. No cargar canciones completas en PCM si no es necesario.
4. Análisis controlado.
5. Uso razonable de memoria.
6. Visualización eficiente.
7. Persistencia compacta.

Especial atención a:

- canciones largas;
- bibliotecas grandes;
- múltiples carátulas;
- análisis concurrente;
- crossfade;
- visualizador.

---

# 28. Compatibilidad Android

Respetar la estrategia de permisos definida en el spec y adaptarla a la implementación actual.

No solicitar permisos innecesarios.

El proyecto no debe solicitar:

- ubicación;
- cámara;

si no son necesarios para una funcionalidad explícitamente aprobada.

---

# 29. Reglas para el agente de IA

El agente debe seguir estas reglas:

### REGLA 1
Leer el código antes de modificarlo.

### REGLA 2
No asumir arquitectura.

### REGLA 3
No reconstruir el proyecto.

### REGLA 4
No migrar de framework.

### REGLA 5
No borrar funcionalidades existentes sin justificación.

### REGLA 6
No crear duplicados de servicios o fuentes de verdad.

### REGLA 7
Implementar una fase a la vez.

### REGLA 8
Compilar después de cambios importantes.

### REGLA 9
Ejecutar lint/tests disponibles.

### REGLA 10
Documentar cambios relevantes.

### REGLA 11
Si una decisión arquitectónica es ambigua, detenerse y explicar las alternativas antes de hacer una modificación destructiva.

### REGLA 12
No implementar funcionalidades fuera del alcance de `AETHER_SPEC.md` sin autorización.

---

# 30. Prohibiciones iniciales

Hasta que el usuario lo solicite explícitamente, NO implementar:

- login;
- backend obligatorio;
- sincronización en la nube;
- letras dependientes de Internet;
- redes sociales;
- karaoke;
- exportación de video;
- meteorología;
- reconocimiento facial;
- integración con otros teléfonos;
- ecualizador de 10 bandas en Home;
- segunda skin visual;
- funcionalidades 3D complejas fuera del alcance actual.

Estas funcionalidades pueden pertenecer a ideas futuras, pero no deben distraer la evolución del núcleo de AETHER.

---

# 31. Orden recomendado de evolución

El orden base recomendado es:

```text
FASE 0
Auditoría
        ↓
FASE 1
Arquitectura/persistencia de TrackRecord
        ↓
FASE 2
Analysis Engine
        ↓
FASE 3
TrackProfile
        ↓
FASE 4
DensityTape
        ↓
FASE 5
Presence visual
        ↓
FASE 6
Similarity Engine
        ↓
FASE 7
TransitEngine
        ↓
FASE 8
Onset-aware Crossfade
        ↓
FASE 9
Gestos Presence
        ↓
FASE 10
Marks
        ↓
FASE 11
Ritual
        ↓
FASE 12
Sessions
        ↓
FASE 13
Session Map
        ↓
FASE 14
Optimización y hardening
```

Este orden puede cambiar después de la auditoría si el código existente permite una estrategia mejor.

---

# 32. Criterio de finalización

AETHER no se considera terminado únicamente porque compile.

Una fase se considera terminada cuando:

- compila;
- no introduce errores evidentes;
- conserva funcionalidades anteriores;
- cumple sus requisitos;
- tiene estados de error razonables;
- tiene pruebas o validación manual apropiada;
- su integración con la arquitectura existente está documentada.

El objetivo final es que AETHER deje de ser simplemente:

```text
"un reproductor de MP3"
```

y evolucione hacia:

```text
"un reproductor musical consciente de las características
de las pistas, capaz de visualizar su densidad,
seleccionar transiciones y crear una experiencia
continua de escucha."
```

---

# 33. Protocolo de trabajo obligatorio

Cada nueva fase debe seguir:

```text
1. Leer AETHER_SPEC.md
2. Leer AETHER_EVOLUTION_SPEC.md
3. Leer AUDIT.md / ROADMAP.md si existen
4. Inspeccionar código relacionado
5. Explicar qué se va a modificar
6. Implementar únicamente la fase solicitada
7. Compilar
8. Ejecutar lint/tests disponibles
9. Revisar regresiones
10. Documentar resultado
11. Esperar autorización para la siguiente fase
```

No saltarse directamente del paso 1 al paso 6.

---

# 34. Regla final

> **AETHER debe evolucionar, no reiniciarse.**

Cada nueva funcionalidad debe sentirse como una extensión natural del proyecto existente.

El agente debe preferir:

```text
REUTILIZAR
    >
INTEGRAR
    >
REFACTORIZAR
    >
REEMPLAZAR
    >
RECONSTRUIR
```

La reconstrucción completa solamente está permitida si una auditoría demuestra que el componente actual es incompatible con el objetivo y el usuario aprueba explícitamente su reemplazo.

---

## Fin de AETHER_EVOLUTION_SPEC.md
