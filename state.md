---
created: 2026-10-02
last_updated: 2026-10-02
---

# LECTOR — state

Qué es verdad ahora. Lo que ya pasó vive en `state-history.md`.

## Project

Reproductor de audiolibros para Android, clon propio de Simple ABP: mismo minimalismo, más pulido, con portadas, navegación moderna y marcadores con notas y tags. Funciones y decisiones en `design.md`.

## Status

- Fase de diseño. Sin código
- Inventario de Simple ABP hecho (en `design.md`). Capturas, árboles de UI y textos del APK en `ref/sabp/`
- Lienzo de diseño con todas las pantallas: https://claude.ai/artifact/NnT6pjF1vGhypwmLuweFSC. Decisiones confirmadas y propuestas en `design.md` › "Decisiones de diseño (lienzo)"
- Git local, rama `main`, sin remoto. `.gitignore` excluye `.backups/`. `ref/` contiene nombres de libros del usuario: revisar antes de publicar
- En esta máquina no hay Android Studio, SDK ni Java. Hay `adb` (de scrcpy). El móvil del usuario es un Xiaomi (Android 15): adb no puede inyectar toques sin activar "Depuración USB (ajustes de seguridad)"

## TODO

- [ ] Decidir stack y acceso a archivos (`design.md` › Recomendaciones técnicas)
- [ ] Instalar Android Studio
