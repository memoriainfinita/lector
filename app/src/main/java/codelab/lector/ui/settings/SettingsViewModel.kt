package codelab.lector.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.data.settings.AppearanceSettings
import codelab.lector.data.settings.PlaybackSettings
import codelab.lector.library.ScanState
import codelab.lector.playback.SoundSettings
import codelab.lector.playback.Volume
import codelab.lector.ui.theme.AccentChoice
import codelab.lector.ui.theme.ThemeMode
import codelab.lector.ui.theme.ThemeSchedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ajustes y sus subpáginas (Apariencia, Ecualizador y volumen): leen y guardan en DataStore. */
class SettingsViewModel(private val app: AppContainer) : ViewModel() {

    val playback: StateFlow<PlaybackSettings> =
        app.playbackSettings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackSettings())

    val appearance: StateFlow<AppearanceSettings> =
        app.appearance.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    val scan: StateFlow<ScanState> = app.scanner.state

    val globalSound: StateFlow<SoundSettings> =
        app.playback.globalSound.stateIn(viewModelScope, SharingStarted.Eagerly, SoundSettings())

    val volume: StateFlow<Volume> =
        app.volume.volume.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), app.volume.current())

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // Reproducción
    fun setAutoNextBook(enabled: Boolean) = save { app.playbackSettings.setAutoNextBook(enabled) }
    fun setRewindOnResume(seconds: Int) = save { app.playbackSettings.setRewindOnResumeMs(seconds * 1_000) }
    fun setPlayOnOpen(enabled: Boolean) = save { app.playbackSettings.setPlayOnOpen(enabled) }
    fun setNewBookSpeed(speed: Float) = save { app.playbackSettings.setNewBookSpeed(speed) }
    fun setCoverOutside(enabled: Boolean) = save { app.playbackSettings.setCoverOutside(enabled) }

    // Sonido global
    fun setGlobalSound(sound: SoundSettings) = app.playback.setGlobalSound(sound)
    fun setVolume(level: Int) = app.volume.set(level)

    // Biblioteca
    /** "Volver a buscar libros": búsqueda completa, visible. */
    fun rescan() {
        app.scanner.start(full = true)
    }
    fun setShowCovers(show: Boolean) = save { app.appearance.setShowCovers(show) }

    // Apariencia
    fun setThemeMode(mode: ThemeMode) = save { app.appearance.setMode(mode) }
    fun setSchedule(schedule: ThemeSchedule) = save { app.appearance.setSchedule(schedule) }
    fun setAccent(accent: AccentChoice) = save { app.appearance.setAccent(accent) }
}
