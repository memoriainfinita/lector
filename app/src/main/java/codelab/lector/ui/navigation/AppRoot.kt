package codelab.lector.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import codelab.lector.R
import codelab.lector.container
import codelab.lector.library.hasStorageAccess
import codelab.lector.playback.NowPlaying
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.UndoBar
import codelab.lector.ui.components.rememberUndoState
import codelab.lector.ui.screens.PlaceholderLink
import codelab.lector.ui.screens.PlaceholderScreen
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private enum class StartMode { LOADING, ONBOARDING_PERMISSION, ONBOARDING_FOLDERS, MAIN }

/**
 * Raíz de la app: primer arranque (sin permiso o sin carpetas) o las pestañas.
 * [openPlayer] llega desde la notificación y el widget: abre Escuchando.
 */
@Composable
fun AppRoot(openPlayer: Flow<Unit>) {
    val context = LocalContext.current
    val app = context.container
    var mode by rememberSaveable { mutableStateOf(StartMode.LOADING) }
    LaunchedEffect(Unit) {
        if (mode == StartMode.LOADING) {
            val access = hasStorageAccess(context)
            mode = when {
                !access -> StartMode.ONBOARDING_PERMISSION
                app.database.folders().folders().isEmpty() -> StartMode.ONBOARDING_FOLDERS
                else -> StartMode.MAIN
            }
        }
    }
    val undo = rememberUndoState()
    CompositionLocalProvider(LocalUndoState provides undo) {
        Box(Modifier.fillMaxSize().background(LectorTheme.colors.background).systemBarsPadding()) {
            when (mode) {
                StartMode.LOADING -> Unit
                StartMode.ONBOARDING_PERMISSION, StartMode.ONBOARDING_FOLDERS ->
                    Onboarding(withPermission = mode == StartMode.ONBOARDING_PERMISSION) { mode = StartMode.MAIN }
                StartMode.MAIN -> MainTabs(openPlayer)
            }
        }
    }
}

@Composable
private fun Onboarding(withPermission: Boolean, onFinished: () -> Unit) {
    val stack = rememberNavBackStack(if (withPermission) OnboardingPermissionRoute else OnboardingFoldersRoute)
    NavDisplay(
        backStack = stack,
        onBack = { stack.removeAt(stack.lastIndex) },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<OnboardingPermissionRoute> {
                PlaceholderScreen(
                    stringResource(R.string.onboarding_welcome),
                    links = listOf(PlaceholderLink(stringResource(R.string.onboarding_folders)) { stack.add(OnboardingFoldersRoute) }),
                )
            }
            entry<OnboardingFoldersRoute> {
                PlaceholderScreen(
                    stringResource(R.string.onboarding_folders),
                    links = listOf(
                        PlaceholderLink(stringResource(R.string.folder_picker)) { stack.add(FolderPickerRoute) },
                        PlaceholderLink(stringResource(R.string.onboarding_start), onFinished),
                    ),
                )
            }
            entry<FolderPickerRoute> {
                PlaceholderScreen(stringResource(R.string.folder_picker), onBack = { stack.removeAt(stack.lastIndex) })
            }
        },
    )
}

@Composable
private fun MainTabs(openPlayer: Flow<Unit>) {
    val app = LocalContext.current.container
    val state = rememberNavigationState()
    val navigator = remember(state) { Navigator(state) }
    val nowPlaying by app.playback.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.playback.connect() }
    LaunchedEffect(openPlayer) { openPlayer.collect { navigator.selectTab(ListeningRoute) } }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showBar = !state.showsFullScreen && !(landscape && state.tab == ListeningRoute)
    val playing = nowPlaying
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            NavDisplay(
                entries = state.toDecoratedEntries(routeEntries(state, navigator, playing)),
                onBack = navigator::goBack,
            )
            UndoBar(LocalUndoState.current, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
        if (showBar && playing != null && state.tab != ListeningRoute) {
            MiniPlayer(playing, onOpen = { navigator.selectTab(ListeningRoute) })
        }
        if (showBar) BottomBar(state.tab, listeningEnabled = playing != null, onSelect = navigator::selectTab)
    }
}

