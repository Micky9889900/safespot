package com.safespot.scanner.ads

import com.safespot.scanner.BuildConfig

/** All ad unit IDs in one place. Edit the values in app/build.gradle.kts (ADMOB IDS block). */
object AdIds {
    val BANNER get() = BuildConfig.AD_BANNER_ID
    val INTERSTITIAL get() = BuildConfig.AD_INTERSTITIAL_ID
    val REWARDED get() = BuildConfig.AD_REWARDED_ID
    val NATIVE get() = BuildConfig.AD_NATIVE_ID
    val APP_OPEN get() = BuildConfig.AD_APP_OPEN_ID
}
