package codelab.lector.playback

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Volumen de la app: el volumen multimedia del móvil, como Simple ABP (0..17 en el del usuario). Global. */
data class Volume(val level: Int, val max: Int)

class VolumeControl(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val resolver = context.contentResolver

    fun current() = Volume(audio.getStreamVolume(AudioManager.STREAM_MUSIC), audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))

    /** Sin mostrar el control de volumen del sistema: la hoja ya enseña el valor. */
    fun set(level: Int) {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(0, current().max), 0)
    }

    /** Cambios también desde los botones físicos: el sistema los anota en Settings.System. */
    val volume: Flow<Volume> = callbackFlow {
        trySend(current())
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(current())
            }
        }
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.distinctUntilChanged()
}
