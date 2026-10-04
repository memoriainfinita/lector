# LECTOR — design

Reproductor de audiolibros para Android, clon propio de Simple Audiobook Player (mdmt, "Simple ABP").

## Objetivo

- Mismo minimalismo que Simple ABP: "sencilla, eficaz y perfecta"
- Más pulida, con portadas y navegación más moderna
- Lo primero es el reproductor; los marcadores con notas son un añadido

## Funciones de Simple ABP a replicar

Todas, también las de pago. Detalle en "Simple ABP: inventario".

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
  - Libro identificado por nombre de carpeta y archivos; si falla, por duración total. No por ruta
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
- Deshacer salto: aviso temporal abajo, sobre el minirreproductor si lo hay, con "Deshacer", unos 5 s. Solo en saltos grandes (barra, cambio de capítulo o archivo, ir a un marcador o capítulo), no en ±10 s. Con saltos encadenados vuelve a la posición previa al primero
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
  - Búsqueda en la biblioteca (título, autor, narrador, serie, carpeta) y en marcadores (título, nota, tag), con lo encontrado resaltado
  - Ordenar: escuchados recientemente (por defecto), añadidos recientemente, título, autor, tiempo restante
  - Estados vacíos: biblioteca sin carpetas con "Añadir carpeta"; marcadores vacíos con cómo marcar
  - Libro inaccesible: aviso con "Volver a buscar" y "Quitar"; conserva posición y marcadores
  - Aviso al abrir tras un cierre del sistema con la pantalla apagada: dónde se paró, "Abrir ajustes" (batería) y "Ahora no"
- Color de acento: por defecto #F58F00, el naranja del icono (antes #F0A43A). En tema claro, variante oscura del mismo tono (#B86E0E en el lienzo). Opciones en Ajustes › Apariencia: ámbar, azul, verde, coral, color del sistema (Android 12+) y personalizado
- Ajustes › Apariencia: interruptor "Mostrar portadas" (de Simple ABP, decidido 2026-10-04); sin portadas, la superficie con el título
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
  - Datos: exportar copia completa (JSON) o marcadores como texto; importar y combinar con resumen previo (marcadores nuevos, ya existentes que se omiten, posiciones más recientes, libros no encontrados) e "Importar también los ajustes" opcional
  - Primer arranque: pantalla de permiso ("No sube nada a internet", con "Importar una copia de otro móvil") y pantalla de carpetas con audio encontradas, "Elegir otra carpeta" y "Empezar"
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
  - Elegir valor: hoja común con − / +, valor grande y atajos (segundos de cada salto, tramo repetido al reanudar, retraso de botones, minutos de la pausa diferida)
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
- Portadas: miniatura (lado mayor 1024) en almacenamiento de la app, de la imagen incrustada o, si no hay, de cover / folder / front o la única imagen de la carpeta (solo si la carpeta tiene un único libro)
- Caché `file_meta` por ruta: el escaneo rápido (al abrir) solo relee archivos con tamaño o fecha distintos; "Volver a buscar" relee todo y rehace portadas
- Dos fases: recorrer y leer (progreso: encontrados y carpeta actual), después detectar, aplicar correcciones, reconciliar y guardar en una transacción. Los libros aparecen al terminar
- Reconciliación: por firma; si no, por duración ±1 s entre los que no aparecen; los que faltan quedan inaccesibles con sus datos
- Carpetas candidatas para el primer arranque: niveles 1 y 2 con audio bajo cada almacenamiento

## Reproducción

Aprobado 2026-10-03. Código en `playback/`; pantalla de depuración "LECTOR reproducción".

