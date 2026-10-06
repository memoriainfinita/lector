package codelab.lector.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import codelab.lector.playback.ActionCall
import codelab.lector.playback.PlayerAction
import codelab.lector.playback.SoundSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

data class PlaybackSettings(
    /** Tramo que se repite al reanudar tras una pausa. */
    val rewindOnResumeMs: Int = 3_000,
    val skipDividedBySpeed: Boolean = false,
    val autoNextBook: Boolean = false,
    /** Ajustes › Botones: los 4 huecos de Escuchando, alrededor de play. */
    val playerButtons: List<ActionCall> = DefaultButtons,
    /** Ajustes › Botones: los 4 huecos de la notificación (dos en los extremos y dos junto a play). */
    val notificationButtons: List<ActionCall> = DefaultButtons,
    /** Velocidad con la que empiezan los libros nuevos. */
    val newBookSpeed: Float = 1f,
    /** Ajustes › Reproducir al abrir la app. */
    val playOnOpen: Boolean = false,
    /** Ajustes › Portada en la pantalla de bloqueo: portada en la sesión (bloqueo y notificación). */
    val coverOutside: Boolean = true,
    /** Ajustes › Siguiente archivo desde su posición: al terminar un archivo sonando, el siguiente retoma la suya. */
    val nextFileFromPosition: Boolean = true,
    /** Ajustes › Botones remotos: auricular x1, x2, x3, anterior, siguiente y play (ver [RemoteKey]). */
    val remoteButtons: List<ActionCall> = DefaultRemoteButtons,
    /** Ajustes › Botones remotos › Responder con la app cerrada. */
    val remoteWhenClosed: Boolean = true,
    /** Ajustes › Auricular. */
    val pauseOnUnplug: Boolean = true,
    val resumeOnReplug: Boolean = true,
    /** Pausa diferida (design.md › Pausa diferida): alargar hasta el final del capítulo. */
    val sleepExtend: Boolean = false,
    /** Últimos minutos elegidos con "…": la hoja Elegir valor empieza ahí. */
    val sleepLastMinutes: Int = 30,
    /** Automática por horario: franja (puede cruzar la medianoche) y duración. */
    val sleepAuto: Boolean = false,
    val sleepAutoMinutes: Int = 30,
    val sleepFrom: LocalTime = LocalTime.of(23, 0),
    val sleepTo: LocalTime = LocalTime.of(7, 0),
    /** Al pausar: marcador de pausa (solo el último por libro) y seguir si se mueve el móvil. */
    val sleepMarkPause: Boolean = true,
    val sleepMotionResume: Boolean = true,
    /** Ajustes › Marcadores › Abrir la hoja al marcar: desde la app; auricular y notificación solo guardan. */
    val openSheetOnMark: Boolean = true,
) {
    /** Segundos de los saltos sin hueco propio (minirreproductor sin salto, coche): los del reproductor. */
    val appSkipBackSec: Int get() = playerButtons.firstOrNull { it.action == PlayerAction.SKIP_BACK }?.seconds ?: DEFAULT_SKIP_SEC
    val appSkipForwardSec: Int get() = playerButtons.firstOrNull { it.action == PlayerAction.SKIP_FORWARD }?.seconds ?: DEFAULT_SKIP_SEC
}

const val DEFAULT_SKIP_SEC = 10

/** Anterior, −10, +10, siguiente: en el reproductor y en la notificación. */
val DefaultButtons = listOf(
    ActionCall(PlayerAction.PREVIOUS),
    ActionCall(PlayerAction.SKIP_BACK, DEFAULT_SKIP_SEC),
    ActionCall(PlayerAction.SKIP_FORWARD, DEFAULT_SKIP_SEC),
    ActionCall(PlayerAction.NEXT),
)

/** Auricular x1 play / pausa, x2 marcador, x3 nada; anterior −10, siguiente +10, play play / pausa. */
val DefaultRemoteButtons = listOf(
    ActionCall(PlayerAction.PLAY_PAUSE),
    ActionCall(PlayerAction.ADD_BOOKMARK),
    ActionCall(PlayerAction.NONE),
    ActionCall(PlayerAction.SKIP_BACK, DEFAULT_SKIP_SEC),
    ActionCall(PlayerAction.SKIP_FORWARD, DEFAULT_SKIP_SEC),
    ActionCall(PlayerAction.PLAY_PAUSE),
)

