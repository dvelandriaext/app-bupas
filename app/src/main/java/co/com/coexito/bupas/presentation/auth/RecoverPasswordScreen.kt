package co.com.coexito.bupas.presentation.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.com.coexito.bupas.core.designsystem.components.BupasAlertModal
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasTextButton
import co.com.coexito.bupas.core.designsystem.components.BupasTextField
import co.com.coexito.bupas.core.designsystem.components.BupasTopBar
import co.com.coexito.bupas.core.designsystem.components.CoeAuthSplitLayout
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme

@Composable
fun RecoverPasswordRoute(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecoverPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RecoverPasswordUiEffect.NavigateBackToLogin -> onBackToLogin()
            }
        }
    }

    RecoverPasswordScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun RecoverPasswordScreen(
    uiState: RecoverPasswordUiState,
    onEvent: (RecoverPasswordUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BupasTopBar(
                title = "Recuperar contraseña",
                subtitle = "Central de Accesos • BUPAS Coéxito",
                onBackClick = { onEvent(RecoverPasswordUiEvent.OnBackToLoginClick) }
            )
        }
    ) { innerPadding ->
        CoeAuthSplitLayout(
            title = "Recuperar contraseña",
            subtitle = "Ingrese su usuario corporativo para enviar un código de recuperación",
            modifier = Modifier.padding(innerPadding),
            footerContent = {
                Text(
                    text = "El código de recuperación será enviado al correo registrado en Central de Accesos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        ) {
            BupasTextField(
                value = uiState.username,
                onValueChange = { onEvent(RecoverPasswordUiEvent.OnUsernameChanged(it)) },
                label = "Usuario",
                placeholder = "conductor.bupas",
                isRequired = true,
                isError = uiState.usernameError != null,
                errorMessage = uiState.usernameError,
                enabled = !uiState.isSubmitting,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                }
            )

            Spacer(modifier = Modifier.height(BupasTheme.spacing.xs))

            BupasPrimaryButton(
                text = "Solicitar código",
                enabled = uiState.isSubmitEnabled,
                isLoading = uiState.isSubmitting,
                onClick = { onEvent(RecoverPasswordUiEvent.OnRequestRecoveryCode) }
            )

            BupasTextButton(
                text = "Volver al login",
                onClick = { onEvent(RecoverPasswordUiEvent.OnBackToLoginClick) },
                enabled = !uiState.isSubmitting,
                underlined = false
            )
        }
    }

    val title = uiState.modalTitle
    val message = uiState.modalMessage
    if (title != null && message != null) {
        BupasAlertModal(
            title = title,
            message = message,
            isError = uiState.isModalError,
            onDismiss = { onEvent(RecoverPasswordUiEvent.OnDismissModal) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecoverPasswordScreenPreview() {
    BupasTheme {
        RecoverPasswordScreen(
            uiState = RecoverPasswordUiState(
                username = "",
                usernameTouched = true,
                usernameError = "Campo requerido"
            ),
            onEvent = {}
        )
    }
}
