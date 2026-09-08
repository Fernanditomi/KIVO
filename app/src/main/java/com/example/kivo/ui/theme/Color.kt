package com.example.kivo.ui.theme

import androidx.compose.ui.graphics.Color

// KIVO PRO Palette (2026 Master)
val KivoBlack = Color(0xFF08090D)
val KivoPureBlack = Color(0xFF000000)
val KivoDeepDark = Color(0xFF08090D)

// Surfaces
val KivoSurface1 = Color(0xFF0D0E13)
val KivoSurface2 = Color(0xFF111218)
val KivoSurface3 = Color(0xFF15161D)
val KivoSurface4 = Color(0xFF1B1C24)

// Purples
val KivoPurpleMain = Color(0xFF6200EE)
val KivoPurpleElectric = Color(0xFF7C3CFF)
val KivoPurpleDeep = Color(0xFF3B176D)

// Accents
val KivoPink = Color(0xFFEC4899)
val KivoBlue = Color(0xFF3B82F6)
val KivoGreen = Color(0xFF10B981)
val KivoOrange = Color(0xFFF59E0B)

// Text
val KivoTextPrimary = Color(0xFFFFFFFF)
val KivoTextSecondary = Color(0xFFB3B3B3)
val KivoTextDisabled = Color(0xFF6F7078)

// Borders
val KivoBorder = Color(0xFF252630)

// Aliases for Backward Compatibility (Internal Refactor Path)
val KivoPurple = KivoPurpleMain
val KivoNeonPurple = KivoPurpleElectric
val KivoTextGrey = KivoTextSecondary
val KivoDarkGrey = KivoSurface1
val KivoWhite = KivoTextPrimary
val KivoAction_Start = KivoPurpleMain
val KivoAction_End = KivoPurpleElectric

// Gradients (Centralized)
val GradientPurple = listOf(KivoBlack, KivoPurpleDeep)
val GradientAction = listOf(KivoPurpleMain, KivoPurpleElectric)

// Difficulty Gradients
val Diff_Easy_Start = Color(0xFF00F2A1)
val Diff_Easy_End = Color(0xFF38EF7D)
val Diff_Normal_Start = Color(0xFFF2C94C)
val Diff_Normal_End = Color(0xFFF2994A)
val Diff_Hard_Start = Color(0xFFFF416C)
val Diff_Hard_End = Color(0xFFFF4B2B)
val Diff_Badge_Bg = Color(0x26000000)

// Game Hub / Trivia Aliases
val T_Easy_Start = Diff_Easy_Start
val T_Easy_End = Diff_Easy_End
val T_Med_Start = Diff_Normal_Start
val T_Med_End = Diff_Normal_End
val T_Hard_Start = Diff_Hard_Start
val T_Hard_End = Diff_Hard_End
val T_Option_Bg = KivoSurface2
val T_Eq_Start = KivoPurpleMain
val T_Eq_End = KivoBlue

// Game Hub (Aliased to new system)
val G_Purple_Start = KivoPurpleMain
val G_Purple_End = KivoPurpleElectric
val G_Pink_Start = Color(0xFFFD79A8)
val G_Pink_End = Color(0xFFE84393)
val G_Blue_Start = Color(0xFF0984E3)
val G_Blue_End = Color(0xFF74B9FF)
val G_Red_Start = Color(0xFFD63031)
val G_Red_End = Color(0xFFFF7675)
val G_Turquoise_Start = Color(0xFF00CEC9)
val G_Turquoise_End = Color(0xFF81ECEC)
val G_Gold_Start = Color(0xFFFDCB6E)
val G_Gold_End = Color(0xFFE17055)
val G_Hero_Start = Color(0xFF8E44AD)
val G_Hero_End = Color(0xFF3498DB)

// Legacy M3 Fallbacks
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
