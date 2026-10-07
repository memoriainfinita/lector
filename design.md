# LECTOR — design

Reproductor de audiolibros para Android, clon propio de Simple Audiobook Player (mdmt, "Simple ABP").

## Objetivo

- Mismo minimalismo que Simple ABP: "sencilla, eficaz y perfecta"
- Más pulida, con portadas y navegación más moderna
- Lo primero es el reproductor; los marcadores con notas son un añadido

## Funciones de Simple ABP a replicar

Todas, también las de pago, salvo los retrasos del botón Atrás y de los botones inferiores (2026-10-06, Pantallas › Ajustes › C1). Detalle en "Simple ABP: inventario".

- Reproduce archivos de audio del dispositivo; UI minimalista para audiolibros
- Marcadores
- Botones de salto configurables, en la app y en la notificación
- Pausa diferida para dormir
- Doble panel en horizontal
- Tema oscuro y claro
- Cambio de tema automático según la hora del día
- Control con botones remotos
- Velocidad 0.5x–3.5x
- Capítulos en m4b y mp3
- Ecualizador

## Simple ABP: inventario (1.8.3 free, extraído 2026-10-02)

Origen: textos del APK y capturas de la app en el móvil del usuario.

- Pantalla principal: título del libro y %, barra del libro con marcas de archivo, archivo actual y botón de marcador, barra del archivo (transcurrido, restante y total), 6 botones (anterior, play, siguiente, −10, lista, +10) y barra inferior (RB, V±, DP, tema, menú)
- Lista: árbol de carpeta, archivo y marcadores, con progreso por elemento, resaltado del actual, subir de nivel y menú por elemento (renombrar, borrar, abrir con, olvidar, reiniciar posición)
- Libros recientes: libro con su archivo actual, portada en miniatura, progreso, tamaño y barra
- Posición guardada por libro
- Portadas: mostrar/ocultar y visor a tamaño completo
- Al reanudar tras pausa repite un tramo
- Acción "siguiente libro" manual, además del paso automático
- Almacenamiento principal y externo (SD); selector de archivos como alternativa
- Aviso si el sistema la cierra con pantalla apagada, con acceso a los ajustes para permitir que siga
- "Download file": probablemente descarga archivos abiertos con el selector que no están en el dispositivo (deducido de los textos del APK, sin comprobar). Descartado en LECTOR
- Marcadores: diálogo con título opcional, posición y capítulo. Lista por libro con menú y Export (portapapeles, otra app o archivo). Máximo de marcadores y límite de longitud del título. Función "marca anterior"
- Volumen: de 0 a 17. Preamplificación de −20 a +50 dB. Ecualizador por bandas (pago)
- Pausa diferida: automática por horario (duración, inicio y fin en HH:MM, reanudar si el móvil se mueve en 20 s), temporizador (30, 20, 40, 50 min u otro valor, con opción de alargar hasta el final del capítulo) o al terminar el capítulo. Cuenta el tiempo de reproducción seguida y se reinicia si se interrumpe, salvo por notificaciones
- Botones de salto: cuatro configurables en la app, en la notificación y en los widgets (pequeño, grande izquierda, grande derecha). Deshacer salto. Dividir el tiempo por la velocidad
- Botones remotos: auricular con 1, 2 o 3 pulsaciones, y teclas multimedia asignables. Reaccionan con la app cerrada
- Auricular: pausa al desconectar y reanuda si se reconecta en menos de 10 s
- Otros ajustes: siguiente libro automático, siguiente archivo desde posición distinta de cero, portada en la pantalla de bloqueo, reducir el tramo repetido al reanudar, reproducir al abrir, retraso del botón Atrás y de los botones inferiores, velocidad de los libros nuevos, estilo estándar de notificación
- Notificación: −30 s, anterior, play, siguiente, marcador y barra de progreso
- Horizontal: reproductor y lista lado a lado
- Tema: claro y oscuro, cambio por hora (pago)
- Permisos: medios (`READ_MEDIA_AUDIO` e imágenes), sin acceso a todos los archivos. Decodifica con FFmpeg 4.3.2

## Añadidos propios

- Portadas con protagonismo (Simple ABP solo las tiene en miniatura)
- Navegación más moderna
- Marcador rápido: amplía los marcadores de Simple ABP con notas o tags opcionales
- Recopilación de marcadores dentro de la app
- Portable al cambiar de móvil y combinable

## Más adelante

- Soportar libros (lectura), además de audiolibros
- Android Auto (navegar la biblioteca desde la pantalla del coche). Los controles por Bluetooth del coche funcionan ya con Media3

## Añadidos tras revisión (2026-10-02)

- Libro terminado: marcar como terminado (a mano o al llegar al final) y filtrar la biblioteca por empezados, sin empezar y terminados
- Libro movido o renombrado: al volver a buscar, reconectar posición y marcadores con la misma identificación que al importar
- Icono de la app: libro abierto (página izquierda naranja, derecha crema) con play en la página izquierda y cinta de marcador en la derecha. Por ahora sin fondo (F1), con play y cinta en hueco (transparentes; sobre fondo oscuro se ven negros). Artboard "Icono en uso: L2 F1 sin fondo, play y marcador en hueco" del lienzo. Valores (viewBox 108):
  - Colores: página izquierda #f58f00, página derecha #EDE3D1
  - Libro ancho 30, alto 38, curvatura 3.5, separación del lomo 0
  - Play (hueco): tamaño 16.5, proporción 0.85, desplazamiento X 0, Y −2.5
  - Cinta (M2, hueco): centro X 73.5, ancho 6.5, final en Y 47, nace en el borde superior de la página
  - Pantalla de inicio: F5, cuadrado redondeado con fondo #0A0A0A; play y cinta en hueco se ven negros. El redondeo final lo pone el lanzador (HyperOS en el móvil del usuario). Dentro de la app y en otros usos: F1 sin fondo
  - Resto de propuestas archivadas en el artboard "Icono: propuestas archivadas"
- Descartado: copia de seguridad automática (en Android no se puede elegir OneDrive como destino)

## Biblioteca del usuario

- Carpeta raíz con carpetas por libro
- A veces varios libros en una carpeta
- A veces agrupados por autores
- Lo que hay en el móvil (2026-10-03) es una muestra de la biblioteca: `Audiobooks` y `Music/AUDIOBOOKS`, mp3 y m4b. Casos reales: carpetas `readed` con muchos libros sueltos de un archivo, partes m4b sin capítulos (116 en un libro), subcarpetas de disco, volúmenes en subcarpetas, sagas de libros largos con el mismo álbum

## Recomendaciones aceptadas en planteamiento

Pendientes de confirmar en el lienzo de diseño.

- Detección de libros (revisada 2026-10-03 con la biblioteca real; la versión anterior unía todos los archivos sueltos de una carpeta y trataba todo m4b como libro):
  - Subcarpetas con nombre de disco (CD, Disc, Disk, Disco, Part, Parte + número, al final del nombre) = un solo libro con la carpeta
  - Archivo con capítulos = un libro
  - Mismo álbum = un libro, salvo dos o más archivos de 3 h o más: libros distintos de una saga, y el álbum no se usa como título. Un archivo sin álbum se une al grupo con el que comparte nombre
  - Mismo nombre salvo la numeración ("… - 001", "01_…", "Part 1") = un libro
  - El resto: un libro por archivo
  - Carpeta sin audio con subcarpetas = agrupador (autor); las demás subcarpetas se recorren por separado
  - Las clases de carpeta mandan sobre estas reglas
  - Corrección manual "separar / unir", persistente
  - Riesgo aceptado: volúmenes distintos que solo difieren en el número ("Saga 1", "Saga 2") se unen; se corrige con "Separar"
- Portadas: imagen incrustada en el archivo; si no, imagen de la carpeta. Sin búsqueda en internet
- Navegación: biblioteca plana en cuadrícula de portadas (recientes primero) + vista por carpetas
- Portadas en la cuadrícula de la biblioteca y en el reproductor
- Marcador: título, nota y tags, todos opcionales
- Marcar sin mirar: acción asignable a botón remoto (p. ej. auricular x2) y botón en la notificación
- Recopilación: vista por libro y global, filtro por tag, búsqueda, tocar salta a la posición
- Idioma de la interfaz: español e inglés
- Portabilidad:
  - Exportar/importar JSON con marcadores, posiciones y ajustes
  - Libro identificado por nombre de carpeta y archivos; si falla, por nombre y tamaño de los archivos; si falla, por duración total. No por ruta
  - Marcadores con ID único; fusión sin duplicados
  - Posición de escucha en conflicto: gana la más reciente

## Decisiones de diseño (lienzo)

Lienzo: https://claude.ai/artifact/NnT6pjF1vGhypwmLuweFSC

- Reproductor: variante A. Portada grande: 358 × 411, ancho completo con 16 px de margen
- Tags de marcadores: estilo de la recopilación original (etiquetas en la nota y filtros arriba), rectangulares y pequeñas. Descartados: tags como texto con #, puntos de color, agrupar por tag
- Minirreproductor: la flecha del reproductor lo reduce a una barra abajo del todo; se sigue escuchando mientras se navega. Botones: marcar, −10, play/pausa, +10
- Descartado: reproductor B (fiel a Simple ABP)
- Cuadrícula de la biblioteca: ⋮ pequeño junto al título de cada libro, bajo la portada. Descartados: pulsación larga (no es intuitiva) y ⋯ sobre la portada
- Ecualizador accesible desde el ⋯ del reproductor como hoja, además de en Ajustes
- Capítulos: en libros con capítulos, la segunda barra es la del capítulo; tocar su nombre abre la lista; anterior/siguiente saltan de capítulo. Anterior con más de 3 s dentro del capítulo vuelve a su inicio
- Deshacer salto: aviso temporal abajo, sobre el minirreproductor si lo hay, con "Deshacer", 10 s (antes 5; los demás avisos, 5 s; 2026-10-05). Solo en saltos grandes (barra, cambio de capítulo o archivo, ir a un marcador o capítulo), no en ±10 s. Con saltos encadenados vuelve a la posición previa al primero
- Aviso con "Deshacer" como patrón de la app: saltos, borrar marcador, unir libros, quitar carpeta, borrar tag, reiniciar posición, quitar de recientes. Excepción: borrar del móvil pide confirmación porque no se puede deshacer
- Marcadores: botón de play en cada marcador ("escuchar desde aquí"). Tocar la fila abre la edición (hoja de marcador). El ⋮ queda en copiar texto y borrar. Borrar sin confirmación, con "Deshacer"
- Marcadores sin límite de número ni de longitud del título
- Unir libros: "Unir con otros libros" en el menú del libro abre la selección múltiple. Orden natural de nombre. Los marcadores de cada parte pasan al libro unido; la posición es la última escuchada. Se deshace con el aviso o en Ajustes › Biblioteca › Carpetas › Correcciones
- Biblioteca: filtros En curso / Sin empezar / Terminados como interruptores; sin ninguno activo, toda la biblioteca (vista al abrir). Botón de ordenar. "Marcar como terminado" / "Marcar como no terminado" en el menú del libro
- Carpetas: Ajustes › Biblioteca › Carpetas (añadir, quitar, volver a buscar, correcciones). Añadir abre un explorador propio (principal / SD) con "Usar esta carpeta"; depende del acceso directo a archivos (con SAF sería el selector de Android). Quitar una carpeta conserva posiciones y marcadores. Búsqueda de cambios automática al abrir la app; "Volver a buscar" fuerza una completa
- Popups: filas de 44 px
- Hoja de marcador: botón Listo pequeño, a la derecha
- Muchos tags: fila de filtros de una línea con desplazamiento lateral; en la hoja de marcador solo los más usados; lista completa con búsqueda, crear tag y orden por uso o A–Z, que también asigna tags desde la hoja de marcador ("Listo" en vez de "Aplicar"). Varios tags en el filtro muestran los marcadores con cualquiera de ellos. Gestionar tags en Ajustes (renombrar, unir, borrar); borrar un tag no borra marcadores
- Sin menú inferior (quitado el 2026-10-04: Escuchando ya se abre desde el minirreproductor y basta un icono para Marcadores). Antes: tres pestañas, solo iconos, barra de 48 y después de 40
- Fila bajo los controles (pausa, marcar, velocidad, marcadores): pequeña, iconos con texto mínimo
- Pantallas añadidas el 2026-10-02, aprobadas como están en el lienzo:
  - Widget 4×1 (portada, título y barra, −10, play, +10) y 4×2 (añade capítulo o archivo, tiempos y marcar). Botones configurables en Ajustes › Botones
  - Visor de portada: desde "Ver portada" en el menú del libro y tocando la portada del reproductor; se cierra con × o deslizando hacia abajo
  - Búsqueda en la biblioteca (título, autor, narrador, serie, carpeta) y en marcadores (título, nota, tag), con lo encontrado resaltado. La de la biblioteca filtra la propia Biblioteca, sin pantalla aparte (2026-10-05)
  - Ordenar: escuchados recientemente (por defecto), añadidos recientemente, título, autor, tiempo restante
  - Estados vacíos: biblioteca sin carpetas con "Añadir carpeta"; marcadores vacíos con cómo marcar
  - Libro inaccesible: aviso con "Volver a buscar" y "Quitar"; conserva posición y marcadores
  - Aviso al abrir tras un cierre del sistema con la pantalla apagada: dónde se paró, "Abrir ajustes" (batería) y "Ahora no"
