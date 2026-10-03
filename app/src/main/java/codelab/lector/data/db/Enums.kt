package codelab.lector.data.db

/** Formato de la obra. Lectura se añadirá más adelante. */
enum class BookType { AUDIO }

enum class CoverSource { NONE, EMBEDDED, FOLDER }

enum class BookmarkKind { NORMAL, PAUSE }

enum class CorrectionType { SPLIT, MERGE }

/** Qué es una obra dentro de una carpeta con regla. */
enum class WorkUnit { FOLDER, FILE }

/** Qué pasa al llegar al final de una obra. */
enum class OnFinish { MARK_FINISHED, RESTART }
