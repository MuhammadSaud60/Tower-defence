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
    MACHINE_GUN_FIRE,
    CANNON_FIRE,
    RAPID_FIRE,
    ENEMY_HIT,
    ENEMY_DEATH,
    CANNON_IMPACT,
    BOSS_IMPACT,
    BUILD_TOWER,
    WAVE_START,
    WAVE_CLEAR,
    BOSS_APPEARANCE,
    VICTORY,
    GAME_OVER;

    companion object {
        // Compatibility aliases
        @JvmField val TOWER_FIRE_BULLET = MACHINE_GUN_FIRE
        @JvmField val TOWER_FIRE_CANNON = CANNON_FIRE
        @JvmField val TOWER_FIRE_RAPID = RAPID_FIRE
        @JvmField val CANNON_EXPLOSION = CANNON_IMPACT
    }
}

interface AudioPlayer {
    var isMuted: Boolean
    fun playSound(sound: GameSound)
    fun machineGunFire() = playSound(GameSound.MACHINE_GUN_FIRE)
    fun cannonFire() = playSound(GameSound.CANNON_FIRE)
    fun rapidFire() = playSound(GameSound.RAPID_FIRE)
    fun enemyHit() = playSound(GameSound.ENEMY_HIT)
    fun enemyDeath() = playSound(GameSound.ENEMY_DEATH)
    fun cannonImpact() = playSound(GameSound.CANNON_IMPACT)
    fun bossImpact() = playSound(GameSound.BOSS_IMPACT)
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

        // 1. Machine Gun: Sharp mechanical gunshot crack + bolt clack + low punch (No balloon pop!)
        val bulletData = synthesizeMachineGunCrack(
            sampleRate = sampleRate,
            durationSec = 0.065f,
            peakVolume = 0.65f
        )
        registerTrack(GameSound.MACHINE_GUN_FIRE, bulletData, sampleRate)

        // 2. Cannon: Deep heavy artillery cannon boom with saturated low-end blast + expanding gas roar
        val cannonData = synthesizeHeavyCannonBoom(
            sampleRate = sampleRate,
            durationSec = 0.32f,
            peakVolume = 0.90f
        )
        registerTrack(GameSound.CANNON_FIRE, cannonData, sampleRate)

        // 3. Rapid Fire: Light, crisp, mechanical ratcheting snap - controlled & rhythmic without buzzing
        val rapidData = synthesizeRapidFireMechanical(
            sampleRate = sampleRate,
            durationSec = 0.038f,
            peakVolume = 0.40f
        )
        registerTrack(GameSound.RAPID_FIRE, rapidData, sampleRate)

        // 4. Enemy Hit: Subtle, quiet tactile impact smack
        val hitData = synthesizeEnemyHitTactile(
            sampleRate = sampleRate,
            durationSec = 0.024f,
            peakVolume = 0.22f
        )
        registerTrack(GameSound.ENEMY_HIT, hitData, sampleRate)

        // 5. Enemy Death: Mechanical collapse & defeat crunch
        val deathData = synthesizeEnemyDeathCrunch(
            sampleRate = sampleRate,
            durationSec = 0.085f,
            peakVolume = 0.50f
        )
        registerTrack(GameSound.ENEMY_DEATH, deathData, sampleRate)

        // 6. Cannon Impact: Heavy ground shockwave thump + debris crumble
        val cannonImpactData = synthesizeCannonImpact(
            sampleRate = sampleRate,
            durationSec = 0.24f,
            peakVolume = 0.75f
        )
        registerTrack(GameSound.CANNON_IMPACT, cannonImpactData, sampleRate)

        // 7. Boss Impact: Resonant metallic armor clang + kinetic thud
        val bossImpactData = synthesizeBossImpact(
            sampleRate = sampleRate,
            durationSec = 0.16f,
            peakVolume = 0.65f
        )
        registerTrack(GameSound.BOSS_IMPACT, bossImpactData, sampleRate)

        // 8. Build Tower: Mechanical lock / ratchet click
        val buildData = synthesizeBuildSound(
            sampleRate = sampleRate,
            durationSec = 0.06f,
            peakVolume = 0.55f
        )
        registerTrack(GameSound.BUILD_TOWER, buildData, sampleRate)

