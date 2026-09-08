package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.auth.AuthView
import com.example.kivo.ui.theme.*

private val KivoClerkColors = ClerkColors(
    primary = KivoPurpleMain,
    background = KivoBlack,
    input = KivoSurface2,
    danger = Color(0xFFFF416C),
    success = KivoGreen,
    warning = KivoOrange,
    foreground = KivoTextPrimary,
    mutedForeground = KivoTextSecondary,
    primaryForeground = Color.White,
    inputForeground = Color.White,
    neutral = KivoBorder,
    border = KivoBorder,
    ring = KivoPurpleElectric,
    muted = KivoSurface1,
    secondaryButtonBackground = KivoSurface3,
    secondaryButtonForeground = Color.White,
    shadow = Color(0xFF000000)
)

private val KivoClerkTheme = ClerkTheme(
    colors = KivoClerkColors,
    design = ClerkDesign(borderRadius = 16.dp, logoMaxHeight = 44.dp)
)

@Composable
fun AuthScreen() {
    val clerkInitialized by Clerk.isInitialized.collectAsState()

    if (!clerkInitialized) {
        Box(
            modifier = Modifier.fillMaxSize().background(KivoBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "KIVO",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = KivoPurpleElectric
                )
                Text(
                    text = "Autenticación no configurada.\nAgrega CLERK_PUBLISHABLE_KEY en gradle.properties para continuar.",
                    color = KivoTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
        }
    } else {
        AuthView(
            modifier = Modifier.fillMaxSize(),
            isDismissible = false,
            clerkTheme = KivoClerkTheme
        )
    }
}