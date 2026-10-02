---
created: 2026-10-02
last_updated: 2026-10-02
---

# LECTOR — state

Qué es verdad ahora. Lo que ya pasó vive en `state-history.md`.

## Project

Reproductor de audiolibros para Android, clon propio de Simple ABP: mismo minimalismo, más pulido, con portadas, navegación moderna y marcadores con notas y tags. Funciones y decisiones en `design.md`.

## Status

- Fase de diseño. Sin código. Stack aprobado: Kotlin, Compose, Media3 1.11, Room (`design.md` › Recomendaciones técnicas). Voice como referencia de inspiración, sin copiar código (`design.md` › Referencia: Voice)
- Inventario de Simple ABP hecho (en `design.md`). Capturas, árboles de UI y textos del APK en `ref/sabp/`
- Diseño de pantallas cerrado en el lienzo: https://claude.ai/artifact/NnT6pjF1vGhypwmLuweFSC. Todas las decisiones en `design.md` › "Decisiones de diseño (lienzo)", con icono, color de acento y sistemas de botones y texto. Ideas de Voice ubicadas en el lienzo (versión 141); su ubicación en `design.md` › "Referencia: Voice"
- Git local, rama `main`, sin remoto. `.gitignore` excluye `.backups/`. `ref/` contiene nombres de libros del usuario: revisar antes de publicar
- En esta máquina no hay Android Studio, SDK ni Java. Hay `adb` (de scrcpy). El móvil del usuario es un Xiaomi (Android 15): adb no puede inyectar toques sin activar "Depuración USB (ajustes de seguridad)"

## TODO

- [ ] Instalar JDK, SDK de línea de comandos y Android Studio