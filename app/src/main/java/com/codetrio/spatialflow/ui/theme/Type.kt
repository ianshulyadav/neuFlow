package com.codetrio.spatialflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.googlefonts.Font as GFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.codetrio.spatialflow.R

@OptIn(ExperimentalTextApi::class)
val GoogleSansRounded = FontFamily(
    Font(R.font.google_flex, weight = FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight), FontVariation.Setting("ROND", 100f))))

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(Font(R.font.google_flex, variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 0f))))
val GoogleSansFlexNonRounded = GoogleSansFlex

private val gProvider = GoogleFont.Provider("com.google.android.gms.fonts", "com.google.android.gms", R.array.com_google_android_gms_fonts_certs)
val Montserrat = GoogleFont("Montserrat")
val MontserratFamily = FontFamily(
    GFont(googleFont = Montserrat, fontProvider = gProvider, weight = FontWeight.Black),
    GFont(googleFont = Montserrat, fontProvider = gProvider, weight = FontWeight.Bold),
    GFont(googleFont = Montserrat, fontProvider = gProvider, weight = FontWeight.SemiBold),
    GFont(googleFont = Montserrat, fontProvider = gProvider, weight = FontWeight.Normal))

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 60.sp, textGeometricTransform = TextGeometricTransform(scaleX = 1.5f), letterSpacing = (-0.02).em, lineHeight = 0.95.em),
    displayMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Normal, fontSize = 50.sp, letterSpacing = (-0.02).em, lineHeight = 0.95.em),
    headlineLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp))