/** Una entrada por ruta. De momento, pantallas vacías con sus enlaces. */
@Composable
private fun routeEntries(
    state: NavigationState,
    navigator: Navigator,
    playing: NowPlaying?,
): (NavKey) -> NavEntry<NavKey> {
    val app = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val back = navigator::goBack
    val bookId = playing?.bookId
    val settings = listOf(
        R.string.settings_sleep to SettingsSleepRoute,
        R.string.settings_buttons to SettingsButtonsRoute,
        R.string.settings_sound to SettingsSoundRoute,
        R.string.settings_library to SettingsLibraryRoute,
        R.string.settings_tags to SettingsTagsRoute,
        R.string.settings_appearance to SettingsAppearanceRoute,
        R.string.settings_data to SettingsDataRoute,
    )
    @Composable
    fun link(text: Int, route: Route) = PlaceholderLink(stringResource(text)) { navigator.open(route) }

    return entryProvider<NavKey> {
        entry<LibraryRoute> {
            PlaceholderScreen(
                stringResource(R.string.tab_library),
                subtitle = state.pendingFolder?.let { stringResource(R.string.pending_folder, it) },
                links = listOfNotNull(
                    link(R.string.search_library, LibrarySearchRoute),
                    link(R.string.folder_picker, FolderPickerRoute),
                    bookId?.let { link(R.string.merge_books, MergeBooksRoute(it)) },
                    bookId?.let { link(R.string.split_book, SplitBookRoute(it)) },
                ),
            )
        }
        entry<ListeningRoute> {
            PlaceholderScreen(
                stringResource(R.string.tab_listening),
                subtitle = playing?.title,
                links = listOfNotNull(
                    PlaceholderLink(stringResource(R.string.minimize_player), navigator::minimizePlayer),
                    link(R.string.settings, SettingsRoute),
                    bookId?.let { link(R.string.cover_viewer, CoverViewerRoute(it)) },
                    bookId?.let { id ->
                        PlaceholderLink(stringResource(R.string.go_to_book_folder)) {
                            scope.launch { app.database.books().get(id)?.path?.let(navigator::showFolder) }
                        }
                    },
                    PlaceholderLink(stringResource(R.string.see_all_bookmarks), navigator::showAllBookmarks),
                ),
            )
        }
        entry<BookmarksRoute> {
            PlaceholderScreen(stringResource(R.string.tab_bookmarks), links = listOf(link(R.string.search_bookmarks, BookmarksSearchRoute)))
        }
        entry<LibrarySearchRoute> { PlaceholderScreen(stringResource(R.string.search_library), onBack = back) }
        entry<BookmarksSearchRoute> { PlaceholderScreen(stringResource(R.string.search_bookmarks), onBack = back) }
        entry<SettingsRoute> {
            PlaceholderScreen(stringResource(R.string.settings), onBack = back, links = settings.map { (text, route) -> link(text, route) })
        }
        entry<SettingsLibraryRoute> {
            PlaceholderScreen(stringResource(R.string.settings_library), onBack = back, links = listOf(link(R.string.folder_picker, FolderPickerRoute)))
        }
        entry<SettingsSleepRoute> { PlaceholderScreen(stringResource(R.string.settings_sleep), onBack = back) }
        entry<SettingsButtonsRoute> { PlaceholderScreen(stringResource(R.string.settings_buttons), onBack = back) }
        entry<SettingsSoundRoute> { PlaceholderScreen(stringResource(R.string.settings_sound), onBack = back) }
        entry<SettingsTagsRoute> { PlaceholderScreen(stringResource(R.string.settings_tags), onBack = back) }
        entry<SettingsAppearanceRoute> { PlaceholderScreen(stringResource(R.string.settings_appearance), onBack = back) }
        entry<SettingsDataRoute> { PlaceholderScreen(stringResource(R.string.settings_data), onBack = back) }
        entry<FolderPickerRoute> { PlaceholderScreen(stringResource(R.string.folder_picker), onBack = back) }
        entry<MergeBooksRoute> { PlaceholderScreen(stringResource(R.string.merge_books), onBack = back) }
        entry<SplitBookRoute> { PlaceholderScreen(stringResource(R.string.split_book), onBack = back) }
        entry<CoverViewerRoute> {
            PlaceholderScreen(stringResource(R.string.cover_viewer), onBack = back, backIcon = R.drawable.ic_close)
        }
    }
}

/** Hueco del minirreproductor: de momento, título del libro; tocarlo abre Escuchando. */
@Composable
private fun MiniPlayer(playing: NowPlaying, onOpen: () -> Unit) {
    val c = LectorTheme.colors
    Row(
        Modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.open_player), onClick = onOpen)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(playing.title, style = LectorTheme.type.body, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Menú inferior: solo iconos, barra de 48. Escuchando inactivo sin libro cargado. */
@Composable
private fun BottomBar(tab: TabRoute, listeningEnabled: Boolean, onSelect: (TabRoute) -> Unit) {
    val c = LectorTheme.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            Tabs.forEach { route ->
                val enabled = route != ListeningRoute || listeningEnabled
                val (icon, label) = when (route) {
                    LibraryRoute -> R.drawable.ic_nav_library to R.string.tab_library
                    ListeningRoute -> R.drawable.ic_nav_listening to
                        if (enabled) R.string.tab_listening else R.string.listening_unavailable
                    BookmarksRoute -> R.drawable.ic_bookmark to R.string.tab_bookmarks
                }
                val tint = when {
                    !enabled -> c.inactive
                    route == tab -> c.accent
                    else -> c.textSecondary
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = enabled, role = Role.Tab) { onSelect(route) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(icon), stringResource(label), Modifier.size(20.dp), tint = tint)
                }
            }
        }
    }
}
