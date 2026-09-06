package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(val displayName: String, val icon: String) {
    NONE("Off", "🔇"),
    RAIN("Gentle Rain", "🌧️"),
    NIGHT_BREEZE("Night Breeze", "🍃"),
    COZY_FIRE("Cozy Fireplace", "🔥"),
    ZEN_WAVE("Zen Waves", "🌊")
}

object AmbientSoundPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var playerJob: Job? = null
    var currentSound: AmbientSoundType = AmbientSoundType.NONE
        private set
    var volumeLevel: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    fun play(sound: AmbientSoundType) {
        if (sound == currentSound && isPlaying) return
        stop()
        if (sound == AmbientSoundType.NONE) return

        currentSound = sound
        isPlaying = true

        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(volumeLevel)
            audioTrack?.play()

            playerJob = CoroutineScope(Dispatchers.Default).launch {
                val buffer = ShortArray(bufferSize / 2)
                var phase1 = 0.0
                var phase2 = 0.0
                var b0 = 0.0
                var b1 = 0.0
                var b2 = 0.0

                while (isActive && isPlaying) {
                    when (sound) {
                        AmbientSoundType.RAIN -> {
                            // Pink noise + subtle raindrops
                            for (i in buffer.indices) {
                                val white = (Random.nextDouble() * 2 - 1)
                                b0 = 0.99886 * b0 + white * 0.0555179
                                b1 = 0.99332 * b1 + white * 0.0750759
                                b2 = 0.96900 * b2 + white * 0.1538520
                                var pink = b0 + b1 + b2 + white * 0.5362
                                if (Random.nextInt(1200) == 0) pink += (Random.nextDouble() * 4) // Drop
                                buffer[i] = (pink * 1800).toInt().coerceIn(-32768, 32767).toShort()
                            }
                        }
                        AmbientSoundType.NIGHT_BREEZE -> {
                            // Low modulated soft rumble
                            for (i in buffer.indices) {
                                val white = (Random.nextDouble() * 2 - 1)
                                b0 = 0.995 * b0 + white * 0.025
                                phase1 += 0.0003
                                val mod = 0.5 + 0.5 * sin(phase1)
                                buffer[i] = (b0 * 3500 * mod).toInt().coerceIn(-32768, 32767).toShort()
                            }
                        }
                        AmbientSoundType.COZY_FIRE -> {
                            // Soft hiss + crackles
                            for (i in buffer.indices) {
                                val white = (Random.nextDouble() * 2 - 1)
                                b0 = 0.98 * b0 + white * 0.08
                                var crackle = 0.0
                                if (Random.nextInt(800) == 0) crackle = (Random.nextDouble() * 6 - 3)
                                buffer[i] = ((b0 * 1200) + (crackle * 5000)).toInt().coerceIn(-32768, 32767).toShort()
                            }
                        }
                        AmbientSoundType.ZEN_WAVE -> {
                            // Harmonic ocean wave surge
                            for (i in buffer.indices) {
                                val white = (Random.nextDouble() * 2 - 1)
                                b0 = 0.992 * b0 + white * 0.04
                                phase1 += 0.0005
                                phase2 += 0.0002
                                val surge = (0.5 + 0.5 * sin(phase1)) * (0.6 + 0.4 * sin(phase2))
                                buffer[i] = (b0 * 4200 * surge).toInt().coerceIn(-32768, 32767).toShort()
                            }
                        }
                        AmbientSoundType.NONE -> break
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                }
            }
        } catch (_: Exception) {
            stop()
        }
    }

    fun stop() {
        isPlaying = false
        currentSound = AmbientSoundType.NONE
        playerJob?.cancel()
        playerJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
    }
}
