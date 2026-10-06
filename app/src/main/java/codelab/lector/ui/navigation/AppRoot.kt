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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import codelab.lector.ui.library.MergeBooksScreen
import codelab.lector.ui.library.MergeBooksViewModel
import codelab.lector.ui.library.SplitBookScreen
import codelab.lector.ui.library.SplitBookViewModel
import codelab.lector.ui.library.StorageRoots
import codelab.lector.ui.library.LibraryViewModel
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.PlayerAction
import codelab.lector.ui.formatDuration
import codelab.lector.ui.player.CoverViewer
import codelab.lector.ui.player.MiniPlayer
import codelab.lector.ui.player.MiniPlayerHeight
import codelab.lector.ui.components.LocalBottomInset
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import codelab.lector.ui.player.PlayerSlide
import codelab.lector.ui.player.rememberPlayerSlide
import codelab.lector.ui.player.slideUpFromMini
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import codelab.lector.ui.player.PlayerScreen
import codelab.lector.ui.player.PlayerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.bookmarks.BookmarkSheetsHost
import codelab.lector.ui.bookmarks.BookmarksScreen
import codelab.lector.ui.bookmarks.BookmarksSearchScreen
import codelab.lector.ui.bookmarks.TagsSettingsScreen
import codelab.lector.ui.bookmarks.BookmarksViewModel
import codelab.lector.ui.bookmarks.LocalBookmarkSheets
import codelab.lector.ui.bookmarks.rememberBookmarkSheetsState
import codelab.lector.ui.components.UndoBar
import codelab.lector.ui.components.rememberUndoState
import codelab.lector.ui.screens.PlaceholderLink
import codelab.lector.ui.screens.PlaceholderScreen
import codelab.lector.ui.onboarding.OnboardingFoldersScreen
import codelab.lector.ui.onboarding.OnboardingFoldersViewModel
import codelab.lector.ui.onboarding.OnboardingPermissionScreen
import codelab.lector.ui.settings.AppearanceScreen
import codelab.lector.ui.settings.ButtonsSettingsScreen
import codelab.lector.ui.settings.FolderPickerScreen
import codelab.lector.ui.settings.FolderPickerViewModel
import codelab.lector.ui.settings.LibrarySettingsScreen
import codelab.lector.ui.settings.LibrarySettingsViewModel
import codelab.lector.ui.settings.RemoteButtonsScreen
import codelab.lector.ui.settings.SettingsScreen
import codelab.lector.ui.settings.SettingsViewModel
import codelab.lector.ui.settings.SleepSettingsScreen
import codelab.lector.ui.settings.SoundSettingsScreen
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.flow.Flow

private enum class StartMode { LOADING, ONBOARDING_PERMISSION, ONBOARDING_FOLDERS, MAIN }

/**
 * Raíz de la app: primer arranque (sin permiso o sin carpetas) o las pestañas.
 * [openPlayer] llega desde la notificación y el widget: abre Escuchando. [onReady]: la primera
 * pantalla ya tiene su contenido (quita la pantalla de inicio).
 */
@Composable
fun AppRoot(openPlayer: Flow<Unit>, onReady: () -> Unit = {}) {
    val context = LocalContext.current
    val app = context.container
    var mode by rememberSaveable { mutableStateOf(StartMode.LOADING) }
    val scope = rememberCoroutineScope()
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
    // Primer arranque: sus pantallas no esperan datos.
    LaunchedEffect(mode) { if (mode == StartMode.ONBOARDING_PERMISSION || mode == StartMode.ONBOARDING_FOLDERS) onReady() }
    val undo = rememberUndoState()
    val bookmarkSheets = rememberBookmarkSheetsState()
    CompositionLocalProvider(LocalUndoState provides undo, LocalBookmarkSheets provides bookmarkSheets) {
        Box(Modifier.fillMaxSize().background(LectorTheme.colors.background).systemBarsPadding()) {
            when (mode) {
                StartMode.LOADING -> Unit
                StartMode.ONBOARDING_PERMISSION, StartMode.ONBOARDING_FOLDERS -> key(mode) {
                    Onboarding(
                        withPermission = mode == StartMode.ONBOARDING_PERMISSION,
                        // Permiso retirado con carpetas ya guardadas: directo a la Biblioteca.
                        onGranted = {
                            scope.launch {
                                mode = if (app.database.folders().folders().isEmpty()) StartMode.ONBOARDING_FOLDERS else StartMode.MAIN
                            }
                        },
                        onFinished = { mode = StartMode.MAIN },
                    )
                }
                StartMode.MAIN -> MainScreen(openPlayer, onReady)
            }
        }
    }
}

