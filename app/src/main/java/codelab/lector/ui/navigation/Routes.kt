package codelab.lector.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Destinos de la app (design.md › Navegación). Serializables para guardar las pilas. */
@Serializable
sealed interface Route : NavKey

/** Raíz de la pila. */
@Serializable
data object LibraryRoute : Route

@Serializable
data object ListeningRoute : Route

@Serializable
data object BookmarksRoute : Route

/** Búsquedas: con minirreproductor, como Biblioteca y Marcadores. */
@Serializable
data object LibrarySearchRoute : Route

@Serializable
data object BookmarksSearchRoute : Route

/** Pantallas completas: sin minirreproductor. */
@Serializable
sealed interface FullScreenRoute : Route

@Serializable
data object SettingsRoute : FullScreenRoute

@Serializable
data object SettingsSleepRoute : FullScreenRoute

@Serializable
data object SettingsButtonsRoute : FullScreenRoute

@Serializable
data object SettingsSoundRoute : FullScreenRoute

@Serializable
data object SettingsLibraryRoute : FullScreenRoute

@Serializable
data object SettingsTagsRoute : FullScreenRoute

@Serializable
data object SettingsAppearanceRoute : FullScreenRoute

@Serializable
data object SettingsDataRoute : FullScreenRoute

@Serializable
data object FolderPickerRoute : FullScreenRoute

@Serializable
data class MergeBooksRoute(val bookId: String) : FullScreenRoute

@Serializable
data class SplitBookRoute(val bookId: String) : FullScreenRoute

@Serializable
data class CoverViewerRoute(val bookId: String) : FullScreenRoute

/** Primer arranque: pila propia en lugar de las pestañas. */
@Serializable
data object OnboardingPermissionRoute : Route

@Serializable
data object OnboardingFoldersRoute : Route
