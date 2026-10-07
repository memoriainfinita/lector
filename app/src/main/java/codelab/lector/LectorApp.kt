package codelab.lector

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import codelab.lector.bookmarks.BookmarkStore
import codelab.lector.data.backup.BackupExporter
import codelab.lector.data.backup.BackupImporter
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.settings.AppearanceRepository
import codelab.lector.data.settings.LastScan
import codelab.lector.data.settings.LibrarySettingsRepository
import codelab.lector.data.settings.PlaybackSettingsRepository
import codelab.lector.library.CoverStore
import codelab.lector.library.LibraryCorrections
import codelab.lector.library.LibraryScanner
import codelab.lector.library.Media3MetadataReader
import codelab.lector.playback.PlaybackConnection
import codelab.lector.playback.PlaybackStateHolder
import codelab.lector.playback.RemoteKeyMonitor
import codelab.lector.playback.SleepStateHolder
import codelab.lector.playback.VolumeControl
import codelab.lector.widget.WidgetUpdates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.io.File

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Contenedor de dependencias, sin framework de inyección. */
@OptIn(UnstableApi::class)
class AppContainer(context: Context) {
    /** Trabajo que sobrevive a las pantallas (escaneo). */
    val appScope = CoroutineScope(SupervisorJob())
    val database: LectorDatabase by lazy { LectorDatabase.create(context) }
    val appearance = AppearanceRepository(context.settingsStore)
    val covers = CoverStore(File(context.filesDir, "covers"), "${context.packageName}.covers")
    val scanner by lazy {
        LibraryScanner(database, Media3MetadataReader(context), covers, appScope) { playbackSettings.current().newBookSpeed }
    }
    /** Unir y separar libros. */
    val corrections by lazy { LibraryCorrections(database, scanner, appScope) }
    val playbackSettings = PlaybackSettingsRepository(context.settingsStore)
    val librarySettings = LibrarySettingsRepository(context.settingsStore)
    /** Lo publica el servicio de reproducción; lo leen pantallas y widgets. */
    val nowPlaying = PlaybackStateHolder()
    val playback = PlaybackConnection(context, nowPlaying, playbackSettings, appScope)
    val volume = VolumeControl(context)
    /** Ajustes › Botones remotos abierta: las pulsaciones se resaltan en vez de ejecutarse. */
    val remoteKeys = RemoteKeyMonitor()
    /** Pausa diferida: la publica el servicio. */
    val sleep = SleepStateHolder()
    val bookmarks by lazy { BookmarkStore(database, playback, playbackSettings, appScope) }
    /** Ajustes › Datos. */
    val backup by lazy { BackupExporter(database, context.settingsStore) }
    val importer by lazy { BackupImporter(database, context.settingsStore) }
    /** Widget del libro en curso: lo que dibuja y cuándo se redibuja. */
    val widgets = WidgetUpdates(context, this)

    init {
        // Cada búsqueda terminada queda como la última (Ajustes › Biblioteca). Sin errores: un
        // escaneo fallido no tiene fecha de fin.
        appScope.launch {
            scanner.state.mapNotNull { s -> s.finishedAt?.let { LastScan(it, s.found) } }
                .distinctUntilChanged()
                .collect { librarySettings.setLastScan(it) }
        }
        appScope.launch { widgets.run() }
    }
}

class LectorApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as LectorApp).container
