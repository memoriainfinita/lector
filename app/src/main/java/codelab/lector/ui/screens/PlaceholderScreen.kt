package codelab.lector.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import codelab.lector.R
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.MenuRow
import codelab.lector.ui.theme.LectorTheme

/** Enlace de una pantalla vacía: texto y adónde lleva. */
class PlaceholderLink(val text: String, val onClick: () -> Unit)

/**
 * Pantalla vacía con su título y sus enlaces de navegación (design.md › Navegación).
 * Cada una se sustituye por la pantalla real cuando se construye.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    @DrawableRes backIcon: Int = R.drawable.ic_back,
    subtitle: String? = null,
    links: List<PlaceholderLink> = emptyList(),
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (onBack == null) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(start = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = t.tabTitle, color = c.text)
            }
        } else {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val description = stringResource(if (backIcon == R.drawable.ic_back) R.string.back else R.string.close)
                IconAction(painterResource(backIcon), description, onBack)
                Spacer(Modifier.padding(start = 4.dp))
                Text(title, style = t.subpageTitle, color = c.text)
            }
        }
        if (subtitle != null) {
            Text(subtitle, style = t.secondary, color = c.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        links.forEach { MenuRow(it.text, it.onClick, Modifier.fillMaxWidth()) }
    }
}
