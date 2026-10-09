# LECTOR — history

Lo que salió de `state.md`. Se escribe solo si explica por qué las cosas son como son.

Más reciente arriba.

### 2026-10-10 — Release con clave propia y applicationId para publicar

**Qué cambió:** la release dejó de firmarse con la clave de depuración y el applicationId pasó de `codelab.lector` a `io.github.memoriainfinita.lector` (el paquete Kotlin sigue en `codelab.lector`). La de depuración se firma con la misma clave para seguir instalándose encima de la release. Las órdenes `am start -n` llevan el nombre de clase completo. `ref/` salió del historial (con `filter-repo`, junto con las series de los móviles); bundle previo guardado fuera del repo.
**Por qué:** preparar la publicación en GitHub. Una release pública firmada con la clave de depuración no se puede actualizar de forma fiable, y cambiar firma o applicationId después obliga a desinstalar. El formato `io.github.<usuario>` no depende de tener dominio.

### 2026-10-09 — Ya no se reinstala la release tras las pruebas

**Qué cambió:** al cerrar las pruebas por adb se volvía a instalar la release después de copiar datos con la de depuración. Retirado.
**Por qué:** a petición del usuario: no alternar versiones. En el móvil principal queda la de depuración hasta que pida la release.

### 2026-10-08 — m4b largos con duración 0

**Qué cambió:** salió del TODO, resuelto.
**Por qué:** memoria agotada (256 MB por app) al leer varios m4b largos a la vez en la búsqueda, y el fallo se guardaba en caché como duración 0 sin repetirse. Se descartó antes el índice (moov) al final del archivo: Heir of Caladan lo tiene y se leía bien.

### 2026-10-08 — Datos desde el nombre de carpeta, reader.txt, OPF y NFO: descartados

**Qué cambió:** salieron del TODO, decidido por el usuario.
**Por qué:** el nombre de carpeta solo da el título cuando falta la etiqueta de álbum, y quien organiza al estilo Audiobookshelf (o tiene OPF / NFO de Audiobookshelf o Calibre) suele tener los archivos etiquetados: mandan las etiquetas. Lo que añadía (número de serie, narrador en libros sin etiquetas) no compensaba; OPF / NFO era lo más trabajoso. Reglas de Audiobookshelf consultadas: audiobookshelf.org/docs › Libraries › Book Library Structure.

### 2026-10-08 — Mp3 de tasa variable sin cabecera: descartado

**Qué cambió:** salió del TODO, decidido por el usuario: ni arreglo ni aviso.
**Por qué:** es raro (LAME escribe la cabecera Xing o Info por defecto; aparece en mp3 unidos o cortados con herramientas que la pierden) y el efecto es menor: el libro suena entero, solo los saltos caen desviados y la duración puede salir algo mal. Arreglarlo exigía copiar el `Mp3Extractor` de Media3: en 1.11.1 el salto por índice solo se usa si no hay otra forma de saltar, y con el tamaño del archivo siempre la hay (`computeSeeker`).

### 2026-10-08 — Formatos: por bibliotecas en general, no por la del usuario

**Qué cambió:** el TODO de formatos estaba en "solo si aparecen", tras comprobar que la biblioteca del usuario no tenía esos casos. El usuario lo corrigió: la app es para cualquier biblioteca. Se hicieron el orden por pista, los discos con texto, los formatos no admitidos visibles y mka / mp4 / webm.
**Por qué:** su biblioteca sirve para probar, no para decidir alcance. Además, "ALAC y AC-3 necesitan la extensión de FFmpeg" era falso en el móvil principal: se reproducen con sus decodificadores. Por eso el formato no admitido se decide por el decodificador del móvil (Media3 `MediaCodecUtil`), no por una lista fija de códecs; la lista fija queda solo para wma, aax, aaxc y ape, que Media3 no sabe abrir.

### 2026-10-08 — Importar con libros en otras rutas: por contenido, no por duración

**Qué cambió:** salió del TODO. Un libro importado que en el móvil nuevo está en otra ruta (o un libro de un archivo con la carpeta renombrada) no se reconectaba: la copia no llevaba tamaños, la importación guardaba `sizeBytes = 0` y el paso por contenido no lo veía. Ahora la copia lleva el tamaño de cada archivo, la importación lo guarda y el plan de Combinar también empareja por contenido.
**Por qué:** no se amplió la búsqueda por duración a los libros importados fuera de las carpetas de la biblioteca. Con el contenido no hace falta, y emparejar solo por duración entre libros quitados puede pegar posición y marcadores a otro libro. Las copias anteriores, sin tamaños, siguen como antes: para pasar a un móvil nuevo, exportar de nuevo.

### 2026-10-07 — Biblioteca sin refrescar un libro perdido: era la prueba

**Qué cambió:** salió del TODO. Un libro marcado inaccesible por la búsqueda al abrir seguía en la Biblioteca como disponible (menú completo, sin "no encontrado") hasta reiniciar.
**Por qué:** solo pasó tras sustituir el archivo de la base por adb con la app parada. Por el camino real (borrar la carpeta y volver a abrir, con el proceso vivo y con el proceso muerto) la Biblioteca muestra "no encontrado" y el menú de un libro sin archivos. Tras cambiar la base por adb, no fiarse de lo que muestra la app sin reiniciarla.

### 2026-10-06 — Escuchando: la portada ya se mueve

**Qué cambió:** deslizar la portada hacia abajo actuaba al soltar y la portada no se movía (2026-10-04); ahora la pantalla entera sigue al dedo y Escuchando sube y baja como las hojas, también desde el minirreproductor.
**Por qué:** el usuario quería la app más pulida. No se hizo con ModalBottomSheet como las hojas: abre su propia ventana, que tapaba el aviso "Deshacer" y dejaba Ajustes debajo. Tampoco con el Atrás predictivo de NavDisplay: al soltar acaba siempre con un tween fijo, sin rebote ni velocidad.

### 2026-10-06 — Barras de Escuchando: el tamaño vuelve

**Qué cambió:** las barras crecieron (zona táctil de 44, barras de 8 y 6, tiempos de 15) junto con el tiempo grande y el arrastre fino; después volvieron a su tamaño (24, 6 y 3, tiempos de 13) y se quedaron solo el tiempo grande, el arrastre fino y la barra que engorda al arrastrar.
**Por qué:** el usuario comprobó que con el tiempo grande y el arrastre fino ya es fácil ir al punto que quiere, y no quería perder alto para la portada.

### 2026-10-06 — Girar por adb, otra vez permitido

**Qué cambió:** `state.md` decía que no se tocara el giro automático por adb; ahora se puede girar para probar, avisando al usuario al terminar para que vuelva a encender el giro automático.
**Por qué:** en este móvil el giro automático no se deja volver a encender por adb. El usuario prefiere encenderlo a mano a tener que probar él la vista en horizontal.

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
