package codelab.lector.data.db

/** Formato de la obra. Lectura se añadirá más adelante. */
enum class BookType { AUDIO }

enum class CoverSource { NONE, EMBEDDED, FOLDER }

enum class BookmarkKind { NORMAL, PAUSE }

enum class CorrectionType { SPLIT, MERGE }

/**
 * Qué es una obra dentro de una carpeta con regla. DETECT: las reglas de detección (clase Libros),
 * guardado solo para anular la clase heredada de una carpeta madre.
 */
enum class WorkUnit { FOLDER, FILE, DETECT }

/** Qué pasa al llegar al final de una obra. */
enum class OnFinish { MARK_FINISHED, RESTART }
