package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme

/**
 * Modal informativo o de error con botón OK (HU-001: "Usuario o contraseña inválido." y
 * "Se ha enviado un código de verificación a su correo electrónico").
 */
@Composable
fun BupasAlertModal(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    isError: Boolean = false,
    confirmButtonText: String = "OK"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Info,
                contentDescription = null,
                tint = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            BupasTextButton(
                text = confirmButtonText,
                onClick = onDismiss
            )
        }
    )
}

/**
 * Modal de confirmación Sí / No para el cierre de recolección en campo (HU-006:
 * "¿Está seguro de finalizar la recolección? Una vez finalizada, no podrá modificar
 * la información registrada.").
 */
@Composable
fun BupasConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Sí",
    dismissText: String = "No"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            BupasTextButton(
                text = confirmText,
                onClick = onConfirm
            )
        },
        dismissButton = {
            BupasTextButton(
                text = dismissText,
                onClick = onDismiss
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun BupasDialogsPreview() {
    BupasTheme {
        BupasConfirmDialog(
            title = "Finalizar Recolección",
            message = "¿Está seguro de finalizar la recolección? Una vez finalizada, no podrá modificar la información registrada.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
