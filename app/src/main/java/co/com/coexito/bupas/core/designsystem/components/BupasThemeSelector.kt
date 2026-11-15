package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.core.designsystem.theme.ThemeMode

/**
 * Selector segmentado de 3 opciones para la apariencia (Sistema / Claro / Oscuro).
 */
@Composable
fun BupasThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, BupasTheme.extendedColors.borderDisabled)
    ) {
        Row(
            modifier = Modifier.padding(BupasTheme.spacing.xs / 2),
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeModeOptionButton(
                icon = Icons.Default.DesktopWindows,
                contentDescription = "Tema del sistema",
                isSelected = selectedMode == ThemeMode.SYSTEM,
                onClick = { onModeSelected(ThemeMode.SYSTEM) }
            )
            ThemeModeOptionButton(
                icon = Icons.Default.LightMode,
                contentDescription = "Tema claro",
                isSelected = selectedMode == ThemeMode.LIGHT,
                onClick = { onModeSelected(ThemeMode.LIGHT) }
            )
            ThemeModeOptionButton(
                icon = Icons.Default.DarkMode,
                contentDescription = "Tema oscuro",
                isSelected = selectedMode == ThemeMode.DARK,
                onClick = { onModeSelected(ThemeMode.DARK) }
            )
        }
    }
}

@Composable
private fun ThemeModeOptionButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Diálogo de configuración de apariencia accesible desde la barra superior.
 */
@Composable
fun BupasThemeSettingsDialog(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tema",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedMode.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BupasThemeModeSelector(
                        selectedMode = selectedMode,
                        onModeSelected = onModeSelected
                    )
                }
            }
        },
        confirmButton = {
            BupasTextButton(
                text = "Listo",
                onClick = onDismiss
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun BupasThemeModeSelectorPreview() {
    BupasTheme {
        BupasThemeModeSelector(
            selectedMode = ThemeMode.SYSTEM,
            onModeSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
