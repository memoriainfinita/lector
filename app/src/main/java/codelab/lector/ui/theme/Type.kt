package codelab.lector.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import codelab.lector.R

private fun plexSans(weight: Int) = Font(
    R.font.ibm_plex_sans,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val PlexSans = FontFamily(plexSans(400), plexSans(500), plexSans(600))

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

/** Escala de texto de design.md. */
@Immutable
data class LectorType(
    /** 11: etiquetas, texto sobre portadas pequeñas. */
    val label: TextStyle = sans(11),
    /** 12 mono: metadatos y tiempos. */
    val meta: TextStyle = mono(12),
    /** 13: secundario. */
    val secondary: TextStyle = sans(13),
    /** 14: listas y botones. */
    val body: TextStyle = sans(14),
    /** 15: texto principal de fila. */
    val row: TextStyle = sans(15),
    /** 17: título de hoja. */
    val sheetTitle: TextStyle = sans(17, FontWeight.SemiBold),
    /** 20: título de subpágina. */
    val subpageTitle: TextStyle = sans(20, FontWeight.SemiBold),
    /** 22: título del libro en el reproductor. */
    val bookTitle: TextStyle = sans(22, FontWeight.SemiBold),
    /** 24: título de pestaña. */
    val tabTitle: TextStyle = sans(24, FontWeight.SemiBold),
    /** 28 mono: valores grandes (velocidad, cuenta atrás). */
    val value: TextStyle = mono(28, FontWeight.Medium),
    /** 28: titular de bienvenida. */
    val headline: TextStyle = sans(28, FontWeight.SemiBold),
    /** Cabecera de sección de ajustes: 12, mayúsculas espaciadas. */
    val section: TextStyle = sans(12).copy(letterSpacing = 0.08.em),
)

private fun sans(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = PlexSans, fontSize = size.sp, fontWeight = weight)

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = PlexMono, fontSize = size.sp, fontWeight = weight)
