package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.data.EnvironmentType

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
    BUTTON_CLICK;

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

    // Ambience & Music
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

    private var prefs: SharedPreferences? = null

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
            if (value && !isMuted) bgmTrack?.play() else bgmTrack?.pause()
        }

    override var isAmbienceEnabled: Boolean = true
        set(value) {
            field = value
            savePreference(KEY_AMBIENCE_ENABLED, value)
            val current = currentAmbienceTrack
            if (value && !isMuted) current?.play() else current?.pause()
        }

    override var sfxVolume: Float = DEFAULT_SFX_VOLUME
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            field = clamped
            savePreference(KEY_SFX_VOLUME, clamped)
            updateSfxVolumes()
        }

    override var musicVolume: Float = DEFAULT_MUSIC_VOLUME
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            field = clamped
            savePreference(KEY_MUSIC_VOLUME, clamped)
            bgmTrack?.setVolume(clamped)
        }

    override var ambienceVolume: Float = DEFAULT_AMBIENCE_VOLUME
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            field = clamped
            savePreference(KEY_AMBIENCE_VOLUME, clamped)
            currentAmbienceTrack?.setVolume(clamped)
        }

    // Tracks storage
    private val singleTracks = mutableMapOf<GameSound, AudioTrack>()
    private val machineGunTracks = mutableListOf<AudioTrack>()
    private val rapidFireTracks = mutableListOf<AudioTrack>()
    private val cannonTracks = mutableListOf<AudioTrack>()
    private val ambienceTracks = mutableMapOf<EnvironmentType, AudioTrack>()
    private var bgmTrack: AudioTrack? = null

    // Variations rotation indices
    private var mgIndex = 0
    private var rapidIndex = 0
    private var cannonIndex = 0

    // Currently playing ambience
    private var currentAmbienceTrack: AudioTrack? = null
    private var currentEnvironment: EnvironmentType? = null

    // Event-based cooldown throttling (milliseconds)
    private val lastPlayTimes = mutableMapOf<GameSound, Long>()

    init {
        context?.let { initContext(it) }
        try {
            pregenerateAllAudio()
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Audio synthesis init error: ${e.message}")
        }
    }

    fun initContext(context: Context) {
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

    private fun pregenerateAllAudio() {
        val sampleRate = AudioSynthesizer.DEFAULT_SAMPLE_RATE

        // 1. Machine Gun (3 variations)
        for (i in 0..2) {
            val data = AudioSynthesizer.synthesizeMachineGun(sampleRate, variation = i)
            createStaticTrack(data, sampleRate, sfxVolume)?.let { machineGunTracks.add(it) }
        }

        // 2. Cannon (2 variations)
        for (i in 0..1) {
            val data = AudioSynthesizer.synthesizeCannon(sampleRate, variation = i)
            createStaticTrack(data, sampleRate, sfxVolume)?.let { cannonTracks.add(it) }
        }

        // 3. Rapid Fire (3 variations)
        for (i in 0..2) {
            val data = AudioSynthesizer.synthesizeRapidFire(sampleRate, variation = i)
            createStaticTrack(data, sampleRate, sfxVolume)?.let { rapidFireTracks.add(it) }
        }

        // 4. Frost Gun
        registerSound(GameSound.FROST_FIRE, AudioSynthesizer.synthesizeFrostFire(sampleRate), sampleRate)

        // 5. Projectile Impacts
        registerSound(GameSound.ENEMY_HIT, AudioSynthesizer.synthesizeEnemyHit(sampleRate), sampleRate)
        registerSound(GameSound.HEAVY_ENEMY_HIT, AudioSynthesizer.synthesizeHeavyEnemyHit(sampleRate), sampleRate)
        registerSound(GameSound.CANNON_IMPACT, AudioSynthesizer.synthesizeCannonImpact(sampleRate), sampleRate)
        registerSound(GameSound.FROST_IMPACT, AudioSynthesizer.synthesizeFrostImpact(sampleRate), sampleRate)
        registerSound(GameSound.BOSS_IMPACT, AudioSynthesizer.synthesizeBossImpact(sampleRate), sampleRate)

        // 6. Destructibles
        registerSound(GameSound.TREE_HIT, AudioSynthesizer.synthesizeTreeHit(sampleRate), sampleRate)
        registerSound(GameSound.STONE_HIT, AudioSynthesizer.synthesizeStoneHit(sampleRate), sampleRate)
        registerSound(GameSound.OBJECT_HIT, AudioSynthesizer.synthesizeTreeHit(sampleRate), sampleRate)
        registerSound(GameSound.TREE_DESTROYED, AudioSynthesizer.synthesizeTreeDestroyed(sampleRate), sampleRate)
        registerSound(GameSound.STONE_DESTROYED, AudioSynthesizer.synthesizeStoneDestroyed(sampleRate), sampleRate)
        registerSound(GameSound.OBJECT_DESTROY, AudioSynthesizer.synthesizeStoneDestroyed(sampleRate), sampleRate)

        // 7. Game & UI Events
        registerSound(GameSound.TOWER_PLACED, AudioSynthesizer.synthesizeTowerPlaced(sampleRate), sampleRate)
        registerSound(GameSound.TOWER_UPGRADED, AudioSynthesizer.synthesizeTowerUpgraded(sampleRate), sampleRate)
        registerSound(GameSound.TOWER_SOLD, AudioSynthesizer.synthesizeTowerSold(sampleRate), sampleRate)
        registerSound(GameSound.INVALID_PLACEMENT, AudioSynthesizer.synthesizeInvalidPlacement(sampleRate), sampleRate)
        registerSound(GameSound.ENEMY_DEFEATED, AudioSynthesizer.synthesizeEnemyDeath(sampleRate), sampleRate)
        registerSound(GameSound.BOSS_APPEARANCE, AudioSynthesizer.synthesizeBossAppearance(sampleRate), sampleRate)
        registerSound(GameSound.BOSS_DEFEATED, AudioSynthesizer.synthesizeBossDefeated(sampleRate), sampleRate)
        registerSound(GameSound.WAVE_START, AudioSynthesizer.synthesizeWaveStarted(sampleRate), sampleRate)
        registerSound(GameSound.WAVE_CLEAR, AudioSynthesizer.synthesizeWaveCleared(sampleRate), sampleRate)
        registerSound(GameSound.VICTORY, AudioSynthesizer.synthesizeVictory(sampleRate), sampleRate)
        registerSound(GameSound.GAME_OVER, AudioSynthesizer.synthesizeDefeat(sampleRate), sampleRate)
        registerSound(GameSound.COIN_REWARD, AudioSynthesizer.synthesizeCoinReward(sampleRate), sampleRate)
        registerSound(GameSound.BUTTON_CLICK, AudioSynthesizer.synthesizeButtonClick(sampleRate), sampleRate)

        // 8. Ambience Loops for Environments
        listOf(
            EnvironmentType.GREEN_VALLEY,
            EnvironmentType.DESERT_CANYON,
            EnvironmentType.FOREST_CROSSROADS,
            EnvironmentType.SNOW_VALLEY,
            EnvironmentType.NIGHT_FORTRESS
        ).forEach { env ->
            val ambData = AudioSynthesizer.synthesizeAmbience(sampleRate, env)
            createLoopingTrack(ambData, sampleRate, ambienceVolume)?.let {
                ambienceTracks[env] = it
            }
        }

        // 9. Background Music Loop
        val bgmData = AudioSynthesizer.synthesizeBackgroundMusic(sampleRate)
        bgmTrack = createLoopingTrack(bgmData, sampleRate, musicVolume)
        if (isMusicEnabled && !isMuted && !isGlobalMuted) {
            bgmTrack?.play()
        }
    }

    private fun registerSound(sound: GameSound, pcmData: ByteArray, sampleRate: Int) {
        createStaticTrack(pcmData, sampleRate, sfxVolume)?.let {
            singleTracks[sound] = it
        }
    }

    private fun createStaticTrack(pcmData: ByteArray, sampleRate: Int, volume: Float): AudioTrack? {
        return try {
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
            track.setVolume(volume.coerceIn(0f, 1f))
            track
        } catch (e: Exception) {
            null
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

            track.write(pcmData, 0, pcmData.size)
            val numFrames = pcmData.size / 2
            track.setLoopPoints(0, numFrames, -1) // Loop continuously
            track.setVolume(volume.coerceIn(0f, 1f))
            track
        } catch (e: Exception) {
            null
        }
    }

    private fun updateSfxVolumes() {
        singleTracks.values.forEach { try { it.setVolume(sfxVolume) } catch (_: Exception) {} }
        machineGunTracks.forEach { try { it.setVolume(sfxVolume) } catch (_: Exception) {} }
        rapidFireTracks.forEach { try { it.setVolume(sfxVolume) } catch (_: Exception) {} }
        cannonTracks.forEach { try { it.setVolume(sfxVolume) } catch (_: Exception) {} }
    }

    private fun applyMasterMute(muted: Boolean) {
        if (muted) {
            bgmTrack?.pause()
            currentAmbienceTrack?.pause()
        } else {
            if (isMusicEnabled) bgmTrack?.play()
            if (isAmbienceEnabled) currentAmbienceTrack?.play()
        }
    }

    override fun playSound(sound: GameSound) {
        if (isMuted || isGlobalMuted || !isSfxEnabled) return

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
            else -> 0L
        }

        val last = lastPlayTimes[sound] ?: 0L
        if (now - last < cooldown) return
        lastPlayTimes[sound] = now

        when (sound) {
            GameSound.MACHINE_GUN_FIRE -> {
                if (machineGunTracks.isNotEmpty()) {
                    val track = machineGunTracks[mgIndex % machineGunTracks.size]
                    mgIndex++
                    playStaticTrack(track)
                }
            }
            GameSound.RAPID_FIRE -> {
                if (rapidFireTracks.isNotEmpty()) {
                    val track = rapidFireTracks[rapidIndex % rapidFireTracks.size]
                    rapidIndex++
                    playStaticTrack(track)
                }
            }
            GameSound.CANNON_FIRE -> {
                if (cannonTracks.isNotEmpty()) {
                    val track = cannonTracks[cannonIndex % cannonTracks.size]
                    cannonIndex++
                    playStaticTrack(track)
                }
            }
            else -> {
                val track = singleTracks[sound] ?: return
                playStaticTrack(track)
            }
        }
    }

    private fun playStaticTrack(track: AudioTrack) {
        try {
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (e: Exception) {
            // Hardware busy / non-fatal
        }
    }

    override fun updateMapAmbience(environmentType: EnvironmentType) {
        if (currentEnvironment == environmentType && currentAmbienceTrack != null) return
        currentEnvironment = environmentType

        try {
            currentAmbienceTrack?.pause()
            val targetKey = when (environmentType) {
                EnvironmentType.GREEN_VALLEY -> EnvironmentType.GREEN_VALLEY
                EnvironmentType.DESERT_CANYON -> EnvironmentType.DESERT_CANYON
                EnvironmentType.SNOW_VALLEY -> EnvironmentType.SNOW_VALLEY
                EnvironmentType.NIGHT_FORTRESS -> EnvironmentType.NIGHT_FORTRESS
                else -> EnvironmentType.FOREST_CROSSROADS
            }
            val track = ambienceTracks[targetKey]
            currentAmbienceTrack = track
            track?.setVolume(ambienceVolume)
            if (isAmbienceEnabled && !isMuted && !isGlobalMuted) {
                track?.play()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Error updating map ambience: ${e.message}")
        }
    }

    override fun pauseAmbienceAndMusic() {
        try {
            bgmTrack?.pause()
            currentAmbienceTrack?.pause()
        } catch (_: Exception) {}
    }

    override fun resumeAmbienceAndMusic() {
        try {
            if (!isMuted && !isGlobalMuted) {
                if (isMusicEnabled) bgmTrack?.play()
                if (isAmbienceEnabled) currentAmbienceTrack?.play()
            }
        } catch (_: Exception) {}
    }

    override fun release() {
        try {
            bgmTrack?.stop()
            bgmTrack?.release()
            bgmTrack = null

            currentAmbienceTrack?.stop()
            ambienceTracks.values.forEach {
                try {
                    it.stop()
                    it.release()
                } catch (_: Exception) {}
            }
            ambienceTracks.clear()

            machineGunTracks.forEach { try { it.release() } catch (_: Exception) {} }
            machineGunTracks.clear()

            rapidFireTracks.forEach { try { it.release() } catch (_: Exception) {} }
            rapidFireTracks.clear()

            cannonTracks.forEach { try { it.release() } catch (_: Exception) {} }
            cannonTracks.clear()

            singleTracks.values.forEach { try { it.release() } catch (_: Exception) {} }
            singleTracks.clear()
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
            return instance ?: synchronized(this) {
                instance ?: AndroidAudioPlayer(context).also {
                    instance = it
                }
            }
        }

        fun playButtonClick() {
            instance?.buttonClick()
        }
    }
}
