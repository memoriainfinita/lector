package codelab.lector.library

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import codelab.lector.container
import java.io.FileNotFoundException

/**
 * Portadas para otras apps (Android Auto no acepta `file://`; design.md › Pantallas › Android Auto).
 * Solo lectura y solo los archivos de [CoverStore]: `<id>.jpg`, sin rutas.
 */
class CoverProvider : ContentProvider() {

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("read only")
        val name = uri.lastPathSegment?.takeIf { uri.pathSegments.size == 1 && CoverName.matches(it) }
            ?: throw FileNotFoundException(uri.toString())
        val file = context!!.container.covers.dir.resolve(name)
        if (!file.isFile) throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri) = "image/jpeg"

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    private companion object {
        val CoverName = Regex("[A-Za-z0-9-]+\\.jpg")
    }
}
