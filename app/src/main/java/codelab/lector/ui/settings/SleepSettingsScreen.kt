package codelab.lector.ui.settings

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.formatDuration
import codelab.lector.ui.player.SleepTimerControls
import codelab.lector.ui.theme.LectorTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val AutoPresets = listOf(15f, 20f, 30f, 45f, 60f)

/**
 * Ajustes › Pausa diferida (design.md › Pausa diferida; lienzo `Settings-Sleep`): cuenta atrás,
 * temporizador, automática por horario y qué pasa al pausar.
 */
@Composable
fun SleepSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val sleep by viewModel.sleep.collectAsStateWithLifecycle()
    var pickingDuration by remember { mutableStateOf(false) }
    val time = DateTimeFormatter.ofPattern("HH:mm")

    fun pickTime(initial: LocalTime, onPicked: (LocalTime) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPicked(LocalTime.of(h, m)) }, initial.hour, initial.minute, DateFormat.is24HourFormat(context)).show()
    }

    SettingsPage(stringResource(R.string.settings_sleep), onBack) {
        Row(
            Modifier
                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                .fillMaxWidth()
                .background(c.popup, RoundedCornerShape(10.dp))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (sleep.active) {
                    Text(
                        stringResource(if (sleep.atChapterEnd) R.string.sleep_pauses_at_chapter_end else R.string.sleep_pauses_in),
                        style = t.secondary,
                        color = c.textSecondary,
                    )
                    Text(formatDuration(sleep.remainingMs), style = t.value.copy(fontSize = 28.sp), color = c.accent)
                } else {
                    Text(stringResource(R.string.sleep_off), style = t.secondary, color = c.textSecondary)
                }
            }
            if (sleep.active) OutlineButton(stringResource(R.string.cancel), { viewModel.setSleep(0) })
        }

        SectionHeader(stringResource(R.string.sleep_timer))
        SleepTimerControls(
            sleep,
            playback.sleepExtend,
            playback.sleepLastMinutes,
            onSet = viewModel::setSleep,
            onExtend = viewModel::setSleepExtend,
            onOther = viewModel::setSleepOther,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionHeader(stringResource(R.string.sleep_auto))
        SwitchRow(stringResource(R.string.sleep_auto_enable), playback.sleepAuto, viewModel::setSleepAuto, note = stringResource(R.string.sleep_auto_note))
        ValueRow(
            stringResource(R.string.sleep_duration),
            stringResource(R.string.minutes_value, playback.sleepAutoMinutes),
            { pickingDuration = true },
            enabled = playback.sleepAuto,
        )
        ValueRow(
            stringResource(R.string.sleep_window),
            "${playback.sleepFrom.format(time)} – ${playback.sleepTo.format(time)}",
            {
                pickTime(playback.sleepFrom) { from ->
                    pickTime(playback.sleepTo) { to -> viewModel.setSleepWindow(from, to) }
                }
            },
            enabled = playback.sleepAuto,
        )

        SectionHeader(stringResource(R.string.sleep_on_pause))
        SwitchRow(stringResource(R.string.sleep_mark_pause), playback.sleepMarkPause, viewModel::setSleepMarkPause, note = stringResource(R.string.sleep_mark_pause_note))
        SwitchRow(stringResource(R.string.sleep_motion), playback.sleepMotionResume, viewModel::setSleepMotionResume, note = stringResource(R.string.sleep_motion_note))
    }

    if (pickingDuration) {
        ValuePickerSheet(
            title = stringResource(R.string.sleep_duration),
            subtitle = stringResource(R.string.sleep_auto),
            initial = playback.sleepAutoMinutes.toFloat(),
            step = 1f,
            range = 1f..180f,
            presets = AutoPresets,
            format = { context.getString(R.string.minutes_value, it.toInt()) },
            onDone = { viewModel.setSleepAutoMinutes(it.toInt()) },
            onDismiss = { pickingDuration = false },
        )
    }
}
