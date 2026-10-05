# LECTOR — history

Lo que salió de `state.md`. Se escribe solo si explica por qué las cosas son como son.

Más reciente arriba.

### 2026-10-06 — Sin retrasos de botones

**Qué cambió:** "Retrasar los botones inferiores" y "Retrasar el botón Atrás", del inventario de Simple ABP y del lienzo, salen de LECTOR; sus filas se quitan en Ajustes C2.
**Por qué:** el usuario no detecta retraso en nada en Simple ABP. En LECTOR los toques accidentales ya se corrigen con "Deshacer" o con el salto contrario, y un retraso haría más lento cada toque querido.

### 2026-10-05 — Posición por tramo y "Deshacer" de 10 s

**Qué cambió:** LECTOR guardaba una sola posición por libro; ahora guarda también la de cada tramo (capítulo, o archivo sin capítulos) al salir de él a medias, y volver lo retoma. "Siguiente archivo desde su posición" dejó de estar inactiva. El "Deshacer" de los saltos grandes pasó de 5 a 10 s.
**Por qué:** el usuario pasó de capítulo para probar y ya no pudo volver a donde iba: "anterior" empezaba el capítulo de nuevo. En Simple ABP se comprobó (2026-10-05) que guarda posición por archivo y que la opción "Allow next file to start from non-zero position" solo decide si el paso automático al terminar un archivo retoma el siguiente; los botones siempre retoman. Lista de capítulos: retoma, decidido por el usuario ("si el usuario quiere comenzarlo puede").

### 2026-10-05 — Notificación: saltos cortos junto a play

**Qué cambió:** de −30, anterior, play, siguiente, marcar a capítulo anterior, −10, play, +10, capítulo siguiente. Marcar sale hasta Ajustes C.
**Por qué:** el usuario pasaba de capítulo sin querer desde la notificación, donde no hay "Deshacer". Pidió los capítulos "más pequeños y a los lados": el tamaño lo decide HyperOS (cinco botones iguales); los huecos extra salen en los extremos, así que los capítulos van ahí.

### 2026-10-05 — Sin portadas: lista y portada tipográfica

**Qué cambió:** la decisión del 2026-10-04 ("Mostrar portadas" apagado: la superficie con el título en cuadrícula, filas, búsqueda y reproductor) se sustituyó. Sin portadas, la Biblioteca pasa a lista y Escuchando quita el recuadro; los libros sin portada, con portadas activadas, llevan una portada tipográfica con tono por autor.
**Por qué:** al probarlo, el usuario vio que el reproductor no quitaba la portada y que la cuadrícula sin portadas era una pared de recuadros vacíos. Sin imágenes, la cuadrícula pierde su función; la lista, como Simple ABP, es más densa y legible. Colores aleatorios por libro descartados: no significan nada y chocan con el oscuro de un solo acento.

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
