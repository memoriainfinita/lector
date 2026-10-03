package codelab.lector.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/** Orden fijo de las pestañas: menú inferior y composición de las pilas. */
val Tabs = listOf(LibraryRoute, ListeningRoute, BookmarksRoute)

@Composable
fun rememberNavigationState(): NavigationState {
    val tab = rememberSerializable(serializer = MutableStateSerializer(NavKeySerializer())) {
        mutableStateOf<NavKey>(LibraryRoute)
    }
    val previousTab = rememberSerializable(serializer = MutableStateSerializer(NavKeySerializer())) {
        mutableStateOf<NavKey>(LibraryRoute)
    }
    val library = rememberNavBackStack(LibraryRoute)
    val listening = rememberNavBackStack(ListeningRoute)
    val bookmarks = rememberNavBackStack(BookmarksRoute)
    val fullScreens = rememberNavBackStack()
    return remember {
        NavigationState(
            tab, previousTab,
            mapOf(LibraryRoute to library, ListeningRoute to listening, BookmarksRoute to bookmarks),
            fullScreens,
        )
    }
}

/**
 * Una pila por pestaña, que se conserva al cambiar de pestaña, y encima las pantallas completas.
 * Se sale de la app siempre desde Biblioteca.
 */
@Stable
class NavigationState internal constructor(
    tab: MutableState<NavKey>,
    previousTab: MutableState<NavKey>,
    private val tabStacks: Map<TabRoute, NavBackStack<NavKey>>,
    val fullScreens: NavBackStack<NavKey>,
) {
    private var tabKey by tab
    private var previousTabKey by previousTab

    var tab: TabRoute
        get() = tabKey as TabRoute
        internal set(value) {
            tabKey = value
        }

    /** Pestaña desde la que se abrió Escuchando; adonde vuelve la flecha del reproductor. */
    var previousTab: TabRoute
        get() = previousTabKey as TabRoute
        internal set(value) {
            previousTabKey = value
        }

    /** Carpeta pedida por "Ir a la carpeta"; la Biblioteca la abre en Carpetas y la consume. */
    var pendingFolder by mutableStateOf<String?>(null)

    val showsFullScreen: Boolean get() = fullScreens.isNotEmpty()

    fun stack(tab: TabRoute): NavBackStack<NavKey> = tabStacks.getValue(tab)

    /** Entradas para NavDisplay: Biblioteca, la pestaña activa si es otra y las pantallas completas. */
    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val byTab = Tabs.associateWith { decorate(stack(it), entryProvider) }
        val overlay = decorate(fullScreens, entryProvider)
        val inUse = if (tab == LibraryRoute) listOf(LibraryRoute) else listOf(LibraryRoute, tab)
        return inUse.flatMap { byTab.getValue(it) } + overlay
    }

    @Composable
    private fun decorate(
        stack: NavBackStack<NavKey>,
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
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
    val tab: TabRoute get() = state.tab

    /** Desde el menú inferior: tocar la pestaña activa la devuelve a su raíz. */
    fun selectTab(tab: TabRoute) {
        state.fullScreens.clear()
        if (tab == state.tab) popToRoot(tab) else switchTo(tab)
    }

    fun open(route: Route) {
        when (route) {
            is TabRoute -> selectTab(route)
            is FullScreenRoute -> state.fullScreens.add(route)
            else -> state.stack(state.tab).add(route)
        }
    }

    /**
     * Punto único de Atrás: pantalla completa, después la pila de la pestaña, después Biblioteca.
     * Aquí se aplicará "Retrasar el botón Atrás".
     */
    fun goBack() {
        val stack = state.stack(state.tab)
        when {
            state.fullScreens.isNotEmpty() -> state.fullScreens.removeAt(state.fullScreens.lastIndex)
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            state.tab != LibraryRoute -> switchTo(LibraryRoute)
        }
    }

    /** Flecha del reproductor: vuelve a la pestaña anterior. */
    fun minimizePlayer() {
        switchTo(state.previousTab.takeIf { it != ListeningRoute } ?: LibraryRoute)
    }

    /** "Ir a la carpeta": Biblioteca › Carpetas, en esa carpeta. */
    fun showFolder(path: String) {
        state.fullScreens.clear()
        popToRoot(LibraryRoute)
        state.pendingFolder = path
        switchTo(LibraryRoute)
    }

    /** "Ver todos los marcadores". */
    fun showAllBookmarks() {
        state.fullScreens.clear()
        popToRoot(BookmarksRoute)
        switchTo(BookmarksRoute)
    }

    private fun switchTo(tab: TabRoute) {
        if (tab == state.tab) return
        state.previousTab = state.tab
        state.tab = tab
    }

    private fun popToRoot(tab: TabRoute) {
        val stack = state.stack(tab)
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}
