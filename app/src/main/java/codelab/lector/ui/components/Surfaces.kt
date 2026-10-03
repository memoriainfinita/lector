package codelab.lector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import codelab.lector.ui.theme.LectorTheme

/** Hoja inferior: fondo de superficie, esquinas 18, asa 40 × 4. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectorSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = LectorTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        containerColor = c.surface,
        contentColor = c.text,
        scrimColor = c.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(c.outline, RoundedCornerShape(2.dp)),
            )
        },
        content = content,
    )
}

/** Título de hoja con acción opcional a la derecha (p. ej. Listo pequeño). */
@Composable
fun SheetTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = LectorTheme.type.sheetTitle, color = LectorTheme.colors.text, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun SheetDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = LectorTheme.colors.dividerOnSurface)
}

/** Diálogo: superficie emergente, esquinas 14, botones a la derecha. */
@Composable
fun LectorDialog(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, shape)
                .background(c.popup, shape)
                .border(1.dp, c.track, shape)
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = LectorTheme.type.sheetTitle, color = c.text)
            content()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) { buttons() }
        }
    }
}

/** Cabecera de sección de ajustes, en acento. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = LectorTheme.type.section,
        color = LectorTheme.colors.accent,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
    )
}

/** Espaciador horizontal fino para listas sobre el fondo. */
@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LectorTheme.colors.divider))
}