/** Primer arranque: permiso, sustituido por Carpetas al concederlo. Atrás en cualquiera de las dos sale. */
@Composable
private fun Onboarding(withPermission: Boolean, onGranted: () -> Unit, onFinished: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val stack = rememberNavBackStack(if (withPermission) OnboardingPermissionRoute else OnboardingFoldersRoute)
    NavDisplay(
        backStack = stack,
        onBack = { stack.removeAt(stack.lastIndex) },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<OnboardingPermissionRoute> { OnboardingPermissionScreen(onGranted) }
            entry<OnboardingFoldersRoute> {
                OnboardingFoldersScreen(
                    viewModel { OnboardingFoldersViewModel(app, storageRootsOf(context)) },
                    onPickFolder = { stack.add(FolderPickerRoute) },
                    onStart = onFinished,
                )
            }
            entry<FolderPickerRoute> { FolderPicker(onClose = { stack.removeAt(stack.lastIndex) }) }
        },
    )
}

@Composable
private fun MainScreen(openPlayer: Flow<Unit>, onReady: () -> Unit) {
    val app = LocalContext.current.container
    val state = rememberNavigationState()
    val navigator = remember(state) { Navigator(state) }
    // Movimiento de Escuchando, compartido con el minirreproductor. Al terminar de bajar, sale de la pila.
    val slide = rememberPlayerSlide(onClosed = { if (state.top == ListeningRoute) navigator.goBack() })
    navigator.beforeOpenPlayer = slide::prepareOpen
    // Arriba otra pantalla (Escuchando desde la notificación): no hay que esperar a la Biblioteca.
    LaunchedEffect(state.top) { if (state.top != LibraryRoute) onReady() }
    // Del ámbito de la Activity: minirreproductor y aviso de salto, fuera de las pantallas.
    val player: PlayerViewModel = viewModel { PlayerViewModel(app) }
    val nowPlaying by player.nowPlaying.collectAsStateWithLifecycle()
    val settings by player.settings.collectAsStateWithLifecycle()
    val appearance by player.appearance.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.playback.connect() }
    // Búsqueda rápida de cambios al abrir la app, discreta; no se repite al girar la pantalla.
    var scanned by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!scanned) {
            scanned = true
            app.scanner.start(quiet = true)
        }
    }
    // Ajustes › Reproducir al abrir la app: una vez por apertura, cuando el último libro está cargado.
    var autoPlayChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoPlayChecked) {
            autoPlayChecked = true
            if (app.playbackSettings.current().playOnOpen) {
                val loaded = withTimeoutOrNull(5_000) { app.playback.state.filterNotNull().first() }
                if (loaded != null && !loaded.playWhenReady) player.act(PlayerAction.PLAY_PAUSE)
            }
        }
    }
    LaunchedEffect(openPlayer) { openPlayer.collect { navigator.openPlayer() } }
    JumpUndo(nowPlaying, onUndo = { player.act(PlayerAction.UNDO_JUMP) })

    val playing = nowPlaying
    // Sesión de escucha: empieza cuando algo suena o se abre Escuchando. Antes, al abrir la app,
    // el libro cargado en pausa solo se ve en "Seguir escuchando"; después, en el minirreproductor.
    var session by rememberSaveable { mutableStateOf(false) }
    // Con Escuchando arriba, cuando ya ha subido: la Biblioteca de debajo no cambia mientras se ve.
    var playerShown by remember { mutableStateOf(false) }
    LaunchedEffect(state.top) { if (state.top != ListeningRoute) playerShown = false }
    val starts = playerShown || (playing?.playWhenReady == true && state.top != ListeningRoute)
    LaunchedEffect(starts) { if (starts) session = true }
    // Abajo del todo, con un libro cargado; no en Escuchando ni en las pantallas completas.
    val showMini = session && playing != null && state.top != ListeningRoute && !state.showsFullScreen
    // El minirreproductor va superpuesto: el área de las pantallas no cambia de tamaño al
    // aparecer, así abrir o cerrar pantallas no desplaza nada. Las pantallas reservan su hueco,
    // también debajo de Escuchando: al bajarla, la pantalla que se ve ya lo tiene.
    val miniSpace = session && playing != null && !state.showsFullScreen
    val sceneStrategies = remember { listOf(ListeningSceneStrategy()) }
    val mini: @Composable () -> Unit = {
        playing?.let { np ->
            MiniPlayer(
                np,
                left = settings.playerButtons[1],
                right = settings.playerButtons[2],
                onAct = player::act,
                onCall = player::act,
                onOpen = navigator::openPlayer,
                showCover = appearance.showCovers,
            )
        }
    }
    // El de debajo de Escuchando ya está en pantalla: hasta entonces, el de arriba no se oculta.
    var underlayShown by remember { mutableStateOf(false) }
    // Al cerrar Escuchando, el minirreproductor ya se veía debajo de ella: entra sin fundido.
    var previousTop by remember { mutableStateOf<NavKey?>(null) }
    val fromPlayer = previousTop == ListeningRoute
    SideEffect { previousTop = state.top }
    Box(Modifier.fillMaxSize().onSizeChanged { slide.height = it.height.toFloat() }) {
        CompositionLocalProvider(
            LocalBottomInset provides if (miniSpace) MiniPlayerHeight else 0.dp,
            LocalListeningUnderlay provides {
                if (session) {
                    mini()
                    DisposableEffect(Unit) {
                        underlayShown = true
                        onDispose { underlayShown = false }
                    }
                }
            },
        ) {
            NavDisplay(
                entries = state.toDecoratedEntries(routeEntries(state, navigator, slide, session, onReady, onPlayerShown = { playerShown = true })),
                sceneStrategies = sceneStrategies,
                onBack = navigator::goBack,
            )
        }
        // Mientras se arrastra hacia arriba sigue aquí, invisible, para no cortar el gesto: el que
        // se ve es el de debajo de Escuchando.
        // Al abrir Escuchando, se queda hasta que está el de debajo y se va sin fundido.
        val toPlayer = state.top == ListeningRoute
        androidx.compose.animation.AnimatedVisibility(
            showMini || slide.fromMini || (toPlayer && session && !underlayShown),
            Modifier.align(Alignment.BottomCenter),
            enter = if (fromPlayer) EnterTransition.None else fadeIn(),
            exit = if (toPlayer) ExitTransition.None else fadeOut(),
        ) {
            Box(
                Modifier
                    .alpha(if (state.top == ListeningRoute && underlayShown) 0f else 1f)
                    .slideUpFromMini(slide, navigator::openPlayer),
            ) { mini() }
        }
        BookmarkSheetsHost(
            LocalBookmarkSheets.current,
            onSeeAll = navigator::showAllBookmarks,
            onManageTags = { navigator.open(SettingsTagsRoute) },
        )
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
        val left = until?.minus(System.currentTimeMillis())
        if (left != null && text != null && left > 0) undo.show(text, onUndo, left)
    }
}

