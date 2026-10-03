package codelab.lector.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Miniaturas de portada por libro, generadas al escanear. */
class CoverStore(private val dir: File) {

    fun file(bookId: String) = File(dir, "$bookId.jpg")

    fun has(bookId: String) = file(bookId).exists()

    fun delete(bookId: String) {
        file(bookId).delete()
    }

    fun save(bookId: String, bytes: ByteArray): Boolean =
        write(bookId) { opts -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) }

    fun save(bookId: String, image: File): Boolean =
        write(bookId) { opts -> BitmapFactory.decodeFile(image.path, opts) }

    private fun write(bookId: String, decode: (BitmapFactory.Options) -> Bitmap?): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        if (bounds.outWidth <= 0) return false
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MaxSize) sample *= 2
        val bitmap = decode(BitmapFactory.Options().apply { inSampleSize = sample }) ?: return false
        val scale = MaxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        dir.mkdirs()
        val tmp = File(dir, "$bookId.tmp")
        tmp.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return tmp.renameTo(file(bookId))
    }

    private companion object {
        /** Lado mayor: suficiente para la portada grande del reproductor (358 × 411 dp). */
        const val MaxSize = 1024
    }
}
