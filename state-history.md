# LECTOR — history

Lo que salió de `state.md`. Se escribe solo si explica por qué las cosas son como son.

Más reciente arriba.

### 2026-10-04 — Sin menú inferior: una sola pila

**Qué cambió:** la navegación aprobada el 2026-10-03 (menú inferior con Biblioteca, Escuchando y Marcadores, una pila por pestaña) se sustituyó por una sola pila con Biblioteca en la raíz. Marcadores y Ajustes se abren desde la cabecera de Biblioteca; Escuchando, desde el minirreproductor.
**Por qué:** el usuario quería ganar espacio. Escuchando ya tenía acceso por el minirreproductor y la barra solo aportaba Marcadores, que cabe en un icono. Antes de quitarla se redujo de 48 a 40.

### 2026-10-02 — Inventario de Simple ABP con adb en un Xiaomi

**Qué cambió:** el inventario se sacó de los textos del APK (`resources.arsc` leído con un script Python, sin androguard) y de capturas automáticas (`uiautomator dump` + `screencap` cada vez que cambiaba la pantalla) mientras el usuario navegaba a mano.
**Por qué:** HyperOS bloquea `adb shell input tap` (falta INJECT_EVENTS) salvo que se active "Depuración USB (ajustes de seguridad)". En Git Bash, `MSYS_NO_PATHCONV=1` es necesario para rutas `/sdcard/...`.

### 2026-10-02 — Reproductor B y variantes de tags descartados

**Qué cambió:** se eligió el reproductor A (portada grande). Descartados el reproductor B (estructura de Simple ABP pulida) y tres formas de tags (texto con #, puntos de color, agrupar por tag).
**Por qué:** el usuario prefirió la portada como protagonista y las etiquetas rectangulares pequeñas.

### 2026-10-02 — Nombre: MARGO → AUDITIO → LECTOR

**Qué cambió:** el proyecto se llamó MARGO, luego AUDITIO, y quedó LECTOR.
**Por qué:** MARGO (margen) ponía las notas por delante del reproductor. AUDITIO se cambió por LECTOR (el esclavo que leía en voz alta), que admite soportar libros de lectura más adelante.
