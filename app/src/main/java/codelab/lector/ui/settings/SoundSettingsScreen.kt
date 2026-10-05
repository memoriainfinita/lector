package codelab.lector.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.playback.EqBands
import codelab.lector.playback.SoundSettings
import codelab.lector.ui.components.LectorSlider
import codelab.lector.ui.components.LectorSwitch
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.VerticalSlider
import codelab.lector.ui.formatDb
import codelab.lector.ui.theme.LectorTheme
import kotlin.math.roundToInt

private val BandLabels = listOf("100", "300", "1k", "3k", "8k")

/**
 * Ajustes › Ecualizador y volumen (lienzo "Ecualizador y volumen"): el sonido global, el de los
 * libros sin sonido propio. Mismos pasos que la hoja Sonido del reproductor (1 dB; bandas 0.5 dB).
 */
@Composable
fun SoundSettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val stored by viewModel.globalSound.collectAsStateWithLifecycle()
    // Tras el primer cambio manda la copia local: el guardado vuelve por el flujo con retraso y
    // haría saltar el deslizador mientras se arrastra.
    var edited by remember { mutableStateOf<SoundSettings?>(null) }
    val sound = edited ?: stored
    fun send(value: SoundSettings) {
        val s = value.clamped()
        if (s != sound) {
            edited = s
            viewModel.setGlobalSound(s)
        }
    }
    SettingsPage(stringResource(R.string.settings_sound), onBack) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Labeled(stringResource(R.string.volume), "${volume.level} / ${volume.max}", Modifier.padding(top = 16.dp))
            LectorSlider(volume.level.toFloat(), { viewModel.setVolume(it.roundToInt()) }, valueRange = 0f..volume.max.toFloat().coerceAtLeast(1f))

            Labeled(stringResource(R.string.preamp), stringResource(R.string.db_value, formatDb(sound.preampDb)), Modifier.padding(top = 18.dp))
            LectorSlider(
                sound.preampDb,
                { send(sound.copy(preampDb = it.roundToInt().toFloat())) },
                valueRange = SoundSettings.PreampMinDb..SoundSettings.PreampMaxDb,
            )
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.db_value, formatDb(SoundSettings.PreampMinDb)), style = t.label, color = c.textTertiary, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.db_value, formatDb(SoundSettings.PreampMaxDb)), style = t.label, color = c.textTertiary)
            }

            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.equalizer), style = t.row, color = c.text, modifier = Modifier.weight(1f))
                LectorSwitch(sound.eqEnabled, { send(sound.copy(eqEnabled = it)) })
            }
            Row(Modifier.fillMaxWidth().height(240.dp).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EqBands.indices.forEach { i ->
                    val value = sound.bandsDb.getOrElse(i) { 0f }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            // Como en la hoja Sonido: preamplificación y bandas a 0, sin tocar el interruptor ni el volumen.
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) {
                OutlineButton(stringResource(R.string.reset), { send(SoundSettings(eqEnabled = sound.eqEnabled)) })
            }
        }
    }
}

@Composable
private fun Labeled(label: String, value: String, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Row(modifier.fillMaxWidth()) {
        Text(label, style = t.row, color = c.text, modifier = Modifier.weight(1f))
        Text(value, style = t.meta.copy(fontSize = 14.sp), color = c.textSecondary)
    }
}
