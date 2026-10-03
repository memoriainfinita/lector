package codelab.lector.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Preamplificación, ecualizador y limitador dentro de ExoPlayer (design.md › Reproducción).
 * Siempre activo para poder cambiar de neutro a ajustado sin reconfigurar: en neutro copia tal cual.
 * Los ajustes llegan desde el hilo principal; al cambiar, la cadena vieja y la nueva se funden ~20 ms.
 */
@OptIn(UnstableApi::class)
class SoundProcessor : BaseAudioProcessor() {

    @Volatile private var pending = SoundSettings()
    private var applied: SoundSettings? = null
    private var chain: SoundChain? = null
    private var fadingFrom: SoundChain? = null
    private var fadeFrame = 0
    private var fadeFrames = 0
    private var format = AudioFormat.NOT_SET
    private var work = FloatArray(0)
    private var old = FloatArray(0)

    fun setSettings(settings: SoundSettings) {
        pending = settings.clamped()
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        // Formato nuevo o salto: cadena nueva sin fundido (no hay audio previo con el que enlazar).
        format = inputAudioFormat
        applied = pending
        chain = SoundChain(pending, format.sampleRate, format.channelCount)
        fadingFrom = null
        fadeFrames = (format.sampleRate * FadeSeconds).toInt()
    }

    override fun onReset() {
        chain = null
        fadingFrom = null
        applied = null
        format = AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val current = chain ?: return passThrough(inputBuffer)
        val wanted = pending
        if (wanted != applied) {
            fadingFrom = current
            chain = SoundChain(wanted, format.sampleRate, format.channelCount)
            applied = wanted
            fadeFrame = 0
        }
        val active = chain!!
        val from = fadingFrom
        if (from == null && active.isNeutral) return passThrough(inputBuffer)

        val float = format.encoding == C.ENCODING_PCM_FLOAT
        val count = if (float) size / 4 else size / 2
        if (work.size < count) work = FloatArray(count)
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        for (i in 0 until count) work[i] = if (float) input.getFloat() else input.getShort() / 32768f

        if (from != null) {
            if (old.size < count) old = FloatArray(count)
            System.arraycopy(work, 0, old, 0, count)
            from.process(old, count)
            active.process(work, count)
            val channels = format.channelCount
            var i = 0
            while (i + channels <= count) {
                val t = (fadeFrame.toFloat() / fadeFrames).coerceAtMost(1f)
                for (c in 0 until channels) work[i + c] = old[i + c] * (1 - t) + work[i + c] * t
                fadeFrame++
                i += channels
            }
            if (fadeFrame >= fadeFrames) fadingFrom = null
        } else {
            active.process(work, count)
        }

        val out = replaceOutputBuffer(size)
        for (i in 0 until count) {
            if (float) out.putFloat(work[i])
            else out.putShort((work[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        }
        out.flip()
    }

    private fun passThrough(inputBuffer: ByteBuffer) {
        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()
    }

    private companion object {
        const val FadeSeconds = 0.02
    }
}
