package codelab.lector.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codelab.lector.R
import codelab.lector.ui.theme.LectorTheme

/*
 * Sistema de botones de design.md. Zona táctil mínima 44 dp aunque el botón se vea menor.
 */

@Composable
private fun ButtonBase(
    onClick: () -> Unit,
    height: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    border: BorderStroke? = null,
    horizontalPadding: Dp = 14.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(height)
                .clip(shape)
                .background(background, shape)
                .let { if (border != null) it.border(border, shape) else it }
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

private fun TextStyle.weight(w: FontWeight) = copy(fontWeight = w)

/** Principal relleno (Listo, Aplicar, Guardar…): 40 / radio 6 / 14 seminegrita. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val c = LectorTheme.colors
    ButtonBase(onClick, 40.dp, RoundedCornerShape(6.dp), modifier, background = if (danger) c.danger else c.accent, horizontalPadding = 16.dp) {
        Text(text, style = LectorTheme.type.body.weight(FontWeight.SemiBold), color = c.onAccent)
    }
}

/** Destacado (primer arranque, estados vacíos): 48 / radio 8 / 15 seminegrita. */
@Composable
fun HeroButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    ButtonBase(onClick, 48.dp, RoundedCornerShape(8.dp), modifier, background = c.accent, horizontalPadding = 20.dp) {
        Text(text, style = LectorTheme.type.row.weight(FontWeight.SemiBold), color = c.onAccent)
    }
}

/** Secundario con borde: 36 / radio 6 / 13. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    ButtonBase(onClick, 36.dp, RoundedCornerShape(6.dp), modifier, border = BorderStroke(1.dp, c.outline), horizontalPadding = 12.dp) {
        Text(text, style = LectorTheme.type.secondary, color = c.text)
    }
}

/** Texto (Deshacer, Cancelar, Ahora no…): 40 / radio 6 / 14. */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LectorTheme.colors.textSecondary,
    bold: Boolean = false,
) {
    ButtonBase(onClick, 40.dp, RoundedCornerShape(6.dp), modifier, horizontalPadding = 12.dp) {
        Text(text, style = LectorTheme.type.body.let { if (bold) it.weight(FontWeight.SemiBold) else it }, color = color)
    }
}

/** Enlace dentro de una hoja (Ver todos, Restablecer…): 36 / 13. */
@Composable
fun SheetLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ButtonBase(onClick, 36.dp, RoundedCornerShape(0.dp), modifier, horizontalPadding = 0.dp) {
        Text(text, style = LectorTheme.type.secondary, color = LectorTheme.colors.accent)
    }
}

/** Segmentado (Libros / Carpetas, tema, idioma): 36 / 14. [disabled]: opciones atenuadas que no se eligen. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Set<Int> = emptySet(),
) {
    val c = LectorTheme.colors
    Row(
        modifier = modifier
            .background(c.surface, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) c.track else Color.Transparent)
                    .selectable(isSelected, enabled = i !in disabled, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = LectorTheme.type.body.let { if (isSelected) it.weight(FontWeight.Medium) else it },
                    color = when {
                        isSelected -> c.text
                        i in disabled -> c.inactive
                        else -> c.textSecondary
                    },
                )
            }
        }
    }
}

/** Segmentado con iconos (Libros / Carpetas en la cabecera): 36 de alto, 44 de ancho por opción. El texto va como descripción. */
@Composable
fun IconSegmentedControl(icons: List<Painter>, labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    Row(
        modifier = modifier
            .background(c.surface, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icons.forEachIndexed { i, icon ->
            val isSelected = i == selected
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) c.track else Color.Transparent)
                    .selectable(isSelected, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, labels[i], Modifier.size(20.dp), tint = if (isSelected) c.text else c.textSecondary)
            }
        }
    }
}

/** Opción en hoja (velocidad, pausa, valores): 36 / radio 2 / 13 mono. */
@Composable
fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .defaultMinSize(minWidth = 48.dp)
            .clip(shape)
            .background(if (selected) c.accent else Color.Transparent)
            .let { if (selected) it else it.border(1.dp, c.outline, shape) }
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = LectorTheme.type.meta.copy(fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
            color = if (selected) c.onAccent else c.text,
        )
    }
}

/** Etiqueta o filtro: 28 / radio 2 / 12 mono. Discontinua para filtros de sistema (sin tag, pausa). */
@Composable
fun TagChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, leading: Painter? = null) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(2.dp)
    Box(modifier = modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .height(28.dp)
                .clip(shape)
                .background(if (selected) c.accent else Color.Transparent)
                .let { if (selected) it else it.border(1.dp, c.outline, shape) }
                .selectable(selected, role = Role.Checkbox, onClick = onClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (leading != null) Icon(leading, null, Modifier.size(14.dp), tint = if (selected) c.onAccent else c.textSecondary)
            Text(text, style = LectorTheme.type.meta, color = if (selected) c.onAccent else c.text)
        }
    }
}

/** Fila de menú: 44 / 14–15, icono 20. */
@Composable
fun MenuRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    trailing: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val c = LectorTheme.colors
    val color = when {
        !enabled -> c.inactive
        danger -> c.danger
        else -> c.text
    }
    Row(
        modifier = modifier
            .height(44.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = if (enabled && !danger) c.iconSoft else color)
        Text(text, style = LectorTheme.type.row, color = color, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = LectorTheme.type.meta, color = c.textSecondary)
    }
}

/** Icono: zona de 44. */
@Composable
fun IconAction(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LectorTheme.colors.text,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, contentDescription, Modifier.size(iconSize), tint = tint)
    }
}

/** Casilla del lienzo: 20 / radio 3; marcada, acento con la marca. Solo dibujo: la fila la hace pulsable. */
@Composable
fun CheckBox(checked: Boolean, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .size(20.dp)
            .then(if (checked) Modifier.background(c.accent, shape) else Modifier.border(1.dp, c.outline, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(12.dp), tint = c.onAccent)
    }
}

/** Interruptor del lienzo: pista 42 × 24, botón 18. */
@Composable
fun LectorSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(checked, role = Role.Switch) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(24.dp)
                .background(if (checked) c.accent else c.track, RoundedCornerShape(12.dp))
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .background(if (checked) c.background else c.textSecondary, RoundedCornerShape(9.dp)),
            )
        }
    }
}
