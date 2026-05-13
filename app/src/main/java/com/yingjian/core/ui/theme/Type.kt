package com.yingjian.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.font.Font
import com.yingjian.R

// Default font families — Noto Serif SC ExtraLight is loaded from res/font for printer-style text
val NotoSansSC = FontFamily.Default
val SourceSans3 = FontFamily.Default

val YingJianTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoSansSC,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Light
    ),
    headlineLarge = TextStyle(
        fontFamily = NotoSansSC,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Normal
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansSC,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyLarge = TextStyle(
        fontFamily = SourceSans3,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = SourceSans3,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = SourceSans3,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    )
)

val PrinterTextStyle = TextStyle(
    fontFamily = FontFamily(
        Font(R.font.noto_serif_sc_extralight, FontWeight.ExtraLight)
    ),
    fontWeight = FontWeight.ExtraLight,
    fontSize = 11.sp,
    letterSpacing = 0.2.em
)
