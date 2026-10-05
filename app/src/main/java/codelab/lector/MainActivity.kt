package codelab.lector

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.data.settings.AppearanceSettings
import codelab.lector.ui.navigation.AppRoot
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** AppCompatActivity: necesaria para el idioma por app en Android 8–12. Única Activity (singleTask). */
class MainActivity : AppCompatActivity() {
    /** Peticiones de abrir Escuchando (notificación, widget). */
    private val openPlayer = Channel<Unit>(Channel.CONFLATED)

    /** La primera pantalla ya tiene su contenido: se quita la pantalla de inicio. */
    @Volatile
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de inicio con el icono hasta que la Biblioteca está completa, en vez de negro y
        // luego la cabecera sola. Como mucho SplashMaxMs, por si algo tarda.
        val started = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition { !ready && SystemClock.uptimeMillis() - started < SplashMaxMs }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        val appearance = container.appearance
        setContent {
            val settings by appearance.settings.collectAsStateWithLifecycle(AppearanceSettings())
            LectorTheme(settings) {
                SystemBars()
                AppRoot(openPlayer.receiveAsFlow(), onReady = { ready = true })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_PLAYER) openPlayer.trySend(Unit)
    }

    /** Iconos de la barra de estado según el tema activo. */
    @Composable
    private fun SystemBars() {
        val dark = LectorTheme.colors.isDark
        val transparent = android.graphics.Color.TRANSPARENT
        LaunchedEffect(dark) {
            val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
    }

    companion object {
        const val ACTION_OPEN_PLAYER = "codelab.lector.action.OPEN_PLAYER"
        private const val SplashMaxMs = 2_000L
    }
}
