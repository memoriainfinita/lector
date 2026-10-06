package codelab.lector.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
fun rememberNavigationState(): NavigationState {
    val stack = rememberNavBackStack(LibraryRoute)
    return remember { NavigationState(stack) }
}

/**
 * Una sola pila con Biblioteca en la raíz (design.md › Navegación). Escuchando, Marcadores y las
 * pantallas completas se abren encima; Atrás en Biblioteca sale de la app.
 */
@Stable
class NavigationState internal constructor(val stack: NavBackStack<NavKey>) {

    /** Carpeta pedida por "Ir a la carpeta"; la Biblioteca la abre en Carpetas y la consume. */
    var pendingFolder by mutableStateOf<String?>(null)

    val top: NavKey get() = stack.last()

    val showsFullScreen: Boolean get() = top is FullScreenRoute

    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
        backStack = stack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
}

/** Órdenes de navegación de las pantallas. Todo Atrás pasa por [goBack]. */
class Navigator(private val state: NavigationState) {

    /** Escuchando va a aparecer arriba: empieza abajo para subir. */
    var beforeOpenPlayer: () -> Unit = {}

    /** Abre encima. Si ya está en la pila, la sube arriba en vez de duplicarla. */
    fun open(route: Route) {
        val stack = state.stack
        if (route == LibraryRoute) return popToRoot()
        stack.remove(route)
        stack.add(route)
    }

    fun openPlayer() {
        if (state.top != ListeningRoute) beforeOpenPlayer()
        open(ListeningRoute)
    }

    /**
     * Punto único de Atrás. En la raíz no hace nada: NavDisplay deja que el sistema salga de la app.
     * Aquí se aplicará "Retrasar el botón Atrás".
     */
    fun goBack() {
        val stack = state.stack
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** "Ir a la carpeta": Biblioteca › Carpetas, en esa carpeta. */
    fun showFolder(path: String) {
        popToRoot()
        state.pendingFolder = path
    }

    /** "Ver todos los marcadores". */
    fun showAllBookmarks() = open(BookmarksRoute)

    private fun popToRoot() {
        val stack = state.stack
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}
