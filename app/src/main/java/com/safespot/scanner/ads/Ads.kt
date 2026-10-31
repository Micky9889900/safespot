package com.safespot.scanner.ads

import android.app.Activity
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.safespot.scanner.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ads live only in SafeSpot's own UI. Nothing is ever injected into VPN traffic.
 * Banner: bottom of screens. Interstitial: after scan results (max 1 per 90 s).
 * Rewarded: unlocks a Deep Scan.
 */
object Ads {
    private val started = AtomicBoolean(false)
    var ready by mutableStateOf(false); private set
    private var inter: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    var rewardedReady by mutableStateOf(false); private set
    private var lastInter = 0L

    fun init(a: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(a)
        info.requestConsentInfoUpdate(a, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(a) { if (info.canRequestAds()) start(a) }
        }, { })
        if (info.canRequestAds()) start(a)
    }

    private fun start(a: Activity) {
        if (!started.compareAndSet(false, true)) return
        Thread { MobileAds.initialize(a) {}; a.runOnUiThread { ready = true; loadInter(a); loadRewarded(a) } }.start()
    }

    private fun loadInter(a: Activity) {
        if (!ready || inter != null) return
        InterstitialAd.load(a, BuildConfig.AD_INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { inter = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { inter = null }
        })
    }
    private fun loadRewarded(a: Activity) {
        if (!ready || rewarded != null) return
        RewardedAd.load(a, BuildConfig.AD_REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; rewardedReady = true }
            override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null; rewardedReady = false }
        })
    }

    /** Show after scan results. [then] always runs. */
    fun interstitial(a: Activity, then: () -> Unit = {}) {
        val ad = inter; val now = SystemClock.elapsedRealtime()
        if (ad == null || now - lastInter < 90_000) { then(); loadInter(a); return }
        inter = null; lastInter = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { then(); loadInter(a) }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { then(); loadInter(a) }
        }
        ad.show(a)
    }

    /** onDone(true) when the reward was earned. If no ad is available we let the user through. */
    fun rewarded(a: Activity, onDone: (Boolean) -> Unit) {
        val ad = rewarded
        if (ad == null) { onDone(true); loadRewarded(a); return }
        rewarded = null; rewardedReady = false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { onDone(earned); loadRewarded(a) }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { onDone(true); loadRewarded(a) }
        }
        ad.show(a) { earned = true }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val w = LocalConfiguration.current.screenWidthDp
    if (!Ads.ready) return
    val view = remember { AdView(ctx).apply { adUnitId = BuildConfig.AD_BANNER_ID; setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, w)); loadAd(AdRequest.Builder().build()) } }
    DisposableEffect(view) { onDispose { view.destroy() } }
    AndroidView(factory = { view }, modifier = modifier.fillMaxWidth())
}
