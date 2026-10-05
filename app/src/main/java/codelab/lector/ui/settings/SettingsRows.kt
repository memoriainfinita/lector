package codelab.lector.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codelab.lector.R
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LectorSwitch
import codelab.lector.ui.components.LocalBottomInset
import codelab.lector.ui.components.OptionChip
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.theme.LectorTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** Subpágina de ajustes: flecha de Atrás, título de 20 y contenido desplazable. */
@Composable
fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = LectorTheme.colors
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack)
            Spacer(Modifier.size(4.dp))
            Text(title, style = LectorTheme.type.subpageTitle, color = c.text)
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            content()
            Spacer(Modifier.height(24.dp + LocalBottomInset.current))
        }
    }
}

/**
 * Fila de ajustes: título de 15, nota de 13 y lo de la derecha. Sin [enabled], atenuada: su función
 * aún no está hecha.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    trailing: @Composable () -> Unit = {},
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = role, onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = t.row, color = if (enabled) c.text else c.inactive)
            if (note != null) Text(note, style = t.secondary, color = if (enabled) c.textSecondary else c.inactive)
        }
        trailing()
    }
}

/** Interruptor: toda la fila lo cambia. */
@Composable
fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, note: String? = null, enabled: Boolean = true) {
    SettingRow(title, note = note, enabled = enabled, onClick = { onChange(!checked) }, role = Role.Switch) {
        LectorSwitch(checked, onChange, enabled = enabled)
    }
}

/** Valor a la derecha (mono 13 si [mono]); tocar abre su hoja o su subpágina. */
@Composable
fun ValueRow(title: String, value: String, onClick: () -> Unit, note: String? = null, enabled: Boolean = true, mono: Boolean = true) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    SettingRow(title, note = note, enabled = enabled, onClick = onClick) {
        Text(value, style = if (mono) t.meta.copy(fontSize = 13.sp) else t.secondary, color = if (enabled) c.textSecondary else c.inactive)
    }
}

/** Abre una subpágina o un ajuste del sistema: flecha a la derecha. */
@Composable
fun LinkRow(title: String, onClick: () -> Unit, note: String? = null, enabled: Boolean = true, chevron: Boolean = true) {
    val c = LectorTheme.colors
    SettingRow(title, note = note, enabled = enabled, onClick = onClick) {
        if (chevron) Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = if (enabled) c.textSecondary else c.inactive)
    }
}

/**
 * Hoja "Elegir valor" (lienzo): título y subtítulo, − / valor grande / +, atajos y Listo. El valor
 * se guarda con Listo; cerrar la hoja lo descarta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ValuePickerSheet(
    title: String,
    initial: Float,
    step: Float,
    range: ClosedFloatingPointRange<Float>,
    presets: List<Float>,
    format: (Float) -> String,
    onDone: (Float) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var value by remember { mutableFloatStateOf(initial) }
    fun set(v: Float) {
        value = ((v / step).roundToInt() * step).coerceIn(range.start, range.endInclusive)
    }
    LectorSheet(onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = t.sheetTitle, color = c.text)
                if (subtitle != null) Text(subtitle, style = t.secondary, color = c.textSecondary)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundStep(R.drawable.ic_remove, stringResource(R.string.less), value > range.start) { set(value - step) }
                Text(format(value), style = t.value, color = c.text, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                RoundStep(R.drawable.ic_add, stringResource(R.string.more), value < range.endInclusive) { set(value + step) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p -> OptionChip(format(p), selected = abs(value - p) < step / 2, onClick = { set(p) }) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PrimaryButton(stringResource(R.string.done), {
                    onDone(value)
                    onDismiss()
                })
            }
        }
    }
}

/** Botón redondo de 44 con borde (− / + de Elegir valor). */
@Composable
private fun RoundStep(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LectorTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, c.outline), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, Modifier.size(18.dp), tint = if (enabled) c.text else c.inactive)
    }
}

/** Círculo del color de acento (fila "Color de acento"). */
@Composable
fun Swatch(color: Color, size: Int = 18) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}
