package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.data.EnvironmentType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

enum class GameSound {
    // Weapons
    MACHINE_GUN_FIRE,
    CANNON_FIRE,
    RAPID_FIRE,
    FROST_FIRE,

    // Projectile Impacts
    ENEMY_HIT,
    HEAVY_ENEMY_HIT,
    CANNON_IMPACT,
    FROST_IMPACT,
    BOSS_IMPACT,

    // Destructibles
    TREE_HIT,
    STONE_HIT,
    TREE_DESTROYED,
    STONE_DESTROYED,
    OBJECT_HIT,
    OBJECT_DESTROY,

    // Game & UI Events
    TOWER_PLACED,
    TOWER_UPGRADED,
    TOWER_SOLD,
    INVALID_PLACEMENT,
    ENEMY_DEFEATED,
    BOSS_APPEARANCE,
    BOSS_DEFEATED,
    WAVE_START,
    WAVE_CLEAR,
    VICTORY,
    GAME_OVER,
    COIN_REWARD,
    BUTTON_CLICK,

    // Enemy Abilities & Boss Mechanics
    RUNNER_DASH,
    SHIELD_HIT,
    SHIELD_BREAK,
    ARMOR_BREAK,
    HEAL_PULSE,
    SUMMON_MINIONS,
    STEALTH_CLOAK,
    BOSS_SHOCKWAVE,
    BOSS_PHASE_CHANGE;

    companion object {
        // Backward compatibility aliases
        @JvmField val BUILD_TOWER = TOWER_PLACED
        @JvmField val ENEMY_DEATH = ENEMY_DEFEATED
        @JvmField val TOWER_FIRE_BULLET = MACHINE_GUN_FIRE
        @JvmField val TOWER_FIRE_CANNON = CANNON_FIRE
        @JvmField val TOWER_FIRE_RAPID = RAPID_FIRE
        @JvmField val CANNON_EXPLOSION = CANNON_IMPACT
    }
}

interface AudioPlayer {
    var isMuted: Boolean
    var isSfxEnabled: Boolean
    var isMusicEnabled: Boolean
    var isAmbienceEnabled: Boolean

    var sfxVolume: Float
    var musicVolume: Float
    var ambienceVolume: Float

    fun playSound(sound: GameSound)

    // Weapons
    fun machineGunFire() = playSound(GameSound.MACHINE_GUN_FIRE)
    fun cannonFire() = playSound(GameSound.CANNON_FIRE)
    fun rapidFire() = playSound(GameSound.RAPID_FIRE)
    fun frostFire() = playSound(GameSound.FROST_FIRE)

    // Projectile Impacts
    fun enemyHit() = playSound(GameSound.ENEMY_HIT)
    fun heavyEnemyHit() = playSound(GameSound.HEAVY_ENEMY_HIT)
    fun cannonImpact() = playSound(GameSound.CANNON_IMPACT)
    fun frostImpact() = playSound(GameSound.FROST_IMPACT)
    fun bossImpact() = playSound(GameSound.BOSS_IMPACT)

    // Destructibles
    fun treeHit() = playSound(GameSound.TREE_HIT)
    fun stoneHit() = playSound(GameSound.STONE_HIT)
    fun treeDestroyed() = playSound(GameSound.TREE_DESTROYED)
    fun stoneDestroyed() = playSound(GameSound.STONE_DESTROYED)
    fun objectHit() = playSound(GameSound.OBJECT_HIT)
    fun objectDestroy() = playSound(GameSound.OBJECT_DESTROY)

    // Game & UI Events
    fun towerPlaced() = playSound(GameSound.TOWER_PLACED)
    fun towerUpgraded() = playSound(GameSound.TOWER_UPGRADED)
    fun towerSold() = playSound(GameSound.TOWER_SOLD)
    fun invalidPlacement() = playSound(GameSound.INVALID_PLACEMENT)
    fun enemyDeath() = playSound(GameSound.ENEMY_DEFEATED)
    fun bossAppearance() = playSound(GameSound.BOSS_APPEARANCE)
    fun bossDefeated() = playSound(GameSound.BOSS_DEFEATED)
    fun waveStart() = playSound(GameSound.WAVE_START)
    fun waveClear() = playSound(GameSound.WAVE_CLEAR)
    fun victory() = playSound(GameSound.VICTORY)
    fun defeat() = playSound(GameSound.GAME_OVER)
    fun coinReward() = playSound(GameSound.COIN_REWARD)
    fun buttonClick() = playSound(GameSound.BUTTON_CLICK)

    // Enemy Abilities & Boss Mechanics
    fun runnerDash() = playSound(GameSound.RUNNER_DASH)
    fun shieldHit() = playSound(GameSound.SHIELD_HIT)
    fun shieldBreak() = playSound(GameSound.SHIELD_BREAK)
    fun armorBreak() = playSound(GameSound.ARMOR_BREAK)
    fun healPulse() = playSound(GameSound.HEAL_PULSE)
    fun summonMinions() = playSound(GameSound.SUMMON_MINIONS)
    fun stealthCloak() = playSound(GameSound.STEALTH_CLOAK)
    fun bossShockwave() = playSound(GameSound.BOSS_SHOCKWAVE)
    fun bossPhaseChange() = playSound(GameSound.BOSS_PHASE_CHANGE)

    // Ambience & Music
    var isInBattlefield: Boolean

    val isRainActive: Boolean
    fun setRainActive(isRaining: Boolean)
    fun updateMapAmbience(environmentType: EnvironmentType)
    fun pauseAmbienceAndMusic()
    fun resumeAmbienceAndMusic()
    fun release()
}

/**
 * Event-based audio manager for mobile tower defense.
 * Features:
 * - Event-based throttling to prevent sound stacking and lag.
 * - Multi-shot round-robin variations for Machine Gun, Rapid Fire, and Cannon.
 * - Distinct sounds for every tower, projectile impact, destructible, boss event, and UI interaction.
 * - Looping background ambience tailored to each map (Valley, Desert, Forest).
 * - Upbeat, gentle background music loop that does not overpower gameplay.
 * - Independent volume controls & persistence via SharedPreferences.
 */
class AndroidAudioPlayer(context: Context? = null) : AudioPlayer {

    private var appContext: Context? = context?.applicationContext
    private var prefs: SharedPreferences? = null

    override var isInBattlefield: Boolean = false
        set(value) {
            field = value
            if (value) {
                sfxMixer.resume()
                if (!isMuted && !isGlobalMuted) {
                    if (isMusicEnabled) {
                        try { bgmTrack?.play() } catch (_: Exception) {}
                    }
                    if (isAmbienceEnabled) {
                        try { currentAmbienceTrack?.play() } catch (_: Exception) {}
                        if (isRainRequested) {
                            try { rainTrack?.play() } catch (_: Exception) {}
                        }
                    }
                }
            } else {
                try { bgmTrack?.pause() } catch (_: Exception) {}
                try { currentAmbienceTrack?.pause() } catch (_: Exception) {}
                try { rainTrack?.pause() } catch (_: Exception) {}
            }
        }

    override var isMuted: Boolean = false
        set(value) {
            field = value
            applyMasterMute(value)
        }

    override var isSfxEnabled: Boolean = true
        set(value) {
            field = value
            savePreference(KEY_SFX_ENABLED, value)
        }

    override var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            savePreference(KEY_MUSIC_ENABLED, value)
            if (value && !isMuted && !isGlobalMuted && isInBattlefield) {
                try { bgmTrack?.play() } catch (_: Exception) {}
            } else {
                try { bgmTrack?.pause() } catch (_: Exception) {}
            }
        }

    override var isAmbienceEnabled: Boolean = true
        set(value) {
            field = value
            savePreference(KEY_AMBIENCE_ENABLED, value)
            val current = currentAmbienceTrack
            if (value && !isMuted && !isGlobalMuted && isInBattlefield) {
                try { current?.play() } catch (_: Exception) {}
                if (isRainRequested) {
                    try { rainTrack?.play() } catch (_: Exception) {}
                }
            } else {
                try { current?.pause() } catch (_: Exception) {}
                try { rainTrack?.pause() } catch (_: Exception) {}
            }
        }

    override var sfxVolume: Float = DEFAULT_SFX_VOLUME
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            field = clamped
            savePreference(KEY_SFX_VOLUME, clamped)
        }

    override var musicVolume: Float = DEFAULT_MUSIC_VOLUME
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            field = clamped
            savePreference(KEY_MUSIC_VOLUME, clamped)
            try { bgmTrack?.setVolume(clamped) } catch (_: Exception) {}
        }

    override var ambienceVolume: Float = DEFAULT_AMBIENCE_VOLUME
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            field = clamped
            savePreference(KEY_AMBIENCE_VOLUME, clamped)
            try { currentAmbienceTrack?.setVolume(clamped) } catch (_: Exception) {}
            try { rainTrack?.setVolume(clamped * 0.95f) } catch (_: Exception) {}
        }

    // Direct PCM SFX software mixer using a single AudioTrack stream (zero Codec2 / SoundPool dependency)
    private val sfxMixer = PcmSfxMixer(AudioSynthesizer.DEFAULT_SAMPLE_RATE)
    private val soundMap = ConcurrentHashMap<GameSound, ShortArray>()
    private val machineGunVariations = CopyOnWriteArrayList<ShortArray>()
    private val rapidFireVariations = CopyOnWriteArrayList<ShortArray>()
    private val cannonVariations = CopyOnWriteArrayList<ShortArray>()

    @Volatile
    private var isReleased = false
    private var loaderThread: Thread? = null

    // BGM & Ambience: looping track for BGM, active environment ambience, and dedicated rain soundscape
    private var bgmTrack: AudioTrack? = null
    private var currentAmbienceTrack: AudioTrack? = null
    private var currentEnvironment: EnvironmentType? = null
    private var rainTrack: AudioTrack? = null
    @Volatile private var isRainRequested: Boolean = false

    override val isRainActive: Boolean
        get() = isRainRequested

    // Variations rotation indices
    private var mgIndex = 0
    private var rapidIndex = 0
    private var cannonIndex = 0

    // Event-based cooldown throttling (milliseconds)
    private val lastPlayTimes = ConcurrentHashMap<GameSound, Long>()

    init {
        context?.let { initContext(it) }
        sfxMixer.start()
        try {
            pregenerateAllAudio()
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Audio synthesis init error: ${e.message}")
        }
    }

    fun initContext(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        try {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs?.let { p ->
                isSfxEnabled = p.getBoolean(KEY_SFX_ENABLED, true)
                isMusicEnabled = p.getBoolean(KEY_MUSIC_ENABLED, true)
                isAmbienceEnabled = p.getBoolean(KEY_AMBIENCE_ENABLED, true)
                sfxVolume = p.getFloat(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME)
                musicVolume = p.getFloat(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME)
                ambienceVolume = p.getFloat(KEY_AMBIENCE_VOLUME, DEFAULT_AMBIENCE_VOLUME)
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Failed to load audio preferences: ${e.message}")
        }
    }

    private fun savePreference(key: String, value: Any) {
        try {
            val editor = prefs?.edit() ?: return
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Failed to save audio preference: ${e.message}")
        }
    }

    private fun pcmBytesToShortArray(bytes: ByteArray): ShortArray {
        val shorts = ShortArray(bytes.size / 2)
        for (i in shorts.indices) {
            val b0 = bytes[i * 2].toInt() and 0xFF
            val b1 = bytes[i * 2 + 1].toInt()
            shorts[i] = ((b1 shl 8) or b0).toShort()
        }
        return shorts
    }

    private fun pregenerateAllAudio() {
        val sampleRate = AudioSynthesizer.DEFAULT_SAMPLE_RATE

        // 1. Immediate UI essentials loaded in-memory (zero latency, zero disk IO, zero Codec2 queries)
        soundMap[GameSound.BUTTON_CLICK] = pcmBytesToShortArray(AudioSynthesizer.synthesizeButtonClick(sampleRate))
        soundMap[GameSound.COIN_REWARD] = pcmBytesToShortArray(AudioSynthesizer.synthesizeCoinReward(sampleRate))

        // 2. Synthesize background music and remaining SFX in background daemon thread
        loaderThread = Thread {
            try {
                // Background Music Loop
                val bgmData = AudioSynthesizer.synthesizeBackgroundMusic(sampleRate)
                if (!isReleased) {
                    bgmTrack = createLoopingTrack(bgmData, sampleRate, musicVolume)
                    if (isMusicEnabled && !isMuted && !isGlobalMuted && isInBattlefield) {
                        try {
                            bgmTrack?.play()
                        } catch (_: Exception) {}
                    }
                }

                // Weapons
                for (i in 0..2) {
                    val data = AudioSynthesizer.synthesizeMachineGun(sampleRate, variation = i)
                    machineGunVariations.add(pcmBytesToShortArray(data))
                }
                for (i in 0..1) {
                    val data = AudioSynthesizer.synthesizeCannon(sampleRate, variation = i)
                    cannonVariations.add(pcmBytesToShortArray(data))
                }
                for (i in 0..2) {
                    val data = AudioSynthesizer.synthesizeRapidFire(sampleRate, variation = i)
                    rapidFireVariations.add(pcmBytesToShortArray(data))
                }

                fun register(sound: GameSound, bytes: ByteArray) {
                    soundMap[sound] = pcmBytesToShortArray(bytes)
                }

                register(GameSound.FROST_FIRE, AudioSynthesizer.synthesizeFrostFire(sampleRate))

                // Projectile Impacts
                register(GameSound.ENEMY_HIT, AudioSynthesizer.synthesizeEnemyHit(sampleRate))
                register(GameSound.HEAVY_ENEMY_HIT, AudioSynthesizer.synthesizeHeavyEnemyHit(sampleRate))
                register(GameSound.CANNON_IMPACT, AudioSynthesizer.synthesizeCannonImpact(sampleRate))
                register(GameSound.FROST_IMPACT, AudioSynthesizer.synthesizeFrostImpact(sampleRate))
                register(GameSound.BOSS_IMPACT, AudioSynthesizer.synthesizeBossImpact(sampleRate))

                // Destructibles
                register(GameSound.TREE_HIT, AudioSynthesizer.synthesizeTreeHit(sampleRate))
                register(GameSound.STONE_HIT, AudioSynthesizer.synthesizeStoneHit(sampleRate))
                register(GameSound.OBJECT_HIT, AudioSynthesizer.synthesizeTreeHit(sampleRate))
                register(GameSound.TREE_DESTROYED, AudioSynthesizer.synthesizeTreeDestroyed(sampleRate))
                register(GameSound.STONE_DESTROYED, AudioSynthesizer.synthesizeStoneDestroyed(sampleRate))
                register(GameSound.OBJECT_DESTROY, AudioSynthesizer.synthesizeStoneDestroyed(sampleRate))

                // Game & UI Events
                register(GameSound.TOWER_PLACED, AudioSynthesizer.synthesizeTowerPlaced(sampleRate))
                register(GameSound.TOWER_UPGRADED, AudioSynthesizer.synthesizeTowerUpgraded(sampleRate))
                register(GameSound.TOWER_SOLD, AudioSynthesizer.synthesizeTowerSold(sampleRate))
                register(GameSound.INVALID_PLACEMENT, AudioSynthesizer.synthesizeInvalidPlacement(sampleRate))
                register(GameSound.ENEMY_DEFEATED, AudioSynthesizer.synthesizeEnemyDeath(sampleRate))
                register(GameSound.BOSS_APPEARANCE, AudioSynthesizer.synthesizeBossAppearance(sampleRate))
                register(GameSound.BOSS_DEFEATED, AudioSynthesizer.synthesizeBossDefeated(sampleRate))
                register(GameSound.WAVE_START, AudioSynthesizer.synthesizeWaveStarted(sampleRate))
                register(GameSound.WAVE_CLEAR, AudioSynthesizer.synthesizeWaveCleared(sampleRate))
                register(GameSound.VICTORY, AudioSynthesizer.synthesizeVictory(sampleRate))
                register(GameSound.GAME_OVER, AudioSynthesizer.synthesizeDefeat(sampleRate))

                // Enemy Abilities & Boss Mechanics
                register(GameSound.RUNNER_DASH, AudioSynthesizer.synthesizeRunnerDash(sampleRate))
                register(GameSound.SHIELD_HIT, AudioSynthesizer.synthesizeShieldHit(sampleRate))
                register(GameSound.SHIELD_BREAK, AudioSynthesizer.synthesizeShieldBreak(sampleRate))
                register(GameSound.ARMOR_BREAK, AudioSynthesizer.synthesizeArmorBreak(sampleRate))
                register(GameSound.HEAL_PULSE, AudioSynthesizer.synthesizeHealPulse(sampleRate))
                register(GameSound.SUMMON_MINIONS, AudioSynthesizer.synthesizeSummonMinions(sampleRate))
                register(GameSound.STEALTH_CLOAK, AudioSynthesizer.synthesizeStealthCloak(sampleRate))
                register(GameSound.BOSS_SHOCKWAVE, AudioSynthesizer.synthesizeBossShockwave(sampleRate))
                register(GameSound.BOSS_PHASE_CHANGE, AudioSynthesizer.synthesizeBossPhaseChange(sampleRate))
            } catch (_: InterruptedException) {
                // Background worker stopped cleanly
            } catch (e: Exception) {
                Log.w("AudioPlayer", "Background audio synthesis warning: ${e.message}")
            }
        }.apply {
            isDaemon = true
            name = "AudioPreloader"
            start()
        }
    }

    private fun createLoopingTrack(pcmData: ByteArray, sampleRate: Int, volume: Float): AudioTrack? {
        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                return null
            }

            track.write(pcmData, 0, pcmData.size)
            val numFrames = pcmData.size / 2
            track.setLoopPoints(0, numFrames, -1) // Loop continuously
            track.setVolume(volume.coerceIn(0f, 1f))
            track
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Failed to create looping track: ${e.message}")
            null
        }
    }

    private fun applyMasterMute(muted: Boolean) {
        if (muted) {
            try { bgmTrack?.pause() } catch (_: Exception) {}
            try { currentAmbienceTrack?.pause() } catch (_: Exception) {}
            try { rainTrack?.pause() } catch (_: Exception) {}
            sfxMixer.pause()
        } else {
            sfxMixer.resume()
            if (isMusicEnabled && !isGlobalMuted && isInBattlefield) {
                try { bgmTrack?.play() } catch (_: Exception) {}
            }
            if (isAmbienceEnabled && !isGlobalMuted && isInBattlefield) {
                try { currentAmbienceTrack?.play() } catch (_: Exception) {}
                if (isRainRequested) {
                    try { rainTrack?.play() } catch (_: Exception) {}
                }
            }
        }
    }

    override fun playSound(sound: GameSound) {
        if (isMuted || isGlobalMuted || !isSfxEnabled) return

        // Prevent in-game combat and wave sounds from playing automatically outside of active gameplay
        val isUiSound = sound == GameSound.BUTTON_CLICK || sound == GameSound.COIN_REWARD
        if (!isUiSound && !isInBattlefield) return

        // Event-based throttling to prevent sound stacking and lag
        val now = System.currentTimeMillis()
        val cooldown = when (sound) {
            GameSound.MACHINE_GUN_FIRE -> 50L
            GameSound.RAPID_FIRE -> 38L
            GameSound.CANNON_FIRE -> 110L
            GameSound.FROST_FIRE -> 75L
            GameSound.ENEMY_HIT -> 45L
            GameSound.HEAVY_ENEMY_HIT -> 55L
            GameSound.CANNON_IMPACT -> 85L
            GameSound.FROST_IMPACT -> 70L
            GameSound.BOSS_IMPACT -> 90L
            GameSound.TREE_HIT, GameSound.STONE_HIT, GameSound.OBJECT_HIT -> 45L
            GameSound.TREE_DESTROYED, GameSound.STONE_DESTROYED, GameSound.OBJECT_DESTROY -> 60L
            GameSound.ENEMY_DEFEATED -> 50L
            GameSound.COIN_REWARD -> 70L
            GameSound.BUTTON_CLICK -> 40L
            GameSound.INVALID_PLACEMENT -> 60L
            GameSound.RUNNER_DASH -> 120L
            GameSound.SHIELD_HIT -> 60L
            GameSound.SHIELD_BREAK -> 150L
            GameSound.ARMOR_BREAK -> 150L
            GameSound.HEAL_PULSE -> 200L
            GameSound.SUMMON_MINIONS -> 250L
            GameSound.STEALTH_CLOAK -> 180L
            GameSound.BOSS_APPEARANCE -> 1000L
            GameSound.BOSS_SHOCKWAVE -> 300L
            GameSound.BOSS_PHASE_CHANGE -> 500L
            else -> 0L
        }

        val last = lastPlayTimes[sound] ?: 0L
        if (now - last < cooldown) return
        lastPlayTimes[sound] = now

        val vol = sfxVolume.coerceIn(0f, 1f)

        try {
            when (sound) {
                GameSound.MACHINE_GUN_FIRE -> {
                    if (machineGunVariations.isNotEmpty()) {
                        val samples = machineGunVariations[mgIndex % machineGunVariations.size]
                        mgIndex++
                        sfxMixer.play(samples, vol)
                    }
                }
                GameSound.RAPID_FIRE -> {
                    if (rapidFireVariations.isNotEmpty()) {
                        val samples = rapidFireVariations[rapidIndex % rapidFireVariations.size]
                        rapidIndex++
                        sfxMixer.play(samples, vol)
                    }
                }
                GameSound.CANNON_FIRE -> {
                    if (cannonVariations.isNotEmpty()) {
                        val samples = cannonVariations[cannonIndex % cannonVariations.size]
                        cannonIndex++
                        sfxMixer.play(samples, vol)
                    }
                }
                else -> {
                    val samples = soundMap[sound] ?: return
                    sfxMixer.play(samples, vol)
                }
            }
        } catch (_: Exception) {
            // Non-fatal playback error
        }
    }

    override fun setRainActive(isRaining: Boolean) {
        if (isRainRequested == isRaining && (rainTrack != null || !isRaining)) return
        isRainRequested = isRaining

        if (isRaining) {
            if (rainTrack == null) {
                try {
                    val sampleRate = AudioSynthesizer.DEFAULT_SAMPLE_RATE
                    val rainData = AudioSynthesizer.synthesizeRainSound(sampleRate)
                    rainTrack = createLoopingTrack(rainData, sampleRate, ambienceVolume * 0.95f)
                } catch (e: Exception) {
                    Log.w("AudioPlayer", "Failed to synthesize rain sound: ${e.message}")
                }
            }
            if (isAmbienceEnabled && !isMuted && !isGlobalMuted && isInBattlefield) {
                try { rainTrack?.play() } catch (_: Exception) {}
            }
        } else {
            try { rainTrack?.pause() } catch (_: Exception) {}
        }
    }

    override fun updateMapAmbience(environmentType: EnvironmentType) {
        if (currentEnvironment == environmentType && currentAmbienceTrack != null) {
            if (environmentType == EnvironmentType.TEMPEST_RAIN) {
                setRainActive(true)
            }
            return
        }
        currentEnvironment = environmentType

        try {
            currentAmbienceTrack?.stop()
            currentAmbienceTrack?.release()
            currentAmbienceTrack = null

            val sampleRate = AudioSynthesizer.DEFAULT_SAMPLE_RATE
            val ambData = AudioSynthesizer.synthesizeAmbience(sampleRate, environmentType)
            currentAmbienceTrack = createLoopingTrack(ambData, sampleRate, ambienceVolume)

            if (isAmbienceEnabled && !isMuted && !isGlobalMuted && isInBattlefield) {
                currentAmbienceTrack?.play()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Error updating map ambience: ${e.message}")
        }

        // Automatic rain sound activation for rainy environment
        if (environmentType == EnvironmentType.TEMPEST_RAIN) {
            setRainActive(true)
        } else if (!isRainRequested) {
            setRainActive(false)
        }
    }

    override fun pauseAmbienceAndMusic() {
        try {
            bgmTrack?.pause()
            currentAmbienceTrack?.pause()
            rainTrack?.pause()
            sfxMixer.pause()
        } catch (_: Exception) {}
    }

    override fun resumeAmbienceAndMusic() {
        try {
            sfxMixer.resume()
            if (!isMuted && !isGlobalMuted && isInBattlefield) {
                if (isMusicEnabled) bgmTrack?.play()
                if (isAmbienceEnabled) {
                    currentAmbienceTrack?.play()
                    if (isRainRequested) {
                        rainTrack?.play()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override fun release() {
        isReleased = true
        loaderThread?.interrupt()
        loaderThread = null
        try {
            bgmTrack?.stop()
            bgmTrack?.release()
            bgmTrack = null

            currentAmbienceTrack?.stop()
            currentAmbienceTrack?.release()
            currentAmbienceTrack = null

            rainTrack?.stop()
            rainTrack?.release()
            rainTrack = null

            sfxMixer.stop()
            soundMap.clear()
            machineGunVariations.clear()
            rapidFireVariations.clear()
            cannonVariations.clear()
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Error releasing audio tracks: ${e.message}")
        }
    }

    companion object {
        const val PREFS_NAME = "tower_defense_audio_prefs"
        const val KEY_SFX_ENABLED = "sfx_enabled"
        const val KEY_SFX_VOLUME = "sfx_volume"
        const val KEY_MUSIC_ENABLED = "music_enabled"
        const val KEY_MUSIC_VOLUME = "music_volume"
        const val KEY_AMBIENCE_ENABLED = "ambience_enabled"
        const val KEY_AMBIENCE_VOLUME = "ambience_volume"

        const val DEFAULT_SFX_VOLUME = 0.85f
        const val DEFAULT_MUSIC_VOLUME = 0.40f
        const val DEFAULT_AMBIENCE_VOLUME = 0.35f

        var isGlobalMuted: Boolean = false
        fun isSoundEnabled(): Boolean = !isGlobalMuted
        fun setSoundEnabled(enabled: Boolean) {
            isGlobalMuted = !enabled
            instance?.let { it.isMuted = !enabled }
        }

        @Volatile
        private var instance: AndroidAudioPlayer? = null

        fun getInstance(context: Context? = null): AndroidAudioPlayer {
            val player = instance ?: synchronized(this) {
                instance ?: AndroidAudioPlayer(context).also {
                    instance = it
                }
            }
            if (context != null) {
                player.initContext(context)
            }
            return player
        }

        fun playButtonClick() {
            instance?.buttonClick()
        }
    }
}

/**
 * Direct PCM Software SFX Mixer.
 * Routes audio directly into a single streaming AudioTrack sink, eliminating
 * SoundPool, Codec2 queries, file I/O, and system component bottlenecks on emulator and hardware.
 */
class PcmSfxMixer(private val sampleRate: Int = AudioSynthesizer.DEFAULT_SAMPLE_RATE) {
    private var sfxTrack: AudioTrack? = null
    @Volatile private var isRunning = false
    @Volatile private var isPaused = false
    private val activeVoices = CopyOnWriteArrayList<ActiveVoice>()
    private val voiceLock = Object()

    data class ActiveVoice(
        val samples: ShortArray,
        var position: Int = 0,
        val volume: Float
    )

    fun start() {
        if (isRunning) return
        isRunning = true
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = maxOf(minBuf, sampleRate / 10 * 2) // ~100ms buffer
            sfxTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            sfxTrack?.play()
        } catch (e: Exception) {
            Log.w("PcmSfxMixer", "Failed to initialize SFX AudioTrack: ${e.message}")
            return
        }

        Thread {
            val chunkSize = 441 // 20ms chunk at 22050Hz
            val mixBuffer = ShortArray(chunkSize)

            while (isRunning) {
                if (isPaused || activeVoices.isEmpty()) {
                    try {
                        synchronized(voiceLock) {
                            if (isRunning && (isPaused || activeVoices.isEmpty())) {
                                voiceLock.wait(80)
                            }
                        }
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                mixBuffer.fill(0)
                for (voice in activeVoices) {
                    val remaining = voice.samples.size - voice.position
                    val toMix = minOf(chunkSize, remaining)
                    val vol = voice.volume
                    for (i in 0 until toMix) {
                        val mixed = mixBuffer[i] + (voice.samples[voice.position + i] * vol).toInt()
                        mixBuffer[i] = mixed.coerceIn(-32768, 32767).toShort()
                    }
                    voice.position += toMix
                    if (voice.position >= voice.samples.size) {
                        activeVoices.remove(voice)
                    }
                }

                try {
                    sfxTrack?.write(mixBuffer, 0, chunkSize)
                } catch (_: Exception) {}
            }
        }.apply {
            isDaemon = true
            name = "PcmSfxMixerThread"
            start()
        }
    }

    fun play(samples: ShortArray, volume: Float) {
        if (!isRunning || isPaused || volume <= 0f) return
        activeVoices.add(ActiveVoice(samples, 0, volume.coerceIn(0f, 1f)))
        synchronized(voiceLock) {
            voiceLock.notifyAll()
        }
    }

    fun pause() {
        isPaused = true
        try { sfxTrack?.pause() } catch (_: Exception) {}
    }

    fun resume() {
        isPaused = false
        try { sfxTrack?.play() } catch (_: Exception) {}
        synchronized(voiceLock) {
            voiceLock.notifyAll()
        }
    }

    fun stop() {
        isRunning = false
        synchronized(voiceLock) {
            voiceLock.notifyAll()
        }
        try {
            sfxTrack?.stop()
            sfxTrack?.release()
            sfxTrack = null
            activeVoices.clear()
        } catch (_: Exception) {}
    }
}
