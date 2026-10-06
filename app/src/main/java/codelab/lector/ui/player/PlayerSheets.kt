package codelab.lector.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.playback.EqBands
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.PlayerAction
import codelab.lector.playback.SoundSettings
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LectorSlider
import codelab.lector.ui.components.LectorSwitch
import codelab.lector.ui.components.MenuRow
import codelab.lector.ui.components.OptionChip
import codelab.lector.ui.components.SheetDivider
import codelab.lector.ui.components.SheetTitle
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.components.VerticalSlider
import codelab.lector.ui.formatDb
import codelab.lector.ui.formatDuration
import codelab.lector.ui.formatSpeed
import codelab.lector.ui.theme.LectorTheme
import codelab.lector.ui.theme.ThemeMode
import kotlin.math.roundToInt

private const val MinSpeed = 0.5f
private const val MaxSpeed = 3.5f
private val SpeedPresets = listOf(0.8f, 1.0f, 1.25f, 1.5f, 2.0f)

/** Menú ⋯ del reproductor. */
@Composable
fun PlayerMenuSheet(
    np: NowPlaying,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onSound: () -> Unit,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onFolder: () -> Unit,
    onSettings: () -> Unit,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val dark = LectorTheme.colors.isDark
    val themeLabel = stringResource(
        when (appearance.mode) {
            ThemeMode.DARK -> R.string.theme_dark
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.SYSTEM -> R.string.theme_system
        },
    )
    LectorSheet(onDismiss) {
        Column(Modifier.padding(bottom = 14.dp)) {
            MenuRow(stringResource(R.string.settings_sound), onSound, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_equalizer))
            MenuRow(stringResource(R.string.speed), onSpeed, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_speed), trailing = formatSpeed(np.speed))
            MenuRow(stringResource(R.string.settings_sleep), onSleep, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_sleep))
            MenuRow(stringResource(R.string.go_to_book_folder), onFolder, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_folder))
            MenuRow(
                stringResource(R.string.next_book),
                {
                    viewModel.act(PlayerAction.NEXT_BOOK)
                    onDismiss()
                },
                Modifier.fillMaxWidth(),
                painterResource(R.drawable.ic_next_book),
            )
            MenuRow(
                stringResource(R.string.theme),
                { viewModel.toggleTheme(showingDark = dark) },
                Modifier.fillMaxWidth(),
                painterResource(R.drawable.ic_theme),
                trailing = themeLabel,
            )
            SheetDivider(Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
            MenuRow(stringResource(R.string.settings), onSettings, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_settings))
        }
    }
}

/** Velocidad: − / deslizador / +, atajos y saltar silencios. Pasos de 0.05. */
@Composable
fun SpeedSheet(np: NowPlaying, newBookSpeed: Float, viewModel: PlayerViewModel, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var speed by remember { mutableFloatStateOf(np.speed) }
    fun set(value: Float) {
        val snapped = (Math.round(value * 20) / 20f).coerceIn(MinSpeed, MaxSpeed)
        if (snapped != speed) {
            speed = snapped
            viewModel.setSpeed(snapped)
        }
    }
    LectorSheet(onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.speed), style = t.sheetTitle, color = c.text, modifier = Modifier.weight(1f))
                Text(formatSpeed(speed), style = t.value, color = c.accent)
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepButton("−", stringResource(R.string.slower)) { set(speed - 0.05f) }
                    LectorSlider(speed, ::set, Modifier.weight(1f), valueRange = MinSpeed..MaxSpeed)
                    StepButton("+", stringResource(R.string.faster)) { set(speed + 0.05f) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp)) {
                    Text(formatSpeed(MinSpeed), style = t.label, color = c.textTertiary, modifier = Modifier.weight(1f))
                    Text(formatSpeed(MaxSpeed), style = t.label, color = c.textTertiary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpeedPresets.forEach { p ->
                    OptionChip(
                        String.format(java.util.Locale.ROOT, if (p * 100 % 10 == 0f) "%.1f" else "%.2f", p),
                        selected = kotlin.math.abs(speed - p) < 0.001f,
                        onClick = { set(p) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(Modifier.heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.skip_silence), style = t.body, color = c.text, modifier = Modifier.weight(1f))
                LectorSwitch(np.skipSilence, viewModel::setSkipSilence)
            }
            Text(stringResource(R.string.speed_note, formatSpeed(newBookSpeed)), style = t.secondary, color = c.textSecondary)
        }
    }
}

/** Botón cuadrado de 36 con borde (− / +). */
@Composable
private fun StepButton(text: String, description: String, onClick: () -> Unit) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(36.dp)
            .clip(shape)
            .border(BorderStroke(1.dp, c.outline), shape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LectorTheme.type.row.copy(fontSize = 18.sp), color = c.text)
    }
}

private val BandLabels = listOf("100", "300", "1k", "3k", "8k")

/**
 * Sonido: volumen del móvil, sonido propio del libro, preamplificación y ecualizador.
 * Los cambios suenan en vivo; se envían al cambiar el valor redondeado (1 dB, bandas 0.5 dB).
 */
