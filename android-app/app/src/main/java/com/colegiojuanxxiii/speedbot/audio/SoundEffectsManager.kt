package com.colegiojuanxxiii.speedbot.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sintetizador de audio nativo con AudioTrack para bocina continua,
 * feedback de botones/motor y alerta de tiempo agotado (idéntico al audio web).
 */
class SoundEffectsManager {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var isHornPlaying = false
    private var hornJob: Job? = null
    private var hornTrack: AudioTrack? = null

    fun startHorn() {
        if (isHornPlaying) return
        isHornPlaying = true

        hornJob = scope.launch {
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuffer)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                isHornPlaying = false
                return@launch
            }

            hornTrack = track
            val chunkSamples = minBuffer / 2
            val buffer = ShortArray(chunkSamples)
            var phase1 = 0.0
            var phase2 = 0.0
            val step1 = 420.0 / sampleRate
            val step2 = 435.0 / sampleRate

            try {
                track.play()
                while (isActive && isHornPlaying) {
                    for (i in 0 until chunkSamples) {
                        // Onda diente de sierra (420Hz) + onda cuadrada (435Hz)
                        val saw = 2.0 * (phase1 - kotlin.math.floor(phase1 + 0.5))
                        val square = if (phase2 < 0.5) 1.0 else -1.0
                        val mixed = ((saw * 0.5 + square * 0.5) * 0.25 * Short.MAX_VALUE)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        buffer[i] = mixed.toShort()

                        phase1 = (phase1 + step1) % 1.0
                        phase2 = (phase2 + step2) % 1.0
                    }
                    track.write(buffer, 0, chunkSamples)
                }
            } catch (_: Exception) {
                // Ignorar error de ciclo de vida de audio
            } finally {
                try {
                    track.pause()
                    track.flush()
                    track.release()
                } catch (_: Exception) {
                }
                if (hornTrack === track) {
                    hornTrack = null
                }
            }
        }
    }

    fun stopHorn() {
        isHornPlaying = false
        hornJob?.cancel()
        hornJob = null
    }

    fun playBeep(freqHz: Double = 880.0, durationMs: Int = 100) {
        scope.launch {
            playTone(
                startFreq = freqHz,
                endFreq = freqHz,
                durationMs = durationMs,
                volume = 0.16,
                triangle = false
            )
        }
    }

    fun playWarning() {
        scope.launch {
            playTone(
                startFreq = 300.0,
                endFreq = 150.0,
                durationMs = 600,
                volume = 0.28,
                triangle = true
            )
        }
    }

    private fun playTone(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Double,
        triangle: Boolean
    ) {
        val totalSamples = ((sampleRate * durationMs) / 1000).coerceAtLeast(1)
        val samples = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = startFreq + (endFreq - startFreq) * t
            phase += freq / sampleRate
            val frac = phase % 1.0

            val wave = if (triangle) {
                2.0 * kotlin.math.abs(2.0 * frac - 1.0) - 1.0
            } else {
                sin(2.0 * PI * frac)
            }

            // Envolvente suave al inicio y desvanecimiento exponencial al final
            val attack = (i / (sampleRate * 0.005)).coerceAtMost(1.0)
            val decay = exp(-3.5 * t)
            val amp = wave * volume * attack * decay
            samples[i] = (amp * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        val byteCount = totalSamples * 2
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteCount)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (_: Exception) {
            return
        }

        try {
            track.write(samples, 0, totalSamples)
            track.play()
            Thread.sleep(durationMs.toLong() + 30L)
        } catch (_: Exception) {
        } finally {
            try {
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        stopHorn()
        scope.cancel()
    }
}