- Color de acento: por defecto #F58F00, el naranja del icono (antes #F0A43A). En tema claro, variante oscura del mismo tono (#B86E0E en el lienzo). Opciones en Ajustes › Apariencia: ámbar, azul, verde, coral, color del sistema (Android 12+) y personalizado
- Ajustes › Apariencia: interruptor "Mostrar portadas" (de Simple ABP, decidido 2026-10-04). Sin portadas: Biblioteca en lista y Escuchando sin recuadro (lienzo, versión 151; Pantallas › Sin portadas)
- Color personalizado: hoja con tono, luminosidad, hex y vista previa en oscuro y claro. Si falta contraste avisa con "Poco contraste" y "Ajustar" (corrige la luminosidad), sin bloquear. Botón Listo pequeño a la derecha
- Contenido del lienzo registrado en la revisión del 2026-10-02:
  - Reproductor: subtítulo con autor y narrador
  - Menú ⋯ del reproductor: ecualizador y volumen, velocidad, pausa diferida, ir a la carpeta del libro, siguiente libro, tema, ajustes
  - Velocidad: hoja con − / +, rango 0.5x–3.5x y atajos 0.8, 1.0, 1.25, 1.5, 2.0. Se guarda por libro; aparte, la velocidad de los libros nuevos
  - Pausa diferida: hoja con 20, 30, 35, 40 min u otro valor, al terminar el capítulo, alargar hasta el final del capítulo y enlace a horario automático
  - Marcadores del libro (desde la fila bajo los controles): hoja con "Marcar aquí", la posición actual marcada como "estás aquí" entre los marcadores, "Ver todos los marcadores" y "Exportar"
  - Notificación desplegada (−30, anterior, pausa, siguiente, marcar, barra) y recogida
  - Menú del libro: cabecera con título, "autor · narrador · serie n" (lo que haya en las etiquetas) y tiempos; marcadores, ver portada, ir a la carpeta, separar en libros, unir con otros libros, marcar como terminado, reiniciar posición, quitar de recientes, renombrar, abrir con…, borrar del móvil
  - Biblioteca: tarjeta "Seguir escuchando" con el libro actual sobre los filtros. Vista Carpetas con ruta, carpetas de autor y progreso o número de marcadores por libro
  - Recopilación con filtro activo: "n marcadores con [tag]", "Quitar filtro"; exportar exporta solo los filtrados
  - Horizontal: reproductor a la izquierda y panel con pestañas Archivos y Marcadores a la derecha
  - Ajustes › Marcadores: "Abrir la hoja al marcar". Desde la app abre la hoja; desde auricular y notificación solo se guarda
  - Ajustes › Sistema: "Seguir con la pantalla apagada" abre el ajuste de batería de Android
  - Datos: exportar copia completa (JSON) o marcadores como texto; importar y combinar con resumen previo (marcadores nuevos, ya existentes que se omiten, posiciones más recientes, libros no encontrados) e "Importar también los ajustes" opcional. Los libros no encontrados entran como quitados, con sus marcadores; la copia debe llevar los datos del libro (título, autor, firma) (2026-10-05)
  - Primer arranque: pantalla de permiso ("Reproductor de audiolibros" / "Lee los audiolibros de tus carpetas. Necesita acceso a los archivos. No usa internet." desde el 2026-10-06; la app no declara el permiso INTERNET), con "Importar una copia de otro móvil") y pantalla de carpetas con audio encontradas, "Elegir otra carpeta" y "Empezar"
  - Tema Oscuro / Claro / Sistema; idioma Español / English / Sistema
- Escaneo en curso (2026-10-03): línea fina de progreso bajo el selector Libros / Carpetas con "Buscando libros… [n] encontrados" y la carpeta actual. Artboard "Biblioteca: buscando". En la app los libros nuevos aparecen al terminar la búsqueda (Pantallas › Biblioteca › A)
- Iconos en uso (versión 148): artboard con el logo definitivo y los iconos de la app, generado desde res/drawable. Las demás propuestas de icono, archivadas
- Sin pantalla propia (2026-10-03): Ajustes › Permisos abre los ajustes de Android; Acerca de es solo texto
- Widget: dos botones de salto, izquierda −10 s y derecha +10 s, iguales en 4×1 y 4×2. Sustituye el modelo de Simple ABP (pequeño, grande izquierda, grande derecha)
- Acciones asignables a botones (reproductor, notificación, widget, auricular, teclas multimedia): saltar atrás, saltar adelante, capítulo o archivo anterior, capítulo o archivo siguiente, play / pausa, añadir marcador, ir al marcador anterior, deshacer salto, nada. "Ir al marcador anterior" corresponde a "Previous mark" de Simple ABP (comportamiento exacto sin comprobar). Se eligen en una hoja al tocar cada botón en Ajustes › Botones
- Exportar marcadores: hoja con vista previa en texto y tres destinos: copiar, guardar como archivo (.txt), compartir con otra app (menú de Android). Respeta el filtro activo
- Popups añadidos:
  - Renombrar (libro y tag): diálogo con campo, Cancelar y Guardar. Renombrar un libro solo cambia el nombre en LECTOR; carpeta y archivos no se tocan
  - Borrar del móvil: confirmación con número de archivos, tamaño y ruta; "No se puede deshacer"; los marcadores se conservan en la recopilación. Botón Borrar en coral
  - Separar en libros: lista de archivos con casilla en el que empieza cada libro nuevo, en espejo de "Unir libros". Posición y marcadores pasan al libro que contiene cada archivo. Se deshace en Correcciones
  - Elegir valor: hoja común con − / +, valor grande y atajos (segundos de cada salto, tramo repetido al reanudar, minutos de la pausa diferida)
  - Unir con otro tag: hoja con elección única del tag destino; el tag original desaparece
  - Horas, "Abrir con…" y elegir archivo usan los selectores de Android
- Sistema de botones (zona táctil mínima 44 px aunque el botón se vea menor):
  | Tipo | Alto | Radio | Letra |
  |---|---|---|---|
  | Principal relleno (Listo, Aplicar, Guardar…) | 40 | 6 | 14 seminegrita |
  | Destacado (primer arranque, estados vacíos) | 48 | 8 | 15 seminegrita |
  | Secundario con borde | 36 | 6 | 13 |
  | Texto (Deshacer, Cancelar, Ahora no…) | 40 | 6 | 14 |
  | Enlace dentro de una hoja (Ver todos, Restablecer…) | 36 | — | 13 |
  | Segmentado (tema, idioma) | 36 | — | 14 |
  | Segmentado con iconos (Libros / Carpetas) | 36 × 44 por opción | — | icono 20 |
  | Opción en hoja (velocidad, pausa, valores) | 36 | 2 | 13 mono |
  | Etiqueta o filtro | 28 | 2 | 12 mono |
  | Fila de menú | 44 | — | 14–15 |
  | Icono | 44 | — | — |
- Escala de texto: 11 etiquetas y texto sobre portadas pequeñas; 12 metadatos y tiempos en mono; 13 secundario; 14 listas y botones; 15 texto principal de fila; 17 título de hoja; 20 título de subpágina; 22 título del libro en el reproductor; 24 título de Biblioteca; 28 valores grandes (velocidad, cuenta atrás) y titular de bienvenida. Las portadas de ejemplo del lienzo quedan fuera

## Recomendaciones técnicas

Aprobado 2026-10-02.

- Stack: Kotlin, Jetpack Compose, Media3 1.11, Room, Navigation3, Coil. Sin extensión FFmpeg de momento (biblioteca en mp3 y m4b)
- Descartados: Flutter, React Native y Capacitor. Las funciones propias (notificación, auricular, widgets, ecualizador, escaneo, sensor, Android Auto) son Android nativo y acabarían en Kotlin igualmente
- Acceso a archivos: `MANAGE_EXTERNAL_STORAGE` (SAF es lento recorriendo bibliotecas grandes y tiene fallos de permisos conocidos). Instalación por adb, sin Google Play
- Botones de la notificación con `CommandButton` de Media3
- Pulsaciones del auricular (1, 2, 3): conteo propio en `MediaSession.Callback.onMediaButtonEvent`
- Proyecto de un módulo, o de dos o tres, sin analítica
- Herramientas: compilar por línea de comandos (Gradle + adb); Android Studio para pruebas
- Android mínimo: 8.0
- Libro con campo de tipo (de momento solo audio), para añadir lectura sin rehacer la base de datos

## Arquitectura

Aprobado 2026-10-03.

- Orden de construcción por dependencias: datos → sistema visual → escaneo → reproducción → navegación y pantallas → resto de funciones. Sin atajos para tener antes algo usable
- Un módulo `app` con paquetes `data`, `library`, `playback`, `ui`
- Capa de datos con repositorios y capa de interfaz; coroutines y flows entre capas; un ViewModel por pantalla; flujo unidireccional. Sin capa de dominio
- Una sola Activity con Navigation3
- Inyección de dependencias manual, sin Hilt
- Reproducción con `MediaLibraryService` desde el principio (lo exige Android Auto)
- Un único sistema de acciones (las asignables a botones) que usan reproductor, notificación, widget, auricular y teclas multimedia
- Desde el sistema visual: tema, acento e idioma dinámicos (nada fijo en código, textos en recursos) y el aviso con "Deshacer" como pieza común

## Sistema visual

Aprobado 2026-10-03. Código en `ui/theme` y `ui/components`; catálogo de depuración "LECTOR catálogo".

