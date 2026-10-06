package codelab.lector.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay

/**
 * Escuchando encima de la pantalla anterior, que sigue dibujada debajo: se ve mientras Escuchando
 * sube, baja o se arrastra (design.md › Escuchando). El movimiento lo hace la propia pantalla; el
 * cambio de escena es instantáneo.
 */
internal data class ListeningScene(
    val below: NavEntry<NavKey>,
    val listening: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> {
    override val key: Any = listening.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(below, listening)
    override val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) {
            below.Content()
            // El minirreproductor ya está en su sitio mientras Escuchando baja.
            Box(Modifier.align(Alignment.BottomCenter)) { LocalListeningUnderlay.current() }
            listening.Content()
        }
    }

    companion object {
        private const val KEY = "lector.listening"

        /** Metadatos de la entrada de Escuchando: la marca y entrar y salir sin transición. */
        val metadata: Map<String, Any> = mapOf(KEY to true) +
            NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
            NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }

        fun isListening(entry: NavEntry<NavKey>) = entry.metadata[KEY] == true
    }
}

/** Lo que va entre la pantalla anterior y Escuchando: el minirreproductor, si hay sesión. */
internal val LocalListeningUnderlay = compositionLocalOf<@Composable () -> Unit> { {} }

/** Escuchando arriba con algo debajo: [ListeningScene]. Lo demás, una pantalla sola. */
internal class ListeningSceneStrategy : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (entries.size < 2 || !ListeningScene.isListening(entries.last())) return null
        return ListeningScene(entries[entries.size - 2], entries.last(), entries.dropLast(1))
    }
}
