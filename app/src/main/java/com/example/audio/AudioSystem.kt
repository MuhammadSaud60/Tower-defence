package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class GameSound {
    TOWER_FIRE_BULLET,
    TOWER_FIRE_CANNON,
    TOWER_FIRE_RAPID,
    ENEMY_HIT,
    ENEMY_DEATH,
    CANNON_EXPLOSION,
    WAVE_START,
    WAVE_CLEAR,
    BOSS_APPEARANCE,
    VICTORY,
    GAME_OVER
}

interface AudioPlayer {
    var isMuted: Boolean
    fun playSound(sound: GameSound)
}

/**
 * High-performance, clean arcade sound synthesizer for mobile tower defense.
 * Uses 16-bit PCM static AudioTracks for instant, zero-latency playback without
 * device vibration or telephone DTMF tones.
 * Includes cooldown throttling to prevent audio congestion on rapid firing.
 */
class AndroidAudioPlayer : AudioPlayer {
    override var isMuted: Boolean = false
    private val tracks = mutableMapOf<GameSound, AudioTrack>()
    private val lastPlayTimes = mutableMapOf<GameSound, Long>()

    init {
        try {
            pregenerateSounds()
        } catch (e: Exception) {
            Log.w("AudioPlayer", "AudioTrack synthesis initialization error: ${e.message}")
        }
    }

