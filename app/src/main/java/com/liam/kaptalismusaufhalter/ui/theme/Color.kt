package com.liam.kaptalismusaufhalter.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens extracted 1:1 from the design file's CSS custom properties.
val ColorBg = Color(0xFFF5EAD8)
val ColorSurface = Color(0xFFEBDDC5)
val ColorText = Color(0xFF201E1D)

val ColorAccent = Color(0xFFC67139)
val ColorAccent100 = Color(0xFFFFF2EB)
val ColorAccent200 = Color(0xFFFFE1D0)
val ColorAccent300 = Color(0xFFFFC6A5)
val ColorAccent400 = Color(0xFFF6A06B)
val ColorAccent700 = Color(0xFF8C491A)
val ColorAccent800 = Color(0xFF643312)
val ColorAccent900 = Color(0xFF402310)

val ColorAccent2 = Color(0xFF7A8A5E)
val ColorAccent2100 = Color(0xFFF0FAE1)
val ColorAccent2200 = Color(0xFFE1EECC)
val ColorAccent2300 = Color(0xFFCCDBB2)
val ColorAccent2600 = Color(0xFF728157)
val ColorAccent2700 = Color(0xFF56633F)
val ColorAccent2800 = Color(0xFF3D472B)

val ColorNeutral300 = Color(0xFFDCD3C4)
val ColorNeutral400 = Color(0xFFC0B6A5)
val ColorNeutral600 = Color(0xFF82796A)
val ColorNeutral700 = Color(0xFF645C50)

// Dark Entscheidung screen only - a separate token set in the design, not the shared theme.
val ColorDecisionBg = Color(0xFF272E1B)
val ColorDecisionText = Color(0xFFF2F5E9)
val ColorDecisionText300 = Color(0xFFCCDBB2)
val ColorDecisionText400 = Color(0xFFAEBF92)

// Legacy aliases kept so existing call sites keep compiling; prefer the ColorXxx tokens above.
val Background = ColorBg
val Surface = ColorSurface
val Primary = ColorAccent
val OnPrimary = ColorBg
val Secondary = ColorAccent2
val OnSecondary = ColorBg
val OnBackground = ColorText
val Outline = ColorNeutral700
val TrackColor = ColorNeutral300