/** Una entrada por ruta. Las que aún no están hechas son pantallas vacías con sus enlaces. */
@Composable
private fun routeEntries(
    state: NavigationState,
    navigator: Navigator,
    slide: PlayerSlide,
    session: Boolean,
    onReady: () -> Unit,
    onPlayerShown: () -> Unit,
): (NavKey) -> NavEntry<NavKey> {
    val app = LocalContext.current.container
    val back = navigator::goBack
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
                onLoaded = onReady,
                storage = remember { storageRootsOf(context) },
            )
        }
        entry<ListeningRoute>(metadata = ListeningScene.metadata) {
            PlayerScreen(
                viewModel = viewModel { PlayerViewModel(app) },
                slide = slide,
                onOpenSettings = { navigator.open(SettingsRoute) },
                onOpenSleepSettings = { navigator.open(SettingsSleepRoute) },
                onOpenCover = { navigator.open(CoverViewerRoute(it)) },
                onShowFolder = navigator::showFolder,
                onShown = onPlayerShown,
            )
        }
        entry<BookmarksRoute> {
            BookmarksScreen(
                viewModel { BookmarksViewModel(app) },
                onBack = back,
                onSearch = { navigator.open(BookmarksSearchRoute) },
                onManageTags = { navigator.open(SettingsTagsRoute) },
            )
        }
        entry<BookmarksSearchRoute> { BookmarksSearchScreen(viewModel { BookmarksViewModel(app) }, onBack = back) }
        entry<SettingsRoute> {
            SettingsScreen(
                viewModel = viewModel { SettingsViewModel(app) },
                onBack = back,
                onSound = { navigator.open(SettingsSoundRoute) },
                onAppearance = { navigator.open(SettingsAppearanceRoute) },
                onLibrary = { navigator.open(SettingsLibraryRoute) },
                onButtons = { navigator.open(SettingsButtonsRoute) },
                onRemote = { navigator.open(SettingsRemoteRoute) },
                onSleep = { navigator.open(SettingsSleepRoute) },
                onTags = { navigator.open(SettingsTagsRoute) },
            )
        }
        entry<SettingsLibraryRoute> {
            val context = LocalContext.current
            LibrarySettingsScreen(
                viewModel = viewModel { LibrarySettingsViewModel(app, storageRootsOf(context)) },
                onBack = back,
                onAddFolder = { navigator.open(FolderPickerRoute) },
            )
        }
        entry<SettingsSleepRoute> { SleepSettingsScreen(viewModel { SettingsViewModel(app) }, onBack = back) }
        entry<SettingsButtonsRoute> { ButtonsSettingsScreen(viewModel { SettingsViewModel(app) }, onBack = back) }
        entry<SettingsRemoteRoute> { RemoteButtonsScreen(viewModel { SettingsViewModel(app) }, onBack = back) }
        entry<SettingsSoundRoute> { SoundSettingsScreen(viewModel { SettingsViewModel(app) }, onBack = back) }
        entry<SettingsTagsRoute> { TagsSettingsScreen(onBack = back) }
        entry<SettingsAppearanceRoute> { AppearanceScreen(viewModel { SettingsViewModel(app) }, onBack = back) }
        entry<SettingsDataRoute> { PlaceholderScreen(stringResource(R.string.settings_data), onBack = back) }
        entry<FolderPickerRoute> { FolderPicker(onClose = back) }
        entry<MergeBooksRoute> { key ->
            val context = LocalContext.current
            MergeBooksScreen(viewModel { MergeBooksViewModel(app, key.bookId, storageRoots(context).map { it.path }) }, onClose = back)
        }
        entry<SplitBookRoute> { key -> SplitBookScreen(viewModel { SplitBookViewModel(app, key.bookId) }, onClose = back) }
        entry<CoverViewerRoute> { key -> CoverViewer(key.bookId, onClose = back) }
    }
}

/** Almacenamiento principal y SD, para rutas y explorador. */
private fun storageRootsOf(context: android.content.Context) =
    StorageRoots(Environment.getExternalStorageDirectory().path, storageRoots(context).map { it.path })

/** Explorador de carpetas, desde Ajustes, la Biblioteca vacía o el primer arranque. */
@Composable
private fun FolderPicker(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    FolderPickerScreen(viewModel { FolderPickerViewModel(app, storageRootsOf(context)) }, onClose)
}