@Composable
fun SoundSheet(np: NowPlaying, viewModel: PlayerViewModel, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    var sound by remember(np.ownSound) { mutableStateOf(np.sound) }
    fun send(value: SoundSettings) {
        val s = value.clamped()
        if (s != sound) {
            sound = s
            viewModel.setSound(s)
        }
    }
    LectorSheet(onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sound), style = t.sheetTitle, color = c.text, modifier = Modifier.weight(1f))
                TextButton(stringResource(R.string.reset), { send(SoundSettings(eqEnabled = sound.eqEnabled)) })
            }
            LabeledSlider(stringResource(R.string.volume), "${volume.level} / ${volume.max}") {
                LectorSlider(
                    volume.level.toFloat(),
                    { viewModel.setVolume(it.roundToInt()) },
                    valueRange = 0f..volume.max.toFloat().coerceAtLeast(1f),
                )
            }
            SheetDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.own_sound), style = t.body, color = c.text)
                    Text(stringResource(R.string.own_sound_note), style = t.secondary, color = c.textSecondary)
                }
                LectorSwitch(np.ownSound, viewModel::setOwnSound)
            }
            LabeledSlider(stringResource(R.string.preamp), stringResource(R.string.db_value, formatDb(sound.preampDb))) {
                LectorSlider(
                    sound.preampDb,
                    { send(sound.copy(preampDb = it.roundToInt().toFloat())) },
                    valueRange = SoundSettings.PreampMinDb..SoundSettings.PreampMaxDb,
                )
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.equalizer), style = t.body, color = c.text, modifier = Modifier.weight(1f))
                LectorSwitch(sound.eqEnabled, { send(sound.copy(eqEnabled = it)) })
            }
            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EqBands.indices.forEach { i ->
                    val value = sound.bandsDb.getOrElse(i) { 0f }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(formatDb(value), style = t.label, color = c.textSecondary)
                        VerticalSlider(
                            value,
                            { v ->
                                val bands = sound.bandsDb.toMutableList().also { it[i] = Math.round(v * 2) / 2f }
                                send(sound.copy(bandsDb = bands))
                            },
                            Modifier.weight(1f),
                            valueRange = -SoundSettings.BandMaxDb..SoundSettings.BandMaxDb,
                            enabled = sound.eqEnabled,
                        )
                        Text(BandLabels.getOrElse(i) { "" }, style = t.label, color = c.textTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, slider: @Composable () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = t.body, color = c.text, modifier = Modifier.weight(1f))
            Text(value, style = t.meta.copy(fontSize = 14.sp), color = c.textSecondary)
        }
        slider()
    }
}

/** Capítulos: pasados atenuados con marca, el actual resaltado con su progreso. Tocar va al tramo, donde se dejó. */
@Composable
fun ChaptersSheet(np: NowPlaying, onSegment: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (np.segmentIndex - 2).coerceAtLeast(0))
    // Su propia lista ya se desplaza.
    LectorSheet(onDismiss, scrollable = false) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Sin capítulos, los archivos del libro (como la pestaña Archivos en horizontal).
            Text(stringResource(if (np.hasChapters) R.string.chapters else R.string.files), style = t.sheetTitle, color = c.text, modifier = Modifier.weight(1f))
            Text(
                if (np.hasChapters) stringResource(R.string.chapters_count, np.segments.size)
                else pluralStringResource(R.plurals.files_count, np.segments.size, np.segments.size),
                style = t.secondary,
                color = c.textSecondary,
            )
        }
        LazyColumn(state = list, modifier = Modifier.padding(bottom = 12.dp)) {
            itemsIndexed(np.segments) { i, _ -> SegmentRow(np, i, onSegment, base = c.surface, highlight = c.popup) }
        }
    }
}

/**
 * Fila de capítulo o archivo: pasados atenuados con marca, el actual resaltado con su progreso.
 * La usan la hoja de capítulos y el panel Archivos de Escuchando en horizontal.
 */
@Composable
internal fun SegmentRow(np: NowPlaying, i: Int, onSegment: (Int) -> Unit, base: Color, highlight: Color) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val seg = np.segments[i]
    val current = i == np.segmentIndex
    val past = i < np.segmentIndex
    Row(
        Modifier
            .fillMaxWidth()
            .height(if (current) 52.dp else 44.dp)
            .background(if (current) highlight else base)
            .clickable(role = Role.Button) { onSegment(i) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (current) c.accent else base))
        Row(
            Modifier.padding(start = 17.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val tone = when {
                current -> c.accent
                past -> c.textTertiary
                else -> c.textSecondary
            }
            Text("${i + 1}", style = t.meta, color = tone, modifier = Modifier.width(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    seg.title,
                    style = if (current) t.body.copy(fontWeight = FontWeight.Medium) else t.body,
                    color = if (past) c.textTertiary else c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (current) {
                    val length = (seg.endMs - seg.startMs).coerceAtLeast(1)
                    val f = ((np.positionMs - seg.startMs).toFloat() / length).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(c.outline)) {
                        Box(Modifier.fillMaxWidth(f).height(2.dp).background(c.accent))
                    }
                }
            }
            Text(formatDuration(seg.startMs), style = t.meta, color = if (past) c.textTertiary else c.textSecondary)
            if (past) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), tint = c.textTertiary)
            } else {
                Spacer(Modifier.width(14.dp))
            }
        }
    }
}
