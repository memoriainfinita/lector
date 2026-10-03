package codelab.lector.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Hueco inferior que deben dejar las pantallas de pestaña: el minirreproductor va superpuesto
 * (así el área de la pestaña no cambia de tamaño al aparecer) y el contenido no debe quedar debajo.
 */
val LocalBottomInset = compositionLocalOf { 0.dp }
