package codelab.lector.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import codelab.lector.library.LibrarySort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Última búsqueda terminada: fecha y libros encontrados (Ajustes › Biblioteca). */
data class LastScan(val at: Long, val books: Int)

/** Orden y tamaño de la cuadrícula. Los filtros no se guardan: al abrir, toda la biblioteca. */
class LibrarySettingsRepository(private val store: DataStore<Preferences>) {

    val sort: Flow<LibrarySort> = store.data.map { p ->
        p[SORT]?.let { runCatching { LibrarySort.valueOf(it) }.getOrNull() } ?: LibrarySort.RECENT
    }.distinctUntilChanged()

    suspend fun setSort(sort: LibrarySort) = store.edit { it[SORT] = sort.name }

    /** Columnas en vertical (1, 2 o 3) o [ListLevel], elegidas con el pellizco. En horizontal, el doble. */
    val gridColumns: Flow<Int> = store.data.map { p -> (p[GRID_COLUMNS] ?: 2).coerceIn(MinColumns, MaxColumns) }.distinctUntilChanged()

    suspend fun setGridColumns(columns: Int) = store.edit { it[GRID_COLUMNS] = columns.coerceIn(MinColumns, MaxColumns) }

    /** Casilla "No disponibles" de ordenar: muestra los libros quitados de la biblioteca. */
    val showUnavailable: Flow<Boolean> = store.data.map { p -> p[SHOW_UNAVAILABLE] ?: false }.distinctUntilChanged()

    suspend fun setShowUnavailable(show: Boolean) = store.edit { it[SHOW_UNAVAILABLE] = show }

    val lastScan: Flow<LastScan?> = store.data.map { p ->
        p[LAST_SCAN_AT]?.let { LastScan(it, p[LAST_SCAN_BOOKS] ?: 0) }
    }.distinctUntilChanged()

    suspend fun setLastScan(scan: LastScan) = store.edit {
        it[LAST_SCAN_AT] = scan.at
        it[LAST_SCAN_BOOKS] = scan.books
    }

    companion object {
        const val MinColumns = 1
        /** Nivel del pellizco tras 3 columnas: lista con portada pequeña. */
        const val ListLevel = 4
        const val MaxColumns = ListLevel
        private val SORT = stringPreferencesKey("library_sort")
        private val GRID_COLUMNS = intPreferencesKey("library_grid_columns")
        private val SHOW_UNAVAILABLE = booleanPreferencesKey("library_show_unavailable")
        private val LAST_SCAN_AT = longPreferencesKey("library_last_scan_at")
        private val LAST_SCAN_BOOKS = intPreferencesKey("library_last_scan_books")
    }
}
