package codelab.lector.ui.settings

import android.app.TimePickerDialog
import android.os.Build
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.components.SegmentedControl
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.theme.AccentChoice
import codelab.lector.ui.theme.AccentPreset
import codelab.lector.ui.theme.DarkBackground
import codelab.lector.ui.theme.LectorTheme
import codelab.lector.ui.theme.LightBackground
import codelab.lector.ui.theme.ThemeMode
import codelab.lector.ui.theme.adjustForDark
import codelab.lector.ui.theme.darkColors
import codelab.lector.ui.theme.hasLowContrast
import codelab.lector.ui.theme.hsl
import codelab.lector.ui.theme.lightColors
import codelab.lector.ui.theme.lightVariant
import codelab.lector.ui.theme.resolveAccent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Modes = listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)
private val Languages = listOf("es", "en", "")

/** Ajustes › Apariencia (lienzo "Apariencia"): tema, cambio por hora, color de acento e idioma. */
@Composable
fun AppearanceScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = LectorTheme.colors
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val schedule = appearance.schedule
    var customSheet by remember { mutableStateOf(false) }
    val time = DateTimeFormatter.ofPattern("HH:mm")

    fun pickTime(initial: LocalTime, onPicked: (LocalTime) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPicked(LocalTime.of(h, m)) }, initial.hour, initial.minute, DateFormat.is24HourFormat(context)).show()
    }

    SettingsPage(stringResource(R.string.settings_appearance), onBack) {
        SectionHeader(stringResource(R.string.theme))
        SegmentedControl(
            Modes.map { stringResource(themeLabel(it)) },
            selected = Modes.indexOf(appearance.mode),
            onSelect = { viewModel.setThemeMode(Modes[it]) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        SwitchRow(stringResource(R.string.theme_change_by_time), schedule.enabled, { viewModel.setSchedule(schedule.copy(enabled = it)) })
        ValueRow(stringResource(R.string.light_at), schedule.lightAt.format(time), {
            pickTime(schedule.lightAt) { viewModel.setSchedule(schedule.copy(lightAt = it)) }
        }, enabled = schedule.enabled)
        ValueRow(stringResource(R.string.dark_at), schedule.darkAt.format(time), {
            pickTime(schedule.darkAt) { viewModel.setSchedule(schedule.copy(darkAt = it)) }
        }, enabled = schedule.enabled)

        SectionHeader(stringResource(R.string.accent_color))
        Row(Modifier.padding(horizontal = 15.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val dark = c.isDark
            AccentPreset.entries.forEach { preset ->
                AccentSwatch(
                    if (dark) preset.dark else preset.light,
                    stringResource(presetName(preset)),
                    selected = appearance.accent == AccentChoice.Preset(preset),
                ) { viewModel.setAccent(AccentChoice.Preset(preset)) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AccentSwatch(null, stringResource(R.string.accent_system), selected = appearance.accent == AccentChoice.System, icon = R.drawable.ic_palette) {
                    viewModel.setAccent(AccentChoice.System)
                }
            }
            val custom = appearance.accent as? AccentChoice.Custom
            AccentSwatch(
                custom?.let { if (dark) it.dark else lightVariant(it.dark) },
                stringResource(R.string.accent_custom),
                selected = custom != null,
                icon = if (custom == null) R.drawable.ic_add else null,
                dashed = custom == null,
            ) { customSheet = true }
        }

        SectionHeader(stringResource(R.string.language))
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        SegmentedControl(
            listOf(stringResource(R.string.language_es), stringResource(R.string.language_en), stringResource(R.string.theme_system)),
            selected = Languages.indexOfFirst { it.isNotEmpty() && current.startsWith(it) }.takeIf { it >= 0 } ?: 2,
            onSelect = { i ->
                // Rehace la Activity con el idioma nuevo; la pila de navegación se conserva.
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(Languages[i]))
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
    }

    if (customSheet) {
        val start = (appearance.accent as? AccentChoice.Custom)?.dark ?: resolveAccent(appearance.accent, dark = true)
        CustomAccentSheet(start, onDone = { viewModel.setAccent(AccentChoice.Custom(it)) }, onDismiss = { customSheet = false })
    }
}

private fun presetName(preset: AccentPreset) = when (preset) {
    AccentPreset.AMBER -> R.string.accent_amber
    AccentPreset.BLUE -> R.string.accent_blue
    AccentPreset.GREEN -> R.string.accent_green
    AccentPreset.CORAL -> R.string.accent_coral
}

/** Círculo de 40; elegido, con anillo de 2 a 3 de distancia. Sin color: borde (o discontinuo) e icono. */
@Composable
private fun AccentSwatch(color: Color?, name: String, selected: Boolean, icon: Int? = null, dashed: Boolean = false, onClick: () -> Unit) {
    val c = LectorTheme.colors
    Box(
        Modifier
            .size(50.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, c.text, CircleShape) else Modifier)
            .clickable(role = Role.RadioButton, onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    when {
                        color != null -> Modifier.background(color)
                        dashed -> Modifier.dashedCircle(c.inactive)
                        else -> Modifier.border(1.dp, c.outline, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(if (icon == R.drawable.ic_add) 16.dp else 18.dp), tint = c.iconSoft)
        }
    }
}

/** Borde discontinuo de 1 (color personalizado sin elegir). */
private fun Modifier.dashedCircle(color: Color) = drawBehind {
    val stroke = 1.dp.toPx()
    drawCircle(
        color,
        radius = size.minDimension / 2 - stroke / 2,
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}

/**
 * Hoja "Color personalizado": tono, luminosidad, hex y vista previa en oscuro y claro. El color elegido
 * es el del tema oscuro; el claro se deriva. Si falta contraste avisa, con "Ajustar", sin bloquear.
 */
@Composable
private fun CustomAccentSheet(initial: Color, onDone: (Color) -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val start = remember { initial.hsl() }
    var hue by remember { mutableFloatStateOf(start[0]) }
    // Un gris no tiene tono: se le da saturación para que el tono se vea al moverlo.
    val saturation = remember { if (start[1] < 0.2f) 0.75f else start[1] }
    var lightness by remember { mutableFloatStateOf(start[2].coerceIn(MinLightness, MaxLightness)) }
    val color = Color.hsl(hue, saturation, lightness)
    val light = lightVariant(color)
    var hex by remember { mutableStateOf(color.hex()) }
    fun setColor(new: Color) {
        val (h, _, l) = new.hsl()
        hue = h
        lightness = l.coerceIn(MinLightness, MaxLightness)
        hex = Color.hsl(hue, saturation, lightness).hex()
    }

    LectorSheet(onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.accent_custom), style = t.sheetTitle, color = c.text)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.hue), style = t.secondary, color = c.textSecondary)
                GradientSlider(
                    fraction = hue / 360f,
                    brush = Brush.horizontalGradient((0..6).map { Color.hsl(it * 60f % 360f, 0.85f, 0.6f) }),
                    thumb = color,
                    description = stringResource(R.string.hue),
                ) {
                    hue = it * 360f
                    hex = Color.hsl(hue, saturation, lightness).hex()
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.lightness), style = t.secondary, color = c.textSecondary)
                GradientSlider(
                    fraction = (lightness - MinLightness) / (MaxLightness - MinLightness),
                    brush = Brush.horizontalGradient(listOf(Color.hsl(hue, saturation, MinLightness), Color.hsl(hue, saturation, 0.5f), Color.hsl(hue, saturation, MaxLightness))),
                    thumb = color,
                    description = stringResource(R.string.lightness),
                ) {
                    lightness = MinLightness + it * (MaxLightness - MinLightness)
                    hex = Color.hsl(hue, saturation, lightness).hex()
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.hex), style = t.secondary, color = c.textSecondary)
                BasicTextField(
                    value = hex,
                    onValueChange = { text ->
                        val clean = text.uppercase().filter { it == '#' || it.isDigit() || it in 'A'..'F' }.take(7)
                        hex = if (clean.startsWith("#")) clean else "#$clean"
                        parseHex(hex)?.let { parsed ->
                            val (h, _, l) = parsed.hsl()
                            hue = h
                            lightness = l.coerceIn(MinLightness, MaxLightness)
                        }
                    },
                    singleLine = true,
                    textStyle = t.meta.copy(fontSize = 14.sp, color = c.text),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
                    modifier = Modifier
                        .width(110.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.background)
                        .border(1.dp, c.track, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                )
                Box(Modifier.size(40.dp).background(color, CircleShape))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Preview(stringResource(R.string.theme_dark), color, dark = true, Modifier.weight(1f))
                Preview(stringResource(R.string.theme_light), light, dark = false, Modifier.weight(1f))
            }

            if (hasLowContrast(color)) {
                Row(
                    Modifier.fillMaxWidth().border(1.dp, c.outline, RoundedCornerShape(8.dp)).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_alert), null, Modifier.size(18.dp), tint = c.iconSoft)
                    Text(stringResource(R.string.low_contrast_dark), style = t.secondary, color = c.iconSoft, modifier = Modifier.weight(1f))
                    TextButton(stringResource(R.string.adjust), { setColor(adjustForDark(color)) }, color = c.accent, bold = true)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PrimaryButton(stringResource(R.string.done), {
                    onDone(color)
                    onDismiss()
                })
            }
        }
    }
}

