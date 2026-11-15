package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme

private val CoeButtonBorderWidth = 2.dp
private val CoeButtonLetterSpacing = 0.4.sp

/**
 * Botón primario estilo `coe-ui-kit` (`coe-button.btn-primary`):
 * forma pill, tipografía Bold en mayúsculas, gradiente azul Coéxito y estado deshabilitado en `#B3D1F2`.
 */
@Composable
fun BupasPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val isInteractive = enabled && !isLoading
    val backgroundBrush = if (isInteractive) {
        Brush.horizontalGradient(
            colors = listOf(
                BupasTheme.extendedColors.primaryGradientStart,
                BupasTheme.extendedColors.primaryGradientEnd
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                BupasTheme.extendedColors.primaryDisabledContainer,
                BupasTheme.extendedColors.primaryDisabledContainer
            )
        )
    }

    val shadowModifier = if (isInteractive) {
        Modifier.shadow(elevation = 4.dp, shape = CircleShape, clip = false)
    } else {
        Modifier
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BupasTheme.spacing.minTouchTarget)
            .then(shadowModifier)
            .clip(CircleShape)
            .background(brush = backgroundBrush, shape = CircleShape),
        enabled = isInteractive,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(
            horizontal = BupasTheme.spacing.lg,
            vertical = 12.dp
        )
    ) {
        CoeButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            indicatorColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * Botón secundario estilo `coe-ui-kit` (`coe-button.btn-secondary`):
 * fondo blanco, forma pill, borde de 2.dp y texto en azul primario.
 */
@Composable
fun BupasSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val borderColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        BupasTheme.extendedColors.borderDisabled
    }

    CoeOutlinedPillButton(
        text = text,
        onClick = onClick,
        borderColor = borderColor,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon
    )
}

/**
 * Botón neutral estilo `coe-ui-kit` (`coe-button.btn-neutral`).
 */
@Composable
fun BupasNeutralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    CoeOutlinedPillButton(
        text = text,
        onClick = onClick,
        borderColor = BupasTheme.extendedColors.borderDisabled,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon
    )
}

/**
 * Botón de error/alerta estilo `coe-ui-kit` (`coe-button.btn-error`).
 */
@Composable
fun BupasErrorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val borderColor = if (enabled) {
        BupasTheme.extendedColors.errorBorderLighter
    } else {
        BupasTheme.extendedColors.borderDisabled
    }

    CoeOutlinedPillButton(
        text = text,
        onClick = onClick,
        borderColor = borderColor,
        contentColor = MaterialTheme.colorScheme.error,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon
    )
}

/**
 * Botón tipo enlace estilo `coe-ui-kit` (`coe-button.btn-link`):
 * texto en mayúsculas Bold con subrayado inferior opcional.
 */
@Composable
fun BupasTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    underlined: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        BupasTheme.extendedColors.primaryDisabledContainer
    }

    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = BupasTheme.spacing.minTouchTarget),
        enabled = enabled,
        shape = CircleShape
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(BupasTheme.spacing.xs))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = CoeButtonLetterSpacing,
                    textDecoration = if (underlined) TextDecoration.Underline else TextDecoration.None
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun CoeOutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    borderColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BupasTheme.spacing.minTouchTarget),
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(CoeButtonBorderWidth, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = BupasTheme.extendedColors.borderDisabled
        ),
        contentPadding = PaddingValues(
            horizontal = BupasTheme.spacing.lg,
            vertical = 12.dp
        )
    ) {
        CoeButtonContent(
            text = text,
            isLoading = false,
            leadingIcon = leadingIcon,
            indicatorColor = contentColor
        )
    }
}

@Composable
private fun CoeButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    indicatorColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = indicatorColor,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(BupasTheme.spacing.sm))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(BupasTheme.spacing.sm))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = CoeButtonLetterSpacing
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BupasButtonsPreview() {
    BupasTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BupasPrimaryButton(text = "Iniciar Sesión", onClick = {})
                BupasPrimaryButton(text = "Deshabilitado", onClick = {}, enabled = false)
                BupasSecondaryButton(text = "+ Agregar Provenientes", onClick = {})
                BupasNeutralButton(text = "Cancelar", onClick = {})
                BupasErrorButton(text = "Anular Recolección", onClick = {})
                BupasTextButton(text = "¿Olvidaste tu contraseña?", onClick = {})
            }
        }
    }
}
