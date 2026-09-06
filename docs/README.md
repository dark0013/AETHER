# AETHER - Aplicación de Música (v0.2 "AETHER Look")

AETHER es un reproductor de música para Android con una estética moderna basada en Material Design 3.

## Funcionalidades Actuales (v0.2)

- **Diseño AETHER Look**: Interfaz oscura (#121212) con acentos lavanda (#C4B5FD).
- **Escaneo Inteligente**: Filtra audios de WhatsApp/Telegram y archivos basura (duration > 0 y >= 30s).
- **Gestión de Arte de Álbum**: Carga portadas de discos desde MediaStore con Coil; placeholders elegantes si no hay imagen.
- **Búsqueda en Tiempo Real**: Filtra tu biblioteca instantáneamente por título o artista.
- **Controles Avanzados**: Soporte para Shuffle y Repeat integrados con Media3.
- **Mini Player con Progreso**: Barra de progreso visual integrada en la parte inferior.
- **Now Playing Expandido**: Bottom sheet con arte a gran escala, controles grandes y navegación completa.
- **Reproducción en Segundo Plano**: Media3 Session Service para continuidad de audio.

## Arquitectura

- **UI**: Jetpack Compose (Material 3).
- **Imágenes**: Coil Compose.
- **Motor de Audio**: Media3 (ExoPlayer).
- **Patrón**: MVVM con StateFlow y Combine para reactividad.

---
*Este documento describe el estado actual del desarrollo del proyecto AETHER.*
