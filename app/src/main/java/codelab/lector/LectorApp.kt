package codelab.lector

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.settings.AppearanceRepository

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Contenedor de dependencias, sin framework de inyección. */
class AppContainer(context: Context) {
    val database: LectorDatabase by lazy { LectorDatabase.create(context) }
    val appearance = AppearanceRepository(context.settingsStore)
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
