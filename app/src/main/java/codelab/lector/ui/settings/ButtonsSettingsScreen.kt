package codelab.lector.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.data.settings.DEFAULT_SKIP_SEC
import codelab.lector.playback.ActionCall
import codelab.lector.playback.AssignableActions
import codelab.lector.playback.PlayerAction
import codelab.lector.playback.iconRes
import codelab.lector.playback.isSkip
import codelab.lector.playback.nameRes
import codelab.lector.playback.skipText
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.theme.LectorTheme

private val SkipPresets = listOf(5f, 10f, 15f, 30f, 60f)

/** Hueco que se está cambiando: del reproductor o de la notificación, y su posición (0–3). */
private data class SlotRef(val notification: Boolean, val index: Int)

/**
 * Ajustes › Botones (design.md › Pantallas › Ajustes › C; lienzo `Settings-Buttons` y
 * `Action-Picker`): los 4 huecos del reproductor y de la notificación, los del widget (atenuados
 * hasta que haya widgets) y dividir el salto por la velocidad. Tocar un hueco abre la hoja de
 * acciones; un salto pide después sus segundos.
 */
@Composable
fun ButtonsSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf<SlotRef?>(null) }
    var seconds by remember { mutableStateOf<Pair<SlotRef, ActionCall>?>(null) }
    fun current(ref: SlotRef) = (if (ref.notification) playback.notificationButtons else playback.playerButtons)[ref.index]
    fun save(ref: SlotRef, call: ActionCall) =
        if (ref.notification) viewModel.setNotificationButton(ref.index, call) else viewModel.setPlayerButton(ref.index, call)

    SettingsPage(stringResource(R.string.settings_buttons), onBack) {
        SectionHeader(stringResource(R.string.buttons_player))
        SlotGrid(playback.playerButtons) { choosing = SlotRef(notification = false, it) }
        SectionHeader(stringResource(R.string.buttons_notification))
        SlotGrid(playback.notificationButtons) { choosing = SlotRef(notification = true, it) }
        SectionHeader(stringResource(R.string.buttons_widget))
        // Llegan con los widgets.
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val back = ActionCall(PlayerAction.SKIP_BACK, DEFAULT_SKIP_SEC)
            val forward = ActionCall(PlayerAction.SKIP_FORWARD, DEFAULT_SKIP_SEC)
            SlotTile(stringResource(R.string.widget_left), back, Modifier.weight(1f), enabled = false) {}
            SlotTile(stringResource(R.string.widget_right), forward, Modifier.weight(1f), enabled = false) {}
        }
        SwitchRow(
            stringResource(R.string.skip_divided_by_speed),
            playback.skipDividedBySpeed,
            viewModel::setSkipDividedBySpeed,
            note = stringResource(R.string.skip_divided_by_speed_note),
        )
    }

    choosing?.let { ref ->
        ActionPickerSheet(
            title = stringResource(if (ref.notification) R.string.button_of_notification else R.string.button_of_player, ref.index + 1),
            selected = current(ref).action,
            onPick = { action ->
                choosing = null
                if (action.isSkip) {
                    val was = current(ref)
                    seconds = ref to ActionCall(action, if (was.action.isSkip) was.seconds else DEFAULT_SKIP_SEC)
                } else {
                    save(ref, ActionCall(action))
                }
            },
            onDismiss = { choosing = null },
        )
    }
    seconds?.let { (ref, call) ->
        val context = LocalContext.current
        ValuePickerSheet(
            title = stringResource(call.action.nameRes()),
            subtitle = stringResource(if (ref.notification) R.string.button_of_notification else R.string.button_of_player, ref.index + 1),
            initial = call.seconds.toFloat(),
            step = 1f,
            range = 1f..120f,
            presets = SkipPresets,
            format = { context.getString(R.string.seconds_value, it.toInt()) },
            onDone = { save(ref, call.copy(seconds = it.toInt())) },
            onDismiss = { seconds = null },
        )
    }
}

@Composable
private fun SlotGrid(calls: List<ActionCall>, onSlot: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        calls.forEachIndexed { i, call -> SlotTile("${i + 1}", call, Modifier.weight(1f)) { onSlot(i) } }
    }
}

/** Casilla de un hueco: número arriba y la acción (salto en texto, icono o "Nada"). */
@Composable
private fun SlotTile(label: String, call: ActionCall, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val shape = RoundedCornerShape(6.dp)
    val name = stringResource(call.action.nameRes())
    Column(
        modifier
            .height(56.dp)
            .clip(shape)
            .border(BorderStroke(1.dp, c.outline), shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = name, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        val tint = if (enabled) c.text else c.inactive
        Text(label, style = t.label, color = if (enabled) c.textSecondary else c.inactive)
        val icon = call.action.iconRes()
        when {
            call.action.isSkip -> Text("${call.skipText()} s", style = t.meta.copy(fontSize = 14.sp), color = tint)
            icon != null -> Icon(
                painterResource(icon),
                name,
                Modifier.size(20.dp),
                tint = if (enabled && call.action == PlayerAction.ADD_BOOKMARK) c.accent else tint,
            )
            else -> Text(name, style = t.secondary, color = c.textSecondary)
        }
    }
}

/** Hoja "Elegir acción de un botón": las acciones con la actual resaltada y marcada. */
@Composable
private fun ActionPickerSheet(title: String, selected: PlayerAction, onPick: (PlayerAction) -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    LectorSheet(onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = t.sheetTitle, color = c.text)
            Text(stringResource(R.string.button_actions_note), style = t.secondary, color = c.textSecondary)
        }
        AssignableActions.forEach { action ->
            val isSelected = action == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable(role = Role.Button) { onPick(action) }
                    .then(if (isSelected) Modifier.background(c.popup) else Modifier)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(action.nameRes()),
                    style = t.row,
                    color = if (action == PlayerAction.NONE) c.textSecondary else c.text,
                    modifier = Modifier.weight(1f),
                )
                when {
                    isSelected -> Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = c.accent)
                    action.isSkip -> Text(if (action == PlayerAction.SKIP_BACK) "− s" else "+ s", style = t.meta.copy(fontSize = 13.sp), color = c.textSecondary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
