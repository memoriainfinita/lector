package codelab.lector.ui

import java.util.Locale

/** Tiempo de reproducción: "h:mm:ss" desde una hora, si no "m:ss". */
fun formatDuration(ms: Long): String {
    val total = ms.coerceAtLeast(0) / 1000
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
}

/** Velocidad con dos decimales como mucho: 1.0x, 1.25x, 0.8x. */
fun formatSpeed(speed: Float): String {
    val hundredths = Math.round(speed * 100)
    val text = if (hundredths % 10 == 0) String.format(Locale.ROOT, "%.1f", hundredths / 100f)
    else String.format(Locale.ROOT, "%.2f", hundredths / 100f)
    return "${text}x"
}

/** Tamaño con dos decimales, en unidades de 1000 como Android: "200.42 MB", "1.27 GB". */
fun formatSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.coerceAtLeast(0).toDouble()
    var unit = 0
    while (value >= 1000 && unit < units.lastIndex) {
        value /= 1000
        unit++
    }
    return if (unit == 0) "${bytes.coerceAtLeast(0)} B" else String.format(Locale.ROOT, "%.2f %s", value, units[unit])
}

/** Decibelios con signo: +3 dB, 0 dB, −1.5 dB (signo menos tipográfico). */
fun formatDb(db: Float): String {
    val tenths = Math.round(db * 10)
    val abs = kotlin.math.abs(tenths)
    val number = if (abs % 10 == 0) "${abs / 10}" else String.format(Locale.ROOT, "%.1f", abs / 10f)
    return when {
        tenths > 0 -> "+$number"
        tenths < 0 -> "−$number"
        else -> number
    }
}