- Dos entregas: A (servicio, posición, navegación, acciones, sesión, conexión con la interfaz) y B (velocidad y procesamiento de sonido)
- `MediaLibraryService` con ExoPlayer; foco de audio; pausa al desconectar el auricular. Todo en el hilo principal (Media3 1.11 lo exige)
- Libro = lista de reproducción con un elemento por archivo. Capítulos calculados sobre la posición desde la tabla `chapter`, sin cortar el audio. Posición global del libro ↔ archivo + punto
- Posición guardada cada ~5 s sonando, al pausar, al saltar y al cerrar el servicio, con `positionUpdatedAt` y `lastPlayedAt`. Al abrir un libro se restauran posición, velocidad y saltar silencios
- Saltos ±N s cruzando archivos. Anterior / siguiente: capítulo si el libro tiene capítulos, si no archivo; anterior con más de 3 s vuelve al inicio. Ajustes de salto: segundos por botón, dividir el tiempo por la velocidad, siguiente archivo desde posición distinta de cero
- Deshacer salto: solo saltos grandes (barra, cambio de capítulo o archivo, ir a un marcador o capítulo). Con saltos encadenados vuelve a la posición previa al primero
- Tramo repetido al reanudar tras una pausa: valor en DataStore, 3 s por defecto
- Al terminar, según la clase de carpeta: Libros, terminado y siguiente libro si está activado (desactivado por defecto); Episodios, terminado sin pasar a otra obra; Álbumes y Sesiones, vuelve al inicio, nunca terminado
- Acciones: un solo conjunto con ejecutor (saltar atrás, saltar adelante, anterior, siguiente, play / pausa, añadir marcador, ir al marcador anterior, deshacer salto, siguiente libro). Marcar guarda en la posición actual; la hoja llega con las pantallas. Ir al marcador anterior: el inmediatamente anterior a la posición
- Notificación: −30, anterior, play, siguiente, marcar. −30 y marcar con `setMediaButtonPreferences` y `CommandButton`; anterior y siguiente son los del sistema, que `LectorPlayer` pasa por los tramos (como botones propios, Media3 los quita de las acciones estándar que leen coches y pantallas de bloqueo). Acciones propias como `SessionCommand`, concedidas en `onConnectAsync`
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
- Pendiente: "siguiente archivo desde posición distinta de cero" (ajuste de Simple ABP, comportamiento sin comprobar); sin implementar

## Navegación

Aprobado 2026-10-03. Revisado 2026-10-04: sin menú inferior, una sola pila.

- Dependencias: Navigation3 1.2.0 (`navigation3-runtime`, `navigation3-ui`), `lifecycle-viewmodel-navigation3` 2.11.0, plugin de serialización de Kotlin 2.4.20 y `kotlinx-serialization-core`
- Una sola pila con Biblioteca en la raíz; Atrás en Biblioteca sale de la app
- Escuchando y Marcadores se abren encima. Si ya están en la pila, suben arriba en vez de duplicarse. Atrás vuelve a la pantalla anterior. Marcadores lleva flecha de Atrás y título de 20, como las subpantallas (lienzo, versión 148)
- Cabecera de Biblioteca: buscar, Marcadores y Ajustes. Ajustes también desde el ⋯ del reproductor
- Todo Atrás pasa por un punto único, para aplicar "Retrasar el botón Atrás" (comportamiento en Simple ABP sin comprobar)
- Con minirreproductor: Biblioteca, Marcadores, búsqueda en la biblioteca y búsqueda en marcadores
- Biblioteca: Libros / Carpetas es un selector de la misma pantalla. En Carpetas se entra en subcarpetas con la ruta arriba; Atrás sube un nivel
- Pantallas completas, sin minirreproductor: Ajustes y sus subpáginas (Pausa diferida, Botones, Ecualizador y volumen, Carpetas, Gestionar tags, Apariencia, Datos), explorador de carpetas, unir libros, separar en libros, visor de portada. El visor se cierra con × o deslizando hacia abajo
- Hojas, menús emergentes y diálogos: estado de su pantalla, no entradas de navegación. Atrás cierra primero la hoja
- De hoja a pantalla completa: "Horario automático y más" → Ajustes › Pausa diferida; "Gestionar" en la lista de tags → Ajustes › Gestionar tags; "Ajustes" en el ⋯ del reproductor → Ajustes. Al volver, la pantalla de origen sin la hoja
- Explorador de carpetas desde Ajustes › Carpetas, la biblioteca vacía ("Añadir carpeta") y el primer arranque ("Elegir otra carpeta")
- Saltos:
  - "Ir a la carpeta" (menú del libro y ⋯ del reproductor) → vuelve a la raíz, Biblioteca › Carpetas, en esa carpeta
  - "Ver todos los marcadores" (hoja de marcadores del libro) → Marcadores
  - Tocar un libro en la cuadrícula o en la búsqueda lo carga y abre Escuchando