    private fun pregenerateSounds() {
        val sampleRate = 22050

        // 1. Machine Gun: Crisp rapid punch/pop (850Hz -> 180Hz)
        val bulletData = synthesizeChirp(
            sampleRate = sampleRate,
            durationSec = 0.042f,
            startFreq = 850f,
            endFreq = 180f,
            decayRate = 65f,
            noiseMix = 0.15f,
            peakVolume = 0.65f
        )
        registerTrack(GameSound.TOWER_FIRE_BULLET, bulletData, sampleRate)

        // 2. Cannon: Deep, heavy low-frequency punchy boom (140Hz -> 38Hz)
        val cannonData = synthesizeCannonBoom(
            sampleRate = sampleRate,
            durationSec = 0.18f,
            startFreq = 140f,
            endFreq = 38f,
            decayRate = 18f,
            peakVolume = 0.85f
        )
        registerTrack(GameSound.TOWER_FIRE_CANNON, cannonData, sampleRate)

        // 3. Rapid Fire: High-tech snappy laser/blip (1400Hz -> 650Hz)
        val rapidData = synthesizeChirp(
            sampleRate = sampleRate,
            durationSec = 0.032f,
            startFreq = 1400f,
            endFreq = 650f,
            decayRate = 80f,
            noiseMix = 0.05f,
            peakVolume = 0.55f
        )
        registerTrack(GameSound.TOWER_FIRE_RAPID, rapidData, sampleRate)

        // 4. Enemy Hit: Subtle wooden/metal thud (320Hz -> 110Hz, soft)
        val hitData = synthesizeChirp(
            sampleRate = sampleRate,
            durationSec = 0.022f,
            startFreq = 320f,
            endFreq = 110f,
            decayRate = 85f,
            noiseMix = 0.08f,
            peakVolume = 0.35f
        )
        registerTrack(GameSound.ENEMY_HIT, hitData, sampleRate)

        // 5. Enemy Death: Crunchy pop/squish (500Hz -> 90Hz)
        val deathData = synthesizeDeathCrunch(
            sampleRate = sampleRate,
            durationSec = 0.075f,
            peakVolume = 0.60f
        )
        registerTrack(GameSound.ENEMY_DEATH, deathData, sampleRate)

        // 6. Cannon Explosion: Low rumble + shaped noise burst
        val explosionData = synthesizeExplosion(
            sampleRate = sampleRate,
            durationSec = 0.22f,
            peakVolume = 0.80f
        )
        registerTrack(GameSound.CANNON_EXPLOSION, explosionData, sampleRate)

        // 7. Wave Start: Two-tone ascending chime (C5 -> E5)
        val waveStartData = synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(523.25f, 0.11f), Pair(659.25f, 0.13f)),
            peakVolume = 0.65f
        )
        registerTrack(GameSound.WAVE_START, waveStartData, sampleRate)

        // 8. Wave Clear: Three-tone victory chime (C5 -> E5 -> G5)
        val waveClearData = synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(523.25f, 0.09f), Pair(659.25f, 0.09f), Pair(783.99f, 0.16f)),
            peakVolume = 0.70f
        )
        registerTrack(GameSound.WAVE_CLEAR, waveClearData, sampleRate)

        // 9. Boss Appearance: Deep ominous horn rumble (82Hz swelling with harmonics)
        val bossData = synthesizeBossHorn(
            sampleRate = sampleRate,
            durationSec = 0.45f,
            peakVolume = 0.85f
        )
        registerTrack(GameSound.BOSS_APPEARANCE, bossData, sampleRate)

        // 10. Victory: Fanfare arpeggio (C5 - E5 - G5 - C6)
        val victoryData = synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(
                Pair(523.25f, 0.11f),
                Pair(659.25f, 0.11f),
                Pair(783.99f, 0.11f),
                Pair(1046.50f, 0.22f)
            ),
            peakVolume = 0.75f
        )
        registerTrack(GameSound.VICTORY, victoryData, sampleRate)

        // 11. Game Over: Somber descending chime
        val gameOverData = synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(
                Pair(440.00f, 0.11f),
                Pair(392.00f, 0.11f),
                Pair(349.23f, 0.11f),
                Pair(311.13f, 0.16f)
            ),
            peakVolume = 0.70f
        )
        registerTrack(GameSound.GAME_OVER, gameOverData, sampleRate)
    }

    private fun registerTrack(sound: GameSound, pcmData: ByteArray, sampleRate: Int) {
        try {
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            tracks[sound] = track
        } catch (e: Exception) {
            // Graceful fallback on devices/tests where AudioTrack is unavailable
        }
    }

    companion object {
        var isGlobalMuted: Boolean = false
        fun isSoundEnabled(): Boolean = !isGlobalMuted
        fun setSoundEnabled(enabled: Boolean) {
            isGlobalMuted = !enabled
        }
    }

    override fun playSound(sound: GameSound) {
        if (isMuted || isGlobalMuted) return

        // Event-based cooldown throttling to prevent audio congestion and buzzing
        val now = System.currentTimeMillis()
        val cooldown = when (sound) {
            GameSound.TOWER_FIRE_BULLET -> 75L
            GameSound.TOWER_FIRE_RAPID -> 60L
            GameSound.ENEMY_HIT -> 90L
            GameSound.ENEMY_DEATH -> 80L
            else -> 0L
        }

        val last = lastPlayTimes[sound] ?: 0L
        if (now - last < cooldown) return
        lastPlayTimes[sound] = now

        try {
            val track = tracks[sound] ?: return
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (e: Exception) {
            // Non-fatal if audio hardware is busy
        }
    }

    // Audio synthesis utilities
    private fun synthesizeChirp(
        sampleRate: Int,
        durationSec: Float,
        startFreq: Float,
        endFreq: Float,
        decayRate: Float,
        noiseMix: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val progress = (i.toFloat() / totalSamples).coerceIn(0f, 1f)
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / sampleRate

            val sine = sin(phase).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f)
            val rawSample = (sine * (1f - noiseMix) + noise * noiseMix)
            val envelope = exp(-t * decayRate) * peakVolume

            val sampleVal = (rawSample * envelope).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun synthesizeCannonBoom(
        sampleRate: Int,
        durationSec: Float,
        startFreq: Float,
        endFreq: Float,
        decayRate: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val progress = (i.toFloat() / totalSamples).coerceIn(0f, 1f)
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / sampleRate

            // Punchy bass with subtle cubic distortion for weight
            var sine = sin(phase).toFloat()
            sine = (1.4f * sine - 0.4f * sine * sine * sine).coerceIn(-1f, 1f)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.12f * exp(-t * 24f)
            val envelope = exp(-t * decayRate) * peakVolume

            val sampleVal = ((sine + noise) * envelope).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun synthesizeDeathCrunch(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val progress = (i.toFloat() / totalSamples).coerceIn(0f, 1f)
            val freq = 500f * (1f - progress * 0.8f)
            phase += 2.0 * PI * freq / sampleRate

            val sine = sin(phase).toFloat() * 0.7f
            val noise = (Random.nextFloat() * 2f - 1f) * 0.3f
            val envelope = exp(-t * 35f) * peakVolume

            val sampleVal = ((sine + noise) * envelope).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun synthesizeExplosion(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 68.0 / sampleRate

            val subBass = sin(phase).toFloat() * 0.5f
            val noise = (Random.nextFloat() * 2f - 1f) * 0.5f
            val envelope = exp(-t * 14f) * peakVolume

            val sampleVal = ((subBass + noise) * envelope).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun synthesizeMelody(
        sampleRate: Int,
        tones: List<Pair<Float, Float>>,
        peakVolume: Float
    ): ByteArray {
        val totalSec = tones.sumOf { it.second.toDouble() }.toFloat()
        val totalSamples = (sampleRate * totalSec).toInt()
        val bytes = ByteArray(totalSamples * 2)

        var sampleIndex = 0
        for ((freq, dur) in tones) {
            val toneSamples = (sampleRate * dur).toInt()
            var phase = 0.0
            for (i in 0 until toneSamples) {
                if (sampleIndex >= totalSamples) break
                val t = i.toFloat() / sampleRate
                phase += 2.0 * PI * freq / sampleRate

                val sine = sin(phase).toFloat()
                val harmonic = sin(phase * 2.0).toFloat() * 0.25f
                val env = exp(-t * 8f) * peakVolume

                val sampleVal = ((sine + harmonic) * env).coerceIn(-1f, 1f)
                val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

                bytes[sampleIndex * 2] = (pcm16 and 0xFF).toByte()
                bytes[sampleIndex * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
                sampleIndex++
            }
        }
        return bytes
    }

    private fun synthesizeBossHorn(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec
            phase1 += 2.0 * PI * 82.0 / sampleRate
            phase2 += 2.0 * PI * 123.0 / sampleRate

            val swell = if (progress < 0.25f) progress / 0.25f else exp(-(progress - 0.25f) * 4f)
            val horn = (sin(phase1) * 0.7 + sin(phase2) * 0.3).toFloat()

            val sampleVal = (horn * swell * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }
}

