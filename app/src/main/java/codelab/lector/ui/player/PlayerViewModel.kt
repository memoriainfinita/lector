package codelab.lector.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.data.db.Book
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.settings.AppearanceSettings
import codelab.lector.data.settings.PlaybackSettings
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.PlaybackError
import codelab.lector.playback.ActionCall
import codelab.lector.playback.PlayerAction
import codelab.lector.playback.SleepState
import codelab.lector.playback.SoundSettings
import codelab.lector.playback.Volume
import codelab.lector.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Reproductor y minirreproductor: estado del motor y órdenes por [codelab.lector.playback.PlaybackConnection]. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(private val app: AppContainer) : ViewModel() {
    private val playback = app.playback

    val nowPlaying: StateFlow<NowPlaying?> = playback.state
    val error: StateFlow<PlaybackError?> = playback.error

    val settings: StateFlow<PlaybackSettings> =
        app.playbackSettings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackSettings())

    val appearance: StateFlow<AppearanceSettings> =
        app.appearance.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    val globalSound: StateFlow<SoundSettings> =
        playback.globalSound.stateIn(viewModelScope, SharingStarted.Eagerly, SoundSettings())

    val volume: StateFlow<Volume> =
        app.volume.volume.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), app.volume.current())

    /** Marcadores normales del libro que suena (sin las marcas de pausa). */
    val bookmarkCount: StateFlow<Int> = playback.state
        .map { it?.bookId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) flowOf(0)
            else app.database.bookmarks().observeForBook(id).map { list -> list.count { it.kind == BookmarkKind.NORMAL } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Libro sin acceso (tarjeta "No se encuentra el libro"), leído de la base de datos. */
    val inaccessibleBook: StateFlow<Book?> = playback.error
        .map { e -> (e as? PlaybackError.Inaccessible)?.let { app.database.books().get(it.bookId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sleep: StateFlow<SleepState> = app.sleep.state

    /** Minutos, 0 apaga o [codelab.lector.playback.LectorCommands.SLEEP_CHAPTER_END]. */
    fun setSleep(minutes: Int) = playback.setSleep(minutes)

    /** "…" de la pausa diferida: se arranca y se recuerda para la próxima vez. */
    fun setSleepOther(minutes: Int) {
        playback.setSleep(minutes)
        viewModelScope.launch { app.playbackSettings.setSleepLastMinutes(minutes) }
    }

    fun setSleepExtend(enabled: Boolean) {
        viewModelScope.launch { app.playbackSettings.setSleepExtend(enabled) }
    }

    fun act(action: PlayerAction) = playback.act(action)

    fun act(call: ActionCall) = playback.act(call)

    fun jumpTo(bookMs: Long) = playback.jumpTo(bookMs)
    fun jumpToSegment(index: Int) = playback.jumpToSegment(index)

    fun setSpeed(speed: Float) = playback.setSpeed(speed)

    fun setSkipSilence(enabled: Boolean) = playback.setSkipSilence(enabled)

    fun setOwnSound(enabled: Boolean) = playback.setOwnSound(enabled)

    /** Sonido de la hoja: el propio del libro si lo tiene, si no el global. */
    fun setSound(sound: SoundSettings) {
        if (nowPlaying.value?.ownSound == true) playback.setBookSound(sound) else playback.setGlobalSound(sound)
    }

    fun setVolume(level: Int) = app.volume.set(level)

    /** Tema desde el menú del reproductor: alterna oscuro y claro a partir del que se ve. */
    fun toggleTheme(showingDark: Boolean) {
        viewModelScope.launch { app.appearance.setMode(if (showingDark) ThemeMode.LIGHT else ThemeMode.DARK) }
    }

    /** Ruta de la carpeta del libro que suena, para "Ir a la carpeta del libro". */
    fun bookFolder(onPath: (String) -> Unit) {
        val id = nowPlaying.value?.bookId ?: return
        viewModelScope.launch { app.database.books().get(id)?.path?.let(onPath) }
    }

    /** "Volver a buscar": búsqueda completa y, si el libro vuelve a estar, se abre en pausa. */
    fun rescan(bookId: String) {
        viewModelScope.launch {
            app.scanner.start(full = true).join()
            if (app.database.books().get(bookId)?.inaccessible == false) {
                playback.clearError()
                playback.open(bookId, play = false)
            }
        }
    }

    fun coverPath(bookId: String): String? = app.covers.file(bookId).takeIf { it.exists() }?.path

    /**
     * "Quitar": lo quita de la biblioteca como el menú del libro, sin borrar nada. Devuelve el
     * "Deshacer", que lo devuelve a la biblioteca.
     */
    fun removeInaccessible(bookId: String): () -> Unit {
        playback.clearError()
        viewModelScope.launch { app.database.books().setRemoved(bookId, true) }
        return { viewModelScope.launch { app.database.books().setRemoved(bookId, false) } }
    }
}