| Uso | Oscuro | Claro |
|---|---|---|
| Fondo | #0A0A0A | #F5F3EF |
| Hojas, tarjetas, segmentado | #161616 | #FFFFFF |
| Menús emergentes y diálogos | #1F1F1F | #FFFFFF |
| Separadores (fondo / hojas) | #222222 / #262626 | #DDD8D0 |
| Pistas, seleccionado | #2A2A2A | #DDD8D0 |
| Bordes | #3A3A3A | #C9C3B9 |
| Marcas de barra, inactivo | #5A5A5A | #A8A29A |
| Texto principal / secundario / terciario | #EDEDED / #9A9A9A / #7A7A7A | #1A1A1A / #6B6B6B / #8A8580 |
| Iconos suaves | #CFCFCF | #3A3A3A |
| Texto sobre acento | #0A0A0A | #FFFFFF |
| Peligro (Borrar) | #E5484D | #B3261E |
| Velo | negro 50 % | negro 50 % |

- Claro: hojas, menús, bordes, terciario y peligro propuestos sobre lo que había en el lienzo (solo el reproductor en claro)
- Peligro en rojo, no coral: se distingue del acento coral. Lienzo actualizado (versión 144)
- Acentos (oscuro / claro): ámbar #F58F00 / #B86E0E, azul #7FB8E0 / #2F6F9E, verde #9BC67A / #4F7F2E, coral #E08A7F / #B0493C. Personalizado: el claro se deriva con el mismo tono, oscurecido hasta contraste 3:1 sobre el fondo claro. "Poco contraste" y "Ajustar" con el mismo umbral sobre el fondo oscuro. Color del sistema en Android 12+
- Tema por hora, cuando está activo, manda sobre Oscuro / Claro / Sistema. Por defecto: claro a las 08:00, oscuro a las 21:00
- Tema y acento en DataStore. Idioma con el selector por app de AppCompat; inglés como base y español traducido
- Fuentes IBM Plex Sans (variable) y Plex Mono, incluidas en la app (OFL)
- Componentes: los diez tipos de botón, interruptor, hoja, diálogo, cabecera de sección y aviso "Deshacer" (5 s, abajo, sobre el minirreproductor si lo hay). Diálogo con radio 14; segmentado con radio 10 / 7
- Icono: adaptativo, fondo #0A0A0A y libro L2 con play y cinta M2 en hueco (huecos reales, también en el icono temático monocromo). Dentro de la app, F1 sin fondo

## Escaneo

Aprobado 2026-10-03. Código en `library/`; pantalla de depuración "LECTOR escaneo".

- Permiso: acceso a todos los archivos (Android 11+), lectura clásica en 8–10
- Recorre almacenamiento principal y SD. Formatos: mp3, m4a, m4b, aac, ogg, oga, opus, flac, wav. Salta carpetas y archivos ocultos (la papelera de Android deja lo borrado como `.trashed-…`) y `Android/`; no salta carpetas con `.nomedia`
- Metadatos con `media3-inspector` (`MetadataRetriever`): duración (también mp3 de tasa constante sin cabecera), etiquetas y capítulos (interfaz `Chapter`: ID3 CHAP y MP4). Título: álbum, título de pista (libro de un archivo) o carpeta / archivo. Autor: artista del álbum o artista. Narrador: compositor. Serie y número: TXXX SERIES / SERIES-PART, MVNM / MVIN
- Portadas: miniatura (lado mayor 1024) en almacenamiento de la app, de la imagen incrustada o, si no hay, de una imagen de carpeta (2026-10-07). Sitios, por orden: la carpeta del libro, sus discos y sus subcarpetas sin audio (`Scans/`, `Artwork/`…). Elección: cover / folder / front; si no, la que contiene cover o front; si no, la que lleva el nombre del libro (carpeta, título o archivo); si no, la primera por nombre. Nunca las que contienen "back" ni las miniaturas de Windows Media Player (`AlbumArtSmall`, `AlbumArt_{…}_Small`). En una carpeta con varios libros, solo la que lleva el nombre del libro
- Caché `file_meta` por ruta: el escaneo rápido (al abrir) solo relee archivos con tamaño o fecha distintos; "Volver a buscar" relee todo y rehace portadas
- Dos fases: recorrer y leer (progreso: encontrados y carpeta actual), después detectar, aplicar correcciones, reconciliar y guardar en una transacción. Los libros aparecen al terminar
- Reconciliación: por firma; si no, por contenido (nombre y tamaño de cada archivo, sin carpetas: libro movido o carpeta renombrada; entre todos los libros, como la firma; los importados, con los tamaños de la copia desde el 2026-10-08) (2026-10-07); si no, por duración ±1 s entre los que no aparecen; los que faltan quedan inaccesibles con sus datos, salvo los reagrupados (todos sus archivos siguen y son de otros libros): marcadores y posición pasan a esos libros y el antiguo se borra (Pantallas › Biblioteca › B)
- Carpetas candidatas para el primer arranque: niveles 1 y 2 con audio bajo cada almacenamiento

## Reproducción

Aprobado 2026-10-03. Código en `playback/`; pantalla de depuración "LECTOR reproducción".

- Dos entregas: A (servicio, posición, navegación, acciones, sesión, conexión con la interfaz) y B (velocidad y procesamiento de sonido)
- `MediaLibraryService` con ExoPlayer; foco de audio; pausa al desconectar el auricular. Todo en el hilo principal (Media3 1.11 lo exige)
- Libro = lista de reproducción con un elemento por archivo. Capítulos calculados sobre la posición desde la tabla `chapter`, sin cortar el audio. Posición global del libro ↔ archivo + punto
- Posición guardada cada ~5 s sonando, al pausar, al saltar y al cerrar el servicio, con `positionUpdatedAt` y `lastPlayedAt`. Al abrir un libro se restauran posición, velocidad y saltar silencios
- Saltos ±N s cruzando archivos. Anterior / siguiente: capítulo si el libro tiene capítulos, si no archivo; anterior con más de 3 s vuelve al inicio. Ajustes de salto: segundos por botón, dividir el tiempo por la velocidad, siguiente archivo desde posición distinta de cero
- Deshacer salto: solo saltos grandes (barra, cambio de capítulo o archivo, ir a un marcador o capítulo). Con saltos encadenados vuelve a la posición previa al primero. Ventana de 10 s, la misma que el aviso (2026-10-05)
- Posición por tramo (2026-10-05): al salir con un salto de un tramo (capítulo, o archivo sin capítulos) a medias, se guarda dónde estaba (tabla `segment_position`, base de datos versión 4). Volver a él con anterior, siguiente, la lista de capítulos o la notificación retoma ese punto; para empezarlo, anterior con más de 3 s dentro. Salir en los primeros 3 s deja lo guardado; en los últimos 5 s, o pasar al siguiente tramo sonando, lo borra. Reiniciar el libro y terminarlo borran todo. Como Simple ABP, que guarda posición por archivo (comprobado en la app 2026-10-05)
- Siguiente archivo desde su posición (Simple ABP: "Allow next file to start from non-zero position", comprobado 2026-10-05): al terminar un archivo sonando, el siguiente retoma su posición guardada; apagado, empieza en 0. No afecta a los botones, que siempre retoman. Entre capítulos de un mismo archivo no se aplica: el audio sigue de corrido. Activado por defecto
- Tramo repetido al reanudar tras una pausa: valor en DataStore, 3 s por defecto
- Al terminar, según la clase de carpeta: Libros, terminado y siguiente libro si está activado (desactivado por defecto); Episodios, terminado sin pasar a otra obra; Álbumes y Sesiones, vuelve al inicio, nunca terminado
- Acciones: un solo conjunto con ejecutor (saltar atrás, saltar adelante, anterior, siguiente, play / pausa, añadir marcador, ir al marcador anterior, deshacer salto, siguiente libro). Marcar guarda en la posición actual; la hoja llega con las pantallas. Ir al marcador anterior: el inmediatamente anterior a la posición
- Notificación (2026-10-05): capítulo anterior, −N, play, +N, capítulo siguiente, con los segundos de los saltos de la app. Antes: −30, anterior, play, siguiente, marcar; un toque sin querer en anterior o siguiente saltaba de capítulo sin "Deshacer". −N y +N como `CommandButton` en `SLOT_BACK` / `SLOT_FORWARD`; capítulos como botones propios en `SLOT_OVERFLOW`, que HyperOS pone en los extremos. Los cinco del mismo tamaño (lo decide el sistema). Marcar no cabe por defecto; se asigna a un hueco en Ajustes › Botones (Ajustes › C1, 2026-10-06). Con los huecos ocupados, Media3 quita anterior y siguiente de las acciones de `PlaybackState`; las teclas multimedia siguen funcionando (comprobado con `KEYCODE_MEDIA_NEXT` y con el auricular Bluetooth del usuario). Sin comprobar en coche o reloj. Acciones propias como `SessionCommand`, concedidas en `onConnectAsync`
- Sesión con título, autor, capítulo y portada (notificación y pantalla de bloqueo)
- Conexión con la interfaz (`PlaybackConnection`): un punto que expone el estado (libro, posición, capítulo, sonando, velocidad) para reproductor, minirreproductor, horizontal y widgets. `connect()` al abrir la app arranca el servicio, que carga el último libro en pausa
- Cierre del sistema: se guarda que estaba sonando, para el aviso al abrir
- Archivo que falta al reproducir: libro inaccesible, conserva posición y marcadores
- Velocidad 0.5x–3.5x por libro en pasos de 0.05, sin cambiar el tono; también desde la sesión (`setPlaybackSpeed`). Libros nuevos con la velocidad global (DataStore), que usa el escaneo
- Volumen: el volumen multimedia del móvil (`AudioManager`, sin mostrar el control del sistema), como Simple ABP: el 0..17 del inventario es el rango del móvil del usuario. Global
- Sonido: procesador de audio propio dentro de ExoPlayer, antes de saltar silencios y de la velocidad. No el ecualizador del sistema (depende del móvil, no llega a +50 dB):
  - Preamplificación −20 a +50 dB
  - Ecualizador de 5 bandas para voz: estantería grave 100 Hz, picos 300 Hz, 1 kHz y 3 kHz (Q 1), estantería aguda 8 kHz; ±12 dB
  - Limitador siempre activo a −1 dBFS (solo actúa si satura)
  - Cambios en vivo con fundido de 20 ms; en neutro no toca la señal
- Sonido global en DataStore (preamplificación, ecualizador activado, bandas). Sonido propio en el libro; al activarlo copia el global, al desactivarlo vuelve al global y al reactivarlo copia de nuevo. Base de datos versión 2: `book.eqEnabled` (migración automática), porque la hoja tiene interruptor del ecualizador también en el sonido propio
- Fundido de volumen del temporizador: con la pausa diferida, sobre el volumen del reproductor
- Fuera: pausa diferida, auricular 1/2/3 y teclas asignables, reanudar al reconectar en 10 s, widgets, Android Auto, pantallas
- Duración 0 en el escaneo: el reproductor la lee al cargar el archivo y se guarda en `book_file`, `book` y `file_meta`. Casos (2026-10-03): `00.12 The Heir of Caladan.m4b` y `00.13 Princess of Dune.m4b`, MP4 de ~1 GB con el índice (`moov`) al final
- `00.05 Mentats of Dune.m4b` no es audio: es un ZIP (cabecera `PK`) con el m4b dentro. Ningún extractor lo lee; error de reproducción
- Pendiente: capítulos de los MP4 con el índice al final (el escaneo no los lee; Heir of Caladan sin capítulos en la base de datos, sin comprobar si los tiene)

## Pausa diferida

