package com.scheduleapp.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary Indigo
val Indigo50 = Color(0xFFEEF2FF)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo200 = Color(0xFFC7D2FE)
val Indigo500 = Color(0xFF6366F1)
val Indigo600 = Color(0xFF4F46E5)
val Indigo700 = Color(0xFF4338CA)
val Indigo900 = Color(0xFF1E1B4B)

// Automotive & Productivity Accent Colors
val Emerald500 = Color(0xFF10B981) // Success
val Emerald600 = Color(0xFF059669)
val Amber500 = Color(0xFFF59E0B)   // Warning
val Amber600 = Color(0xFFD97706)
val Crimson500 = Color(0xFFEF4444) // Overdue / Urgent
val Crimson600 = Color(0xFFDC2626)
val Blue500 = Color(0xFF3B82F6)    // Fuel / Distance
val Purple500 = Color(0xFF8B5CF6)  // Routine / Habit

// Neutral Slates
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)

// Material 3 Light Color Scheme
val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Indigo500,
    onSecondary = Color.White,
    secondaryContainer = Indigo50,
    onSecondaryContainer = Indigo700,
    tertiary = Blue500,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF1E3A8A),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Crimson600,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

// Material 3 Dark Color Scheme
val DarkColorScheme = darkColorScheme(
    primary = Indigo200,
    onPrimary = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo100,
    secondary = Indigo500,
    onSecondary = Color.White,
    secondaryContainer = Indigo900,
    onSecondaryContainer = Indigo200,
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF1E3A8A),
    tertiaryContainer = Color(0xFF1E40AF),
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate600,
    outlineVariant = Slate700,
    error = Crimson500,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Immutable
data class ExtendedColors(
    val success: Color = Emerald500,
    val onSuccess: Color = Color.White,
    val warning: Color = Amber500,
    val onWarning: Color = Color.White,
    val urgent: Color = Crimson500,
    val onUrgent: Color = Color.White,
    val fuel: Color = Blue500,
    val routine: Color = Purple500
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }
