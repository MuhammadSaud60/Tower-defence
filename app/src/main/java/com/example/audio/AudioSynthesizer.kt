package com.example.audio

import com.example.data.EnvironmentType
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural audio synthesizer for tower defense sound effects, ambience loops,
 * and background music. Generates pristine, zero-latency 16-bit PCM mono audio buffers.
 */
object AudioSynthesizer {

    const val DEFAULT_SAMPLE_RATE = 22050

    // --- Weapon Sounds ---

    /**
     * Machine Gun Crack with 3 variations:
     * Short, sharp mechanical firing sound of a small game weapon.
     * Consecutive shots vary slightly in bolt and punch frequency.
     */
    fun synthesizeMachineGun(sampleRate: Int = DEFAULT_SAMPLE_RATE, variation: Int = 0): ByteArray {
        val durationSec = when (variation % 3) {
            0 -> 0.052f
            1 -> 0.048f
            else -> 0.055f
        }
        val boltFreq = when (variation % 3) {
            0 -> 1450.0
            1 -> 1550.0
            else -> 1380.0
        }
        val punchBaseFreq = when (variation % 3) {
            0 -> 165f
            1 -> 178f
            else -> 152f
        }

        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var boltPhase = 0.0
        var punchPhase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate

            // 1. Initial powder snap (fast high-frequency noise transient)
            val noise = (Random.nextFloat() * 2f - 1f)
            val crack = noise * exp(-t * 135f)

            // 2. Mechanical steel bolt slap
            boltPhase += 2.0 * PI * boltFreq / sampleRate
            val bolt = sin(boltPhase).toFloat() * exp(-t * 88f) * 0.45f

            // 3. Kinetic body punch
            val punchFreq = punchBaseFreq - (t / durationSec) * 90f
            punchPhase += 2.0 * PI * punchFreq / sampleRate
            var punch = sin(punchPhase).toFloat()
            punch = (1.4f * punch - 0.4f * punch * punch * punch) * exp(-t * 58f) * 0.65f

            val sampleVal = ((crack * 0.65f + bolt + punch) * 0.65f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Heavy Cannon Boom:
     * Deep and powerful artillery blast, slower firing sound with strong bass/impact.
     */
    fun synthesizeCannon(sampleRate: Int = DEFAULT_SAMPLE_RATE, variation: Int = 0): ByteArray {
        val durationSec = if (variation % 2 == 0) 0.30f else 0.33f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var subPhase = 0.0
        var filterVal = 0f
        val startFreq = if (variation % 2 == 0) 76f else 72f

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec

            // 1. Heavy sub-bass shockwave sweeping down
            val freq = startFreq * (1f - progress * 0.58f)
            subPhase += 2.0 * PI * freq / sampleRate
            var subSine = sin(subPhase).toFloat()
            // Saturated overdriven cubic curve for thunderous weight
            subSine = (1.6f * subSine - 0.6f * subSine * subSine * subSine).coerceIn(-1f, 1f)
            val subEnv = exp(-t * 9.5f)

            // 2. Expanding gas roar (low-pass filtered noise)
            val rawNoise = (Random.nextFloat() * 2f - 1f)
            filterVal = filterVal * 0.82f + rawNoise * 0.18f
            val gasEnv = if (t < 0.012f) t / 0.012f else exp(-(t - 0.012f) * 12f)

            val sampleVal = ((subSine * subEnv * 0.72f + filterVal * gasEnv * 0.58f) * 0.90f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Rapid Fire:
     * Fast, crisp electronic/mechanical firing sound. Energetic, distinct from Machine Gun.
     * NOT balloons, clicks, or bubbles!
     */
    fun synthesizeRapidFire(sampleRate: Int = DEFAULT_SAMPLE_RATE, variation: Int = 0): ByteArray {
        val durationSec = 0.036f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var tonePhase1 = 0.0
        var tonePhase2 = 0.0

        val baseFreq = when (variation % 3) {
            0 -> 880.0
            1 -> 940.0
            else -> 820.0
        }

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate

            // Dual resonant electromagnetic/metallic pulse
            tonePhase1 += 2.0 * PI * baseFreq / sampleRate
            tonePhase2 += 2.0 * PI * (baseFreq * 2.0) / sampleRate

            val tone1 = sin(tonePhase1).toFloat()
            val tone2 = sin(tonePhase2).toFloat() * 0.4f
            val pulse = (tone1 + tone2) * exp(-t * 120f)

            // High-velocity metallic slider transient
            val noise = (Random.nextFloat() * 2f - 1f) * exp(-t * 185f) * 0.45f

            val sampleVal = ((pulse * 0.65f + noise) * 0.50f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Frost Fire:
     * High-frequency crystalline cryo whoosh and resonant chime.
     */
    fun synthesizeFrostFire(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.075f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val freq1 = 720.0 + (1 - exp(-t * 18f)) * 620.0
            val freq2 = 1250.0 + (1 - exp(-t * 18f)) * 950.0
            phase1 += 2.0 * PI * freq1 / sampleRate
            phase2 += 2.0 * PI * freq2 / sampleRate

            val tone = (sin(phase1) * 0.5f + sin(phase2) * 0.3f).toFloat() * exp(-t * 22f)
            val breath = (Random.nextFloat() * 2f - 1f) * exp(-t * 30f) * 0.25f

            val sampleVal = ((tone + breath) * 0.48f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    // --- Projectile Impact Sounds ---

    /**
     * Small Enemy Hit: Quiet, subtle tactile impact smack.
     */
    fun synthesizeEnemyHit(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.022f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 250.0 / sampleRate
            val tone = sin(phase).toFloat() * exp(-t * 125f)
            val noise = (Random.nextFloat() * 2f - 1f) * exp(-t * 140f) * 0.35f

            val sampleVal = ((tone + noise) * 0.22f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Heavy Enemy Hit: Stronger, solid armor-impact sound.
     */
    fun synthesizeHeavyEnemyHit(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.050f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var clangPhase = 0.0
        var punchPhase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            // Metallic armor clang
            clangPhase += 2.0 * PI * 580.0 / sampleRate
            val clang = sin(clangPhase).toFloat() * exp(-t * 65f) * 0.55f

            // Heavy body punch
            val pFreq = 160f - (t / durationSec) * 80f
            punchPhase += 2.0 * PI * pFreq / sampleRate
            val punch = sin(punchPhase).toFloat() * exp(-t * 50f) * 0.65f

            val sampleVal = ((clang + punch) * 0.55f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Cannon Impact: Powerful ground shockwave explosion and debris scatter.
     */
    fun synthesizeCannonImpact(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.22f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0
        var filterVal = 0f

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 56.0 / sampleRate
            val subBass = sin(phase).toFloat() * exp(-t * 15f)

            val noise = (Random.nextFloat() * 2f - 1f)
            filterVal = filterVal * 0.78f + noise * 0.22f
            val debris = filterVal * exp(-t * 18f)

            val sampleVal = ((subBass * 0.70f + debris * 0.55f) * 0.85f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Frost Impact: Crystalline freeze crackle and shattering ice chime.
     */
    fun synthesizeFrostImpact(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.12f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var p1 = 0.0
        var p2 = 0.0
        var p3 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            p1 += 2.0 * PI * 1320.0 / sampleRate
            p2 += 2.0 * PI * 1760.0 / sampleRate
            p3 += 2.0 * PI * 2640.0 / sampleRate

            val shimmer = (sin(p1) * 0.4f + sin(p2) * 0.35f + sin(p3) * 0.25f).toFloat() * exp(-t * 24f)
            val glassShatter = (Random.nextFloat() * 2f - 1f) * exp(-t * 38f) * 0.4f

            val sampleVal = ((shimmer + glassShatter) * 0.55f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Tree Destroyed: Wood/tree breaking sound (sharp fracture crack + timber crunch).
     */
    fun synthesizeTreeDestroyed(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.18f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var woodPhase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            woodPhase += 2.0 * PI * 650.0 / sampleRate
            // Timber crack snap
            val crack = sin(woodPhase).toFloat() * exp(-t * 55f) * 0.6f
            // Splinter crunch and branch collapse
            val splinters = (Random.nextFloat() * 2f - 1f) * exp(-t * 28f) * 0.55f

            val sampleVal = ((crack + splinters) * 0.70f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Stone Destroyed: Rock/crack impact sound (solid stone fracture + rock crumble).
     */
    fun synthesizeStoneDestroyed(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.20f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var boulderPhase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            boulderPhase += 2.0 * PI * 170.0 / sampleRate
            // Deep boulder strike
            val strike = sin(boulderPhase).toFloat() * exp(-t * 35f) * 0.7f
            // Stone shattering & pebble scatter
            val crumble = (Random.nextFloat() * 2f - 1f) * exp(-t * 24f) * 0.55f

            val sampleVal = ((strike + crumble) * 0.75f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Tree Hit (Direct/Splash): Wood chop / splinter tick.
     */
    fun synthesizeTreeHit(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.038f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 750.0 / sampleRate
            val woodTone = sin(phase).toFloat() * exp(-t * 95f) * 0.5f
            val splinter = (Random.nextFloat() * 2f - 1f) * exp(-t * 120f) * 0.5f

            val sampleVal = ((woodTone + splinter) * 0.45f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Stone Hit (Direct/Splash): Sharp chisel/rock chip crack.
     */
    fun synthesizeStoneHit(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.038f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 1150.0 / sampleRate
            val rockTone = sin(phase).toFloat() * exp(-t * 110f) * 0.5f
            val chip = (Random.nextFloat() * 2f - 1f) * exp(-t * 130f) * 0.5f

            val sampleVal = ((rockTone + chip) * 0.45f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    // --- Boss Audio ---

    /**
     * Boss Appearance: Short ominous brass war horn entrance cue.
     */
    fun synthesizeBossAppearance(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.45f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val progress = t / durationSec
            phase1 += 2.0 * PI * 82.0 / sampleRate
            phase2 += 2.0 * PI * 123.0 / sampleRate
            phase3 += 2.0 * PI * 164.0 / sampleRate

            val swell = if (progress < 0.25f) progress / 0.25f else exp(-(progress - 0.25f) * 4f)
            val horn = (sin(phase1) * 0.55 + sin(phase2) * 0.30 + sin(phase3) * 0.15).toFloat()

            val sampleVal = (horn * swell * 0.75f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Boss Impact / Special Attack: Resonant titanium armor clang + kinetic thud.
     */
    fun synthesizeBossImpact(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.14f
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

            val sampleVal = ((clang + noise) * 0.65f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Boss Defeated: Stronger victory/defeat impact with dramatic explosion & fanfare tail.
     */
    fun synthesizeBossDefeated(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.45f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var subPhase = 0.0
        var chordPhase1 = 0.0
        var chordPhase2 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            // Heavy defeat explosion
            subPhase += 2.0 * PI * 52.0 / sampleRate
            val boom = sin(subPhase).toFloat() * exp(-t * 11f) * 0.6f
            val debris = (Random.nextFloat() * 2f - 1f) * exp(-t * 14f) * 0.4f

            // Heroic fanfare chord tail (C5 523Hz + G5 784Hz)
            chordPhase1 += 2.0 * PI * 523.25 / sampleRate
            chordPhase2 += 2.0 * PI * 783.99 / sampleRate
            val chord = if (t > 0.08f) {
                (sin(chordPhase1) * 0.4f + sin(chordPhase2) * 0.3f).toFloat() * exp(-(t - 0.08f) * 6f)
            } else 0f

            val sampleVal = ((boom + debris + chord) * 0.85f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    // --- Important Game & UI Sounds ---

    /**
     * Tower Placed: Solid mechanical construction latch / snap.
     */
    fun synthesizeTowerPlaced(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.055f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var p1 = 0.0
        var p2 = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            p1 += 2.0 * PI * 850.0 / sampleRate
            p2 += 2.0 * PI * 1300.0 / sampleRate

            val click1 = sin(p1).toFloat() * exp(-t * 110f)
            val click2 = if (t > 0.022f) sin(p2).toFloat() * exp(-(t - 0.022f) * 110f) else 0f

            val sampleVal = ((click1 * 0.5f + click2 * 0.6f) * 0.55f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Tower Upgraded: Energetic ascending power-up chime (E5 -> G5 -> C6).
     */
    fun synthesizeTowerUpgraded(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        return synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(659.25f, 0.05f), Pair(783.99f, 0.05f), Pair(1046.50f, 0.10f)),
            peakVolume = 0.65f
        )
    }

    /**
     * Tower Sold: Cash register / coin refund clink.
     */
    fun synthesizeTowerSold(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        return synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(1396.91f, 0.06f), Pair(1760.00f, 0.10f)),
            peakVolume = 0.60f
        )
    }

    /**
     * Invalid Tower Placement: Short dull error buzz.
     */
    fun synthesizeInvalidPlacement(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.08f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 180f - (t / durationSec) * 45f
            phase += 2.0 * PI * freq / sampleRate
            val buzz = sin(phase).toFloat() * exp(-t * 32f)

            val sampleVal = (buzz * 0.40f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Enemy Defeated: Mechanical collapse & defeat crunch.
     */
    fun synthesizeEnemyDeath(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.075f
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

            val sampleVal = ((tone + noise) * env * 0.50f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    /**
     * Wave Started: Crisp military rally bugle (C5 -> G5).
     */
    fun synthesizeWaveStarted(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        return synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(523.25f, 0.08f), Pair(783.99f, 0.14f)),
            peakVolume = 0.65f
        )
    }

    /**
     * Wave Completed: Cheerful fanfare (C5 -> E5 -> G5 -> C6).
     */
    fun synthesizeWaveCleared(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        return synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(523.25f, 0.07f), Pair(659.25f, 0.07f), Pair(783.99f, 0.07f), Pair(1046.50f, 0.14f)),
            peakVolume = 0.70f
        )
    }

    /**
     * Victory: Heroic victory fanfare.
     */
    fun synthesizeVictory(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        return synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(
                Pair(523.25f, 0.10f),
                Pair(659.25f, 0.10f),
                Pair(783.99f, 0.10f),
                Pair(1046.50f, 0.22f)
            ),
            peakVolume = 0.75f
        )
    }

    /**
     * Defeat: Somber game over phrase.
     */
    fun synthesizeDefeat(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        return synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(
                Pair(440.00f, 0.10f),
                Pair(392.00f, 0.10f),
                Pair(349.23f, 0.10f),
                Pair(311.13f, 0.16f)
            ),
            peakVolume = 0.70f
        )
    }

    /**
     * Coin / Token Reward: Golden coin pickup chime (E6 -> A6).
     */
    fun synthesizeCoinReward(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        return synthesizeMelody(
            sampleRate = sampleRate,
            tones = listOf(Pair(1318.51f, 0.04f), Pair(1760.00f, 0.07f)),
            peakVolume = 0.55f
        )
    }

    /**
     * Button Click: Subtle crisp UI tap.
     */
    fun synthesizeButtonClick(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val durationSec = 0.022f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            phase += 2.0 * PI * 1200.0 / sampleRate
            val tone = sin(phase).toFloat() * exp(-t * 160f)
            val tick = (Random.nextFloat() * 2f - 1f) * exp(-t * 220f) * 0.35f

            val sampleVal = ((tone + tick) * 0.35f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, sampleVal)
        }
        return bytes
    }

    // --- Environment Ambience Loops ---

    /**
     * Subtle environment ambience loop (seamless 3.0-second buffer).
     */
    fun synthesizeAmbience(sampleRate: Int = DEFAULT_SAMPLE_RATE, type: EnvironmentType): ByteArray {
        val durationSec = 3.0f
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)
        var filterVal = 0f

        when (type) {
            EnvironmentType.GREEN_VALLEY -> {
                // Light natural outdoor ambience: gentle wind breeze + sparse distant soft chirps
                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / sampleRate
                    val rawNoise = (Random.nextFloat() * 2f - 1f)
                    filterVal = filterVal * 0.88f + rawNoise * 0.12f
                    // Slow undulating wind swell
                    val windSwell = 0.15f + 0.08f * sin(2.0 * PI * 0.33 * t).toFloat()
                    val wind = filterVal * windSwell

                    // Very sparse, quiet distant bird chirp at t=1.1s and t=2.3s
                    val bird1 = if (t in 1.1f..1.18f) {
                        sin(2.0 * PI * 2800.0 * (t - 1.1f)).toFloat() * exp(-(t - 1.1f) * 45f) * 0.10f
                    } else 0f
                    val bird2 = if (t in 2.3f..2.37f) {
                        sin(2.0 * PI * 3200.0 * (t - 2.3f)).toFloat() * exp(-(t - 2.3f) * 50f) * 0.08f
                    } else 0f

                    val sampleVal = ((wind + bird1 + bird2) * 0.25f).coerceIn(-1f, 1f)
                    writePcm16(bytes, i, sampleVal)
                }
            }
            EnvironmentType.DESERT_CANYON -> {
                // Dry / open environmental ambience: low-pass canyon wind gust
                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / sampleRate
                    val rawNoise = (Random.nextFloat() * 2f - 1f)
                    filterVal = filterVal * 0.82f + rawNoise * 0.18f
                    val gust = 0.18f + 0.10f * sin(2.0 * PI * 0.25 * t).toFloat()

                    val sampleVal = ((filterVal * gust) * 0.28f).coerceIn(-1f, 1f)
                    writePcm16(bytes, i, sampleVal)
                }
            }
            EnvironmentType.SNOW_VALLEY -> {
                // Cold winter mountain valley: whistling cold wind and soft drifting snow resonance
                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / sampleRate
                    val rawNoise = (Random.nextFloat() * 2f - 1f)
                    filterVal = filterVal * 0.90f + rawNoise * 0.10f

                    // Long slow glacial wind sweep
                    val windSwell = 0.18f + 0.12f * sin(2.0 * PI * 0.22 * t).toFloat()
                    val coldWind = filterVal * windSwell

                    // Eerie distant cold winter whistle harmonic (soft resonant whistling tone)
                    val whistleFreq = 620.0 + 80.0 * sin(2.0 * PI * 0.35 * t)
                    val whistle = sin(2.0 * PI * whistleFreq * t).toFloat() * (0.04f + 0.02f * sin(2.0 * PI * 0.22 * t).toFloat())

                    val sampleVal = ((coldWind + whistle) * 0.26f).coerceIn(-1f, 1f)
                    writePcm16(bytes, i, sampleVal)
                }
            }
            EnvironmentType.NIGHT_FORTRESS -> {
                // Nocturnal fortress ambience: deep twilight breeze, rhythmic night crickets, and subtle mystic chime harmonics
                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / sampleRate
                    val rawNoise = (Random.nextFloat() * 2f - 1f)
                    filterVal = filterVal * 0.92f + rawNoise * 0.08f

                    // Deep, gentle night breeze
                    val nightBreeze = filterVal * (0.14f + 0.06f * sin(2.0 * PI * 0.28 * t).toFloat())

                    // Night field crickets: pulsing chirps at 4600 Hz modulated by 5.5 Hz pulse
                    val cricketMod = (sin(2.0 * PI * 5.5 * t).toFloat().coerceAtLeast(0f)).let { it * it }
                    val crickets = sin(2.0 * PI * 4600.0 * t).toFloat() * cricketMod * 0.045f

                    // Distant subtle mystic crystal / bell resonance (880Hz / A5 harmonic)
                    val mysticChime = sin(2.0 * PI * 880.0 * t).toFloat() * (0.015f + 0.008f * sin(2.0 * PI * 0.67 * t).toFloat())

                    val sampleVal = ((nightBreeze + crickets + mysticChime) * 0.26f).coerceIn(-1f, 1f)
                    writePcm16(bytes, i, sampleVal)
                }
            }
            else -> {
                // Forest & others: foliage rustle + subtle evening crickets
                for (i in 0 until totalSamples) {
                    val t = i.toFloat() / sampleRate
                    val rawNoise = (Random.nextFloat() * 2f - 1f)
                    filterVal = filterVal * 0.85f + rawNoise * 0.15f
                    val rustle = filterVal * (0.12f + 0.05f * sin(2.0 * PI * 0.4 * t).toFloat())

                    // Gentle cricket pulse at 4.2 kHz
                    val cricketPulsing = sin(2.0 * PI * 4.0 * t).toFloat().coerceAtLeast(0f)
                    val cricketTone = sin(2.0 * PI * 4200.0 * t).toFloat() * cricketPulsing * 0.06f

                    val sampleVal = ((rustle + cricketTone) * 0.25f).coerceIn(-1f, 1f)
                    writePcm16(bytes, i, sampleVal)
                }
            }
        }
        return bytes
    }

    // --- Background Music Loop ---

    /**
     * Upbeat, gentle cartoon arcade music loop (~6.4 seconds).
     * Marimba/chime melody + soft warm bassline + quiet shaker offbeats.
     */
    fun synthesizeBackgroundMusic(sampleRate: Int = DEFAULT_SAMPLE_RATE): ByteArray {
        val bpm = 100.0
        val beatDuration = (60.0 / bpm).toFloat() // 0.6s per beat
        val totalBeats = 16 // 4 measures of 4/4
        val durationSec = beatDuration * totalBeats // 9.6 seconds
        val totalSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(totalSamples * 2)

        // Melody note frequencies (C-major / A-minor friendly groove)
        val melodyNotes = listOf(
            Pair(0.0f, 261.63f), Pair(0.5f, 329.63f), Pair(1.0f, 392.00f), Pair(1.5f, 440.00f), // C4, E4, G4, A4
            Pair(2.0f, 523.25f), Pair(2.5f, 392.00f), Pair(3.0f, 329.63f), Pair(3.5f, 293.66f), // C5, G4, E4, D4
            Pair(4.0f, 220.00f), Pair(4.5f, 261.63f), Pair(5.0f, 329.63f), Pair(5.5f, 392.00f), // A3, C4, E4, G4
            Pair(6.0f, 329.63f), Pair(6.5f, 293.66f), Pair(7.0f, 261.63f), Pair(7.5f, 293.66f), // E4, D4, C4, D4
            Pair(8.0f, 349.23f), Pair(8.5f, 440.00f), Pair(9.0f, 523.25f), Pair(9.5f, 587.33f), // F4, A4, C5, D5
            Pair(10.0f, 523.25f), Pair(10.5f, 440.00f), Pair(11.0f, 349.23f), Pair(11.5f, 392.00f), // C5, A4, F4, G4
            Pair(12.0f, 392.00f), Pair(12.5f, 493.88f), Pair(13.0f, 587.33f), Pair(13.5f, 659.25f), // G4, B4, D5, E5
            Pair(14.0f, 523.25f), Pair(14.5f, 493.88f), Pair(15.0f, 392.00f), Pair(15.5f, 523.25f)  // C5, B4, G4, C5
        )

        // Bass progression (C -> Am -> F -> G)
        val bassNotes = listOf(
            Pair(0.0f, 130.81f), Pair(2.0f, 130.81f), // C3
            Pair(4.0f, 110.00f), Pair(6.0f, 110.00f), // A2
            Pair(8.0f, 87.31f),  Pair(10.0f, 87.31f), // F2
            Pair(12.0f, 98.00f), Pair(14.0f, 98.00f)  // G2
        )

        for (i in 0 until totalSamples) {
            val t = i.toFloat() / sampleRate
            val currentBeat = t / beatDuration

            // 1. Melody synthesis (soft marimba envelope)
            var melodyVal = 0f
            for ((noteBeat, freq) in melodyNotes) {
                val noteTime = (currentBeat - noteBeat) * beatDuration
                if (noteTime in 0f..0.50f) {
                    val phase = 2.0 * PI * freq * noteTime
                    val fundamental = sin(phase).toFloat()
                    val harmonic = sin(phase * 2.0).toFloat() * 0.20f
                    val env = exp(-noteTime * 7.5f)
                    melodyVal += (fundamental + harmonic) * env * 0.35f
                }
            }

            // 2. Bass synthesis (warm sine bass)
            var bassVal = 0f
            for ((bassBeat, freq) in bassNotes) {
                val bassTime = (currentBeat - bassBeat) * beatDuration
                if (bassTime in 0f..1.1f) {
                    val phase = 2.0 * PI * freq * bassTime
                    val sine = sin(phase).toFloat()
                    val env = exp(-bassTime * 2.5f)
                    bassVal += sine * env * 0.32f
                }
            }

            // 3. Shaker / percussion cadence on offbeats
            var percVal = 0f
            val beatFrac = currentBeat - currentBeat.toInt()
            if (beatFrac in 0.48f..0.56f) {
                val pTime = (beatFrac - 0.48f) * beatDuration
                val noise = (Random.nextFloat() * 2f - 1f)
                percVal = noise * exp(-pTime * 60f) * 0.08f
            }

            val mix = ((melodyVal + bassVal + percVal) * 0.35f).coerceIn(-1f, 1f)
            writePcm16(bytes, i, mix)
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
                writePcm16(bytes, sampleIndex, sampleVal)
                sampleIndex++
            }
        }
        return bytes
    }

    private fun writePcm16(bytes: ByteArray, index: Int, sample: Float) {
        val pcm = (sample * 32767).toInt().coerceIn(-32768, 32767)
        val byteIndex = index * 2
        if (byteIndex + 1 < bytes.size) {
            bytes[byteIndex] = (pcm and 0xFF).toByte()
            bytes[byteIndex + 1] = ((pcm shr 8) and 0xFF).toByte()
        }
    }
}
