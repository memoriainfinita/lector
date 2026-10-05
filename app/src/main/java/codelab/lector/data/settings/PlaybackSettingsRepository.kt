package codelab.lector.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import codelab.lector.playback.SoundSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PlaybackSettings(
    /** Tramo que se repite al reanudar tras una pausa. */
    val rewindOnResumeMs: Int = 3_000,
    val skipDividedBySpeed: Boolean = false,
    val autoNextBook: Boolean = false,
    /** Segundos de los botones de salto de la app (−10 / +10). */
    val appSkipBackSec: Int = 10,
    val appSkipForwardSec: Int = 10,
    /** Segundos del salto atrás de la notificación (−30). */
    val notificationSkipBackSec: Int = 30,
    /** Velocidad con la que empiezan los libros nuevos. */
    val newBookSpeed: Float = 1f,
    /** Ajustes › Reproducir al abrir la app. */
    val playOnOpen: Boolean = false,
    /** Ajustes › Portada en la pantalla de bloqueo: portada en la sesión (bloqueo y notificación). */
    val coverOutside: Boolean = true,
)

/** Libro que sonaba cuando el proceso murió sin pasar por una pausa o un cierre normal. */
data class InterruptedPlayback(val bookId: String)

class PlaybackSettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<PlaybackSettings> = store.data.map { p ->
        val d = PlaybackSettings()
        PlaybackSettings(
            rewindOnResumeMs = p[REWIND_MS] ?: d.rewindOnResumeMs,
            skipDividedBySpeed = p[SKIP_DIVIDE] ?: d.skipDividedBySpeed,
            autoNextBook = p[AUTO_NEXT] ?: d.autoNextBook,
            appSkipBackSec = p[APP_BACK] ?: d.appSkipBackSec,
            appSkipForwardSec = p[APP_FORWARD] ?: d.appSkipForwardSec,
            notificationSkipBackSec = p[NOTIF_BACK] ?: d.notificationSkipBackSec,
            newBookSpeed = p[NEW_BOOK_SPEED] ?: d.newBookSpeed,
            playOnOpen = p[PLAY_ON_OPEN] ?: d.playOnOpen,
            coverOutside = p[COVER_OUTSIDE] ?: d.coverOutside,
        )
    }

    /** Sonido global (Ajustes › Ecualizador y volumen); lo usan los libros sin sonido propio. */
    val globalSound: Flow<SoundSettings> = store.data.map { p ->
        SoundSettings(
            preampDb = p[PREAMP] ?: 0f,
            eqEnabled = p[EQ_ON] ?: false,
            bandsDb = p[EQ_BANDS]?.split(",")?.mapNotNull(String::toFloatOrNull) ?: SoundSettings().bandsDb,
        ).clamped()
    }.distinctUntilChanged()

    suspend fun setGlobalSound(sound: SoundSettings) = store.edit {
        val s = sound.clamped()
        it[PREAMP] = s.preampDb
        it[EQ_ON] = s.eqEnabled
        it[EQ_BANDS] = s.bandsDb.joinToString(",")
    }

    suspend fun setNewBookSpeed(speed: Float) = store.edit { it[NEW_BOOK_SPEED] = speed }

    suspend fun setAutoNextBook(enabled: Boolean) = store.edit { it[AUTO_NEXT] = enabled }

    suspend fun setRewindOnResumeMs(ms: Int) = store.edit { it[REWIND_MS] = ms.coerceAtLeast(0) }

    suspend fun setPlayOnOpen(enabled: Boolean) = store.edit { it[PLAY_ON_OPEN] = enabled }

    suspend fun setCoverOutside(enabled: Boolean) = store.edit { it[COVER_OUTSIDE] = enabled }

    suspend fun current(): PlaybackSettings = settings.first()

    suspend fun lastBookId(): String? = store.data.first()[LAST_BOOK]

    suspend fun setLastBook(bookId: String) = store.edit { it[LAST_BOOK] = bookId }

    /** Marca que suena: si el proceso muere sin limpiarla, al abrir se avisa. */
    suspend fun setPlaying(bookId: String?) = store.edit {
        if (bookId == null) it.remove(PLAYING_BOOK) else it[PLAYING_BOOK] = bookId
    }

    /** Lee y limpia la marca de reproducción interrumpida por el sistema. */
    suspend fun takeInterrupted(): InterruptedPlayback? {
        val id = store.data.first()[PLAYING_BOOK] ?: return null
        store.edit { it.remove(PLAYING_BOOK) }
        return InterruptedPlayback(id)
    }

    private companion object {
        val REWIND_MS = intPreferencesKey("rewind_on_resume_ms")
        val SKIP_DIVIDE = booleanPreferencesKey("skip_divided_by_speed")
        val AUTO_NEXT = booleanPreferencesKey("auto_next_book")
        val APP_BACK = intPreferencesKey("app_skip_back_sec")
        val APP_FORWARD = intPreferencesKey("app_skip_forward_sec")
        val NOTIF_BACK = intPreferencesKey("notification_skip_back_sec")
        val LAST_BOOK = stringPreferencesKey("last_book_id")
        val PLAYING_BOOK = stringPreferencesKey("playing_book_id")
        val NEW_BOOK_SPEED = floatPreferencesKey("new_book_speed")
        val PLAY_ON_OPEN = booleanPreferencesKey("play_on_open")
        val COVER_OUTSIDE = booleanPreferencesKey("cover_outside")
        val PREAMP = floatPreferencesKey("sound_preamp_db")
        val EQ_ON = booleanPreferencesKey("sound_eq_enabled")
        val EQ_BANDS = stringPreferencesKey("sound_eq_bands")
    }
}
