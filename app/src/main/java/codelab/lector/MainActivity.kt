package codelab.lector

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.data.settings.AppearanceSettings
import codelab.lector.ui.theme.LectorTheme

/** AppCompatActivity: necesaria para el idioma por app en Android 8–12. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appearance = container.appearance
        setContent {
            val settings by appearance.settings.collectAsStateWithLifecycle(AppearanceSettings())
            LectorTheme(settings) {
                SystemBars()
                Box(
                    Modifier.fillMaxSize().background(LectorTheme.colors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.ic_lector_logo), stringResource(R.string.app_name), Modifier.size(144.dp))
                }
            }
        }
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
}