Aprobado 2026-10-06 (lienzo `Sleep-Sheet`, `Settings-Sleep`; Referencia: Voice). Dos entregas: A temporizador y hoja en Escuchando; B Ajustes › Pausa diferida (horario automático, marcar dónde se pausó, seguir si se mueve el móvil). Código en `playback/SleepTimer.kt`.

- Vive en el servicio: sigue con la pantalla apagada. La hoja manda órdenes por la sesión y lee el estado de `app.sleep`
- Temporizador: cuenta solo mientras suena. Una pausa (del usuario, de otra app que se queda el audio, del auricular) lo devuelve a la duración completa; una interrupción breve (notificación, foco transitorio) no, porque no cambia `playWhenReady`
- Al terminar el capítulo: pausa al final del tramo que suena (capítulo, o archivo sin capítulos). Alargar hasta el final del capítulo: cumplido el tiempo, sigue hasta el final del tramo
- Fundido del volumen del reproductor en los últimos 10 s (también al final del capítulo); al pausar vuelve al volumen normal. Sin ajuste
- Al pausar, la pausa diferida se apaga; la automática vuelve a empezar la próxima vez que suene dentro del horario
- Hoja: 20, 30, 35, 40 min y "…" (Elegir valor, 1–180 min, empieza en el último elegido), "Al terminar el capítulo", "Alargar hasta el final del capítulo" (DataStore), "Se pausa en mm:ss" con Apagar, enlace "Horario automático y más". La luna de Escuchando, en acento mientras está activa
- Automática por horario: apagada por defecto, de 23:00 a 7:00, 30 min. Arranca al empezar a sonar dentro de la franja (que puede cruzar la medianoche)
- Marcar dónde se pausó (activado): marcador de clase pausa en la posición, solo el último por libro
- Seguir si muevo el móvil (activado): 30 s tras la pausa, con el acelerómetro y un bloqueo de activación parcial de 30 s; si se mueve, vuelve a sonar con la misma pausa diferida

## Navegación

Aprobado 2026-10-03. Revisado 2026-10-04: sin menú inferior, una sola pila.

- Dependencias: Navigation3 1.2.0 (`navigation3-runtime`, `navigation3-ui`), `lifecycle-viewmodel-navigation3` 2.11.0, plugin de serialización de Kotlin 2.4.20 y `kotlinx-serialization-core`
- Una sola pila con Biblioteca en la raíz; Atrás en Biblioteca sale de la app
- Escuchando y Marcadores se abren encima. Si ya están en la pila, suben arriba en vez de duplicarse. Atrás vuelve a la pantalla anterior. Marcadores lleva flecha de Atrás y título de 20, como las subpantallas (lienzo, versión 148)
- Cabecera de Biblioteca: buscar, Marcadores y Ajustes. Ajustes también desde el ⋯ del reproductor
- Todo Atrás pasa por un punto único (`Navigator.goBack`)
- Con minirreproductor: Biblioteca, Marcadores y búsqueda en marcadores. La búsqueda en la biblioteca no es pantalla: filtra la propia Biblioteca (Pantallas › Biblioteca › D, 2026-10-05)
- Biblioteca: Libros / Carpetas es un selector de la misma pantalla. En Carpetas se entra en subcarpetas con la ruta arriba; Atrás sube un nivel
- Pantallas completas, sin minirreproductor: Ajustes y sus subpáginas (Pausa diferida, Botones, Ecualizador y volumen, Carpetas, Gestionar tags, Apariencia, Datos), explorador de carpetas, unir libros, separar en libros, visor de portada. El visor se cierra con × o deslizando hacia abajo
- Hojas, menús emergentes y diálogos: estado de su pantalla, no entradas de navegación. Atrás cierra primero la hoja
- De hoja a pantalla completa: "Horario automático y más" → Ajustes › Pausa diferida; "Gestionar" en la lista de tags → Ajustes › Gestionar tags; "Ajustes" en el ⋯ del reproductor → Ajustes. Al volver, la pantalla de origen sin la hoja
- Explorador de carpetas desde Ajustes › Carpetas, la biblioteca vacía ("Añadir carpeta") y el primer arranque ("Elegir otra carpeta")
- Saltos:
  - "Ir a la carpeta" (menú del libro y ⋯ del reproductor) → vuelve a la raíz, Biblioteca › Carpetas, en esa carpeta
  - "Ver todos los marcadores" (hoja de marcadores del libro) → Marcadores
  - Tocar un libro en la cuadrícula (también buscando) lo carga y abre Escuchando
- "Marcadores" en el menú del libro abre la hoja de marcadores de ese libro
- "Escuchar desde aquí" en un marcador salta y se queda en Marcadores, con el minirreproductor y "Deshacer"
- Minirreproductor abajo del todo, con un libro cargado y una sesión de escucha en curso, en todas las pantallas salvo Escuchando y las pantallas completas. La sesión empieza cuando algo suena o se abre Escuchando; al abrir la app, el libro cargado en pausa solo sale en "Seguir escuchando" de Biblioteca. Se conserva al girar la pantalla; si la app sigue sonando en segundo plano, al volver hay minirreproductor. La flecha del reproductor y deslizar la portada hacia abajo hacen Atrás; tocar el minirreproductor o arrastrarlo hacia arriba abre Escuchando (2026-10-06). La sesión empieza cuando Escuchando ha terminado de subir
- Aviso "Deshacer" único para toda la app, abajo, encima del minirreproductor si lo hay
- Libro inaccesible: tarjeta dentro de Escuchando
- Entradas desde fuera: widget y notificación abren Escuchando encima de Biblioteca. El aviso de cierre del sistema aparece al abrir, sobre Escuchando
- Primer arranque: pila propia (permiso → carpetas) en lugar de la principal cuando falta el permiso o no hay carpetas. "Empezar" la sustituye por la principal. "Importar una copia de otro móvil" usa el selector de archivos de Android
- Horizontal: el reproductor ocupa toda la pantalla. El doble panel se hace con la pantalla del reproductor
- Un ViewModel por pantalla, ligado a su entrada de navegación y creado desde `AppContainer`

## Pantallas

Se hacen una a una sobre la navegación; las que faltan son pantallas vacías con sus enlaces.

### Escuchando

Hecha 2026-10-03.

- Primera pantalla, antes que la Biblioteca: solo depende del motor; la Biblioteca necesita abrir libros en el reproductor
- Portada: marco de 358 × 411 hasta cargar la imagen; después toma la proporción de la imagen en el mismo espacio, sin recortar. Sin portada: superficie con el título. Tocarla abre el visor
- Barra del libro con marcas al inicio de cada tramo (capítulo o archivo); sin marcas si quedan a menos de 4 dp de media. Las dos barras se arrastran, los tiempos siguen al dedo y al soltar salta con "Deshacer"
- Barras (2026-10-06): mismo tamaño que antes (6 y 3, zona de 24, tiempos de 13) para no quitar sitio a la portada; la posición, en blanco. Al arrastrar, la barra engorda 4 y sale un círculo en el punto. Se probaron más grandes (44 de zona, 8 y 6, tiempos de 15) y no hacía falta con el tiempo grande y el arrastre fino
- Cada barra con sus números encima (2026-10-06): tiempos del libro sobre la barra naranja y, tras un hueco de 10, nombre y tiempos del tramo sobre la gris. Antes los del libro iban debajo de su barra y se mezclaban con los del tramo. Porcentaje del libro entre los dos tiempos, centrado (2026-10-06): el título y autor · narrador ocupan todo el ancho
- Más alto para la portada (2026-10-06): play de 68 (antes 76), y menos aire bajo la portada (14), sobre las barras (8), antes de los controles (6) y en la fila inferior (4 arriba, 2 abajo); hueco bajo la barra naranja de 2
- Sin cabecera con portada (2026-10-06): raya de las hojas (40 × 4) centrada en una franja de 24; deslizarla hacia abajo o tocarla minimiza, como la portada. Sin "Escuchando". Descartadas: flecha sobre la portada (tapa la imagen) y nada (el gesto no se ve). Sin portadas o con el libro inaccesible, la cabecera de 48 con la flecha y "Escuchando"
- Portada con 8 de margen a los lados (antes 16). Entre autor y tiempos del libro, 14. Fila del tramo de 26 de alto mínimo y barra gris subida 4, pegada a su fila (2026-10-06)
- Lista de tramos (2026-10-06): sin capítulos, tocar el nombre del archivo abre la misma hoja como "Archivos", si el libro tiene más de uno, como la pestaña Archivos en horizontal
- Tiempo grande al arrastrar (2026-10-06): sobre la portada (sin portada, en su hueco; en horizontal, arriba de la columna), tiempo de 28 mono, desplazamiento desde la posición al empezar ("+2:07:06"), capítulo en el que cae si el libro tiene capítulos y "Fino 1/2" o "Fino 1/4"
- Arrastre fino (2026-10-06): tocar va al punto del dedo; al arrastrar, el avance va con el dedo y se divide entre 2 con el dedo a más de 64 dp por encima de la barra y entre 4 a más de 128
- Aviso de salto: "Saltado desde [posición]" con Deshacer
- Play / pausa según "va a sonar": no parpadea mientras carga
- ⋯ en la fila bajo los controles, a la derecha (su menú sale por abajo). Cabecera solo con la flecha y "Escuchando". Lienzo actualizado (versión 146)
- Subir y bajar (2026-10-06): Escuchando sube desde abajo al abrirse, con la pantalla anterior debajo (escena propia de Navigation3, ListeningScene). Arrastrar hacia abajo la raya, la portada o el hueco sin portada mueve la pantalla entera con el dedo; al soltar pasados 120 dp o con un gesto de más de 400 dp/s termina de bajar, si no vuelve con un rebote. Atrás, la flecha y tocar la raya bajan con la misma animación. En horizontal, desde la portada. Solo hacia abajo. El minirreproductor está debajo de Escuchando mientras baja y se queda sin fundido. Arrastrar el minirreproductor hacia arriba la abre siguiendo al dedo: pasados 120 dp o con un gesto rápido termina de subir, si no vuelve a bajar. Recortada a su área: no asoma por detrás de la barra de navegación del sistema. Antes: actuaba al soltar y la portada no se movía (2026-10-04)
- Pausa diferida y marcadores del libro: inactivos hasta sus funciones. Marcar guarda sin hoja hasta Marcadores
- Menú ⋯: Tema alterna oscuro y claro a partir del que se ve
- Velocidad: − / + en pasos de 0.05
- Saltos con icono: el número va en el centro del círculo de la flecha, no del icono (1 dp a un lado), con cifras de ancho fijo (2026-10-05)
- Sonido: cambios en vivo, enviados al cambiar el valor redondeado (1 dB; bandas 0.5 dB). Restablecer pone a 0 preamplificación y bandas, sin tocar el interruptor del ecualizador ni el volumen. Bandas rotuladas 100, 300, 1k, 3k, 8k
- Libro inaccesible: "Volver a buscar" hace una búsqueda completa y, si el libro vuelve, lo abre en pausa; "Quitar" lo quita de la biblioteca como el menú del libro (Pantallas › Biblioteca › A), con "Deshacer", y cierra Escuchando (2026-10-05)
- Minirreproductor: superpuesto abajo del todo, con fundido; las pantallas reservan su alto (62) abajo, así abrir o cerrar pantallas no desplaza nada. Muestra tramo · posición en el tramo. De lado a lado, sin márgenes ni esquinas redondeadas (la línea de progreso hace de borde superior), y fondo de superficie al 85 %: se ve pasar el contenido por debajo (2026-10-04)
- Horizontal (2026-10-05), lienzo "Horizontal a doble panel": cuando la pantalla es más ancha que alta. Sin cabecera; portada a la izquierda (toma el ancho que deja la columna central, hasta 300; tocar y deslizar como en vertical), columna central con título, barras, controles (los huecos de Ajustes › Botones de 44, saltos con la flecha circular como en vertical desde el 2026-10-06, antes en texto; play 56) y fila pausa diferida · marcar · velocidad · ⋯, sin el botón de marcadores del libro. Panel derecho de 280 con pestañas Capítulos (o Archivos, si el libro no tiene capítulos) y Marcadores, esta inactiva hasta su función; filas de la hoja de capítulos, siguen al tramo en curso, tocar salta con "Deshacer". Libro inaccesible: portada a la izquierda, título y tarjeta a la derecha

