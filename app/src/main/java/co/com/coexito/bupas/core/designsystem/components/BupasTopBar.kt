package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BupasTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    environmentLabel: String? = null,
    onBackClick: (() -> Unit)? = null,
    onThemeSettingsClick: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier,
        title = {
            androidx.compose.foundation.layout.Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (!environmentLabel.isNullOrBlank()) {
                        BupasEnvironmentPill(label = environmentLabel)
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            }
        },
        actions = {
            if (onThemeSettingsClick != null) {
                IconButton(onClick = onThemeSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Contrast,
                        contentDescription = "Configurar apariencia"
                    )
                }
            }
            if (onLogoutClick != null) {
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Salida segura"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun BupasEnvironmentPill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = BupasTheme.spacing.xs,
                vertical = BupasTheme.spacing.xs / 2
            )
        )
    }
}

@Preview
@Composable
private fun BupasTopBarPreview() {
    BupasTheme {
        BupasTopBar(
            title = "Órdenes Asignadas BUPAS",
            subtitle = "Conductor: Juan Rodríguez",
            environmentLabel = "DEV",
            onLogoutClick = {}
        )
    }
}
