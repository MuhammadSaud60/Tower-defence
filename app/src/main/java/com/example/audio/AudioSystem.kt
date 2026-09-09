package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

enum class GameSound {
    TOWER_FIRE_BULLET,
    TOWER_FIRE_CANNON,
    TOWER_FIRE_RAPID,
    ENEMY_HIT,
    ENEMY_DEATH,
    CANNON_EXPLOSION,
    WAVE_START,
    BOSS_APPEARANCE,
    VICTORY,
    GAME_OVER
}

interface AudioPlayer {
    var isMuted: Boolean
    fun playSound(sound: GameSound)
}

class AndroidAudioPlayer : AudioPlayer {
    override var isMuted: Boolean = false
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
        } catch (e: Exception) {
            Log.w("AudioPlayer", "Failed to initialize ToneGenerator", e)
        }
    }

    override fun playSound(sound: GameSound) {
        if (isMuted) return
        try {
            when (sound) {
                GameSound.TOWER_FIRE_BULLET -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                GameSound.TOWER_FIRE_CANNON -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 80)
                GameSound.TOWER_FIRE_RAPID -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 25)
                GameSound.ENEMY_HIT -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 20)
                GameSound.ENEMY_DEATH -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 60)
                GameSound.CANNON_EXPLOSION -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_LOW_L, 90)
                GameSound.WAVE_START -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                GameSound.BOSS_APPEARANCE -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300)
                GameSound.VICTORY -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_SS, 400)
                GameSound.GAME_OVER -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_LOW_L, 500)
            }
        } catch (e: Exception) {
            // Ignore sound error in emulator/headless environments
        }
    }
}
