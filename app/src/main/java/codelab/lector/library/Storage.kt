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

/** Archivos de audio dentro de [dir] y sus subcarpetas, sin ocultos ni `Android/`. */
fun countAudioFiles(dir: File): Int = dir.listFiles().orEmpty().sumOf { f ->
    when {
        f.isDirectory -> if (isScannableDir(f)) countAudioFiles(f) else 0
        !f.name.startsWith(".") && extensionOf(f.name) in AudioExtensions -> 1
        else -> 0
    }
}

/** Subcarpetas que recorre el escaneo, por nombre sin distinguir mayúsculas (explorador). */
fun scannableSubfolders(dir: File): List<File> =
    dir.listFiles().orEmpty().filter { it.isDirectory && isScannableDir(it) }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

/** Carpetas con audio para el primer arranque: niveles 1 y 2 bajo cada almacenamiento, por nombre. */
fun findAudioFolders(roots: List<File>, maxDepth: Int = 2): List<AudioFolder> {
    val out = mutableListOf<AudioFolder>()
    fun visit(dir: File, depth: Int) {
        scannableSubfolders(dir).forEach { sub ->
            val n = countAudioFiles(sub)
            if (n > 0) {
                out += AudioFolder(sub, n)
                if (depth < maxDepth) visit(sub, depth + 1)
            }
        }
    }
    roots.forEach { visit(it, 1) }
    return out
}
