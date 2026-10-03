package codelab.lector.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Ajustes de sonido: globales o propios de un libro. */
data class SoundSettings(
    val preampDb: Float = 0f,
    val eqEnabled: Boolean = false,
    val bandsDb: List<Float> = List(EqBands.size) { 0f },
) {
    /** Valores dentro de sus rangos y con el número de bandas correcto. */
    fun clamped() = copy(
        preampDb = preampDb.coerceIn(PreampMinDb, PreampMaxDb),
        bandsDb = List(EqBands.size) { (bandsDb.getOrElse(it) { 0f }).coerceIn(-BandMaxDb, BandMaxDb) },
    )

    companion object {
        const val PreampMinDb = -20f
        const val PreampMaxDb = 50f
        const val BandMaxDb = 12f
    }
}

enum class FilterKind { LOW_SHELF, PEAK, HIGH_SHELF }

data class EqBand(val frequencyHz: Float, val kind: FilterKind)

/** Cinco bandas pensadas para voz (design.md › Reproducción). */
val EqBands = listOf(
    EqBand(100f, FilterKind.LOW_SHELF),
    EqBand(300f, FilterKind.PEAK),
    EqBand(1_000f, FilterKind.PEAK),
    EqBand(3_000f, FilterKind.PEAK),
    EqBand(8_000f, FilterKind.HIGH_SHELF),
)

/**
 * Filtro biquad (RBJ Audio EQ Cookbook), forma directa I, un estado por canal.
 * Pendiente de estantería S = 1; Q de los picos 1.0 (algo más de una octava).
 */
class Biquad(band: EqBand, gainDb: Float, sampleRate: Int, channels: Int) {
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a1: Double
    private val a2: Double
    private val x1 = DoubleArray(channels)
    private val x2 = DoubleArray(channels)
    private val y1 = DoubleArray(channels)
    private val y2 = DoubleArray(channels)

    init {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2 * PI * band.frequencyHz.coerceAtMost(sampleRate * 0.45f) / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val c: DoubleArray = when (band.kind) {
            FilterKind.PEAK -> {
                val alpha = sinW / (2 * PeakQ)
                doubleArrayOf(1 + alpha * a, -2 * cosW, 1 - alpha * a, 1 + alpha / a, -2 * cosW, 1 - alpha / a)
            }
            FilterKind.LOW_SHELF, FilterKind.HIGH_SHELF -> {
                val alpha = sinW / 2 * sqrt(2.0)
                val sq = 2 * sqrt(a) * alpha
                if (band.kind == FilterKind.LOW_SHELF) doubleArrayOf(
                    a * ((a + 1) - (a - 1) * cosW + sq), 2 * a * ((a - 1) - (a + 1) * cosW), a * ((a + 1) - (a - 1) * cosW - sq),
                    (a + 1) + (a - 1) * cosW + sq, -2 * ((a - 1) + (a + 1) * cosW), (a + 1) + (a - 1) * cosW - sq,
                ) else doubleArrayOf(
                    a * ((a + 1) + (a - 1) * cosW + sq), -2 * a * ((a - 1) + (a + 1) * cosW), a * ((a + 1) + (a - 1) * cosW - sq),
                    (a + 1) - (a - 1) * cosW + sq, 2 * ((a - 1) - (a + 1) * cosW), (a + 1) - (a - 1) * cosW - sq,
                )
            }
        }
        b0 = c[0] / c[3]; b1 = c[1] / c[3]; b2 = c[2] / c[3]; a1 = c[4] / c[3]; a2 = c[5] / c[3]
    }

    fun process(x: Double, channel: Int): Double {
        val y = b0 * x + b1 * x1[channel] + b2 * x2[channel] - a1 * y1[channel] - a2 * y2[channel]
        x2[channel] = x1[channel]; x1[channel] = x
        y2[channel] = y1[channel]; y1[channel] = y
        return y
    }

    private companion object {
        const val PeakQ = 1.0
    }
}

/**
 * Limitador de picos sin anticipación: si una muestra superaría [ceiling], baja la ganancia al
 * instante; la recupera despacio (≈ 200 ms). La ganancia es común a todos los canales.
 */
class Limiter(sampleRate: Int, private val ceiling: Double = Ceiling) {
    private var gain = 1.0
    private val release = 1 - exp(-1.0 / (ReleaseSeconds * sampleRate))

    /** Procesa una trama (todas las muestras de un instante) en su sitio. */
    fun process(frame: DoubleArray) {
        var peak = 0.0
        for (v in frame) peak = maxOf(peak, abs(v))
        // Recupera hacia 1 y nunca deja que esta trama pase del techo.
        gain += (1.0 - gain) * release
        if (peak * gain > ceiling) gain = ceiling / peak
        for (i in frame.indices) frame[i] *= gain
    }

    companion object {
        /** −1 dBFS. */
        val Ceiling = 10.0.pow(-1.0 / 20)
        const val ReleaseSeconds = 0.2
    }
}

/** Cadena completa sobre muestras en coma flotante intercaladas: preamplificación, ecualizador, limitador. */
class SoundChain(settings: SoundSettings, sampleRate: Int, private val channels: Int) {
    private val s = settings.clamped()
    private val preamp = 10.0.pow(s.preampDb / 20.0)
    private val filters = if (s.eqEnabled) {
        EqBands.zip(s.bandsDb).filter { (_, g) -> g != 0f }.map { (b, g) -> Biquad(b, g, sampleRate, channels) }
    } else emptyList()
    private val limiter = Limiter(sampleRate)
    private val frame = DoubleArray(channels)

    /** Sin cambios en la señal: el procesador puede quedarse inactivo. */
    val isNeutral: Boolean = s.preampDb == 0f && filters.isEmpty()

    /** Procesa [samples] (tramas completas) en su sitio. */
    fun process(samples: FloatArray, count: Int) {
        var i = 0
        while (i + channels <= count) {
            for (c in 0 until channels) {
                var v = samples[i + c] * preamp
                for (f in filters) v = f.process(v, c)
                frame[c] = v
            }
            limiter.process(frame)
            for (c in 0 until channels) samples[i + c] = frame[c].toFloat()
            i += channels
        }
    }
}
