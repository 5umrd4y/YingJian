package com.yingjian.core.util

import android.content.Context
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/**
 * Compose Dp to physical millimeters.
 * Requires densityDpi because Compose Density only provides density: Float (= densityDpi / 160).
 */
fun Dp.toMm(density: Density, densityDpi: Int): Float =
    with(density) { this@toMm.toPx() * 25.4f / densityDpi }

/**
 * Physical millimeters to 300DPI print pixels.
 */
fun Float.mmToPx300Dpi(): Int = (this * 300 / 25.4f).roundToInt()

/**
 * 300DPI print pixels back to physical millimeters.
 */
fun Int.px300DpiToMm(): Float = this * 25.4f / 300

/**
 * Compose Dp to 300DPI print pixels (full chain: Dp -> MM -> 300DPI Px).
 */
fun Dp.toPx300Dpi(density: Density, densityDpi: Int): Int =
    this.toMm(density, densityDpi).mmToPx300Dpi()

/**
 * Get device densityDpi from Context.
 */
fun Context.getDensityDpi(): Int = resources.displayMetrics.densityDpi
