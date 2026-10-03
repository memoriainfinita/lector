package codelab.lector

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.settings.AppearanceRepository
import codelab.lector.library.CoverStore
import codelab.lector.library.LibraryScanner
import codelab.lector.library.Media3MetadataReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Contenedor de dependencias, sin framework de inyección. */
@OptIn(UnstableApi::class)
class AppContainer(context: Context) {
    /** Trabajo que sobrevive a las pantallas (escaneo). */
    val appScope = CoroutineScope(SupervisorJob())
    val database: LectorDatabase by lazy { LectorDatabase.create(context) }
    val appearance = AppearanceRepository(context.settingsStore)
    val covers = CoverStore(File(context.filesDir, "covers"))
    val scanner by lazy { LibraryScanner(database, Media3MetadataReader(context), covers, appScope) }
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
