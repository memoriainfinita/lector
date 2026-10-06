package codelab.lector.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import codelab.lector.R
import codelab.lector.playback.LectorCommands
import codelab.lector.playback.SleepState
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LectorSwitch
import codelab.lector.ui.components.OptionChip
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.SheetLink
import codelab.lector.ui.formatDuration
import codelab.lector.ui.settings.ValuePickerSheet
import codelab.lector.ui.theme.LectorTheme

/** Atajos de la pausa diferida (lienzo `Sleep-Sheet`). */
val SleepPresets = listOf(20, 30, 35, 40)
private val OtherPresets = listOf(15f, 45f, 60f, 90f, 120f)

/**
 * Pausa diferida desde Escuchando (design.md › Pausa diferida; lienzo `Sleep-Sheet`): estado con
 * Apagar, atajos, al terminar el capítulo, alargar y enlace a Ajustes › Pausa diferida.
 */
@Composable
fun SleepSheet(
    state: SleepState,
    extend: Boolean,
    lastMinutes: Int,
    onSet: (Int) -> Unit,
    onExtend: (Boolean) -> Unit,
    onOther: (Int) -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    LectorSheet(onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.settings_sleep), style = t.sheetTitle, color = c.text)
                    SleepStatus(state)
                }
                if (state.active) OutlineButton(stringResource(R.string.sleep_turn_off), { onSet(0) })
            }
            SleepTimerControls(state, extend, lastMinutes, onSet, onExtend, onOther)
            SheetLink(stringResource(R.string.sleep_more), onMore)
        }
    }
}

/** "Se pausa en 34:12", "Se pausa al terminar el capítulo · 12:03" o "Apagada". */
@Composable
fun SleepStatus(state: SleepState) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    if (!state.active) {
        Text(stringResource(R.string.sleep_off), style = t.secondary, color = c.textSecondary)
        return
    }
    // Un solo texto: si no cabe, baja de línea entero y la hora no se parte.
    val label = stringResource(if (state.atChapterEnd) R.string.sleep_pauses_at_chapter_end else R.string.sleep_pauses_in)
    val time = SpanStyle(fontFamily = t.meta.fontFamily, color = c.accent)
    Text(
        buildAnnotatedString {
            append(label)
            append(' ')
            withStyle(time) { append(formatDuration(state.remainingMs)) }
        },
        style = t.secondary,
        color = c.textSecondary,
    )
}

/**
 * Atajos de minutos y "…", al terminar el capítulo y alargar: en la hoja y en Ajustes › Pausa
 * diferida. [onSet]: minutos, 0 apaga, [LectorCommands.SLEEP_CHAPTER_END]. [onOther]: minutos de "…",
 * que se recuerdan.
 */
@Composable
fun SleepTimerControls(
    state: SleepState,
    extend: Boolean,
    lastMinutes: Int,
    onSet: (Int) -> Unit,
    onExtend: (Boolean) -> Unit,
    onOther: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val minutes = state.minutes.takeIf { state.active }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SleepPresets.forEach { m ->
                OptionChip("${m}m", selected = minutes == m, onClick = { onSet(m) }, modifier = Modifier.weight(1f))
            }
            OptionChip(
                if (minutes != null && minutes !in SleepPresets) "${minutes}m" else "…",
                selected = minutes != null && minutes !in SleepPresets,
                onClick = { picking = true },
                modifier = Modifier.weight(1f),
            )
        }
        OptionChip(
            stringResource(R.string.sleep_chapter_end),
            selected = state.active && state.minutes == null,
            onClick = { onSet(LectorCommands.SLEEP_CHAPTER_END) },
            modifier = Modifier.fillMaxWidth(),
            mono = false,
        )
        Row(
            Modifier.heightIn(min = 36.dp).toggleable(extend, role = Role.Switch, onValueChange = onExtend),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sleep_extend), style = t.body, color = c.text, modifier = Modifier.weight(1f))
            LectorSwitch(extend, onExtend)
        }
    }
    if (picking) {
        ValuePickerSheet(
            title = stringResource(R.string.settings_sleep),
            subtitle = stringResource(R.string.sleep_other_note),
            initial = lastMinutes.toFloat(),
            step = 1f,
            range = 1f..180f,
            presets = OtherPresets,
            format = { context.getString(R.string.minutes_value, it.toInt()) },
            onDone = { onOther(it.toInt()) },
            onDismiss = { picking = false },
        )
    }
}
