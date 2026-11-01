package com.safespot.scanner.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

/** Designer art in res/drawable-nodpi, looked up by name so missing files simply fall back. */
@Composable
fun artId(name: String): Int { val c = LocalContext.current
    return remember(name) { c.resources.getIdentifier(name, "drawable", c.packageName) } }

/** Draws the named art if it exists, otherwise [fallback]. */
@Composable
fun Art(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, alpha: Float = 1f, filter: ColorFilter? = null, fallback: @Composable () -> Unit = {}) {
    val id = artId(name)
    if (id != 0) Image(painterResource(id), null, modifier, contentScale = scale, alpha = alpha, colorFilter = filter) else fallback()
}

fun toolArt(id: String) = when (id) { "#scan" -> "ic_tool_spy"; "#deep" -> "ic_tool_antivirus"; "#vpn" -> "ic_tool_vpn"; "" -> ""; else -> "ic_tool_$id" }
