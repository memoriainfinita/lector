package codelab.lector.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import codelab.lector.R
import codelab.lector.ui.components.HeroButton
import codelab.lector.ui.components.LectorDialog
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LectorSwitch
import codelab.lector.ui.components.ListDivider
import codelab.lector.ui.components.MenuRow
import codelab.lector.ui.components.OptionChip
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.components.SegmentedControl
import codelab.lector.ui.components.SheetDivider
import codelab.lector.ui.components.SheetLink
import codelab.lector.ui.components.SheetTitle
import codelab.lector.ui.components.TagChip
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.components.UndoBar
import codelab.lector.ui.components.rememberUndoState
import codelab.lector.ui.theme.AccentPreset
import codelab.lector.ui.theme.LectorTheme
import codelab.lector.ui.theme.hasLowContrast
import codelab.lector.ui.theme.lightVariant

/** Solo depuración: todas las piezas del sistema visual, en oscuro y en claro. */
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Catalog() }
    }
}

private val customSample = Color(0xFF8E6BD8)

@Composable
private fun Catalog() {
    var dark by remember { mutableStateOf(true) }
    var accentIndex by remember { mutableIntStateOf(0) }
    val accents = AccentPreset.entries.map { if (dark) it.dark else it.light } +
        (if (dark) customSample else lightVariant(customSample))

    LectorTheme(dark = dark, accent = accents[accentIndex]) {
        val c = LectorTheme.colors
        val t = LectorTheme.type
        val undo = rememberUndoState()
        var sheetOpen by remember { mutableStateOf(false) }
        var dialogOpen by remember { mutableStateOf(false) }
        var switchOn by remember { mutableStateOf(true) }
        var segment by remember { mutableIntStateOf(0) }
        var speed by remember { mutableIntStateOf(2) }
        var tag by remember { mutableStateOf(true) }

        Box(Modifier.fillMaxSize().background(c.background).safeDrawingPadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 80.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_lector_logo), null, Modifier.size(48.dp))
                    Text("Catálogo", style = t.tabTitle, color = c.text, modifier = Modifier.padding(start = 12.dp))
                }
                SegmentedControl(listOf("Oscuro", "Claro"), if (dark) 0 else 1, { dark = it == 0 }, Modifier.padding(horizontal = 20.dp))
                Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    accents.forEachIndexed { i, color ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(color, RoundedCornerShape(20.dp))
                                .let { if (i == accentIndex) it.border(2.dp, c.text, RoundedCornerShape(20.dp)) else it }
                                .clickable { accentIndex = i },
                        )
                    }
                }
                Text(
                    "El quinto es personalizado (claro derivado). Poco contraste: ${hasLowContrast(customSample)}",
                    style = t.secondary, color = c.textSecondary, modifier = Modifier.padding(horizontal = 20.dp),
                )

                SectionHeader("Colores")
                FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "fondo" to c.background, "superficie" to c.surface, "emergente" to c.popup, "separador" to c.divider,
                        "pista" to c.track, "borde" to c.outline, "inactivo" to c.inactive, "texto" to c.text,
                        "secundario" to c.textSecondary, "terciario" to c.textTertiary, "iconos" to c.iconSoft,
                        "acento" to c.accent, "peligro" to c.danger,
                    ).forEach { (name, color) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(52.dp).background(color, RoundedCornerShape(6.dp)).border(1.dp, c.outline, RoundedCornerShape(6.dp)))
                            Text(name, style = t.label, color = c.textSecondary)
                        }
                    }
                }

                SectionHeader("Texto")
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf<Pair<String, TextStyle>>(
                        "28 Bienvenida" to t.headline, "1.25x" to t.value, "24 Biblioteca" to t.tabTitle,
                        "22 Dune" to t.bookTitle, "20 Apariencia" to t.subpageTitle, "17 Velocidad" to t.sheetTitle,
                        "15 Texto de fila" to t.row, "14 Listas y botones" to t.body, "13 Secundario" to t.secondary,
                        "12 1:47:57 / 26:10:12" to t.meta, "11 ETIQUETA" to t.label,
                    ).forEach { (text, style) -> Text(text, style = style, color = c.text) }
                }

                SectionHeader("Botones")
                FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Listo", {})
                    OutlineButton("Exportar", {})
                    TextButton("Cancelar", {})
                    TextButton("Deshacer", {}, color = c.accent, bold = true)
                    SheetLink("Ver todos", {})
                    PrimaryButton("Borrar", {}, danger = true)
                }
                HeroButton("Dar permiso", {}, Modifier.fillMaxWidth().padding(horizontal = 20.dp))

                SectionHeader("Opciones y etiquetas")
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("0.8", "1.0", "1.25", "1.5", "2.0").forEachIndexed { i, s ->
                        OptionChip(s, i == speed, { speed = i }, Modifier.weight(1f))
                    }
                }
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TagChip("idea", tag, { tag = !tag })
                    TagChip("cita", false, {})
                    TagChip("sin tag", false, {})
                }

                SectionHeader("Ajustes")
                SegmentedControl(listOf("Libros", "Carpetas"), segment, { segment = it }, Modifier.padding(horizontal = 20.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Pausar al desconectar", style = t.row, color = c.text)
                        Text("Auricular", style = t.secondary, color = c.textSecondary)
                    }
                    LectorSwitch(switchOn, { switchOn = it })
                }
                ListDivider()
                MenuRow("Abrir hoja", { sheetOpen = true }, Modifier.fillMaxWidth(), trailing = "3")
                MenuRow("Abrir diálogo", { dialogOpen = true }, Modifier.fillMaxWidth())
                MenuRow("Mostrar Deshacer", { undo.show("Marcador borrado") {} }, Modifier.fillMaxWidth())
                MenuRow("Borrar del móvil", {}, Modifier.fillMaxWidth(), danger = true)
            }

            UndoBar(undo, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
        }

        if (sheetOpen) {
            LectorSheet(onDismiss = { sheetOpen = false }) {
                SheetTitle("Velocidad", action = { PrimaryButton("Listo", { sheetOpen = false }) })
                Spacer(Modifier.height(12.dp))
                SheetDivider()
                MenuRow("Marcadores", {}, Modifier.fillMaxWidth(), trailing = "3")
                MenuRow("Ver portada", {}, Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))
            }
        }
        if (dialogOpen) {
            LectorDialog(
                title = "¿Borrar «Dharma Bums» del móvil?",
                onDismiss = { dialogOpen = false },
                buttons = {
                    TextButton("Cancelar", { dialogOpen = false })
                    PrimaryButton("Borrar", { dialogOpen = false }, danger = true)
                },
            ) {
                Text("Se borran 12 archivos (200.42 MB). No se puede deshacer.", style = t.body, color = c.textSecondary)
            }
        }
    }
}
