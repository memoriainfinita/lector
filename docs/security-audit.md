# Auditoría de seguridad

| Campo | Valor |
|-------|-------|
| Fecha | 2026-10-15 |
| Commit | `ef26614` |
| Método | Revisión estática del código. Sin compilar, sin pruebas en dispositivo |

## Alcance

- Manifiestos `main` y `debug`
- Build y firma (`app/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`, wrapper)
- Componentes exportados: `PlaybackService`, `MediaButtonReceiver`, `LectorWidgetReceiver`, `CoverProvider`, `MainActivity`
- `FileProvider` y `file_paths.xml`
- Importación y exportación de copias (`data/backup/`)
- Ajustes en DataStore (`data/settings/`)
- Escáner, parser CUE, lector de metadatos, portadas
- Borrado de archivos, compartir, portapapeles
- Historial git (50 commits)

## Hallazgos

| ID | Severidad | Resumen | Verificación |
|----|-----------|---------|--------------|
| H1 | Media-baja | Copia manipulada → "Borrar del móvil" borra archivos ajenos al libro | Lectura de código |
| H2 | Media-baja | Copia manipulada con "Importar también los ajustes" → cierre en cada arranque | Lectura de código |
| H6 | Baja-media | Apps sin privilegios cargan URIs arbitrarias en el reproductor | Lectura de código y documentación Media3 |
| H3 | Baja | APK debug firmado con la clave de release | Lectura de configuración |
| H4 | Informativo | Cualquier app instalada navega la biblioteca | Lectura de código |
| H5 | Informativo | `MANAGE_EXTERNAL_STORAGE` | Lectura del manifiesto |

### H1. Borrado de archivos fuera del libro

- `Import.kt` (`addMissing`) guarda `path` y `files[].path` de la copia sin validar
- `LibraryViewModel.kt:244` borra `File(base, relativePath)` sin comprobar que quede dentro de `base`
- Admite `../` y rutas absolutas
- Condiciones: importar la copia, devolver el libro a la biblioteca, confirmar "Borrar del móvil"
- `MANAGE_EXTERNAL_STORAGE` extiende el alcance a todo el almacenamiento
- Arreglo propuesto: comprobar con `canonicalPath` que el archivo está bajo `base` antes de borrar; rechazar `..` y rutas absolutas al importar

### H2. Cierre persistente al arrancar

- `importSettings` (`Import.kt`) acepta cualquier clave, tipo y valor
- `AppearanceRepository.kt:36-37` (`theme_light_at`, `theme_dark_at`) y `PlaybackSettingsRepository.kt:128-129` (`sleep_from`, `sleep_to`) llaman a `LocalTime.ofSecondOfDay` sin capturar errores
- Valor fuera de 0..86399 → excepción
- `MainActivity.kt:40` lee la apariencia al arrancar; el valor persiste en DataStore
- Recuperación: borrar los datos de la app (se pierden posiciones y marcadores)
- Condición: marcar "Importar también los ajustes"
- Arreglo propuesto: validar el rango al leer y al importar

### H6. URIs arbitrarias en el reproductor desde otras apps

- El `onAddMediaItems` por defecto de Media3 acepta elementos con `LocalConfiguration` (URI) ([referencia](https://developer.android.com/reference/androidx/media3/session/MediaSession.Callback))
- `onConnectAsync` (`PlaybackService.kt:241-242`) da a los controladores sin privilegios los comandos por defecto, incluidos los de cambiar elementos
- `onSetMediaItems` (`PlaybackService.kt:336`) delega en Media3 los elementos con URI
- `LectorPlayer.handleSetMediaItems` (`LectorPlayer.kt:41-42`) solo los ignora con un libro cargado
- `handleAddMediaItems` no está sobrescrito: añadir elementos llega a ExoPlayer aunque haya libro
- Efecto posible: una app sin permiso de almacenamiento hace que Lector abra archivos con su acceso a todo el almacenamiento, lee sus etiquetas por la sesión y altera la lista del libro
- No reproducido en dispositivo
- Arreglo propuesto: quitar los comandos de cambio de elementos a controladores sin privilegios en `onConnectAsync`, o rechazar elementos con URI en `onAddMediaItems` para controladores ajenos

### H3. Debug firmado con la clave de release

- `app/build.gradle.kts:36`
- El APK debug se instala sobre el de release y es depurable: `run-as` por adb lee la base de datos
- `src/debug/AndroidManifest.xml` exporta `CatalogActivity`, `ScanActivity`, `PlaybackActivity`
- `PlaybackActivity` acepta `--es open <id>`
- Mitigación: distribuir solo el APK release

### H4. Biblioteca visible para otras apps

- `PlaybackService` exportado, necesario para Android Auto y Bluetooth
- Un controlador sin privilegios ve títulos, autores, progreso y títulos de marcadores, y controla la reproducción
- Sin acceso a notas de marcadores ni a archivos por esta vía (ver H6)
- Comandos propios solo para la app, controladores de confianza y Android Auto (`PlaybackService.kt:241`)

### H5. `MANAGE_EXTERNAL_STORAGE`

- Acceso a todo el almacenamiento compartido
- Decisión de diseño: sin SAF por rendimiento, sin Google Play
- Amplifica H1 y H6

## Sin problemas encontrados

- Sin permiso `INTERNET` ni código de red
- Historial git sin claves, contraseñas, tokens, IPs ni rutas personales
- Firma leída de propiedades de Gradle del usuario, fuera del repo
- `CoverProvider`: solo lectura, nombre `[A-Za-z0-9-]+\.jpg`, un segmento, sin traversal
- `FileProvider` no exportado, limitado a `/storage`
- Único `PendingIntent` con `FLAG_IMMUTABLE`
- Sin `@RawQuery` ni SQL concatenado; migraciones automáticas declaradas
- Enums de ajustes con `runCatching`
- Preamplificador, ecualizador y velocidad acotados al aplicarse
- Errores de CUE y metadatos capturados durante el escaneo
- Acciones del widget validadas con `PlayerAction.valueOf` y `runCatching`
- `MainActivity` solo atiende `ACTION_OPEN_PLAYER`, sin extras
- `BookEngine.open` solo abre IDs presentes en la base de datos (`BookEngine.kt:97`)
- Exportar, copiar y compartir solo por acción del usuario, con selector del sistema
- Repositorios de dependencias: `google()` y `mavenCentral()`, con `FAIL_ON_PROJECT_REPOS`

## Sin verificar

- Manifiesto fusionado: no compilado; no comprobado que ninguna dependencia añada `INTERNET`
- Checksum de `gradle/wrapper/gradle-wrapper.jar` frente al oficial de Gradle 9.8.0: el proxy de la sesión bloqueó `services.gradle.org`
- Avisos de seguridad de dependencias: búsqueda sin resultados para Media3 1.11.1; no consultadas NVD ni GitHub Advisory Database
