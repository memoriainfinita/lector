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
- "Download file": [PENDIENTE: qué hace]
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
- Icono de la app: libro abierto (página izquierda ámbar, derecha crema) con play en la página izquierda, sobre fondo negro. Artboard "Icono: L2 con play a la izquierda (ajustable)" del lienzo. Valores elegidos (viewBox 108):
  - Colores: fondo #0A0A0A, página izquierda #F0A43A, página derecha #EDE3D1, play #0A0A0A
  - Radio del fondo 28; libro ancho 30, alto 38, curvatura 3.5, separación del lomo 0, desplazamiento 0/0
  - Play: tamaño 16.5, proporción 0.85, desplazamiento X 0, Y −2.5
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
- Propuestas en el lienzo, sin confirmar:
  - Capítulos: en libros con capítulos, la segunda barra es la del capítulo; tocar su nombre abre la lista; anterior/siguiente saltan de capítulo
  - Deshacer salto: aviso temporal sobre el menú inferior con "Deshacer"
  - Menú de marcador (⋮ en cada marcador): escuchar desde aquí, editar, copiar texto, borrar
  - Unir libros: "Unir con otros libros" en el menú del libro abre la selección múltiple
  - Biblioteca: filtros En curso / Sin empezar / Terminados y botón de ordenar; "Marcar como terminado" en el menú del libro
  - Carpetas: Ajustes › Biblioteca › Carpetas (añadir, quitar, volver a buscar, correcciones). Añadir abre un explorador propio (principal / SD) con "Usar esta carpeta"
- Marcadores: botón de play en cada marcador ("escuchar desde aquí") junto al ⋮; el menú del marcador queda en editar, copiar texto, borrar
- Popups: filas de 44 px
- Hoja de marcador: botón Listo pequeño, a la derecha
- Muchos tags (propuesto, sin confirmar): fila de filtros de una línea con desplazamiento lateral; en la hoja solo los más usados; lista completa con búsqueda, crear tag y orden por uso o A–Z; gestionar tags en Ajustes
- Menú inferior de la app: solo iconos, pequeño (barra de 48 px)
- Fila bajo los controles (pausa, marcar, velocidad, marcadores): pequeña, iconos con texto mínimo

## Recomendaciones técnicas

Sin aprobar.

- Stack: Kotlin, Jetpack Compose, Media3, Room
- Acceso a archivos: `MANAGE_EXTERNAL_STORAGE` (SAF es lento recorriendo bibliotecas grandes). Instalación por adb, sin Google Play. En duda: Simple ABP funciona solo con permisos de medios (`READ_MEDIA_AUDIO`); decidir con el stack
- Android mínimo: 8.0
- Libro con campo de tipo (de momento solo audio), para añadir lectura sin rehacer la base de datos

## Limitaciones conocidas

- Varios libros sueltos en una carpeta sin capítulos no se distinguen de un libro en partes: requieren "separar" manual

## Riesgos técnicos

- Formatos y capítulos: Simple ABP decodifica con FFmpeg. Media3 de serie no cubre algunos formatos (p. ej. wma) ni lee bien capítulos de m4b. Requiere lector de capítulos propio o la extensión FFmpeg de Media3