### Biblioteca

Aprobado 2026-10-04. Cuatro entregas: A cuadrícula, B carpetas, C menú del libro, D búsqueda.

- Datos: una consulta Room devuelve cada libro con su posición en el libro (duración de los archivos anteriores a `positionFile` + `positionMs`), sus tiempos y su número de marcadores. La usan cuadrícula, Carpetas y búsqueda. Sin cambios en el esquema
- Portadas: los archivos de `CoverStore` con Coil, los mismos del reproductor

#### A. Cuadrícula

- Cabecera en una fila: "Biblioteca", selector Libros / Carpetas con iconos (cuadrícula y carpeta; el texto va como descripción), buscar, Marcadores y Ajustes. Con texto no cabía con tres iconos y letra grande. En horizontal, 52 de alto y se va con el scroll de la cuadrícula, con la línea de búsqueda debajo
- "Seguir escuchando": portada 60, título, barra en acento y play redondo en acento. Muestra el libro cargado; sin él, el último escuchado que no esté quitado de recientes ni terminado; sin ninguno, no aparece. Tocar la tarjeta abre Escuchando; play reproduce o pausa sin salir de la Biblioteca. Solo sin sesión de escucha: con ella, el libro ya está en el minirreproductor y la tarjeta no sale (nunca los dos a la vez, como en el lienzo)
- Filtros En curso / Sin empezar / Terminados (etiqueta 28) y botón de ordenar a la derecha. Varios activos: la unión. Ninguno: toda la biblioteca. Los filtros no se guardan
- Ordenar: popup "Ordenar por" con cinco opciones de 44 y marca en acento en la activa. El orden se guarda en DataStore. Debajo, separador y casilla "No disponibles", guardada en DataStore y desactivada por defecto: muestra los libros quitados, atenuados, mezclados en el orden elegido y sujetos a los filtros (2026-10-05)
  - Escuchados recientemente: los nunca escuchados al final, por fecha de añadido. Los quitados de recientes, como si no se hubieran escuchado; volver a escucharlos les quita la marca
  - Tiempo restante: a 1x, sin la velocidad del libro
- Cuadrícula de 2 columnas, separación 18 entre filas y 14 entre columnas, márgenes 20. Tarjeta: portada cuadrada, barra de 3, título 14, línea mono 12; ⋮ junto al título
- Línea mono: "44% · quedan 3:52:09", "sin empezar · 6:56:54", "terminado · 6:56:54" (sin barra); hasta dos líneas si no cabe
- Libro cargado: contorno en acento alrededor de la portada y barra en acento; los demás, barra gris
- Carpeta de Episodios o Sesiones: una sola tarjeta (portada apilada, "12 sesiones", sin barra) que abre la carpeta en la vista Carpetas; su ⋮ abre la hoja de clase (inactivo hasta B). Se agrupa por la carpeta que contiene los archivos, no por la que tiene la regla: una regla en "Podcasts" da una tarjeta por podcast
- Tocar un libro lo carga, empieza a sonar y abre Escuchando. Tocar el libro cargado solo abre Escuchando
- Libro inaccesible: sigue en la Biblioteca, atenuado; "no encontrado" en la línea mono. Tocarlo abre Escuchando con la tarjeta de libro inaccesible. "Quitar" en esa tarjeta y "Quitar de la biblioteca" en el menú del libro hacen lo mismo: lo quitan sin borrar nada, con "Deshacer" (2026-10-05)
- Libro quitado: fuera de la Biblioteca salvo con "No disponibles"; entonces atenuado, con "no disponible · n marcadores" (o el porcentaje) en la línea mono. Tocarlo abre la hoja de marcadores del libro. Conserva posición, marcadores, notas y tags; sus marcadores siguen en la recopilación. Si sus archivos vuelven, la búsqueda lo reconecta por firma o duración y vuelve a la Biblioteca (2026-10-05)
- Solo se muestran los libros de las carpetas actuales; los de una carpeta quitada reaparecen al volver a añadirla
- Columnas con el pellizco: 1, 2 o 3 en vertical (por defecto 2), el doble en horizontal. Un nivel por gesto; se guarda en DataStore
- Tras 3 columnas, un nivel más: lista con portada de 44 a la izquierda (como en Unir) y la fila de la lista sin portadas (título, autor, línea mono, barra si está en curso, ⋮; cargado en acento). Carpetas de Episodios o Sesiones con la portada de su primer libro en lugar del icono. En horizontal, dos columnas de filas. Separar los dedos vuelve a 3 columnas. Con "Mostrar portadas" apagado, la lista sin portadas, sin pellizco (2026-10-06)
- Sin portadas (Ajustes › Mostrar portadas): lista en vez de cuadrícula (Pantallas › Sin portadas)
- Al abrir la app, búsqueda rápida de cambios en segundo plano
- Buscando: bajo el selector, línea fina de progreso y "Buscando libros… n encontrados" con la carpeta actual. Los libros nuevos aparecen al terminar (el escaneo guarda en una transacción); dos tarjetas grises al final mientras dura la búsqueda. Así solo la búsqueda completa ("Volver a buscar"), el cambio de clase de carpeta y la primera búsqueda. La rápida al abrir es discreta (2026-10-05): solo la línea, superpuesta al borde inferior de la cabecera, sin texto ni tarjetas; no ocupa sitio y no desplaza la cuadrícula al terminar
- Sin carpetas: icono, "Aún no hay libros", "LECTOR busca los audiolibros dentro y los ordena por libro" y "Añadir carpeta" (destacado), que abre el explorador de carpetas
- Reserva abajo los 62 del minirreproductor

#### B. Carpetas

- Arriba, flecha de subir y ruta: "Almacenamiento principal /" (o "Tarjeta SD /") en gris y la carpeta actual en blanco. Atrás sube un nivel
- Árbol a partir de las rutas de los libros guardados, sin recorrer el disco. Una carpeta que es un libro es fila de libro y no se abre. En la raíz, las carpetas de la biblioteca; con una sola, se entra directamente en ella
- Fila de carpeta: icono 48, nombre y subtítulo: la clase con el número de obras si no es Libros ("Sesiones · 12"); si no, "Carpeta de autor" (solo subcarpetas) o el número de libros ("12 libros"). Su ⋮ abre la hoja de clase
- Fila de libro: portada 48, título 15 y línea mono "1:47:57 / 26:10:12 · 6%" con barra; sin empezar, "26:27:41 · sin empezar". Con marcadores, su número en lugar del porcentaje ("3 marcadores"). Libro cargado: título en acento y fila resaltada
- Hoja "Clase de carpeta": cabecera con nombre, ruta y número de archivos; cuatro opciones con su explicación y la nota final; se aplica al tocar. Cambiar de clase lanza una búsqueda rápida que reagrupa los libros
- "Ir a la carpeta" (`pendingFolder` de la navegación) abre Carpetas en la carpeta donde aparece el libro. La tarjeta de Episodios o Sesiones de la cuadrícula abre su carpeta; su ⋮, la hoja de clase
- Clase "Libros" en una subcarpeta de una carpeta con otra clase: se guarda como regla propia, para anular la heredada. Elegir la misma clase que la carpeta madre quita la regla propia
- Reagrupar (cambio de clase): si los archivos de un libro que ya no aparece siguen existiendo y ahora son de otros libros, cada marcador pasa al libro que contiene su archivo, la posición al del archivo en curso (si es más reciente que la suya) y el libro antiguo se borra. Solo queda inaccesible si le faltan archivos. Misma regla para Unir y Separar (Biblioteca › E). Pasan también velocidad y sonido (al juntar, los del libro antiguo más reciente) y terminado (al separar, si el antiguo lo estaba; al juntar, solo si lo estaban todos). No pasan el nombre propio ni "quitado de recientes". Solo a los libros que crea ese escaneo: uno que ya existía conserva sus ajustes (2026-10-05)

#### C. Menú del libro

- Hoja con cabecera: portada 52, título, "autor · narrador · serie n" y línea mono "posición / total · % · tamaño"
- Opciones: Marcadores [n], Ver portada, Ir a la carpeta, Separar en libros, Unir con otros libros, Marcar como terminado / no terminado, Reiniciar posición, Quitar de recientes; separador; Renombrar, Abrir con…, Borrar del móvil en coral
- Libro que es una carpeta entera: además, "Clase de carpeta" tras Unir, que abre la hoja de clase de esa carpeta. En Carpetas sale como fila de libro y no tenía otra forma de cambiar su clase (2026-10-05). Solo si la carpeta es un libro, como en Carpetas: no en un libro de archivos sueltos ni en uno unido, cuya ruta es la carpeta que lo contiene (2026-10-06)
- Reiniciar posición y Quitar de recientes: aviso con "Deshacer". Reiniciar el libro que suena lo lleva al inicio en pausa
- Renombrar: diálogo con Cancelar y Guardar; solo cambia el nombre en LECTOR
- Abrir con…: comparte el primer archivo del libro, o el que está en curso, por FileProvider
- Borrar del móvil: confirmación con número de archivos, tamaño, ruta y "No se puede deshacer"; conserva los marcadores. Si es el libro que suena, primero se descarga del reproductor. Borra solo los archivos del libro; el libro queda inaccesible con posición, marcadores y portada
- Libro sin archivos (inaccesible o quitado): sin Ir a la carpeta, Separar, Unir, Abrir con… ni Borrar del móvil. En su lugar, "Quitar de la biblioteca" (inaccesible) o "Devolver a la biblioteca" (quitado), sin coral y con "Deshacer": no borra nada (2026-10-05)

#### D. Búsqueda

Sin pantalla aparte: filtra la propia Biblioteca (decidido 2026-10-05; sustituye a la pantalla "Buscar en la biblioteca" del lienzo).

- La lupa convierte la cabecera en el campo: flecha atrás, campo y ×, con el teclado abierto. Mientras se busca, el selector Libros / Carpetas y los demás iconos se ocultan
- La flecha o Atrás cierran la búsqueda y la Biblioteca vuelve a como estaba, arriba del todo
- La cuadrícula se filtra a medida que se escribe, con lo encontrado en acento en el título de la tarjeta. Bajo el campo, línea "n libros · título, autor o carpeta"
- Busca en título, autor, narrador, serie y carpeta (ruta dentro de la carpeta de la biblioteca). Sin distinguir mayúsculas ni acentos; cada palabra tiene que aparecer. En memoria sobre la lista de la biblioteca
- Carpeta de Episodios o Sesiones: sale su tarjeta si coincide su nombre o alguno de sus archivos, como con los filtros
- Filtros y orden se mantienen y se combinan con la búsqueda. "Seguir escuchando" se oculta mientras se busca
- Encuentra siempre los libros quitados, atenuados, aunque "No disponibles" esté apagada
- Desde Carpetas, buscar pasa a la vista Libros; al cerrar, vuelve a donde estaba
- Tocar un resultado lo carga y abre Escuchando; un quitado, como en la cuadrícula