- "Marcadores" en el menú del libro abre la hoja de marcadores de ese libro
- "Escuchar desde aquí" en un marcador salta y se queda en Marcadores, con el minirreproductor y "Deshacer"
- Minirreproductor abajo del todo, con un libro cargado y una sesión de escucha en curso, en todas las pantallas salvo Escuchando y las pantallas completas. La sesión empieza cuando algo suena o se abre Escuchando; al abrir la app, el libro cargado en pausa solo sale en "Seguir escuchando" de Biblioteca. Se conserva al girar la pantalla; si la app sigue sonando en segundo plano, al volver hay minirreproductor. La flecha del reproductor y deslizar la portada hacia abajo hacen Atrás; tocar el minirreproductor abre Escuchando
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
- Aviso de salto: "Saltado desde [posición]" con Deshacer
- Play / pausa según "va a sonar": no parpadea mientras carga
- ⋯ en la fila bajo los controles, a la derecha (su menú sale por abajo). Cabecera solo con la flecha y "Escuchando". Lienzo actualizado (versión 146)
- Deslizar la portada hacia abajo hace lo mismo que la flecha: Atrás. Actúa al soltar pasado el umbral de 120, el del visor; la portada no se mueve, y en el visor tampoco. Solo hacia abajo: a los lados no hay gesto (2026-10-04)
- Pausa diferida y marcadores del libro: inactivos hasta sus funciones. Marcar guarda sin hoja hasta Marcadores
- Menú ⋯: Tema alterna oscuro y claro a partir del que se ve
- Velocidad: − / + en pasos de 0.05
- Sonido: cambios en vivo, enviados al cambiar el valor redondeado (1 dB; bandas 0.5 dB). Restablecer pone a 0 preamplificación y bandas, sin tocar el interruptor del ecualizador ni el volumen. Bandas rotuladas 100, 300, 1k, 3k, 8k
- Libro inaccesible: "Volver a buscar" hace una búsqueda completa y, si el libro vuelve, lo abre en pausa; "Quitar" deja de mostrarlo (Escuchando inactivo) y conserva posición y marcadores
- Minirreproductor: superpuesto abajo del todo, con fundido; las pantallas reservan su alto (62) abajo, así abrir o cerrar pantallas no desplaza nada. Muestra tramo · posición en el tramo. De lado a lado, sin márgenes ni esquinas redondeadas (la línea de progreso hace de borde superior), y fondo de superficie al 85 %: se ve pasar el contenido por debajo (2026-10-04)
- Horizontal: pendiente

### Biblioteca

Aprobado 2026-10-04. Cuatro entregas: A cuadrícula, B carpetas, C menú del libro, D búsqueda.

- Datos: una consulta Room devuelve cada libro con su posición en el libro (duración de los archivos anteriores a `positionFile` + `positionMs`), sus tiempos y su número de marcadores. La usan cuadrícula, Carpetas y búsqueda. Sin cambios en el esquema
- Portadas: los archivos de `CoverStore` con Coil, los mismos del reproductor

#### A. Cuadrícula