/** "SKIP_BACK:10,PREVIOUS:0,…". Uno ilegible o de otro número de huecos: los de por defecto. */
private fun decodeButtons(text: String?, defaults: List<ActionCall> = DefaultButtons): List<ActionCall> {
    val calls = text?.split(',')?.map { part ->
        val action = runCatching { PlayerAction.valueOf(part.substringBefore(':')) }.getOrNull() ?: return defaults
        ActionCall(action, part.substringAfter(':', "0").toIntOrNull() ?: 0)
    } ?: return defaults
    return calls.takeIf { it.size == defaults.size } ?: defaults
}

private fun encodeButtons(calls: List<ActionCall>) = calls.joinToString(",") { "${it.action.name}:${it.seconds}" }

/** Libro que sonaba cuando el proceso murió sin pasar por una pausa o un cierre normal. */
data class InterruptedPlayback(val bookId: String)

class PlaybackSettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<PlaybackSettings> = store.data.map { p ->
        val d = PlaybackSettings()
        PlaybackSettings(
            rewindOnResumeMs = p[REWIND_MS] ?: d.rewindOnResumeMs,
            skipDividedBySpeed = p[SKIP_DIVIDE] ?: d.skipDividedBySpeed,
            autoNextBook = p[AUTO_NEXT] ?: d.autoNextBook,
            playerButtons = decodeButtons(p[PLAYER_BUTTONS]),
            notificationButtons = decodeButtons(p[NOTIFICATION_BUTTONS]),
            newBookSpeed = p[NEW_BOOK_SPEED] ?: d.newBookSpeed,
            playOnOpen = p[PLAY_ON_OPEN] ?: d.playOnOpen,
            coverOutside = p[COVER_OUTSIDE] ?: d.coverOutside,
            nextFileFromPosition = p[NEXT_FILE_FROM_POSITION] ?: d.nextFileFromPosition,
            remoteButtons = decodeButtons(p[REMOTE_BUTTONS], DefaultRemoteButtons),
            remoteWhenClosed = p[REMOTE_WHEN_CLOSED] ?: d.remoteWhenClosed,
            pauseOnUnplug = p[PAUSE_ON_UNPLUG] ?: d.pauseOnUnplug,
            resumeOnReplug = p[RESUME_ON_REPLUG] ?: d.resumeOnReplug,
            sleepExtend = p[SLEEP_EXTEND] ?: d.sleepExtend,
            sleepLastMinutes = p[SLEEP_LAST_MIN] ?: d.sleepLastMinutes,
            sleepAuto = p[SLEEP_AUTO] ?: d.sleepAuto,
            sleepAutoMinutes = p[SLEEP_AUTO_MIN] ?: d.sleepAutoMinutes,
            sleepFrom = p[SLEEP_FROM]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: d.sleepFrom,
            sleepTo = p[SLEEP_TO]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: d.sleepTo,
            sleepMarkPause = p[SLEEP_MARK] ?: d.sleepMarkPause,
            sleepMotionResume = p[SLEEP_MOTION] ?: d.sleepMotionResume,
            openSheetOnMark = p[OPEN_SHEET_ON_MARK] ?: d.openSheetOnMark,
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

    suspend fun setSkipDividedBySpeed(enabled: Boolean) = store.edit { it[SKIP_DIVIDE] = enabled }

    suspend fun setPlayerButton(index: Int, call: ActionCall) = store.edit {
        it[PLAYER_BUTTONS] = encodeButtons(decodeButtons(it[PLAYER_BUTTONS]).toMutableList().also { list -> list[index] = call })
    }

    suspend fun setNotificationButton(index: Int, call: ActionCall) = store.edit {
        it[NOTIFICATION_BUTTONS] = encodeButtons(decodeButtons(it[NOTIFICATION_BUTTONS]).toMutableList().also { list -> list[index] = call })
    }

    suspend fun setRemoteButton(index: Int, call: ActionCall) = store.edit {
        it[REMOTE_BUTTONS] = encodeButtons(decodeButtons(it[REMOTE_BUTTONS], DefaultRemoteButtons).toMutableList().also { list -> list[index] = call })
    }

    suspend fun setRemoteWhenClosed(enabled: Boolean) = store.edit { it[REMOTE_WHEN_CLOSED] = enabled }

    suspend fun setPauseOnUnplug(enabled: Boolean) = store.edit { it[PAUSE_ON_UNPLUG] = enabled }

    suspend fun setResumeOnReplug(enabled: Boolean) = store.edit { it[RESUME_ON_REPLUG] = enabled }

    suspend fun setSleepExtend(enabled: Boolean) = store.edit { it[SLEEP_EXTEND] = enabled }

    suspend fun setSleepLastMinutes(minutes: Int) = store.edit { it[SLEEP_LAST_MIN] = minutes }

    suspend fun setSleepAuto(enabled: Boolean) = store.edit { it[SLEEP_AUTO] = enabled }

    suspend fun setSleepAutoMinutes(minutes: Int) = store.edit { it[SLEEP_AUTO_MIN] = minutes }

    suspend fun setSleepWindow(from: LocalTime, to: LocalTime) = store.edit {
        it[SLEEP_FROM] = from.toSecondOfDay()
        it[SLEEP_TO] = to.toSecondOfDay()
    }

    suspend fun setSleepMarkPause(enabled: Boolean) = store.edit { it[SLEEP_MARK] = enabled }

    suspend fun setSleepMotionResume(enabled: Boolean) = store.edit { it[SLEEP_MOTION] = enabled }

    suspend fun setOpenSheetOnMark(enabled: Boolean) = store.edit { it[OPEN_SHEET_ON_MARK] = enabled }

    suspend fun setAutoNextBook(enabled: Boolean) = store.edit { it[AUTO_NEXT] = enabled }

    suspend fun setRewindOnResumeMs(ms: Int) = store.edit { it[REWIND_MS] = ms.coerceAtLeast(0) }

    suspend fun setPlayOnOpen(enabled: Boolean) = store.edit { it[PLAY_ON_OPEN] = enabled }

    suspend fun setCoverOutside(enabled: Boolean) = store.edit { it[COVER_OUTSIDE] = enabled }

    suspend fun setNextFileFromPosition(enabled: Boolean) = store.edit { it[NEXT_FILE_FROM_POSITION] = enabled }

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
        val PLAYER_BUTTONS = stringPreferencesKey("player_buttons")
        val NOTIFICATION_BUTTONS = stringPreferencesKey("notification_buttons")
        val LAST_BOOK = stringPreferencesKey("last_book_id")
        val PLAYING_BOOK = stringPreferencesKey("playing_book_id")
        val NEW_BOOK_SPEED = floatPreferencesKey("new_book_speed")
        val PLAY_ON_OPEN = booleanPreferencesKey("play_on_open")
        val COVER_OUTSIDE = booleanPreferencesKey("cover_outside")
        val NEXT_FILE_FROM_POSITION = booleanPreferencesKey("next_file_from_position")
        val REMOTE_BUTTONS = stringPreferencesKey("remote_buttons")
        val REMOTE_WHEN_CLOSED = booleanPreferencesKey("remote_when_closed")
        val PAUSE_ON_UNPLUG = booleanPreferencesKey("pause_on_unplug")
        val RESUME_ON_REPLUG = booleanPreferencesKey("resume_on_replug")
        val SLEEP_EXTEND = booleanPreferencesKey("sleep_extend")
        val SLEEP_LAST_MIN = intPreferencesKey("sleep_last_minutes")
        val SLEEP_AUTO = booleanPreferencesKey("sleep_auto")
        val SLEEP_AUTO_MIN = intPreferencesKey("sleep_auto_minutes")
        val SLEEP_FROM = intPreferencesKey("sleep_from")
        val SLEEP_TO = intPreferencesKey("sleep_to")
        val SLEEP_MARK = booleanPreferencesKey("sleep_mark_pause")
        val SLEEP_MOTION = booleanPreferencesKey("sleep_motion_resume")
        val OPEN_SHEET_ON_MARK = booleanPreferencesKey("open_sheet_on_mark")
        val PREAMP = floatPreferencesKey("sound_preamp_db")
        val EQ_ON = booleanPreferencesKey("sound_eq_enabled")
        val EQ_BANDS = stringPreferencesKey("sound_eq_bands")
    }
}