private const val MinLightness = 0.15f
private const val MaxLightness = 0.9f

/** Tarjeta de vista previa: el acento sobre el fondo de ese tema (botón, hora y barra). */
@Composable
private fun Preview(label: String, accent: Color, dark: Boolean, modifier: Modifier) {
    val colors = if (dark) darkColors(accent) else lightColors(accent)
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (dark) DarkBackground else LightBackground)
            .then(if (dark) Modifier.border(1.dp, colors.dividerOnSurface, shape) else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label.uppercase(), style = LectorTheme.type.section.copy(fontSize = 11.sp), color = colors.textSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_play), null, Modifier.size(18.dp), tint = colors.onAccent)
            }
            Text("13:42", style = LectorTheme.type.meta.copy(fontSize = 13.sp), color = accent)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(colors.track, RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(0.45f).height(4.dp).background(accent, RoundedCornerShape(2.dp)))
        }
    }
}

/** Deslizador con degradado (12 de alto) y botón de 24 del color elegido, con borde. */
@Composable
private fun GradientSlider(fraction: Float, brush: Brush, thumb: Color, description: String, onChange: (Float) -> Unit) {
    val c = LectorTheme.colors
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) }
            },
    ) {
        val width = maxWidth
        Canvas(Modifier.fillMaxWidth().height(12.dp).align(Alignment.Center)) {
            drawRoundRect(brush, cornerRadius = CornerRadius(6.dp.toPx()))
        }
        val x = with(LocalDensity.current) { (width.toPx() * fraction.coerceIn(0f, 1f)).roundToInt() }
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(x - 12.dp.roundToPx(), 2.dp.roundToPx()) }
                .size(24.dp)
                .background(thumb, CircleShape)
                .border(BorderStroke(3.dp, c.text), CircleShape),
        )
    }
}

private fun Color.hex() = "#%06X".format(toArgb() and 0xFFFFFF)

private fun parseHex(text: String): Color? {
    val digits = text.removePrefix("#")
    if (digits.length != 6) return null
    return digits.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}
