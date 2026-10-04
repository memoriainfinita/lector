---
created: 2026-10-02
last_updated: 2026-10-04
---

# LECTOR — state

Qué es verdad ahora. Lo que ya pasó vive en `state-history.md`.

## Project

Reproductor de audiolibros para Android, clon propio de Simple ABP: mismo minimalismo, más pulido, con portadas, navegación moderna y marcadores con notas y tags. Funciones y decisiones en `design.md`.

## Status

- Esqueleto Compose funcionando en el móvil (2026-10-03): un módulo `app`, applicationId `codelab.lector`, minSdk 26, compile/targetSdk 37. AGP 9.4.1 (Kotlin incorporado, sin plugin kotlin-android), Kotlin/plugin Compose 2.4.20, Compose BOM 2026.09.00, Gradle 9.8.0 por wrapper. Compilar: `gradlew assembleDebug` con JAVA_HOME en el jbr de Android Studio (la shell Bash de Claude Code no hereda ANDROID_HOME ni GRADLE_USER_HOME: exportarlos); instalar: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Media3, Room, Navigation3 y Coil se añaden con su función
- Arquitectura y modelo de datos aprobados (`design.md` › Arquitectura, Modelo de datos). Base de datos Room 3 hecha y probada (6 pruebas en el ordenador: gradlew testDebugUnitTest). Sistema visual hecho (`design.md` › Sistema visual; catálogo en la versión de depuración, entrada "LECTOR catálogo"). Escaneo hecho y probado con la biblioteca real (`design.md` › Escaneo; pantalla de depuración "LECTOR escaneo"; acceso a archivos concedido por adb: `adb shell appops set --uid codelab.lector MANAGE_EXTERNAL_STORAGE allow`). Base de datos del móvil: `adb exec-out run-as codelab.lector cat databases/lector.db` (más -wal y -shm). Motor de reproducción hecho y probado en el móvil, entregas A y B (`design.md` › Reproducción; pantalla de depuración "LECTOR reproducción", manejable por adb con extras de intent, ver `PlaybackActivity`). Base de datos en versión 2. Navegación hecha (`design.md` › Navegación): una pila por pestaña, pantallas completas encima, primer arranque y apertura de Escuchando desde la notificación (MainActivity singleTask). Todas las rutas con pantallas vacías y sus enlaces. Escuchando hecha (design.md › Pantallas): reproductor, hojas de ⋯, velocidad, sonido y capítulos, minirreproductor y visor de portada. Biblioteca, entrega A hecha (design.md › Pantallas › Biblioteca): cuadrícula con Seguir escuchando, filtros, ordenar, pellizco de 1 a 3 columnas, estados vacío y buscando, búsqueda rápida al abrir. Sin menú inferior desde el 2026-10-04: una sola pila con Biblioteca en la raíz (design.md › Navegación). Carpetas, ⋮ y búsqueda aún vacíos. Siguiente: Biblioteca B (Carpetas), después C (menú del libro) y D (búsqueda)
- Stack aprobado: Kotlin, Compose, Media3 1.11, Room (`design.md` › Recomendaciones técnicas). Voice como referencia de inspiración, sin copiar código (`design.md` › Referencia: Voice)
- Inventario de Simple ABP hecho (en `design.md`). Capturas, árboles de UI y textos del APK en `ref/sabp/`
- Diseño de pantallas cerrado en el lienzo: https://claude.ai/artifact/NnT6pjF1vGhypwmLuweFSC. Todas las decisiones en `design.md` › "Decisiones de diseño (lienzo)", con icono, color de acento y sistemas de botones y texto. Ideas de Voice ubicadas en el lienzo (versión 141); su ubicación en `design.md` › "Referencia: Voice". Clases de carpeta y escaneo añadidos en la versión 143. Enlaces del prototipo ajustados a la navegación en la versión 145; ⋯ abajo e iconos en uso en la versión 146
- Git local, rama `main`, sin remoto. `.gitignore` excluye `.backups/`. `ref/` contiene nombres de libros del usuario: revisar antes de publicar
- Entorno: Android Studio 2026.2 en C:\Program Files\Android\Android Studio (el instalador ignora --location), JDK incluido OpenJDK 25.0.3 en su jbr\. SDK en D:\Android\Sdk (android-37.0, build-tools 36.0.0, sin cmdline-tools). Variables de usuario ANDROID_HOME=D:\Android\Sdk y GRADLE_USER_HOME=D:\Android\gradle. adb de scrcpy en el PATH; el del SDK no. Móvil Xiaomi (Android 15): adb no puede inyectar toques sin "Depuración USB (ajustes de seguridad)". Xiaomi: requiere "Instalar vía USB" activado en Opciones de desarrollador

## TODO

- [ ] Lienzo: quitar el menú inferior de las artboards, cabecera de Biblioteca en una fila (selector con iconos, buscar, Marcadores, Ajustes con engranaje), minirreproductor pegado abajo y semitransparente. Regenerar "Iconos en uso" (iconos nuevos: buscar, ordenar, ⋮ vertical, engranaje)
- [ ] Biblioteca C: decidir "Quitar de la biblioteca" para libros inaccesibles (propuesta: borra si no tiene marcadores; con marcadores sigue atenuado). Hoy no hay forma de quitarlos y quedan dos en el móvil (Mentats borrado y su ".trashed-…")
- [ ] Revisar la portada de la notificación: el log de systemui da `EACCES` al leer `files/covers/…jpg`; parece que la sesión pasa una ruta privada de la app. Sin comprobar en pantalla

