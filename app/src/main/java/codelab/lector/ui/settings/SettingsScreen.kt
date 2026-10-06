package codelab.lector.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.formatSpeed
import codelab.lector.ui.theme.LectorTheme
import codelab.lector.ui.theme.ThemeMode
import java.time.format.DateTimeFormatter

private val SpeedPresets = listOf(0.8f, 1.0f, 1.25f, 1.5f, 2.0f)
private val RewindPresets = listOf(0f, 2f, 3f, 5f, 10f)

private enum class Picker { REWIND, NEW_BOOK_SPEED }

/**
 * Ajustes (lienzo "Ajustes"). Las filas de funciones que aún no existen salen atenuadas e inactivas
 * (design.md › Pantallas › Ajustes).
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSound: () -> Unit,
    onAppearance: () -> Unit,
    onLibrary: () -> Unit,
    onButtons: () -> Unit,
    onRemote: () -> Unit,
    onSleep: () -> Unit,
    onTags: () -> Unit,
) {
    val context = LocalContext.current
    val c = LectorTheme.colors
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<Picker?>(null) }
    val seconds = { v: Float -> context.getString(R.string.seconds_value, v.toInt()) }

    SettingsPage(stringResource(R.string.settings), onBack) {
        SectionHeader(stringResource(R.string.settings_playback))
        SwitchRow(stringResource(R.string.auto_next_book), playback.autoNextBook, viewModel::setAutoNextBook, note = stringResource(R.string.auto_next_book_note))
        SwitchRow(stringResource(R.string.next_file_from_position), playback.nextFileFromPosition, viewModel::setNextFileFromPosition, note = stringResource(R.string.next_file_from_position_note))
        ValueRow(
            stringResource(R.string.rewind_on_resume),
            seconds(playback.rewindOnResumeMs / 1_000f),
            { picker = Picker.REWIND },
            note = stringResource(R.string.rewind_on_resume_note),
        )
        SwitchRow(stringResource(R.string.play_on_open), playback.playOnOpen, viewModel::setPlayOnOpen)
        ValueRow(stringResource(R.string.new_book_speed), formatSpeed(playback.newBookSpeed), { picker = Picker.NEW_BOOK_SPEED })

        SectionHeader(stringResource(R.string.settings_section_sound))
        LinkRow(stringResource(R.string.equalizer), onSound, note = stringResource(R.string.equalizer_note))

        SectionHeader(stringResource(R.string.settings_sleep))
        val time = DateTimeFormatter.ofPattern("HH:mm")
        LinkRow(
            stringResource(R.string.sleep_timer_and_schedule),
            onSleep,
            note = if (playback.sleepAuto) {
                stringResource(R.string.sleep_auto_summary, playback.sleepAutoMinutes, playback.sleepFrom.format(time), playback.sleepTo.format(time))
            } else {
                null
            },
        )

        SectionHeader(stringResource(R.string.settings_buttons))
        LinkRow(stringResource(R.string.skip_buttons), onButtons, note = stringResource(R.string.skip_buttons_note))
        LinkRow(stringResource(R.string.remote_buttons), onRemote, note = stringResource(R.string.remote_buttons_note))

        SectionHeader(stringResource(R.string.settings_headset))
        SwitchRow(stringResource(R.string.pause_on_unplug), playback.pauseOnUnplug, viewModel::setPauseOnUnplug)
        SwitchRow(
            stringResource(R.string.resume_on_replug),
            playback.resumeOnReplug,
            viewModel::setResumeOnReplug,
            note = stringResource(R.string.resume_on_replug_note),
            enabled = playback.pauseOnUnplug,
        )

        SectionHeader(stringResource(R.string.settings_library))
        LinkRow(stringResource(R.string.library_folders), onLibrary, note = foldersSummary(folders))
        SettingRow(
            stringResource(R.string.rescan_books),
            note = if (scan.running && !scan.quiet) stringResource(R.string.scanning_found, scan.found) else null,
            enabled = !(scan.running && !scan.quiet),
            onClick = viewModel::rescan,
        ) {
            Icon(painterResource(R.drawable.ic_reset), null, Modifier.size(18.dp), tint = c.textSecondary)
        }
        SwitchRow(stringResource(R.string.show_covers), appearance.showCovers, viewModel::setShowCovers)

        SectionHeader(stringResource(R.string.tab_bookmarks))
        LinkRow(stringResource(R.string.settings_tags), onTags, note = stringResource(R.string.manage_tags_note))
        SwitchRow(stringResource(R.string.open_sheet_on_bookmark), playback.openSheetOnMark, viewModel::setOpenSheetOnMark, note = stringResource(R.string.open_sheet_on_bookmark_note))

        SectionHeader(stringResource(R.string.settings_appearance))
        ValueRow(stringResource(R.string.theme), stringResource(themeLabel(appearance.mode)), onAppearance, mono = false)
        SwitchRow(
            stringResource(R.string.theme_by_time),
            appearance.schedule.enabled,
            { viewModel.setSchedule(appearance.schedule.copy(enabled = it)) },
            note = stringResource(R.string.theme_by_time_note, appearance.schedule.lightAt.format(time), appearance.schedule.darkAt.format(time)),
        )
        SettingRow(stringResource(R.string.accent_color), onClick = onAppearance) { Swatch(c.accent) }
        ValueRow(stringResource(R.string.language), stringResource(languageLabel()), onAppearance, mono = false)

        SectionHeader(stringResource(R.string.settings_notification))
        SwitchRow(stringResource(R.string.cover_on_lock_screen), playback.coverOutside, viewModel::setCoverOutside)

        SectionHeader(stringResource(R.string.settings_data))
        LinkRow(stringResource(R.string.export_all), {}, note = stringResource(R.string.export_all_note), enabled = false, chevron = false)
        LinkRow(stringResource(R.string.import_merge), {}, note = stringResource(R.string.import_merge_note), enabled = false, chevron = false)

        SectionHeader(stringResource(R.string.settings_system))
        LinkRow(stringResource(R.string.keep_screen_off), { openBatterySettings(context) }, note = stringResource(R.string.keep_screen_off_note))
        LinkRow(stringResource(R.string.permissions), { openAppDetails(context) })
        SettingRow(stringResource(R.string.about), note = stringResource(R.string.about_version, appVersion(context)))
    }

    when (picker) {
        Picker.REWIND -> ValuePickerSheet(
            title = stringResource(R.string.rewind_on_resume),
            subtitle = stringResource(R.string.rewind_on_resume_note),
            initial = playback.rewindOnResumeMs / 1_000f,
            step = 1f,
            range = 0f..30f,
            presets = RewindPresets,
            format = seconds,
            onDone = { viewModel.setRewindOnResume(it.toInt()) },
            onDismiss = { picker = null },
        )
        Picker.NEW_BOOK_SPEED -> ValuePickerSheet(
            title = stringResource(R.string.new_book_speed),
            initial = playback.newBookSpeed,
            step = 0.05f,
            range = 0.5f..3.5f,
            presets = SpeedPresets,
            format = ::formatSpeed,
            onDone = viewModel::setNewBookSpeed,
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

/** "Audiobooks" con una carpeta; "Audiobooks · 3 carpetas" con varias; nada sin carpetas. */
@Composable
private fun foldersSummary(folders: List<String>): String? {
    val first = folders.firstOrNull()?.substringAfterLast('/') ?: return null
    return if (folders.size == 1) first
    else stringResource(R.string.dot_join, first, pluralStringResource(R.plurals.folders_count, folders.size, folders.size))
}

fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SYSTEM -> R.string.theme_system
}

/** Idioma elegido en la app (AppCompat); sin elección, el del sistema. */
fun languageLabel(): Int {
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return when {
        tag.startsWith("es") -> R.string.language_es
        tag.startsWith("en") -> R.string.language_en
        else -> R.string.theme_system
    }
}

/**
 * "Seguir con la pantalla apagada": si la app aún tiene la optimización de batería, el diálogo de
 * Android para quitarla; si ya no la tiene, la ficha de la app (en Xiaomi, ahí está el ahorro de batería).
 */
@SuppressLint("BatteryLife")
private fun openBatterySettings(context: Context) {
    val power = context.getSystemService(PowerManager::class.java)
    val intent = if (power?.isIgnoringBatteryOptimizations(context.packageName) == false) {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    } else {
        appDetailsIntent(context)
    }
    runCatching { context.startActivity(intent) }.onFailure { openAppDetails(context) }
}

private fun appDetailsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

private fun openAppDetails(context: Context) {
    runCatching { context.startActivity(appDetailsIntent(context)) }
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
