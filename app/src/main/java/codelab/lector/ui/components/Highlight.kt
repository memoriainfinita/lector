package codelab.lector.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import codelab.lector.library.searchHighlights
import codelab.lector.ui.theme.LectorTheme

/** Lo encontrado en acento (búsquedas de la Biblioteca y de Marcadores). */
@Composable
fun highlighted(text: String, terms: List<String>): AnnotatedString {
    val accent = LectorTheme.colors.accent
    return buildAnnotatedString {
        append(text)
        searchHighlights(text, terms).forEach { addStyle(SpanStyle(color = accent), it.first, it.last + 1) }
    }
}