- Cabecera en una fila: "Biblioteca", selector Libros / Carpetas con iconos (cuadrícula y carpeta; el texto va como descripción), buscar, Marcadores y Ajustes. Con texto no cabía con tres iconos y letra grande. En horizontal, 52 de alto y se va con el scroll de la cuadrícula, con la línea de búsqueda debajo
- "Seguir escuchando": portada 60, título, barra en acento y play redondo en acento. Muestra el libro cargado; sin él, el último escuchado que no esté quitado de recientes ni terminado; sin ninguno, no aparece. Tocar la tarjeta abre Escuchando; play reproduce o pausa sin salir de la Biblioteca. Solo sin sesión de escucha: con ella, el libro ya está en el minirreproductor y la tarjeta no sale (nunca los dos a la vez, como en el lienzo)
- Filtros En curso / Sin empezar / Terminados (etiqueta 28) y botón de ordenar a la derecha. Varios activos: la unión. Ninguno: toda la biblioteca. Los filtros no se guardan
- Ordenar: popup "Ordenar por" con cinco opciones de 44 y marca en acento en la activa. El orden se guarda en DataStore
  - Escuchados recientemente: los nunca escuchados al final, por fecha de añadido. Los quitados de recientes, como si no se hubieran escuchado; volver a escucharlos les quita la marca
  - Tiempo restante: a 1x, sin la velocidad del libro
- Cuadrícula de 2 columnas, separación 18 entre filas y 14 entre columnas, márgenes 20. Tarjeta: portada cuadrada, barra de 3, título 14, línea mono 12; ⋮ junto al título
- Línea mono: "44% · quedan 3:52:09", "sin empezar · 6:56:54", "terminado · 6:56:54" (sin barra); hasta dos líneas si no cabe
- Libro cargado: contorno en acento alrededor de la portada y barra en acento; los demás, barra gris
- Carpeta de Episodios o Sesiones: una sola tarjeta (portada apilada, "12 sesiones", sin barra) que abre la carpeta en la vista Carpetas; su ⋮ abre la hoja de clase (inactivo hasta B). Se agrupa por la carpeta que contiene los archivos, no por la que tiene la regla: una regla en "Podcasts" da una tarjeta por podcast
- Tocar un libro lo carga, empieza a sonar y abre Escuchando. Tocar el libro cargado solo abre Escuchando
- Libro inaccesible: sigue en la Biblioteca, atenuado, para mantener a mano sus marcadores; "no encontrado" en la línea mono. Tocarlo abre Escuchando con la tarjeta de libro inaccesible. "Quitar" en Escuchando solo cierra la tarjeta
- Solo se muestran los libros de las carpetas actuales; los de una carpeta quitada reaparecen al volver a añadirla
- Columnas con el pellizco: 1, 2 o 3 en vertical (por defecto 2), el doble en horizontal. Un nivel por gesto; se guarda en DataStore
- Sin portadas (Ajustes › Apariencia › Mostrar portadas): la superficie con el título en cuadrícula, filas, búsqueda y reproductor
- Al abrir la app, búsqueda rápida de cambios en segundo plano
- Buscando: bajo el selector, línea fina de progreso y "Buscando libros… n encontrados" con la carpeta actual. Los libros nuevos aparecen al terminar (el escaneo guarda en una transacción); dos tarjetas grises al final mientras dura la búsqueda
- Sin carpetas: icono, "Aún no hay libros", "LECTOR busca los audiolibros dentro y los ordena por libro" y "Añadir carpeta" (destacado), que abre el explorador de carpetas
- Reserva abajo los 62 del minirreproductor

#### B. Carpetas

- Arriba, flecha de subir y ruta: "Almacenamiento principal /" en gris y la carpeta actual en blanco. Atrás sube un nivel
- Árbol a partir de las rutas de los libros guardados, sin recorrer el disco. Una carpeta que es un libro es fila de libro y no se abre. En la raíz, las carpetas de la biblioteca; con una sola, se entra directamente en ella
- Fila de carpeta: icono 48, nombre y "Carpeta de autor" o la clase con el número ("Sesiones · 12"). Su ⋮ abre la hoja de clase
- Fila de libro: portada 48, título 15 y línea mono "1:47:57 / 26:10:12 · 6%" con barra; sin empezar, "26:27:41 · sin empezar". Con marcadores, su número en lugar del porcentaje ("3 marcadores"). Libro cargado: título en acento y fila resaltada
- Hoja "Clase de carpeta": cabecera con nombre, ruta y número de archivos; cuatro opciones con su explicación y la nota final; se aplica al tocar. Cambiar de clase lanza una búsqueda rápida que reagrupa los libros
- "Ir a la carpeta" (`pendingFolder` de la navegación) abre Carpetas en esa ruta

