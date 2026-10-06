package codelab.lector.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import codelab.lector.data.db.Book
import codelab.lector.data.db.BookFile
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.Bookmark
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.db.SegmentPosition
import codelab.lector.data.settings.PlaybackSettings
import codelab.lector.data.settings.PlaybackSettingsRepository
import codelab.lector.library.CoverStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Motor de reproducción: un libro como lista de archivos sobre ExoPlayer, con posición global,
 * navegación por tramos, deshacer salto, tramo repetido al reanudar y final según la clase de carpeta.
 * Todo en el hilo principal. [scope] vive con el servicio; [appScope] guarda lo pendiente al cerrarlo.
 */
@OptIn(UnstableApi::class)
class BookEngine(
    private val exo: ExoPlayer,
    private val db: LectorDatabase,
    private val covers: CoverStore,
    private val settings: PlaybackSettingsRepository,
    private val holder: PlaybackStateHolder,
    private val sound: SoundProcessor,
    private val scope: CoroutineScope,
    private val appScope: CoroutineScope,
) : Player.Listener {

    private var book: Book? = null
    private var files: List<BookFile> = emptyList()
    private var baseDir: File? = null
    private var timeline = BookTimeline(emptyList())
    private val undo = JumpUndo(UndoWindowMs)
    private var rewindPending = false
    private var segmentIndex = -1
    private var prefs = PlaybackSettings()
    private var ticker: Job? = null
    private var lastSaveAt = 0L
    private var globalSound = SoundSettings()
    /** Posición guardada de cada tramo, en ms de su archivo (design.md › Reproducción). */
    private val memory = mutableMapOf<SegmentKey, Long>()
    /** El último cambio de posición fue un salto: un cambio de tramo no es paso natural. */
    private var seeked = false

    val loaded: Boolean get() = book != null
    val currentPrefs: PlaybackSettings get() = prefs

    init {
        exo.addListener(this)
        scope.launch {
            settings.settings.collect {
                val coverChanged = it.coverOutside != prefs.coverOutside
                prefs = it
                // Portada fuera de la app: se ve al momento en la notificación y el bloqueo.
                if (coverChanged && loaded && exo.mediaItemCount > 0) {
                    val i = exo.currentMediaItemIndex
                    exo.replaceMediaItem(i, mediaItem(i, timeline.segments.getOrNull(timeline.segmentIndexAt(position()))))
                }
            }
        }
        scope.launch {
            settings.globalSound.collect {
                globalSound = it
                applySound()
            }
        }
    }

    // ---- Abrir ----

    suspend fun open(bookId: String, play: Boolean) {
        if (book?.id == bookId) {
            if (play) play()
            return
        }
        save()
        val b = db.books().get(bookId) ?: return holder.fail(PlaybackError.Failed("book not found"))
        val fs = db.books().files(bookId)
        val dir = baseDirOf(b, fs)
        if (fs.isEmpty() || fs.any { !File(dir, it.relativePath).isFile }) {
            markInaccessible(b.id)
            return
        }
        if (b.inaccessible) db.books().setInaccessible(b.id, false)
        val chapters = db.books().chapters(bookId).groupBy { it.fileId }
        timeline = BookTimeline(
            fs.map { f ->
                TimelineFile(f.relativePath, f.durationMs, chapters[f.id].orEmpty().map { FileChapter(it.title, it.startMs, it.endMs) })
            },
        )
        book = b.copy(inaccessible = false)
        files = fs
        baseDir = dir
        undo.clear()
        rewindPending = false
        segmentIndex = -1
        memory.clear()
        db.segmentPositions().forBook(bookId).forEach { memory[SegmentKey(it.file, it.startMs)] = it.positionMs }
        holder.fail(null)

        var start = b.positionFile?.let(timeline::indexOfFile)?.takeIf { it >= 0 }
            ?.let { FilePosition(it, b.positionMs) } ?: FilePosition(0, 0)
        // Terminado y parado al final: vuelve a empezar.
        if (timeline.totalMs > 0 && timeline.toBook(start) >= timeline.totalMs - EndToleranceMs) start = FilePosition(0, 0)

        val segment = timeline.segments.getOrNull(timeline.segmentIndexAt(timeline.toBook(start)))
        exo.setMediaItems(fs.indices.map { mediaItem(it, segment) }, start.index, start.ms)
        exo.setPlaybackSpeed(b.speed.coerceIn(MinSpeed, MaxSpeed))
        exo.skipSilenceEnabled = b.skipSilence
        applySound()
        exo.prepare()
        settings.setLastBook(bookId)
        if (play) play() else publish()
    }

    /** Carpeta a la que son relativas las rutas de los archivos. En un libro de un archivo, la ruta es el archivo. */
    private fun baseDirOf(b: Book, fs: List<BookFile>): File {
        val single = fs.singleOrNull()
        return if (single != null && b.path.endsWith("/" + single.relativePath)) File(b.path).parentFile!! else File(b.path)
    }

    private fun mediaItem(index: Int, segment: Segment?): MediaItem {
        val b = book!!
        val file = File(baseDir, files[index].relativePath)
        return MediaItem.Builder()
            .setMediaId("${b.id}/$index")
            .setUri(Uri.fromFile(file))
            .setMediaMetadata(metadata(b, segment))
            .build()
    }

    /** Título del libro; debajo, el capítulo si lo hay, si no el autor. Portada para notificación y bloqueo, si está activada. */
    private fun metadata(b: Book, segment: Segment?): MediaMetadata {
        val title = b.customName ?: b.title
        val cover = covers.file(b.id).takeIf { prefs.coverOutside && it.exists() }
        return MediaMetadata.Builder()
            .setTitle(title)
            .setAlbumTitle(title)
            .setArtist(if (timeline.hasChapters && segment != null) segment.title else b.author)
            .setArtworkUri(cover?.let(Uri::fromFile))
            .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
    }

    // ---- Acciones ----

    suspend fun perform(call: ActionCall) {
        if (!loaded && call.action != PlayerAction.NONE) return
        when (call.action) {
            PlayerAction.SKIP_BACK -> skip(-(call.seconds.takeIf { it > 0 } ?: prefs.appSkipBackSec))
            PlayerAction.SKIP_FORWARD -> skip(call.seconds.takeIf { it > 0 } ?: prefs.appSkipForwardSec)
            PlayerAction.PREVIOUS -> previous()
            PlayerAction.NEXT -> next()
            PlayerAction.PLAY_PAUSE -> if (exo.playWhenReady) exo.pause() else play()
            PlayerAction.ADD_BOOKMARK -> addBookmark()
            PlayerAction.PREVIOUS_BOOKMARK -> previousBookmark()
            PlayerAction.UNDO_JUMP -> undoJump()
            PlayerAction.NEXT_BOOK -> nextBook(play = exo.playWhenReady)
            PlayerAction.NONE -> Unit
        }
    }

    fun play() {
        beforePlay()
        exo.play()
    }

    /** Antes de reanudar: repite el tramo si venía de una pausa; tras el final, vuelve al inicio. */
    fun beforePlay() {
        if (!loaded) return
        if (exo.playbackState == Player.STATE_ENDED) {
            seekToBook(0)
        } else if (rewindPending && prefs.rewindOnResumeMs > 0) {
            seekToBook((position() - prefs.rewindOnResumeMs).coerceAtLeast(0))
        }
        rewindPending = false
    }

    fun skip(seconds: Int) {
        val amount = skipAmountMs(kotlin.math.abs(seconds), exo.playbackParameters.speed, prefs.skipDividedBySpeed)
        val target = position() + if (seconds < 0) -amount else amount
        seekToBook(target.coerceIn(0, timeline.totalMs))
    }

    /** Con más de 3 s dentro del tramo, a su inicio; si no, al tramo anterior, donde se dejó. */
    fun previous() {
        val pos = position()
        val i = timeline.segmentIndexAt(pos)
        if (i < 0) return
        val back = timeline.previousSegment(pos)
        if (back == null) jumpTo(timeline.segments[i].startMs) else jumpToSegment(back)
    }

    fun next() {
        val i = timeline.segmentIndexAt(position())
        if (i + 1 < timeline.segments.size) jumpToSegment(i + 1)
    }

    /** Ir a un tramo (anterior, siguiente, lista de capítulos): retoma su posición guardada. */
    fun jumpToSegment(index: Int) {
        val seg = timeline.segments.getOrNull(index) ?: return
        jumpTo(entryPoint(seg, savedBookMs(index)))
    }

    /** Salto grande: barra, capítulo, archivo o marcador. Se puede deshacer. */
    fun jumpTo(bookMs: Long) {
        if (!loaded) return
        recordJump()
        seekToBook(bookMs)
    }

    /** Registra el origen de un salto grande que ejecuta otro (la barra de la notificación). */
    fun recordJump() {
        undo.onJump(position(), System.currentTimeMillis())
        rewindPending = false
    }

    fun undoJump() {
        undo.take()?.let(::seekToBook)
    }

    // ---- Menú del libro ----

    /** Reiniciar posición del libro cargado: al inicio, en pausa. */
    fun reset(bookId: String) {
        if (book?.id != bookId) return
        exo.pause()
        clearMemory()
        seekToBook(0)
    }

    /** Cambios hechos fuera del motor (nombre, terminado): relee el libro y el título de la notificación. */
    suspend fun refresh(bookId: String) {
        if (book?.id != bookId) return
        book = db.books().get(bookId) ?: return
        if (exo.mediaItemCount > 0) {
            val i = exo.currentMediaItemIndex
            exo.replaceMediaItem(i, mediaItem(i, timeline.segments.getOrNull(timeline.segmentIndexAt(position()))))
        }
        publish()
    }

    /** Borrar del móvil el libro cargado: primero se descarga del reproductor. */
    fun unload(bookId: String) {
        if (book?.id != bookId) return
        drop()
    }

    private fun drop() {
        exo.stop()
        exo.clearMediaItems()
        book = null
        files = emptyList()
        timeline = BookTimeline(emptyList())
        publish()
    }

    private suspend fun addBookmark() {
        val b = book ?: return
        val now = System.currentTimeMillis()
        db.bookmarks().upsert(
            Bookmark(
                id = UUID.randomUUID().toString(),
                bookId = b.id,
                file = files[exo.currentMediaItemIndex].relativePath,
                positionMs = exo.currentPosition,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /** Pausa diferida: marcador de pausa en la posición, solo el último por libro. */
    suspend fun addPauseBookmark() {
        val b = book ?: return
        val now = System.currentTimeMillis()
        db.bookmarks().replacePauseMarker(
            Bookmark(
                id = UUID.randomUUID().toString(),
                bookId = b.id,
                file = files[exo.currentMediaItemIndex].relativePath,
                positionMs = exo.currentPosition,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /** Fin del tramo que suena, en ms del libro (pausa diferida al terminar el capítulo). */
    fun currentSegmentEnd(): Long? {
        if (!loaded) return null
        return timeline.segments.getOrNull(timeline.segmentIndexAt(position()))?.endMs
    }

    private suspend fun previousBookmark() {
        val b = book ?: return
        val marks = db.bookmarks().ofKind(b.id, BookmarkKind.NORMAL).mapNotNull { m ->
            timeline.indexOfFile(m.file).takeIf { it >= 0 }?.let { timeline.toBook(FilePosition(it, m.positionMs)) }
        }
        previousBookmarkTarget(marks, position())?.let(::jumpTo)
    }

    private suspend fun nextBook(play: Boolean) {
        val b = book ?: return
        nextBook(b, db.books().all())?.let { open(it.id, play) }
    }

    // ---- Velocidad y sonido ----

    /** Velocidad del libro, de 0.5x a 3.5x en pasos de 0.05. Se guarda en el libro. */
    fun setSpeed(speed: Float) {
        val b = book ?: return
        val s = (Math.round(speed * 20) / 20f).coerceIn(MinSpeed, MaxSpeed)
        exo.setPlaybackSpeed(s)
        book = b.copy(speed = s)
        appScope.launch { db.books().setSpeed(b.id, s) }
        publish()
    }

    fun setSkipSilence(enabled: Boolean) {
        val b = book ?: return
        exo.skipSilenceEnabled = enabled
        book = b.copy(skipSilence = enabled)
        appScope.launch { db.books().setSkipSilence(b.id, enabled) }
        publish()
    }

    /** Sonido propio: al activarlo parte del global; al desactivarlo el libro vuelve al global. */
    fun setOwnSound(enabled: Boolean) {
        val b = book ?: return
        val updated = if (enabled) {
            b.copy(ownSound = true, preampDb = globalSound.preampDb, eqEnabled = globalSound.eqEnabled, eqBands = globalSound.bandsDb)
        } else b.copy(ownSound = false)
        storeSound(updated)
    }

    /** Cambia el sonido propio del libro; sin sonido propio no hace nada (se edita el global). */
    fun setBookSound(value: SoundSettings) {
        val b = book ?: return
        if (!b.ownSound) return
        val s = value.clamped()
        storeSound(b.copy(preampDb = s.preampDb, eqEnabled = s.eqEnabled, eqBands = s.bandsDb))
    }

    private fun storeSound(b: Book) {
        book = b
        appScope.launch { db.books().setSound(b.id, b.ownSound, b.preampDb, b.eqEnabled, b.eqBands) }
        applySound()
        publish()
    }

    private fun effectiveSound(): SoundSettings {
        val b = book
        return if (b != null && b.ownSound) {
            SoundSettings(b.preampDb ?: 0f, b.eqEnabled, b.eqBands ?: SoundSettings().bandsDb).clamped()
        } else globalSound
    }

    private fun applySound() = sound.setSettings(effectiveSound())

    // ---- Posición ----

    fun position(): Long =
        if (!loaded || exo.mediaItemCount == 0) 0 else timeline.toBook(FilePosition(exo.currentMediaItemIndex, exo.currentPosition))

    private fun seekToBook(bookMs: Long) {
        val p = timeline.toFile(bookMs)
        exo.seekTo(p.index, p.ms)
        rewindPending = false
        publish()
        save()
    }

    /** Guarda la posición actual. La escritura va en [appScope] para que termine aunque el servicio se cierre. */
    fun save() {
        val b = book ?: return
        if (exo.mediaItemCount == 0) return
        val index = exo.currentMediaItemIndex
        val file = files.getOrNull(index)?.relativePath ?: return
        val ms = exo.currentPosition
        val now = System.currentTimeMillis()
        lastSaveAt = now
        appScope.launch { db.books().savePosition(b.id, file, ms, now) }
    }

    // ---- Posición por tramo ----

    private fun savedBookMs(index: Int): Long? {
        val key = timeline.segmentKey(index) ?: return null
        return memory[key]?.let { timeline.savedBookMs(key, it) }
    }

    /** Al salir del tramo [index] en [bookMs] con un salto. */
    private fun leave(index: Int, bookMs: Long) {
        val seg = timeline.segments.getOrNull(index) ?: return
        when (leaveAction(seg, bookMs)) {
            LeaveAction.REMEMBER -> remember(index, bookMs)
            LeaveAction.FORGET -> forget(index)
            LeaveAction.KEEP -> Unit
        }
    }

    private fun remember(index: Int, bookMs: Long) {
        val b = book ?: return
        val key = timeline.segmentKey(index) ?: return
        val ms = timeline.toFile(bookMs).ms
        memory[key] = ms
        val now = System.currentTimeMillis()
        appScope.launch { db.segmentPositions().save(SegmentPosition(b.id, key.file, key.startMs, ms, now)) }
    }

    private fun forget(index: Int) {
        val b = book ?: return
        val key = timeline.segmentKey(index) ?: return
        if (memory.remove(key) == null) return
        appScope.launch { db.segmentPositions().forget(b.id, key.file, key.startMs) }
    }

    private fun clearMemory() {
        val b = book ?: return
        memory.clear()
        appScope.launch { db.segmentPositions().clear(b.id) }
    }

    // ---- Eventos del reproductor ----

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady) {
            rewindPending = reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST ||
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY ||
                reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS
            save()
        }
        publish()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val id = book?.id
        appScope.launch { settings.setPlaying(if (isPlaying) id else null) }
        ticker?.cancel()
        if (isPlaying) {
            ticker = scope.launch {
                while (isActive) {
                    tick()
                    delay(TickMs)
                }
            }
        }
        publish()
    }

    private fun tick() {
        updateSegment()
        publish()
        if (System.currentTimeMillis() - lastSaveAt >= SaveEveryMs) save()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateSegment()
        // Archivo terminado sonando: con "Siguiente archivo desde su posición", el siguiente
        // retoma la suya. No es un salto: no se deshace.
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && prefs.nextFileFromPosition) {
            val i = timeline.segmentIndexAt(position())
            val seg = timeline.segments.getOrNull(i)
            if (seg != null) {
                val target = entryPoint(seg, savedBookMs(i))
                if (target != seg.startMs) seekToBook(target)
            }
        }
        publish()
        save()
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (loaded && reason == Player.DISCONTINUITY_REASON_SEEK) {
            val from = timeline.toBook(FilePosition(oldPosition.mediaItemIndex, oldPosition.positionMs))
            val to = timeline.toBook(FilePosition(newPosition.mediaItemIndex, newPosition.positionMs))
            val left = timeline.segmentIndexAt(from)
            if (left >= 0 && left != timeline.segmentIndexAt(to)) leave(left, from)
            seeked = true
        }
        updateSegment()
        publish()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> fixUnknownDuration()
            Player.STATE_ENDED -> scope.launch { onFinished() }
        }
        publish()
    }

    override fun onPlayerError(error: PlaybackException) {
        val b = book ?: return holder.fail(PlaybackError.Failed(error.errorCodeName))
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION
        ) {
            scope.launch { markInaccessible(b.id) }
        } else {
            holder.fail(PlaybackError.Failed(error.errorCodeName))
        }
    }

    private suspend fun markInaccessible(bookId: String) {
        db.books().setInaccessible(bookId, true)
        if (book?.id == bookId) {
            save()
            drop()
        }
        holder.fail(PlaybackError.Inaccessible(bookId))
    }

    /**
     * Cambio de tramo: actualiza el subtítulo de la notificación sin cortar el audio. Pasar al
     * siguiente sonando, sin salto, es haber escuchado el anterior entero: se borra su posición.
     */
    private fun updateSegment() {
        if (!loaded) return
        val i = timeline.segmentIndexAt(position())
        val jumped = seeked
        seeked = false
        if (i == segmentIndex) return
        val previous = segmentIndex
        segmentIndex = i
        if (!jumped && previous >= 0 && i == previous + 1) forget(previous)
        if (timeline.hasChapters && exo.mediaItemCount > 0) {
            exo.replaceMediaItem(exo.currentMediaItemIndex, mediaItem(exo.currentMediaItemIndex, timeline.segments.getOrNull(i)))
        }
    }

    /** El escaneo no obtuvo la duración (Mentats of Dune): se toma la del reproductor y se guarda. */
    private fun fixUnknownDuration() {
        val b = book ?: return
        val index = exo.currentMediaItemIndex
        val f = files.getOrNull(index) ?: return
        val d = exo.duration
        if (f.durationMs > 0 || d == C.TIME_UNSET || d <= 0) return
        timeline = timeline.withFileDuration(index, d)
        files = files.toMutableList().also { it[index] = f.copy(durationMs = d) }
        val path = File(baseDir, f.relativePath).path
        appScope.launch {
            db.books().fixFileDuration(f.id, b.id, d)
            db.fileMeta().setDuration(path, d)
        }
        segmentIndex = -1
        updateSegment()
    }

    private suspend fun onFinished() {
        val b = book ?: return
        clearMemory()
        when (finishActionFor(db.folders().ruleFor(b.path))) {
            FinishAction.FINISH_THEN_NEXT -> {
                setFinished(b, true)
                if (prefs.autoNextBook) nextBook(play = true)
            }
            FinishAction.FINISH -> setFinished(b, true)
            FinishAction.RESTART -> {
                exo.pause()
                seekToBook(0)
                setFinished(b, false)
            }
        }
    }

    private suspend fun setFinished(b: Book, finished: Boolean) {
        db.books().setFinished(b.id, finished)
        if (book?.id == b.id) book = b.copy(finished = finished)
        save()
    }

    // ---- Estado publicado ----

    fun publish() {
        val b = book
        if (b == null || exo.mediaItemCount == 0) return holder.publish(null)
        val pos = position()
        val segIndex = timeline.segmentIndexAt(pos)
        val seg = timeline.segments.getOrNull(segIndex)
        holder.publish(
            NowPlaying(
                bookId = b.id,
                title = b.customName ?: b.title,
                author = b.author,
                narrator = b.narrator,
                coverPath = covers.file(b.id).takeIf { it.exists() }?.path,
                positionMs = pos,
                durationMs = timeline.totalMs,
                fileIndex = exo.currentMediaItemIndex,
                fileCount = files.size,
                segmentTitle = seg?.title.orEmpty(),
                segmentIndex = segIndex,
                segmentCount = timeline.segments.size,
                segmentStartMs = seg?.startMs ?: 0,
                segmentEndMs = seg?.endMs ?: 0,
                hasChapters = timeline.hasChapters,
                segments = timeline.segments,
                isPlaying = exo.isPlaying,
                playWhenReady = exo.playWhenReady,
                speed = exo.playbackParameters.speed,
                skipSilence = exo.skipSilenceEnabled,
                ownSound = b.ownSound,
                sound = effectiveSound(),
                undoUntil = undo.origin?.let { undo.lastJumpAt + UndoWindowMs },
                undoFromMs = undo.origin,
            ),
        )
    }

    /** Cierre normal del servicio: guarda y quita la marca de "sonando". */
    fun release() {
        save()
        ticker?.cancel()
        exo.removeListener(this)
        appScope.launch { settings.setPlaying(null) }
        holder.publish(null)
    }

    private companion object {
        const val TickMs = 500L
        const val SaveEveryMs = 5_000L
        /** Deshacer un salto grande: más que los demás avisos (5 s). */
        const val UndoWindowMs = 10_000L
        const val EndToleranceMs = 1_000L
        const val MinSpeed = 0.5f
        const val MaxSpeed = 3.5f
    }
}
