package codelab.lector.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import codelab.lector.library.storageRoots
import android.os.Environment
import codelab.lector.ui.library.LibraryScreen
import codelab.lector.ui.library.StorageRoots
import codelab.lector.ui.library.LibraryViewModel
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.PlayerAction
import codelab.lector.ui.formatDuration
import codelab.lector.ui.player.CoverViewer
import codelab.lector.ui.player.MiniPlayer
import codelab.lector.ui.player.MiniPlayerHeight
import codelab.lector.ui.components.LocalBottomInset
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import codelab.lector.ui.player.PlayerScreen
import codelab.lector.ui.player.PlayerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.UndoBar
import codelab.lector.ui.components.rememberUndoState
import codelab.lector.ui.screens.PlaceholderLink
import codelab.lector.ui.screens.PlaceholderScreen
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.flow.Flow

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
                StartMode.MAIN -> MainScreen(openPlayer)
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
private fun MainScreen(openPlayer: Flow<Unit>) {
    val app = LocalContext.current.container
    val state = rememberNavigationState()
    val navigator = remember(state) { Navigator(state) }
    // Del ámbito de la Activity: minirreproductor y aviso de salto, fuera de las pantallas.
    val player: PlayerViewModel = viewModel { PlayerViewModel(app) }
    val nowPlaying by player.nowPlaying.collectAsStateWithLifecycle()
    val settings by player.settings.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.playback.connect() }
    // Búsqueda rápida de cambios al abrir la app; no se repite al girar la pantalla.
    var scanned by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!scanned) {
            scanned = true
            app.scanner.start()
        }
    }
    LaunchedEffect(openPlayer) { openPlayer.collect { navigator.openPlayer() } }
    JumpUndo(nowPlaying, onUndo = { player.act(PlayerAction.UNDO_JUMP) })

    val playing = nowPlaying
    // Sesión de escucha: empieza cuando algo suena o se abre Escuchando. Antes, al abrir la app,
    // el libro cargado en pausa solo se ve en "Seguir escuchando"; después, en el minirreproductor.
    var session by rememberSaveable { mutableStateOf(false) }
    val starts = playing?.playWhenReady == true || state.top == ListeningRoute
    LaunchedEffect(starts) { if (starts) session = true }
    // Abajo del todo, con un libro cargado; no en Escuchando ni en las pantallas completas.
    val showMini = session && playing != null && state.top != ListeningRoute && !state.showsFullScreen
    // El minirreproductor va superpuesto: el área de las pantallas no cambia de tamaño al
    // aparecer, así abrir o cerrar pantallas no desplaza nada. Las pantallas reservan su hueco.
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomInset provides if (showMini) MiniPlayerHeight else 0.dp) {
            NavDisplay(
                entries = state.toDecoratedEntries(routeEntries(state, navigator, session)),
                onBack = navigator::goBack,
            )
        }
        androidx.compose.animation.AnimatedVisibility(showMini, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            playing?.let { np ->
                MiniPlayer(
                    np,
                    skipBack = settings.appSkipBackSec,
                    skipForward = settings.appSkipForwardSec,
                    onAct = player::act,
                    onOpen = navigator::openPlayer,
                )
            }
        }
        UndoBar(
            LocalUndoState.current,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp + if (showMini) MiniPlayerHeight else 0.dp),
        )
    }
}

/** Tras un salto grande, "Saltado desde…" con Deshacer mientras el motor lo permite. */
@Composable
private fun JumpUndo(playing: NowPlaying?, onUndo: () -> Unit) {
    val undo = LocalUndoState.current
    val until = playing?.undoUntil
    val from = playing?.undoFromMs
    val text = from?.let { stringResource(R.string.jumped_from, formatDuration(it)) }
    LaunchedEffect(until) {
        if (until != null && text != null && until > System.currentTimeMillis()) undo.show(text, onUndo)
    }
}

/** Una entrada por ruta. Las que aún no están hechas son pantallas vacías con sus enlaces. */
@Composable
private fun routeEntries(
    state: NavigationState,
    navigator: Navigator,
    session: Boolean,
): (NavKey) -> NavEntry<NavKey> {
    val app = LocalContext.current.container
    val back = navigator::goBack
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
            val context = LocalContext.current
            LibraryScreen(
                viewModel = viewModel { LibraryViewModel(app, storageRoots(context).map { it.path }) },
                pendingFolder = state.pendingFolder,
                onBookmarks = navigator::showAllBookmarks,
                onSettings = { navigator.open(SettingsRoute) },
                onOpenPlayer = navigator::openPlayer,
                onOpenFolder = navigator::showFolder,
                onAddFolder = { navigator.open(FolderPickerRoute) },
                onOpenCover = { navigator.open(CoverViewerRoute(it)) },
                onSplit = { navigator.open(SplitBookRoute(it)) },
                onMerge = { navigator.open(MergeBooksRoute(it)) },
                showContinue = !session,
                onFolderShown = { state.pendingFolder = null },
                storage = remember { StorageRoots(Environment.getExternalStorageDirectory().path, storageRoots(context).map { it.path }) },
            )
        }
        entry<ListeningRoute> {
            PlayerScreen(
                viewModel = viewModel { PlayerViewModel(app) },
                onMinimize = navigator::goBack,
                onOpenSettings = { navigator.open(SettingsRoute) },
                onOpenCover = { navigator.open(CoverViewerRoute(it)) },
                onShowFolder = navigator::showFolder,
            )
        }
        entry<BookmarksRoute> {
            PlaceholderScreen(stringResource(R.string.tab_bookmarks), onBack = back, links = listOf(link(R.string.search_bookmarks, BookmarksSearchRoute)))
        }
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
        entry<CoverViewerRoute> { key -> CoverViewer(key.bookId, onClose = back) }
    }
}
