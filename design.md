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

## Recomendaciones aceptadas en planteamiento

Pendientes de confirmar en el lienzo de diseño.

- Detección de libros:
  - Carpeta con partes sin capítulos = un libro
  - Carpeta con varios m4b o archivos con capítulos = un libro por archivo
  - Carpeta sin audio con subcarpetas = agrupador (autor)
  - Excepción: subcarpetas con nombre de disco (CD, Disc, Parte + número) = un solo libro
  - Corrección manual "separar / unir", persistente
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
- Minirreproductor: la flecha del reproductor lo reduce a una barra sobre el menú inferior; se sigue escuchando mientras se navega. Botones: marcar, −10, play/pausa, +10
- Descartado: reproductor B (fiel a Simple ABP)
- Cuadrícula de la biblioteca: ⋮ pequeño junto al título de cada libro, bajo la portada. Descartados: pulsación larga (no es intuitiva) y ⋯ sobre la portada
- Ecualizador accesible desde el ⋯ del reproductor como hoja, además de en Ajustes
- Capítulos: en libros con capítulos, la segunda barra es la del capítulo; tocar su nombre abre la lista; anterior/siguiente saltan de capítulo. Anterior con más de 3 s dentro del capítulo vuelve a su inicio
- Deshacer salto: aviso temporal sobre el menú inferior con "Deshacer", unos 5 s. Solo en saltos grandes (barra, cambio de capítulo o archivo, ir a un marcador o capítulo), no en ±10 s. Con saltos encadenados vuelve a la posición previa al primero
- Aviso con "Deshacer" como patrón de la app: saltos, borrar marcador, unir libros, quitar carpeta, borrar tag, reiniciar posición, quitar de recientes. Excepción: borrar del móvil pide confirmación porque no se puede deshacer
- Marcadores: botón de play en cada marcador ("escuchar desde aquí"). Tocar la fila abre la edición (hoja de marcador). El ⋮ queda en copiar texto y borrar. Borrar sin confirmación, con "Deshacer"
- Marcadores sin límite de número ni de longitud del título
- Unir libros: "Unir con otros libros" en el menú del libro abre la selección múltiple. Orden natural de nombre. Los marcadores de cada parte pasan al libro unido; la posición es la última escuchada. Se deshace con el aviso o en Ajustes › Biblioteca › Carpetas › Correcciones
- Biblioteca: filtros En curso / Sin empezar / Terminados como interruptores; sin ninguno activo, toda la biblioteca (vista al abrir). Botón de ordenar. "Marcar como terminado" / "Marcar como no terminado" en el menú del libro
- Carpetas: Ajustes › Biblioteca › Carpetas (añadir, quitar, volver a buscar, correcciones). Añadir abre un explorador propio (principal / SD) con "Usar esta carpeta"; depende del acceso directo a archivos (con SAF sería el selector de Android). Quitar una carpeta conserva posiciones y marcadores. Búsqueda de cambios automática al abrir la app; "Volver a buscar" fuerza una completa
- Popups: filas de 44 px
- Hoja de marcador: botón Listo pequeño, a la derecha
- Muchos tags: fila de filtros de una línea con desplazamiento lateral; en la hoja de marcador solo los más usados; lista completa con búsqueda, crear tag y orden por uso o A–Z, que también asigna tags desde la hoja de marcador ("Listo" en vez de "Aplicar"). Varios tags en el filtro muestran los marcadores con cualquiera de ellos. Gestionar tags en Ajustes (renombrar, unir, borrar); borrar un tag no borra marcadores
- Menú inferior de la app: solo iconos, pequeño (barra de 48 px)
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
  | Segmentado (Libros / Carpetas, tema, idioma) | 36 | — | 14 |
  | Opción en hoja (velocidad, pausa, valores) | 36 | 2 | 13 mono |
  | Etiqueta o filtro | 28 | 2 | 12 mono |
  | Fila de menú | 44 | — | 14–15 |
  | Icono | 44 | — | — |
- Escala de texto: 11 etiquetas y texto sobre portadas pequeñas; 12 metadatos y tiempos en mono; 13 secundario; 14 listas y botones; 15 texto principal de fila; 17 título de hoja; 20 título de subpágina; 22 título del libro en el reproductor; 24 título de pestaña; 28 valores grandes (velocidad, cuenta atrás) y titular de bienvenida. Las portadas de ejemplo del lienzo quedan fuera

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

## Referencia: Voice

Voice (PaulWoitaschek/Voice, GPLv3): reproductor de audiolibros de código abierto con el mismo stack. Solo inspiración: no se copia código.

- Fundido de volumen al final del temporizador, siempre activo, sin ajuste
- Reanudar por movimiento: una sola función con la de Simple ABP, ventana de 30 s. Probar en el Xiaomi del usuario (Voice avisa de fallos en algunos móviles)
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
