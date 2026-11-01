package com.safespot.scanner.ads

import android.graphics.Color as AColor
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.safespot.scanner.ui.C
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Native ad card for tool result screens. Clearly labelled "Ad", rendered in its own card with
 * generous spacing above/below so it can never be mistaken for (or tapped instead of) an app button.
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    if (!Ads.ready) return
    val ad = remember { Ads.takeNative(ctx) } ?: run { LaunchedEffect(Unit) { Ads.preloadNative(ctx) }; return }
    DisposableEffect(ad) { onDispose { ad.destroy() } }
    Spacer(Modifier.height(24.dp))
    AndroidView(factory = { c -> build(c, ad, C.Ink.toArgb(), C.Silver.toArgb(), C.Glass.copy(alpha = maxOf(C.Glass.alpha, .10f)).toArgb(), C.GlassBorder.toArgb(), C.Accent.toArgb()) }, modifier = modifier.fillMaxWidth())
    Spacer(Modifier.height(24.dp))
}

private fun build(c: android.content.Context, ad: NativeAd, ink: Int, sub: Int, bg: Int, stroke: Int, accent: Int): NativeAdView {
    fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), c.resources.displayMetrics).toInt()
    val root = NativeAdView(c)
    val col = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(14))
        background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(bg); setStroke(dp(1), stroke) }
    }
    val top = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val badge = TextView(c).apply { text = "Ad"; setTextColor(AColor.BLACK); textSize = 11f; setPadding(dp(6), dp(1), dp(6), dp(1))
        background = GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(0xFFFFB020.toInt()) } }
    val icon = ImageView(c).apply { layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(10) } }
    val head = TextView(c).apply { setTextColor(ink); textSize = 15f; setPadding(dp(10), 0, 0, 0); maxLines = 2 }
    top.addView(badge); top.addView(icon); top.addView(head, LinearLayout.LayoutParams(0, -2, 1f))
    val body = TextView(c).apply { setTextColor(sub); textSize = 13f; setPadding(0, dp(8), 0, dp(8)); maxLines = 3 }
    val media = MediaView(c).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(160)) }
    val cta = Button(c).apply { isAllCaps = false; setTextColor(AColor.WHITE)
        background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(accent) } }
    col.addView(top); col.addView(body); col.addView(media)
    col.addView(cta, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(10) })
    root.addView(col)
    head.text = ad.headline; root.headlineView = head
    body.text = ad.body ?: ""; root.bodyView = body
    ad.icon?.drawable?.let { icon.setImageDrawable(it) } ?: run { icon.visibility = android.view.View.GONE }; root.iconView = icon
    cta.text = ad.callToAction ?: "Learn more"; root.callToActionView = cta
    root.mediaView = media
    root.setNativeAd(ad)
    return root
}
