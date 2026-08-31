package com.liam.kaptalismusaufhalter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.liam.kaptalismusaufhalter.R

// Caprasimo: the display/heading font in the design (amounts, wish names, section titles).
val HeadingFont = FontFamily(Font(R.font.caprasimo_regular, FontWeight.Normal))

// Figtree: the body font in the design.
val BodyFont = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = HeadingFont, fontSize = 42.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = HeadingFont, fontSize = 19.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = HeadingFont, fontSize = 17.sp
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = HeadingFont, fontSize = 15.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 15.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.5.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 12.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.06.em
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 12.5.sp
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 11.5.sp
    )
)
