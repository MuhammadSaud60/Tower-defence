package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single, unified listener for rewarded ad events.
 * AdManager -> RewardAdListener -> GameEngine / GameController
 */
interface RewardAdListener {
    /**
     * Ad opened full screen. Pause game: enemy movement, tower firing, timers, animations, audio.
     */
    fun onAdOpened()

    /**
     * User completed watching the ad and earned the reward.
     * Restores base health (+20% of max base HP) and marks reviveUsed = true.
     */
    fun onUserEarnedReward()

    /**
     * Ad dismissed by user after reward was earned.
     * Resume game only after reward decision completed and game state restored.
     */
    fun onAdCompletedWithReward()

    /**
     * Ad dismissed without user completing / earning reward.
     * Keep game paused on Defeat screen.
     */
    fun onAdClosedWithoutReward()

    /**
     * Ad failed to show.
     */
    fun onAdFailedToShow(errorMessage: String)
}

/**
 * AdState captures the lifecycle status of Rewarded Ads.
 */
sealed class AdState {
    object Idle : AdState()
    object Loading : AdState()
    object Ready : AdState()
    object Showing : AdState()
    data class Error(val message: String) : AdState()
}

/**
 * AdManager coordinates Google AdMob initialization, rewarded ad preloading,
 * presentation, and reward validation.
 *
 * Responsibilities:
 * - Proper lifecycle: Never reuses expired or consumed ad instances.
 * - Single reward callback strictly from onUserEarnedReward.
 * - Prevents duplicate or missing rewards using rewardPending/rewardEarned state flags.
 * - Manages pause/resume synchronization with fullScreenContentCallback.
 * - Immediately loads fresh ad instances upon completion/dismissal for future defeats.
 */
class AdManager private constructor() {