#### C. Menú del libro

- Hoja con cabecera: portada 52, título, "autor · narrador · serie n" y línea mono "posición / total · % · tamaño"
- Opciones: Marcadores [n], Ver portada, Ir a la carpeta, Separar en libros, Unir con otros libros, Marcar como terminado / no terminado, Reiniciar posición, Quitar de recientes; separador; Renombrar, Abrir con…, Borrar del móvil en coral
- Marcadores, Separar y Unir: enlazados a sus pantallas vacías hasta que se hagan; Marcadores inactivo
- Reiniciar posición y Quitar de recientes: aviso con "Deshacer". Reiniciar el libro que suena lo lleva al inicio en pausa
- Renombrar: diálogo con Cancelar y Guardar; solo cambia el nombre en LECTOR
- Abrir con…: comparte el primer archivo del libro, o el que está en curso, por FileProvider
- Borrar del móvil: confirmación con número de archivos, tamaño, ruta y "No se puede deshacer"; conserva los marcadores. Si es el libro que suena, primero se descarga del reproductor

#### D. Búsqueda

- Campo con flecha atrás y × para borrar; línea "n libros · título, autor o carpeta"; filas con portada 52, título, autor y línea mono; lo encontrado resaltado en acento
- Busca en título, autor, narrador, serie y carpeta. Sin distinguir mayúsculas ni acentos, a medida que se escribe, en memoria sobre la lista de la biblioteca
- Tocar un resultado lo carga y abre Escuchando

## Modelo de datos

Aprobado 2026-10-03.

- Room: biblioteca, posiciones, marcadores, tags, correcciones y reglas de carpeta. DataStore: ajustes globales
- Libro:
  - ID interno estable, independiente de la ruta
  - Tipo (de momento solo audio)
  - Firma de identidad: nombre de carpeta + lista ordenada de nombres de archivo; si no coincide, duración total con margen de 1 s. Nunca la ruta
  - Ruta actual, actualizada al volver a buscar
  - Título de etiquetas o carpeta; nombre propio si se renombra en LECTOR
  - Autor, narrador, serie y número en la serie (etiquetas)
  - Duración total
  - Origen de portada: incrustada o imagen de la carpeta
  - Fecha de añadido y de última escucha
  - Oculto de recientes, terminado, inaccesible
  - Posición: archivo y punto dentro de él, con su fecha (en conflicto gana la más reciente)
  - Velocidad, saltar silencios, sonido propio (activado, preamplificación, bandas del ecualizador)
- Archivo: libro, ruta relativa, orden, duración, tamaño
- Capítulo: archivo, título, inicio, fin
- Marcador: UUID, libro, posición (nombre de archivo relativo + punto), título y nota opcionales sin límite, clase (normal o pausa; pausa única por libro, filtro de sistema y no tag), fecha de creación y de última modificación
- Tag: ID y nombre. Relación muchos a muchos con marcadores; el orden por uso se calcula, no se guarda
- Carpeta de la biblioteca: ruta raíz
- Caché de archivo (`file_meta`): ruta, tamaño, fecha, duración, etiquetas, portada sí / no, capítulos
- Corrección: tipo (separar o unir), libros afectados por firma de identidad, archivos donde empieza cada libro (separar), fecha. Se aplica sobre cada escaneo y se puede deshacer
- Regla de carpeta: carpeta y dos ajustes, qué es una obra (la carpeta o cada archivo) y qué pasa al terminar (queda terminada o vuelve al inicio). La heredan las subcarpetas; vale para lo que se añada después

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
