package codelab.lector.debug

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.container
import codelab.lector.data.db.LibraryFolder
import codelab.lector.library.AudioFolder
import codelab.lector.library.allFilesAccessIntent
import codelab.lector.library.findAudioFolders
import codelab.lector.library.hasStorageAccess
import codelab.lector.library.storageRoots
import codelab.lector.ui.components.ListDivider
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.theme.LectorTheme
import codelab.lector.ui.theme.AccentPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Solo depuración: escanea la biblioteca real y lista lo detectado. No modifica archivos. */
class ScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LectorTheme(dark = true, accent = AccentPreset.AMBER.dark) { ScanScreen() } }
    }
}

@Composable
private fun ScanScreen() {
    val context = LocalContext.current
    val app = context.container
    val scope = rememberCoroutineScope()
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val state by app.scanner.state.collectAsStateWithLifecycle()
    val folders by app.database.folders().observeFolders().collectAsStateWithLifecycle(emptyList())
    val books by app.database.books().observeAll().collectAsStateWithLifecycle(emptyList())
    var access by remember { mutableStateOf(hasStorageAccess(context)) }
    var candidates by remember { mutableStateOf<List<AudioFolder>>(emptyList()) }

    LaunchedEffect(access) {
        if (access) candidates = withContext(Dispatchers.IO) { findAudioFolders(storageRoots(context)) }
    }

    LazyColumn(Modifier.fillMaxSize().background(c.background).safeDrawingPadding()) {
        item {
            Text("Escaneo", style = t.tabTitle, color = c.text, modifier = Modifier.padding(20.dp))
            if (!access) {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton("Dar acceso", { context.startActivity(allFilesAccessIntent(context)) })
                    OutlineButton("Comprobar", { access = hasStorageAccess(context) })
                }
            }
            SectionHeader("Carpetas con audio")
        }
        items(candidates) { f ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.dir.path.substringAfter("/0/"), style = t.body, color = c.text)
                    Text("${f.audioFiles} archivos", style = t.meta, color = c.textSecondary)
                }
                val added = folders.any { it.path == f.dir.path }
                OutlineButton(if (added) "Quitar" else "Añadir", {
                    scope.launch {
                        if (added) app.database.folders().removeFolder(f.dir.path)
                        else app.database.folders().addFolder(LibraryFolder(f.dir.path))
                    }
                })
            }
        }
        item {
            SectionHeader("Biblioteca")
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton("Escanear", { app.scanner.start(full = false) })
                OutlineButton("Volver a buscar", { app.scanner.start(full = true) })
            }
            val status = when {
                state.running -> "Buscando… ${state.found} encontrados\n${state.currentFolder.orEmpty().substringAfter("/0/")}"
                state.error != null -> "Error: ${state.error}"
                state.finishedAt != null -> "${state.found} libros"
                else -> "${folders.size} carpetas"
            }
            Text(status, style = t.meta, color = c.textSecondary, modifier = Modifier.padding(20.dp))
            ListDivider()
        }
        items(books, key = { it.id }) { b ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cover(app.covers.file(b.id).path)
                Column(Modifier.weight(1f)) {
                    Text(b.title, style = t.row, color = if (b.inaccessible) c.textTertiary else c.text)
                    Text(listOfNotNull(b.author, b.narrator, b.series?.let { s -> s + (b.seriesPart?.let { " $it" } ?: "") }).joinToString(" · "), style = t.secondary, color = c.textSecondary)
                    Text("${hms(b.totalDurationMs)} · ${b.coverSource.name.lowercase()}${if (b.inaccessible) " · inaccesible" else ""}", style = t.meta, color = c.textSecondary)
                    Text(b.path.substringAfter("/0/"), style = t.label, color = c.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun Cover(path: String) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)).background(LectorTheme.colors.surface)) {
        if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun hms(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
}
