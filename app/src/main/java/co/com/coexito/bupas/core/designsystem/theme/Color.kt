package co.com.coexito.bupas.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta Principal Corporativa Coéxito BUPAS (Calibrada con coe-ui-kit / coexito.com.co)
val BupasPrimary = Color(0xFF004D9D)
val BupasPrimaryDark = Color(0xFF0033A0)
val BupasPrimaryLight = Color(0xFF256EC2)
val BupasPrimaryLighter = Color(0xFFB3D1F2)
val BupasOnPrimary = Color(0xFFFFFFFF)
val BupasPrimaryContainer = Color(0xFFF4F8FD)
val BupasOnPrimaryContainer = Color(0xFF182841)

val BupasSecondary = Color(0xFFED6B10)
val BupasOnSecondary = Color(0xFFFFFFFF)
val BupasSecondaryContainer = Color(0xFFFFF7ED)
val BupasOnSecondaryContainer = Color(0xFFD95308)

val BupasTertiary = Color(0xFF182841)
val BupasOnTertiary = Color(0xFFFFFFFF)
val BupasTertiaryContainer = Color(0xFFF2F6FB)
val BupasOnTertiaryContainer = Color(0xFF0F1B2D)

// Fondos, Superficies y Bordes (coe-ui-kit: $gray-dark, $gray, $gray-light, $gray-lighter, $gray-background)
val BupasBackgroundLight = Color(0xFFF2F4F5)
val BupasOnBackgroundLight = Color(0xFF282D37)
val BupasSurfaceLight = Color(0xFFFFFFFF)
val BupasOnSurfaceLight = Color(0xFF282D37)
val BupasSurfaceVariantLight = Color(0xFFF2F4F5)
val BupasOnSurfaceVariantLight = Color(0xFF3F3F40)
val BupasOutlineLight = Color(0xFF727273)
val BupasOutlineDisabledLight = Color(0xFFD3D3D3)

val BupasBackgroundDark = Color(0xFF0F1B2D)
val BupasOnBackgroundDark = Color(0xFFF8FAFC)
val BupasSurfaceDark = Color(0xFF182841)
val BupasOnSurfaceDark = Color(0xFFF2F4F5)
val BupasSurfaceVariantDark = Color(0xFF282D37)
val BupasOnSurfaceVariantDark = Color(0xFFD3D3D3)
val BupasOutlineDark = Color(0xFF727273)

// Validación y Errores en Rojo (coe-ui-kit $error + HU-001 / HU-006)
val BupasError = Color(0xFFBE2823)
val BupasOnError = Color(0xFFFFFFFF)
val BupasErrorContainer = Color(0xFFFFE6E6)
val BupasOnErrorContainer = Color(0xFF991B1B)
val BupasErrorLighter = Color(0xFFF5BAA3)

// Colores Semánticos por Estado de Orden BUPAS (Calibrados con coe-badge de coe-ui-kit)
val StatusAssignedContainer = Color(0xFFF0F9FF)
val StatusAssignedContent = Color(0xFF0284C7)

val StatusInAttentionContainer = Color(0xFFFFF6E0)
val StatusInAttentionContent = Color(0xFFED6B10)

val StatusPartialContainer = Color(0xFFEDE9FE)
val StatusPartialContent = Color(0xFF6D28D9)

val StatusNotCollectedContainer = Color(0xFFFFE6E6)
val StatusNotCollectedContent = Color(0xFFBE2823)

val StatusCompletedContainer = Color(0xFFEAFCE3)
val StatusCompletedContent = Color(0xFF1E851E)

@Immutable
data class StatusColorPair(
    val containerColor: Color,
    val contentColor: Color
)

@Immutable
data class BupasExtendedColors(
    val ordenAsignada: StatusColorPair = StatusColorPair(
        containerColor = StatusAssignedContainer,
        contentColor = StatusAssignedContent
    ),
    val enAtencion: StatusColorPair = StatusColorPair(
        containerColor = StatusInAttentionContainer,
        contentColor = StatusInAttentionContent
    ),
    val recoleccionParcial: StatusColorPair = StatusColorPair(
        containerColor = StatusPartialContainer,
        contentColor = StatusPartialContent
    ),
    val noRecolectada: StatusColorPair = StatusColorPair(
        containerColor = StatusNotCollectedContainer,
        contentColor = StatusNotCollectedContent
    ),
    val recoleccionRealizada: StatusColorPair = StatusColorPair(
        containerColor = StatusCompletedContainer,
        contentColor = StatusCompletedContent
    ),
    val readOnlySurface: Color = Color(0xFFF2F4F5),
    val successAccent: Color = Color(0xFF1E851E),
    val primaryGradientStart: Color = BupasPrimaryLight,
    val primaryGradientEnd: Color = BupasPrimary,
    val primaryDisabledContainer: Color = BupasPrimaryLighter,
    val borderDisabled: Color = BupasOutlineDisabledLight,
    val errorBorderLighter: Color = BupasErrorLighter
)

val LocalBupasExtendedColors = staticCompositionLocalOf { BupasExtendedColors() }
