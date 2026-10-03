package codelab.lector.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin

class SoundDspTest {
    private val rate = 44_100

    /** Ganancia en dB de [f] para un seno de [hz], medida tras estabilizarse. */
    private fun gainDb(hz: Double, f: (Double) -> Double): Double {
        val n = rate // 1 s
        var inPeak = 0.0
        var outPeak = 0.0
        for (i in 0 until n) {
            val x = 0.01 * sin(2 * PI * hz * i / rate)
            val y = f(x)
            if (i > n / 2) {
                inPeak = maxOf(inPeak, abs(x)); outPeak = maxOf(outPeak, abs(y))
            }
        }
        return 20 * log10(outPeak / inPeak)
    }

    @Test
    fun peakBandReachesItsGainAtCenterAndLittleFarAway() {
        val band = EqBands[2] // 1 kHz
        val q = Biquad(band, 6f, rate, 1)
        assertEquals(6.0, gainDb(1_000.0) { q.process(it, 0) }, 0.2)
        val far = Biquad(band, 6f, rate, 1)
        assertTrue(gainDb(8_000.0) { far.process(it, 0) } < 1.0)
    }

    @Test
    fun shelvesReachTheirGainBeyondTheCorner() {
        val low = Biquad(EqBands[0], -9f, rate, 1)
        assertEquals(-9.0, gainDb(25.0) { low.process(it, 0) }, 0.5)
        val high = Biquad(EqBands[4], 8f, rate, 1)
        assertEquals(8.0, gainDb(18_000.0) { high.process(it, 0) }, 0.5)
        val highAtLow = Biquad(EqBands[4], 8f, rate, 1)
        assertTrue(abs(gainDb(200.0) { highAtLow.process(it, 0) }) < 0.5)
    }

    @Test
    fun limiterKeepsLoudSignalUnderTheCeiling() {
        val chain = SoundChain(SoundSettings(preampDb = 50f), rate, 2)
        val samples = FloatArray(2 * rate) { i -> (0.5 * sin(2 * PI * 440 * (i / 2) / rate)).toFloat() }
        chain.process(samples, samples.size)
        val peak = samples.maxOf { abs(it) }
        assertTrue("peak $peak", peak <= Limiter.Ceiling + 1e-6)
        assertTrue("peak $peak", peak > 0.8)
    }

    @Test
    fun neutralSettingsLeaveTheSignalUntouched() {
        val chain = SoundChain(SoundSettings(eqEnabled = true), rate, 1)
        assertTrue(chain.isNeutral)
        val samples = FloatArray(1000) { (0.3 * sin(it * 0.05)).toFloat() }
        val copy = samples.copyOf()
        chain.process(samples, samples.size)
        assertEquals(copy.toList(), samples.toList())
    }

    @Test
    fun disabledEqualizerIgnoresBandsAndValuesAreClamped() {
        assertTrue(SoundChain(SoundSettings(eqEnabled = false, bandsDb = List(5) { 6f }), rate, 1).isNeutral)
        assertFalse(SoundChain(SoundSettings(preampDb = -3f), rate, 1).isNeutral)
        val s = SoundSettings(preampDb = 80f, bandsDb = listOf(20f, -20f)).clamped()
        assertEquals(50f, s.preampDb)
        assertEquals(listOf(12f, -12f, 0f, 0f, 0f), s.bandsDb)
    }
}
