# Auditoría de seguridad

| Campo | Valor |
|-------|-------|
| Fecha | 2026-10-15 |
| Commit | `ef26614` |
| Método | Revisión estática del código y del APK publicado. Sin pruebas en dispositivo |
| APK | `lector-1.0.0.apk` de la release `v1.0.0` (tag en `042c3ad`; `main` solo añade un commit de docs) |

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
| H7 | Informativo | Auto Backup de Android activo por defecto | Lectura del manifiesto y documentación Android |

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

### H7. Auto Backup de Android

- `AndroidManifest.xml` no declara `allowBackup` ni `dataExtractionRules`; `allowBackup` vale `true` por defecto ([Auto Backup](https://developer.android.com/identity/data/autobackup))
- Con `targetSdk` 37, el sistema copia los directorios internos de la app al Google Drive del usuario, salvo caché y `noBackupFilesDir`
- Datos afectados: `lector.db` (biblioteca, rutas, posiciones, marcadores con notas), DataStore `settings`, portadas
- Cifrado de extremo a extremo con el bloqueo de pantalla en Android 9+
- No es acceso de terceros: los datos quedan en la cuenta del propio usuario
- Inclusión concreta de `databases/` no confirmada en la documentación consultada
- Decisión pendiente: mantener, excluir con `dataExtractionRules`, o `allowBackup="false"` dejando solo la copia propia de Ajustes › Datos

## APK publicado (`v1.0.0`)

Analizado con androguard 4.1.4.

- SHA-256 `a19b7533b0b4d927c1505ba75223b13c416294006694d0e5d0d008c430d2a19c`, igual al digest publicado por GitHub
- Firma APK v2; certificado `CN=memoriainfinita, O=memoriainfinita`, SHA-256 `B5:4A:38:6D:9F:D0:01:60:08:EC:98:1C:55:C6:ED:ED:A1:D6:42:6A:F0:84:74:51:45:73:12:0D:71:F4:11:BF`
- Sin `debuggable`, sin `testOnly`
- Sin clases `codelab.lector.debug`: las actividades de H3 no están en la release
- `allowBackup` sin declarar: confirma H7
- Sin `usesCleartextTraffic` ni `networkSecurityConfig`
- Permisos del manifiesto fusionado:
  - Propios: `MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  - Añadidos por librerías: `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
  - Sin `INTERNET`
- Componentes exportados añadidos por librerías, todos protegidos por permiso de sistema:
  - `BluetoothValidationActivity` (`BLUETOOTH_PRIVILEGED`)
  - `GlanceRemoteViewsService` (`BIND_REMOTEVIEWS`)
  - `SystemJobService` (`BIND_JOB_SERVICE`)
  - `DiagnosticsReceiver`, `ProfileInstallReceiver` (`DUMP`)
- Componentes exportados sin permiso: los mismos del código fuente (`MainActivity`, `PlaybackService`, `MediaButtonReceiver`, `LectorWidgetReceiver`, `CoverProvider`)
- URLs en el DEX: solo de librerías (mensajes de error, enlaces a incidencias)
- Sin patrones de claves o tokens en el DEX; sin keystores ni `local.properties` en el APK

## Sin problemas encontrados

- Sin permiso `INTERNET` ni código de red (confirmado en el APK)
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

- Checksum de `gradle/wrapper/gradle-wrapper.jar` frente al oficial de Gradle 9.8.0: el proxy de la sesión bloqueó `downloads.gradle.org`. SHA-256 local: `238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5`
- Avisos de seguridad de dependencias: búsqueda web sin resultados para Media3 1.11.1; `api.osv.dev` bloqueado por el proxy
- Entorno de la auditoría sin Android SDK ni acceso a Google Maven: sin build, Lint ni tests
- Bytecode del APK: solo inventario de strings, sin decompilar ni comparar con el código fuente

## Cobertura frente a OWASP MASVS

| Categoría | Estado |
|-----------|--------|
| STORAGE | H1, H7. Sin pruebas en dispositivo |
| CRYPTO | No aplica: la app no cifra datos |
| AUTH | No aplica: sin cuentas |
| NETWORK | Sin permiso `INTERNET`, confirmado en el APK |
| PLATFORM | H3, H4, H6; proveedores, intents y widget revisados |
| CODE | H1, H2; dependencias sin escanear |
| RESILIENCE | No revisado; `isMinifyEnabled = false` |
| PRIVACY | H4, H7; logs solo con rutas (3 llamadas a `Log.w`) |

Referencia: [OWASP MAS checklist](https://devguide.owasp.org/04-design/03-mas-checklist)

## Pendiente en local

- [ ] Comparar el SHA-256 del wrapper con `https://services.gradle.org/distributions/gradle-9.8.0-wrapper.jar.sha256`
- [ ] `./gradlew lint` y revisar las categorías de seguridad del informe
- [ ] Escanear dependencias con `osv-scanner` sobre `gradle/libs.versions.toml`
- [ ] Analizar el APK release con [MobSF](https://github.com/MobSF/Mobile-Security-Framework-MobSF)
- [ ] Reproducir H1 en dispositivo con un libro de prueba y una copia manipulada
- [ ] Reproducir H2 importando `theme_light_at` fuera de 0..86399
- [ ] Reproducir H6 con un `MediaController` desde una app de prueba
- [ ] Confirmar qué incluye Auto Backup (H7): `adb shell bmgr backupnow io.github.memoriainfinita.lector` y revisar el conjunto de copia
- [ ] Tests de entradas malformadas en `ImportTest.kt` (rutas con `..`, absolutas, ajustes fuera de rango) y para `parseCue`