#### E. Unir y Separar

Hecha y probada en el móvil (2026-10-06). Código en `library/Corrections.kt` y `ui/library/CorrectionScreens.kt`.

- Cada una guarda una corrección y lanza una búsqueda visible; con otra en curso, se espera y se lanza otra. El reagrupado del escaneo pasa marcadores, posición y ajustes (Carpetas › Reagrupar)
- Unir (lienzo `Merge-Books`): pantalla completa con la carpeta donde aparece el libro, como en Carpetas, y sus libros disponibles en orden natural de ruta; el de origen, marcado. "Unir como un libro" con 2 o más. Al volver, aviso "Libros unidos" con "Deshacer". Partes en orden de nombre; título, portada y carpeta del primero
- Unir libros de carpetas distintas guarda rutas con ".." ("../Vol 2/01.mp3"); el reagrupado las normaliza
- Posición del libro unido: la del escuchado más recientemente. La de cada libro antiguo y sus posiciones por tramo pasan como posición por tramo al libro que tiene su archivo: la lista de capítulos las retoma y vuelven al deshacer. Un libro creado sin posición (al deshacer) empieza en la más reciente (2026-10-06)
- Separar (lienzo `Split-Book`): archivos del libro con casilla, el primero marcado y fijo; "Libro n" sobre cada uno nuevo. Título "Separar en n libros" ("Separar en libros" sin marcas) y botón activo con 2 o más. Sin aviso: se deshace en Correcciones. En el menú, atenuado si el libro tiene un solo archivo
- Libros separados: el título del original; si coincide en todos (el álbum), cada uno lleva su número detrás: "… (2/3)" (2026-10-06)
- Correcciones (Ajustes › Biblioteca, lienzo `Settings-Folders`): sección solo si hay alguna, la más reciente arriba. Etiqueta "separado" o "unido", nombre tomado al crearla (los libros que nombra ya no existen) y "Deshacer", que la borra y vuelve a buscar
- Libro cargado: al terminar cada búsqueda, si ya no existe (unido, separado o reagrupado por cambio de clase), el reproductor pasa al libro que tiene ahora su archivo, en el mismo punto y sonando si sonaba

### Ajustes

Aprobado 2026-10-05. Tres entregas por dependencias: A pantalla principal, Apariencia y Ecualizador y volumen; B Carpetas y explorador de carpetas; C en dos (2026-10-06): C1 Botones del reproductor y de la notificación; C2 botones remotos (auricular, teclas multimedia), Auricular y notificación estándar. Sin retrasos. Código en `ui/settings/`.

- Filas de funciones que aún no existen: atenuadas e inactivas, no ocultas, como la pausa diferida en Escuchando. Pausa diferida, Gestionar tags y Abrir la hoja al marcar, Datos y saltos del widget llegan con sus funciones
- "Siguiente archivo desde su posición": activa desde el 2026-10-05 (Reproducción › Posición por tramo), con la nota "Al terminar uno, el siguiente sigue donde se dejó"

#### A. Pantalla principal, Apariencia y Sonido

Hecha 2026-10-05.

- Pantalla principal con las secciones del lienzo: Reproducción, Sonido, Pausa diferida, Botones, Auricular, Biblioteca, Marcadores, Apariencia, Notificación y bloqueo, Datos, Sistema
- Hoja "Elegir valor": título y nota, − / valor / +, atajos y Listo; se guarda con Listo, cerrar la descarta. Tramo al reanudar: 0–30 s, atajos 0, 2, 3, 5, 10. Velocidad de los libros nuevos: 0.5x–3.5x en pasos de 0.05, atajos de la hoja de velocidad
- Reproducir al abrir la app (nuevo en DataStore): una vez por apertura de la app, cuando el último libro está cargado, si no suena ya
- Portada en la pantalla de bloqueo (nuevo en DataStore): quita la portada de la sesión, también de la notificación, como "Display cover outside the app" de Simple ABP. Se aplica al momento
- Volver a buscar libros: búsqueda completa, con "Buscando libros… n encontrados" en la fila mientras dura
- Seguir con la pantalla apagada: con la optimización de batería activa, el diálogo de Android para quitarla (permiso `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`); sin ella, la ficha de la app (en el Xiaomi abre "Battery details"). Permisos: la ficha de la app. Acerca de: "LECTOR" y la versión
- Ecualizador y volumen: edita el sonido global; mismos pasos que la hoja Sonido; Restablecer como en la hoja
- Apariencia: segmentado de tema, Cambiar por hora con las horas en el selector de Android, seis círculos de acento (color del sistema solo en Android 12+; personalizado con borde discontinuo y + hasta elegirlo) e idioma Español / English / Sistema (AppCompat; la Activity se rehace y la pila se conserva)
- Color personalizado: tono y luminosidad (0.15–0.9) con degradado, hex, vistas previas en oscuro y claro. El aviso dice "Poco contraste en el tema oscuro" (el lienzo dice "en el tema claro", pero la comprobación es sobre el fondo oscuro; el claro se deriva con contraste suficiente). "Ajustar" en acento, no en el color elegido, que puede no leerse

#### B. Carpetas y explorador de carpetas

Aprobado 2026-10-05.

- Escaneo: solo da por perdidos los libros dentro de las carpetas de la lista. Los de una carpeta quitada no se tocan (ni inaccesibles ni candidatos a la reconciliación por duración); una SD sacada sigue en la lista y sus libros quedan inaccesibles
- Fila "Carpetas" en Ajustes: subtítulo con el nombre si hay una, "Audiobooks · 3 carpetas" si hay varias. Abre Ajustes › Biblioteca (lienzo `Settings-Folders`)
- Lista de carpetas: nombre (ruta desde la raíz si no es de primer nivel), "Almacenamiento principal · n libros" o "Tarjeta SD · n libros" y × para quitar
- Quitar: aviso con "Deshacer", sin búsqueda (la Biblioteca solo muestra los libros de las carpetas de la lista). Se puede quitar la última: Biblioteca vacía
- "+ Añadir carpeta" abre el explorador
- "Volver a buscar libros" con "Última: [fecha] · n libros" debajo, guardado en DataStore. Sin "movidos, reconectados": el escaneo no lleva esa cuenta
- "Mostrar portadas" repetido, como en el lienzo
- Correcciones: solo si hay alguna (Biblioteca › E)
- Explorador (pantalla completa, lienzo `Folder-Picker`): selector Principal / Tarjeta SD solo con SD; flecha de subir y ruta; Atrás sube un nivel y en la raíz cierra. Filas de subcarpeta con "n archivos de audio" o "Sin audio", contados en segundo plano. Sin ocultas ni `Android/`. Una carpeta de la biblioteca, o dentro de una, atenuada con "Ya en la biblioteca" y sin abrir
- "Usar esta carpeta": sustituye a las carpetas de la lista que contiene, lanza una búsqueda rápida visible y vuelve a la pantalla de origen
- Se abre desde Ajustes › Biblioteca, "Añadir carpeta" de la Biblioteca vacía y "Elegir otra carpeta" del primer arranque

#### C1. Botones

Hecha 2026-10-06 (lienzo `Settings-Buttons`, `Action-Picker`). C2 (botones remotos, Auricular, notificación estándar y quitar las filas de retrasos) aparte.

- Hueco = acción y, si es un salto, sus segundos; 4 del reproductor y 4 de la notificación en DataStore. Por defecto en los dos: anterior, −10, +10, siguiente (el lienzo dice −30, |<, >|, Marca en la notificación: anterior al cambio del 2026-10-05)
- Acciones de la hoja: saltar atrás, saltar adelante, capítulo o archivo anterior, capítulo o archivo siguiente, play / pausa, añadir marcador, ir al marcador anterior, deshacer salto, nada. Un salto abre después Elegir valor con los segundos: 1–120, atajos 5, 10, 15, 30, 60
- Ajustes › Botones: "Saltos" abre la pantalla; "Botones remotos", atenuada hasta C2. Widget atenuado hasta los widgets. "Dividir el salto por la velocidad" con la nota "A 2x, +10 s salta 5 s de libro: 10 s de escucha" (el lienzo dice 20 s, lo contrario de lo que hace el motor)
- Escuchando: los huecos 1 y 2 a la izquierda de play, 3 y 4 a la derecha. Saltos con el número en la flecha, también en horizontal (44, icono 32); el resto, su icono; "Nada", hueco vacío
- Minirreproductor: marcar y play fijos; los dos saltos siguen a los huecos 2 y 3 del reproductor
- Notificación: hueco 2 en `SLOT_BACK`, 3 en `SLOT_FORWARD`, 1 y 4 en `SLOT_OVERFLOW` (extremos en HyperOS). Iconos de Media3 (saltos, anterior, siguiente, play, marcador); deshacer e ir al marcador anterior con iconos propios (`ic_undo`, `ic_bookmark_previous`). Cambia al guardar
- Saltos sin hueco propio (`COMMAND_SEEK_BACK` / `FORWARD` del sistema): los segundos del primer salto de ese sentido del reproductor, o 10
- Retrasos: Simple ABP no muestra retraso en nada (comprobado por el usuario 2026-10-06). Sin retrasos (decidido 2026-10-06): las dos filas salen en C2. En LECTOR los toques accidentales se corrigen con "Deshacer" o con el salto contrario

#### C2. Botones remotos y Auricular

Aprobado 2026-10-06 (lienzo `Settings-Buttons`, secciones "Botón del auricular" y "Teclas multimedia"; Simple ABP › Remote buttons).

