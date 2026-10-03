package codelab.lector.library

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

/** Acceso a todos los archivos (Android 11+) o lectura clásica (8–10). */
fun hasStorageAccess(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

/** Ajuste del sistema donde se concede el acceso a todos los archivos (Android 11+). */
fun allFilesAccessIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))

/** Almacenamiento principal y tarjetas SD. */
fun storageRoots(context: Context): List<File> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.getSystemService(StorageManager::class.java).storageVolumes.mapNotNull { it.directory }
    } else {
        context.getExternalFilesDirs(null).mapNotNull { dir -> dir?.path?.substringBefore("/Android/")?.let(::File) }
    }

fun isScannableDir(dir: File) = !dir.name.startsWith(".") && dir.name != "Android"

data class AudioFolder(val dir: File, val audioFiles: Int)

/** Carpetas con audio para el primer arranque: niveles 1 y 2 bajo cada almacenamiento. */
fun findAudioFolders(roots: List<File>, maxDepth: Int = 2): List<AudioFolder> {
    fun count(dir: File): Int = dir.listFiles().orEmpty().sumOf { f ->
        when {
            f.isDirectory -> if (isScannableDir(f)) count(f) else 0
            extensionOf(f.name) in AudioExtensions -> 1
            else -> 0
        }
    }
    val out = mutableListOf<AudioFolder>()
    fun visit(dir: File, depth: Int) {
        dir.listFiles().orEmpty().filter { it.isDirectory && isScannableDir(it) }.forEach { sub ->
            val n = count(sub)
            if (n > 0) {
                out += AudioFolder(sub, n)
                if (depth < maxDepth) visit(sub, depth + 1)
            }
        }
    }
    roots.forEach { visit(it, 1) }
    return out
}