    companion object {
        private const val TAG = "AdManager"

        // Rewarded Ad Unit: Rewarded_Continue_After_Defeat
        const val REWARDED_CONTINUE_AFTER_DEFEAT_AD_UNIT_ID = "ca-app-pub-1347232629548060/8680920818"
        const val TEST_REWARDED_AD_UNIT_ID = REWARDED_CONTINUE_AFTER_DEFEAT_AD_UNIT_ID

        @Volatile
        private var instance: AdManager? = null

        fun getInstance(): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager().also { instance = it }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isInitialized = false
    @Volatile
    private var rewardedAd: RewardedAd? = null
    @Volatile
    private var isCurrentlyLoading = false

    // State tracking to prevent duplicate or missing rewards
    @Volatile
    private var rewardPending = false
    @Volatile
    private var rewardEarned = false

    private val _adState = MutableStateFlow<AdState>(AdState.Idle)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    /**
     * Initializes the Mobile Ads SDK. Safe to call multiple times.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        try {

            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(config)

            MobileAds.initialize(appContext) { initStatus ->
                Log.d(TAG, "MobileAds SDK initialized: $initStatus")
                isInitialized = true
                preloadRewardedAd(appContext)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing MobileAds SDK", e)
            _adState.value = AdState.Error(e.message ?: "Ad initialization error")
        }
    }

    /**
     * Preloads a fresh rewarded ad instance for future use.
     * Ensures we never reuse an expired or consumed ad object.
     */
    fun preloadRewardedAd(context: Context) {
        val appContext = context.applicationContext

        // Ensure execution happens on main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { preloadRewardedAd(appContext) }
            return
        }

        if (rewardedAd != null) {
            Log.d(TAG, "A rewarded ad is already ready and cached.")
            _adState.value = AdState.Ready
            return
        }

        if (isCurrentlyLoading) {
            Log.d(TAG, "A rewarded ad is already currently loading.")
            return
        }

        isCurrentlyLoading = true
        _adState.value = AdState.Loading
        Log.d(TAG, "Loading fresh RewardedAd from AdMob...")

        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                appContext,
                REWARDED_CONTINUE_AFTER_DEFEAT_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "Rewarded ad loaded successfully.")
                        rewardedAd = ad
                        isCurrentlyLoading = false
                        _adState.value = AdState.Ready
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(TAG, "Rewarded ad failed to load: code=${loadAdError.code}, msg=${loadAdError.message}")
                        rewardedAd = null
                        isCurrentlyLoading = false
                        _adState.value = AdState.Error(loadAdError.message)
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during RewardedAd.load", e)
            rewardedAd = null
            isCurrentlyLoading = false
            _adState.value = AdState.Error(e.message ?: "Failed to load ad")
        }
    }

    /**
     * Checks if a rewarded ad is currently cached and ready to show.
     */
    fun isAdAvailable(): Boolean = rewardedAd != null

    /**
     * Shows the rewarded ad to the player following the exact lifecycle requirements:
     * - Checks if rewardedAd != null (never shows empty reference)
     * - Destroys the reference immediately before showing so it cannot be reused
     * - Manages rewardPending / rewardEarned state flags
     * - Calls listener.onAdOpened() when full-screen content shows to pause game
     * - Calls listener.onUserEarnedReward() strictly from onUserEarnedReward callback
     * - Calls listener.onAdCompletedWithReward() or onAdClosedWithoutReward() on dismissal
     * - Immediately loads a fresh rewarded ad for future defeats
     */
    fun showRewardedAd(
        activity: Activity,
        listener: RewardAdListener
    ) {
        val appContext = activity.applicationContext

        // Ensure execution happens on main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showRewardedAd(activity, listener) }
            return
        }

        // 4. Handle Ad Loading Correctly: Check if rewardedAd != null
        val adToShow = rewardedAd
        if (adToShow == null) {
            Log.w(TAG, "showRewardedAd requested but no ad is loaded. Triggering load.")
            listener.onAdFailedToShow("Ad not ready yet. Please try again.")
            preloadRewardedAd(appContext)
            return
        }

        // 1. Lifecycle: Clear current ad reference immediately! Destroy old ad instance reference
        rewardedAd = null
        isCurrentlyLoading = false

        // 3. Prevent duplicate and missing rewards: Reward state tracking
        rewardPending = true
        rewardEarned = false
        _adState.value = AdState.Showing

        // 6. Handle All Ad Callbacks
        adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowedFullScreenContent: Pausing game systems.")
                // 5. Game Pause: Pause enemy movement, tower firing, timers, animations, wave system, audio
                listener.onAdOpened()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "onAdDismissedFullScreenContent: rewardEarned=$rewardEarned, rewardPending=$rewardPending")
                _adState.value = AdState.Idle

                // Crucial: Clean fullScreenContentCallback to avoid any leak
                adToShow.fullScreenContentCallback = null

                val wasRewardEarned = rewardEarned
                // Reset state
                rewardPending = false
                rewardEarned = false

                // 5. Resume only after reward decision completed and game state restored
                if (wasRewardEarned) {
                    listener.onAdCompletedWithReward()
                } else {
                    listener.onAdClosedWithoutReward()
                }

                // 1. Immediately prepare/load a new rewarded ad for future use
                preloadRewardedAd(appContext)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "onAdFailedToShowFullScreenContent: ${adError.message}")
                adToShow.fullScreenContentCallback = null

                // Reset state correctly
                rewardPending = false
                rewardEarned = false
                _adState.value = AdState.Error(adError.message)

                listener.onAdFailedToShow(adError.message)

                // Reload ad
                preloadRewardedAd(appContext)
            }
        }

        try {
            // 2. The reward should only happen inside onUserEarnedReward()
            adToShow.show(activity) { rewardItem ->
                Log.d(TAG, "AdMob onUserEarnedReward: type=${rewardItem.type}, amount=${rewardItem.amount}, rewardPending=$rewardPending")
                if (rewardPending) {
                    rewardEarned = true
                    rewardPending = false
                    listener.onUserEarnedReward()
                } else {
                    Log.w(TAG, "Reward callback ignored: rewardPending was false (duplicate prevented).")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during adToShow.show", e)
            adToShow.fullScreenContentCallback = null
            rewardPending = false
            rewardEarned = false
            _adState.value = AdState.Error(e.message ?: "Failed to display ad")
            listener.onAdFailedToShow(e.message ?: "Failed to display ad")
            preloadRewardedAd(appContext)
        }
    }
}
