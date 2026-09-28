package com.example.ads

import android.app.Activity
import android.content.Context
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
 * - Initialize Mobile Ads SDK safely
 * - Preload test rewarded ads ahead of time
 * - Show rewarded ads and grant revive rewards only upon ad completion callback
 * - Never crash when offline or if Play Services are absent
 */
class AdManager private constructor() {

    companion object {
        private const val TAG = "AdManager"

        // Official Google AdMob sample test ad unit ID for Rewarded Video
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        @Volatile
        private var instance: AdManager? = null

        fun getInstance(): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager().also { instance = it }
            }
        }
    }

    private var isInitialized = false
    private var rewardedAd: RewardedAd? = null
    private var isCurrentlyLoading = false

    private val _adState = MutableStateFlow<AdState>(AdState.Idle)
    val adState: StateFlow<AdState> = _adState.asStateFlow()

    /**
     * Initializes the Mobile Ads SDK. Safe to call multiple times.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(config)

            MobileAds.initialize(context.applicationContext) { initStatus ->
                Log.d(TAG, "MobileAds SDK initialized: $initStatus")
                isInitialized = true
                preloadRewardedAd(context.applicationContext)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing MobileAds SDK", e)
            _adState.value = AdState.Error(e.message ?: "Ad initialization error")
        }
    }

    /**
     * Preloads a rewarded ad before it is needed so it is ready on the defeat screen.
     */
    fun preloadRewardedAd(context: Context) {
        if (rewardedAd != null || isCurrentlyLoading) {
            if (rewardedAd != null) {
                _adState.value = AdState.Ready
            }
            return
        }

        isCurrentlyLoading = true
        _adState.value = AdState.Loading

        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context.applicationContext,
                TEST_REWARDED_AD_UNIT_ID,
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
     * Shows the rewarded ad to the player.
     *
     * @param activity The hosting Activity
     * @param onUserEarnedReward Invoked ONLY when the user completely watches the ad and earns the reward
     * @param onAdClosedWithoutReward Invoked if the user dismisses the ad early without earning reward
     * @param onAdFailedToShow Invoked if the ad fails to display
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdClosedWithoutReward: () -> Unit = {},
        onAdFailedToShow: (String) -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            Log.w(TAG, "showRewardedAd requested but no ad is loaded.")
            onAdFailedToShow("Ad not ready")
            preloadRewardedAd(activity)
            return
        }

        var rewardGranted = false
        _adState.value = AdState.Showing

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed fullscreen content.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed fullscreen content. Reward granted: $rewardGranted")
                rewardedAd = null
                _adState.value = AdState.Idle
                // Preload the next rewarded ad for future attempts
                preloadRewardedAd(activity)

                if (!rewardGranted) {
                    onAdClosedWithoutReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _adState.value = AdState.Error(adError.message)
                preloadRewardedAd(activity)
                onAdFailedToShow(adError.message)
            }
        }

        try {
            currentAd.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.type}, amount: ${rewardItem.amount}")
                rewardGranted = true
                onUserEarnedReward()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during RewardedAd.show", e)
            rewardedAd = null
            _adState.value = AdState.Error(e.message ?: "Failed to display ad")
            preloadRewardedAd(activity)
            onAdFailedToShow(e.message ?: "Failed to display ad")
        }
    }
}