- Ajustes › Botones › "Botones remotos": pantalla propia
- Botón del auricular: una, dos y tres pulsaciones, con la hoja de acciones de C1. Por defecto play / pausa, añadir marcador, nada (Decisiones de diseño: marcar sin mirar). Se cuentan solo play / pausa (`KEYCODE_MEDIA_PLAY_PAUSE`) y el botón del auricular de cable (`KEYCODE_HEADSETHOOK`). Con dos y tres en "nada", sin espera; si no, ~400 ms por si llega otra. Se queda en 1/2/3 aunque los auriculares del usuario (JBL) traigan gestos propios: sirve para otros auriculares (decidido 2026-10-06)
- Teclas multimedia: Anterior / Rebobinar −10 s, Siguiente / Avanzar +10 s, Play, Pausa, Stop → play / pausa. El lienzo y Simple ABP dicen 7 s; 10 como los saltos del reproductor y la notificación (decidido 2026-10-06)
- Responder con la app cerrada: activado por defecto. Con el servicio parado, una pulsación lo arranca y retoma el último libro. Apagado, no se reciben (receptor `MediaButtonReceiver` de Media3, desactivado con `PackageManager`). Con la app cerrada solo arrancan las teclas de play (play, play / pausa, auricular): Media3 descarta las demás para no fallar al pasar a primer plano (comprobado 2026-10-06). Media3 atribuye esas pulsaciones a la notificación; se distinguen de los botones de la notificación de Android 12 y anteriores porque estos llevan la sesión en los datos de la intención
- Media3 1.11.1 entrega cada pulsación una vez (comprobado 2026-10-06); se descarta igualmente una repetida en menos de 100 ms (androidx/media#3083)
- Con la pantalla abierta, pulsar un botón resalta su fila y no hace la acción
- Ajustes › Auricular: Pausar al desconectar (activado; hoy pausa siempre) y Reanudar al reconectar (activado): si vuelve en menos de 10 s tras una pausa por desconexión, sigue. Cable y Bluetooth
- Salen de Ajustes las dos filas de retrasos y "Notificación estándar de Android": la de LECTOR ya es la estándar de Android (lo que cambia en Simple ABP, sin comprobar)

#### D. Datos

Aprobado 2026-10-06 (lienzo `Settings-Data`, `Export-Sheet`, `Onboarding-Permission`; Decisiones de diseño › Datos, Recomendaciones › Portabilidad).

- Entregas: A, pantalla Datos con las dos exportaciones y la fila de Ajustes activa; B, Elegir copia, resumen y Combinar, con pruebas en el ordenador; C, "Importar una copia de otro móvil" en el primer arranque
- Copia completa: un `.json` con formato y versión, guardado con el selector de Android. Lleva:
  - Libros: firma, título, nombre propio, autor, duración total, ruta y archivos con su duración y su tamaño; posición con su fecha, terminado, velocidad y sonido propio. Sin id, que cambia de un móvil a otro. La ruta no identifica: solo coloca al libro no encontrado en Carpetas y deja que el escaneo lo reconecte, como a uno quitado aquí (decidido 2026-10-06). El tamaño (desde 2026-10-08, campo `sizeBytes` opcional, sin cambio de versión: las copias anteriores se leen con 0) deja reconectar por contenido al libro que en el otro móvil está en otra ruta o con la carpeta renombrada
  - Marcadores con sus tags (por nombre) y posiciones de tramo (`segment_position`)
  - Correcciones (unir y separar), por firma: sin ellas los libros unidos o separados no se reconocen en el otro móvil
  - Ajustes de DataStore e idioma de la app, salvo los propios del móvil: último libro, libro en curso y última búsqueda
  - Fuera: carpetas de la biblioteca y clases de carpeta, que son rutas de este móvil
- Marcadores como texto: abre la hoja Exportar de Marcadores C con todos los marcadores (vista previa, Copiar, Guardar como .txt, Compartir)
- Elegir copia: selector de Android y resumen previo en la tarjeta del lienzo: marcadores nuevos, ya existentes que se omiten, posiciones más recientes, libros no encontrados aquí; "Importar también los ajustes" apagado por defecto; Cancelar y Combinar
- Combinar:
  - Libro por firma; si no, por contenido (copias con tamaños, 2026-10-08); si no, por duración ±1 s entre los no encontrados, como la reconciliación del escaneo
  - Marcador con un id que ya existe: se omite
  - Posición: gana la de fecha más reciente; con ella pasan velocidad, sonido propio y terminado, como al reagrupar (`carryOver`). Posiciones de tramo, la más reciente de cada una. Nombre propio solo si aquí no hay
  - Marcador de pausa: uno por libro, el más reciente
  - Tags por nombre, sin distinguir mayúsculas; se crean si no existen
  - Correcciones nuevas: se guardan y se lanza una búsqueda discreta para aplicarlas
  - Archivo que no es una copia de LECTOR o de una versión más nueva: aviso en la nota de la fila, sin tocar nada
  - Libro no encontrado: entra como quitado, con sus marcadores; si sus archivos aparecen, el escaneo lo reconecta por firma, contenido o duración
  - Una sola transacción: entra todo o nada
  - Sin "Deshacer": el resumen ya confirma, como Unir

### Sin portadas

Aprobado 2026-10-05 (lienzo, versión 151: "Biblioteca sin portadas: lista", "Biblioteca: libros sin portada", "Reproductor sin portadas").

- Libro sin portada, con portadas activadas: portada tipográfica, con el autor arriba en mayúsculas y el título abajo sobre un tono apagado. El tono sale del primer autor (hasta `/`, `;`, `,` o `&`: "Joseph Goldstein/Joseph Goldstein" y "Joseph Goldstein" comparten tono) o, sin autor, del título; misma luminosidad para todos. Oscuro: fondo oscuro y texto casi blanco del mismo tono; claro: fondo claro y texto oscuro. Miniaturas con el mismo tono, sin autor. Carpeta de Episodios o Sesiones: su clase arriba y tono por su nombre. Título según columnas: 26 / 18 / 14
- "Mostrar portadas" apagado:
  - Biblioteca en lista, como Simple ABP: título, autor, línea mono de la tarjeta y barra si está en curso, ⋮; separador entre filas. Carpetas de Episodios o Sesiones: icono de carpeta, nombre, "Sesiones · 12". Sin pellizco. En horizontal, también una columna
  - "Seguir escuchando" sin miniatura, con el autor bajo el título
  - Vista Carpetas, menú del libro y minirreproductor sin miniatura
  - Escuchando sin recuadro: autor en mayúsculas, título de 28, "Narrado por…" y "6% · quedan…" arriba; el hueco en medio (deslizarlo hacia abajo hace Atrás) y controles en su sitio. En horizontal, sin portada a la izquierda. Libro inaccesible: título arriba y tarjeta abajo
  - El visor de portada sigue abriéndose desde "Ver portada" del menú del libro
- Porcentaje de Escuchando redondeado, como en la tarjeta (antes truncado: 41% frente a 42%)

### Primer arranque

Hecho 2026-10-05 (lienzo `Onboarding-Permission`, `Onboarding-Folder`).

- Permiso: "Dar permiso" abre el ajuste de acceso a todos los archivos de la app (Android 11+) o pide la lectura clásica (8–10; denegada para siempre, la ficha de la app). Al volver con el acceso, Carpetas; con carpetas ya guardadas (permiso retirado), directo a la Biblioteca. Carpetas sustituye a Permiso: Atrás sale de la app
- "Importar una copia de otro móvil" (lienzo, texto de 14 bajo "Dar permiso"): selector de Android y el resumen de Ajustes › Datos en una hoja, con "Importar también los ajustes" activada de entrada (el móvil nuevo no tiene ajustes que perder; decidido 2026-10-06). Al combinar, "Copia combinada" bajo el botón, y los avisos de error en la misma línea. Sin carpetas, todos los libros entran como no encontrados y Empezar los reconecta al buscar
- Carpetas: casillas con las carpetas con audio de niveles 1 y 2, por nombre, buscadas en segundo plano ("Buscando carpetas con audio…"). Fila: ruta desde la raíz y "Almacenamiento principal · n archivos de audio" en todas (no "n libros": contarlos exige la detección)
- Marcadas de entrada las de nombre de audiolibros (audiobook, audio book, audiolibro, libros)
- "Elegir otra carpeta" abre el explorador; la elegida aparece al final de la lista, marcada
- "Empezar", activo con alguna marcada: las carpetas de la biblioteca pasan a ser las marcadas (las contenidas en otra marcada salen) y la Biblioteca abre con la búsqueda visible

### Widget

Hecho 2026-10-07 (lienzo `Widget`).

- Un solo widget que se adapta a su tamaño (Jetpack Glance 1.2.0). Al ponerlo, 4×1
- Bajo (menos de 116 dp): una fila con portada, título y barra del libro, y botones
- Más alto que ancho (1,2 veces) o estrecho (menos de 200 dp): portada arriba ajustada al hueco, título, tramo, barra y tiempos del tramo, y botones abajo a todo lo ancho
- Ancho: portada a la izquierda con los textos, y botones debajo a todo lo ancho
- Textos según la altura: título siempre; tramo, barra y tiempos si caben
- Botones según el ancho, por prioridad: play; los dos huecos del widget (Ajustes › Botones › Saltos en el widget, −10 y +10 por defecto); los huecos de los extremos del reproductor (anterior y siguiente por defecto); marcar. Sin repetir acciones ni "Nada"
- 16 tamaños de referencia (anchos 100, 164, 260, 308; altos 40, 116, 220, 400); Android muestra el mayor que cabe. Android admite 16 como mucho, y una fila de Glance 10 elementos. No `SizeMode.Exact`: da una versión vertical y otra horizontal que Android elige por la orientación del móvil, y con el móvil de lado y el launcher en vertical salía la horizontal
- Botones: la acción va al servicio por la sesión; con el servicio parado lo arranca, carga el último libro y la aplica. Portada y textos abren Escuchando; sin libro, logo y "Elige un libro" abren la Biblioteca
- Datos: el estado del servicio; sin servicio, el último libro de la base de datos, en pausa. Tema, acento y "Mostrar portadas" de la app; portada reducida a 640 px y con las esquinas redondeadas en la imagen; sin portada, la tipográfica
- Se redibuja al cambiar libro, tramo, play / pausa, tema, acento o huecos; en pausa, al cambiar la posición; sonando, al saltar y una vez por minuto

### Marcadores

Aprobado y hecho 2026-10-06. Cuatro entregas. Sin cambios en el esquema (tablas `bookmark`, `tag`, `bookmark_tag` desde la versión 4).

#### A. Hoja de marcador y marcadores del libro

Hecha 2026-10-06.

- Hoja de marcador (lienzo `Bookmark-Sheet`): "Marcador guardado" con posición y Deshacer, título, nota, tags más usados (6, más los que ya lleva), "+ tag" y Listo. La misma hoja crea y edita; al editar, "Marcador" sin Deshacer. Título y nota se guardan al cerrar la hoja por cualquier camino; los tags, al tocarlos. Línea de posición: libro · tramo · posición en el tramo, hasta dos líneas
- Ajustes › Marcadores › "Abrir la hoja al marcar", activado por defecto. Desde la app (fila bajo los controles, huecos de Ajustes › Botones, minirreproductor, "Marcar aquí") abre la hoja; desde auricular y notificación solo guarda
- Hoja de marcadores del libro (lienzo `Book-Bookmarks`): desde Escuchando, "Marcadores [n]" del menú del libro y tocar un libro quitado. "Marcar aquí" y "estás aquí" solo con el libro cargado; play en cada fila si el libro tiene sus archivos. Tocar la fila edita, salvo el marcador de pausa (se sustituye en cada pausa: un título se perdería). El play carga el libro si hace falta, salta con "Deshacer", reproduce y cierra la hoja. "Ver todos los marcadores" y "Exportar" (inactivo hasta C)
- Lista de tags adelantada de B (sin ella "+ tag" no sirve): búsqueda, orden por uso o A–Z, "Crear «x»" (también con Intro), "Gestionar" (Ajustes › Gestionar tags) y Listo. B la usa para filtrar
- Pestaña Marcadores del panel derecho en horizontal: las filas de la hoja del libro, con "estás aquí"
- Las hojas se apilan (libro → marcador → tags): Atrás o tocar fuera vuelve a la anterior. Se conservan al girar

#### B. Recopilación

Hecha 2026-10-06.

- Pantalla Marcadores agrupada por libro (lienzo `Bookmarks-All`, `Bookmarks-Empty`, `Bookmarks-Filtered`, `Bookmark-Item-Menu`), con estado vacío. Cabecera de libro: portada de 28 (sin portadas, sin ella), título y número. El orden de los libros se fija al abrir la pantalla: escuchar desde un marcador no los reordena mientras se mira
- Filtros en una fila con desplazamiento lateral: lista de tags, todos (quita el filtro), tags por uso, sin tag y pausa (discontinuos). Varios, cualquiera de ellos. Con filtro, "n marcadores con [tags]" y "Quitar filtro"; los tags filtrados, en acento dentro de las filas
- Tocar la fila edita (hoja de marcador). ⋮: Copiar texto (formato de Exportar) y Borrar con "Marcador borrado" y "Deshacer". "Escuchar desde aquí" salta y se queda en Marcadores; sin play en libros sin archivos
- Lista de tags como filtro (lienzo `Tag-Picker`): casillas, "sin tag" (sin búsqueda activa), Limpiar y Aplicar; los cambios valen al aplicar. "Pausa" solo en la fila de filtros
- Columna de tiempo con el ancho de "00:00:00" en la letra del sistema (también en la hoja del libro): títulos alineados
- Iconos nuevos: `ic_export`, `ic_filter_lines`, `ic_copy`

#### C. Búsqueda y Exportar

Hecha 2026-10-06.

- Búsqueda en título, nota y tag, con lo encontrado resaltado en acento, como en la Biblioteca (lienzo `Bookmarks-Search`). Cada palabra tiene que estar; sin distinguir mayúsculas ni acentos. Sin los marcadores de pausa. Tocar edita; el play salta y se queda en la búsqueda
- Hoja Exportar con vista previa: Copiar, Guardar como archivo (.txt, selector de Android), Compartir con otra app; respeta el filtro activo (lienzo `Export-Sheet`). Desde la recopilación ("n · filtro: …", archivo "LECTOR – marcadores") y desde la hoja del libro (sus marcadores sin el de pausa, archivo "[libro] – marcadores")
- Texto: libro y, por marcador, "13:42 · título" (el de pausa, "Pausa diferida"), nota y tags con #; libros separados por una línea. El mismo de Copiar texto
- Iconos nuevos: `ic_download`, `ic_share`

#### D. Gestionar tags

Hecha 2026-10-06.

- Ajustes › Gestionar tags (lienzo `Settings-Tags`, `Merge-Tag`), también desde "Gestionar" en la lista de tags: cada tag con su número de marcadores y su ⋮: Renombrar, Unir con otro tag (si hay otro) y Borrar tag en coral. Sin tags, "Aún no hay tags. Se crean desde un marcador."
- Renombrar: diálogo como el de Renombrar libro. Con el nombre de otro tag que ya existe (sin distinguir mayúsculas), se unen
- Unir: hoja "Unir «x» con…" con elección única, Cancelar y Unir; sin "Deshacer" (ya confirma)
- Borrar tag: sin confirmación, "Tag borrado" con "Deshacer", que lo devuelve a los mismos marcadores. Los marcadores no se borran

#### Huecos resueltos (2026-10-06)

- Recopilación: libros por última escucha, el más reciente arriba; dentro de cada libro, marcadores por posición
- "+ tag" en la hoja de marcador abre la lista completa de tags con la búsqueda activa; si lo escrito no existe, "Crear «x»"
- Posición mostrada: tiempo del libro entero, con capítulo o archivo debajo

### Android Auto

Diseño aprobado 2026-10-07. Sin coche: pruebas con el Desktop Head Unit (DHU).

- Navegar la biblioteca desde la pantalla del coche, con `MediaLibraryService` (ya en uso). Código en `playback/CarLibrary.kt`
- Tres pestañas en la raíz (Android Auto admite 4 como mucho, todas navegables; sin paginación):
  - Seguir escuchando: libros a medias, sin los quitados de recientes ni de la biblioteca, el más reciente primero. Cuadrícula
  - Biblioteca: todos los libros sin los quitados, en el orden guardado de la app. Carpetas de Episodios o Sesiones como carpeta con sus libros dentro. Cuadrícula
  - Marcadores: libros con marcadores, el escuchado más recientemente primero; dentro, sus marcadores normales por posición (como la recopilación sin filtro). Título del marcador, o capítulo y tiempo. Tocar uno: "Escuchar desde aquí"
- Cada libro con su estado (sin empezar, a medias con porcentaje, terminado). Tocarlo: abre y suena desde su posición
- Búsqueda por voz o texto con la misma búsqueda de la Biblioteca
- Portadas por un `ContentProvider` de solo lectura (Android Auto no acepta `file://`). En las listas, según "Mostrar portadas"; sin portada, la de Android Auto por defecto. En el reproductor, según "Portada en la pantalla de bloqueo", como la notificación
- Reproductor del coche: los huecos de la notificación. Las acciones propias también para el paquete de Android Auto (`com.google.android.projection.gearhead`)
- Sin Google Play: en el móvil, modo desarrollador de Android Auto y "Fuentes desconocidas"

## Modelo de datos

Aprobado 2026-10-03.

- Room: biblioteca, posiciones, marcadores, tags, correcciones y reglas de carpeta. DataStore: ajustes globales
- Libro:
  - ID interno estable, independiente de la ruta
  - Tipo (de momento solo audio)
  - Firma de identidad: nombre de carpeta + lista ordenada de nombres de archivo; si no coincide, contenido (nombre y tamaño de cada archivo); si no, duración total con margen de 1 s. Nunca la ruta
  - Ruta actual, actualizada al volver a buscar
  - Título de etiquetas o carpeta; nombre propio si se renombra en LECTOR
  - Autor, narrador, serie y número en la serie (etiquetas)
  - Duración total
  - Origen de portada: incrustada o imagen de la carpeta
  - Fecha de añadido y de última escucha
  - Oculto de recientes, terminado, inaccesible, quitado de la biblioteca
  - Posición: archivo y punto dentro de él, con su fecha (en conflicto gana la más reciente)
  - Velocidad, saltar silencios, sonido propio (activado, preamplificación, bandas del ecualizador)
- Archivo: libro, ruta relativa, orden, duración, tamaño
- Capítulo: archivo, título, inicio, fin
- Marcador: UUID, libro, posición (nombre de archivo relativo + punto), título y nota opcionales sin límite, clase (normal o pausa; pausa única por libro, filtro de sistema y no tag), fecha de creación y de última modificación
- Tag: ID y nombre. Relación muchos a muchos con marcadores; el orden por uso se calcula, no se guarda
- Carpeta de la biblioteca: ruta raíz
- Caché de archivo (`file_meta`): ruta, tamaño, fecha, duración, etiquetas, portada sí / no, capítulos
- Corrección: tipo (separar o unir), libros afectados por firma de identidad, archivos donde empieza cada libro (separar), fecha y nombre para Correcciones (base de datos en versión 5). Se aplica sobre cada escaneo y se puede deshacer. Sigue a sus libros si se mueven o se renombra su carpeta (2026-10-07): antes de aplicarla, si sus firmas no aparecen, se reconocen por los archivos (nombre y tamaño) de los libros que salieron de ella en la base; al unir, los libros detectados que juntos tienen exactamente esos archivos, en su orden; al separar, el que los tiene todos. No cuentan los que la base reconoce como otro libro (copias). Se guarda con las firmas nuevas y las que se derivan pasan a las correcciones siguientes. Sin todos los archivos, o en cadenas cuyo resultado intermedio ya no es un libro, queda como está
- Regla de carpeta: carpeta y dos ajustes, qué es una obra (la carpeta, cada archivo o las reglas de detección: clase Libros guardada para anular una heredada) y qué pasa al terminar (queda terminada o vuelve al inicio). La heredan las subcarpetas; vale para lo que se añada después. Sigue a su carpeta si se mueve o se renombra (2026-10-07): si la carpeta ya no está, la regla pasa a la carpeta con los mismos archivos de audio (nombre y tamaño, sacados de los libros de la base que había dentro, con o sin los inaccesibles); la más honda si sus carpetas de encima tienen los mismos; ninguna si hay varias (copias) o la nueva ya tiene regla

### Clases de carpeta

| Clase | Obra | Al terminar | Uso |
|---|---|---|---|
| Libros (por defecto) | reglas de detección | terminada; siguiente libro si está activado | audiolibros, lectures |
| Episodios | cada archivo | terminada, sin pasar a otra obra | podcasts, cursos por lecciones |
| Álbumes | la carpeta | vuelve al inicio, nunca terminada, sin pasar a otra obra | música |
| Sesiones | cada archivo | vuelve al inicio, nunca terminada, sin pasar a otra obra | meditación, yoga nidra |

- Marcadores, velocidad, sonido y pausa diferida iguales en todas las clases
- Se elige en el ⋮ de cada fila de carpeta (vista Carpetas), que abre directamente la hoja "Clase de carpeta": cabecera con nombre y ruta, cuatro opciones con una línea de explicación, nota "Vale para sus subcarpetas y para lo que se añada después". Se aplica al tocar, sin Listo. Artboard "Carpetas › Clase de carpeta" (lienzo, versión 143)
- Fila de carpeta con clase distinta de Libros: el subtítulo la indica ("Sesiones · 12")
- Cuadrícula de la biblioteca: una carpeta de Episodios o Sesiones es una sola tarjeta (portada apilada, "12 sesiones", sin barra de progreso) que abre la carpeta; su ⋮ abre la hoja de clase. Los álbumes son obras normales
- Fuera de alcance: aleatorio, listas de reproducción, navegar por artista

## Referencia: Voice

Voice (PaulWoitaschek/Voice, GPLv3): reproductor de audiolibros de código abierto con el mismo stack. Solo inspiración: no se copia código.

- Fundido de volumen al final del temporizador, siempre activo, sin ajuste
- Reanudar por movimiento: una sola función con la de Simple ABP, ventana de 30 s. En el Xiaomi del usuario funciona con Simple ABP: el sistema lo permite
- Marcador automático al saltar el temporizador: tag de sistema filtrable, oculto por defecto en la recopilación, y solo el último por libro
- Velocidad por libro; la de los libros nuevos sale del ajuste global
- Sonido propio por libro (amplía la ganancia por libro de Voice): cada libro usa el sonido global (preamplificación y ecualizador de Ajustes) o uno propio. El volumen es siempre global
- Saltar silencios: por libro, desactivado por defecto
- Autor, narrador, serie y parte leídos de las etiquetas: para búsqueda y ficha del libro

En el lienzo (2026-10-02):

- Fundido: sin interfaz
- Ajustes › Pausa diferida: sección "Al pausar", fuera de "Automática por horario", válida para toda pausa diferida. Contiene "Marcar dónde se pausó" (activado, solo el último por libro, tag pausa) y "Seguir si muevo el móvil" (30 s)
- Marcadores del libro: el marcador de pausa aparece en su posición con icono de luna, "Pausa diferida · hace [n] h", en gris
- Recopilación: filtro de sistema "pausa" con luna y borde discontinuo, como "sin tag". Sin ese filtro, los marcadores de pausa no aparecen
- Hoja Sonido (⋯ del reproductor › Ecualizador y volumen): volumen arriba; debajo, interruptor "Sonido propio para este libro". Activado, la preamplificación y el ecualizador de la hoja son solo de ese libro, partiendo de los globales; desactivado, el libro vuelve al global. Ajustes › Ecualizador y volumen edita el sonido global
- Hoja Velocidad: interruptor "Saltar silencios" sobre "Se guarda para este libro"
- Menú del libro: línea "autor · narrador · serie n" bajo el título

## Limitaciones conocidas

- Varios libros sueltos en una carpeta sin capítulos no se distinguen de un libro en partes: requieren "separar" manual

## Riesgos técnicos

- Formatos: Simple ABP decodifica con FFmpeg. Media3 de serie no cubre algunos formatos (p. ej. wma). Si aparecen, extensión FFmpeg de Media3 (la precompilada de Jellyfin va por detrás de Media3: comprobar compatibilidad)
- Capítulos m4b: resuelto. Media3 1.11 (agosto 2026) extrae capítulos QuickTime y Nero de mp4/m4a/m4b