        // 9. Wave Start: Two-tone military bugle cue (C5 -> G5)
        val waveStartData = synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(523.25f, 0.10f), Pair(783.99f, 0.14f)),
            peakVolume = 0.65f
        )
        registerTrack(GameSound.WAVE_START, waveStartData, sampleRate)

        // 10. Wave Clear: Triumphant victory fanfare (C5 -> E5 -> G5 -> C6)
        val waveClearData = synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(523.25f, 0.08f), Pair(659.25f, 0.08f), Pair(783.99f, 0.08f), Pair(1046.50f, 0.16f)),
            peakVolume = 0.70f
        )
        registerTrack(GameSound.WAVE_CLEAR, waveClearData, sampleRate)

        // 11. Boss Appearance: Deep ominous war horn rumble
        val bossData = synthesizeBossHorn(
            sampleRate = sampleRate,
            durationSec = 0.45f,
            peakVolume = 0.85f
        )
        registerTrack(GameSound.BOSS_APPEARANCE, bossData, sampleRate)

        // 12. Victory: Heroic victory fanfare
        val victoryData = synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(
                Pair(523.25f, 0.10f),
                Pair(659.25f, 0.10f),
                Pair(783.99f, 0.10f),
                Pair(1046.50f, 0.22f)
            ),
            peakVolume = 0.75f
        )
        registerTrack(GameSound.VICTORY, victoryData, sampleRate)

        // 13. Game Over: Somber defeat tones
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

        // Controlled event-based cooldown throttling to prevent audio congestion and buzzing
        val now = System.currentTimeMillis()
        val cooldown = when (sound) {
            GameSound.MACHINE_GUN_FIRE -> 65L
            GameSound.CANNON_FIRE -> 140L
            GameSound.RAPID_FIRE -> 45L
            GameSound.ENEMY_HIT -> 80L
            GameSound.ENEMY_DEATH -> 70L
            GameSound.CANNON_IMPACT -> 100L
            GameSound.BOSS_IMPACT -> 100L
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

    // --- Mechanical & Weapon Audio Synthesis Utilities ---

    /**
     * Machine Gun Shot: A sharp, crisp mechanical gunshot report.
     * High-speed explosive noise transient + metallic bolt slap + punchy body thump.
     * Absolutely zero balloon-like pitch sweep!
     */
    private fun synthesizeMachineGunCrack(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var boltPhase = 0.0
        var punchPhase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            
            // 1. Initial powder crack (steep high-frequency noise transient)
            val noise = (Random.nextFloat() * 2f - 1f)
            val crackEnv = exp(-t * 110f)
            val crack = noise * crackEnv

            // 2. Mechanical steel bolt slap (~1450Hz resonant ring)
            boltPhase += 2.0 * PI * 1450.0 / sampleRate
            val bolt = sin(boltPhase).toFloat() * exp(-t * 85f) * 0.45f

            // 3. Kinetic body punch (fast 170Hz -> 75Hz thump)
            val punchFreq = 170f - (t / durationSec) * 95f
            punchPhase += 2.0 * PI * punchFreq / sampleRate
            var punch = sin(punchPhase).toFloat()
            // Saturated curve for punch
            punch = (1.4f * punch - 0.4f * punch * punch * punch) * exp(-t * 50f) * 0.65f

            val sampleVal = ((crack * 0.6f + bolt + punch) * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Heavy Cannon Boom: Deep, massive artillery blast.
     * Low-frequency saturated shockwave + low-pass filtered expanding fireball roar.
     */
    private fun synthesizeHeavyCannonBoom(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var subPhase = 0.0
        var filterVal = 0f

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec

            // 1. Heavy sub-bass shockwave (78Hz sweeping down to 34Hz)
            val freq = 78f * (1f - progress * 0.58f)
            subPhase += 2.0 * PI * freq / sampleRate
            var subSine = sin(subPhase).toFloat()
            // Overdriven saturated cubic curve for heavy blast weight
            subSine = (1.5f * subSine - 0.5f * subSine * subSine * subSine).coerceIn(-1f, 1f)
            val subEnv = exp(-t * 9.5f)

            // 2. Expanding gas roar (low-pass filtered noise rumble)
            val rawNoise = (Random.nextFloat() * 2f - 1f)
            filterVal = filterVal * 0.82f + rawNoise * 0.18f
            val gasEnv = if (t < 0.012f) t / 0.012f else exp(-(t - 0.012f) * 13f)

            val sampleVal = ((subSine * subEnv * 0.70f + filterVal * gasEnv * 0.55f) * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Rapid Fire Mechanical: Crisp, light, mechanical ratcheting click.
     * Tight mechanical transient that layers smoothly into a rhythmic chatter without buzzing.
     */
    private fun synthesizeRapidFireMechanical(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var clickPhase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate

            // Sharp micro-click noise
            val noise = (Random.nextFloat() * 2f - 1f) * exp(-t * 160f)

            // High metallic tap (~1100Hz)
            clickPhase += 2.0 * PI * 1100.0 / sampleRate
            val tap = sin(clickPhase).toFloat() * exp(-t * 120f)

            val sampleVal = ((noise * 0.55f + tap * 0.50f) * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Cannon Impact: Heavy ground shockwave thump + debris crumble.
     */
    private fun synthesizeCannonImpact(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0
        var filterVal = 0f

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 58.0 / sampleRate
            val subBass = sin(phase).toFloat() * exp(-t * 16f)

            val noise = (Random.nextFloat() * 2f - 1f)
            filterVal = filterVal * 0.78f + noise * 0.22f
            val debris = filterVal * exp(-t * 18f)

            val sampleVal = ((subBass * 0.65f + debris * 0.55f) * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Boss Impact: Heavy armor plate clang + deep kinetic thud.
     */
    private fun synthesizeBossImpact(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var p1 = 0.0
        var p2 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            p1 += 2.0 * PI * 360.0 / sampleRate
            p2 += 2.0 * PI * 740.0 / sampleRate

            val clang = (sin(p1) * 0.6f + sin(p2) * 0.4f).toFloat() * exp(-t * 26f)
            val noise = (Random.nextFloat() * 2f - 1f) * exp(-t * 40f) * 0.3f

            val sampleVal = ((clang + noise) * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Enemy Hit: Quiet, subtle tactile impact smack.
     */
    private fun synthesizeEnemyHitTactile(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 240.0 / sampleRate
            val tone = sin(phase).toFloat() * exp(-t * 110f)
            val noise = (Random.nextFloat() * 2f - 1f) * exp(-t * 130f) * 0.35f

            val sampleVal = ((tone + noise) * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Enemy Death: Crunchy mechanical defeat & breakdown.
     */
    private fun synthesizeEnemyDeathCrunch(
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
            val freq = 420f * (1f - progress * 0.75f)
            phase += 2.0 * PI * freq / sampleRate

            val tone = sin(phase).toFloat() * 0.5f
            val noise = (Random.nextFloat() * 2f - 1f) * 0.5f
            val env = exp(-t * 32f)

            val sampleVal = ((tone + noise) * env * peakVolume).coerceIn(-1f, 1f)
            val pcm16 = (sampleVal * 32767).toInt().coerceIn(-32768, 32767)

            bytes[i * 2] = (pcm16 and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm16 shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Tower Build/Upgrade: Mechanical ratchet lock.
     */
    private fun synthesizeBuildSound(
        sampleRate: Int,
        durationSec: Float,
        peakVolume: Float
    ): ByteArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var p1 = 0.0
        var p2 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            p1 += 2.0 * PI * 850.0 / sampleRate
            p2 += 2.0 * PI * 1350.0 / sampleRate

            val click1 = sin(p1).toFloat() * exp(-t * 110f)
            val click2 = if (t > 0.025f) sin(p2).toFloat() * exp(-(t - 0.025f) * 110f) else 0f

            val sampleVal = ((click1 * 0.5f + click2 * 0.6f) * peakVolume).coerceIn(-1f, 1f)
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

