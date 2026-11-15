package co.com.coexito.bupas.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.com.coexito.bupas.core.designsystem.components.BupasAlertModal
import co.com.coexito.bupas.core.designsystem.components.BupasOtpField
import co.com.coexito.bupas.core.designsystem.components.BupasPasswordField
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasTextButton
import co.com.coexito.bupas.core.designsystem.components.BupasTextField
import co.com.coexito.bupas.core.designsystem.components.BupasThemeModeSelector
import co.com.coexito.bupas.core.designsystem.components.CoeAuthSplitLayout
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.core.designsystem.theme.ThemeMode
import co.com.coexito.bupas.core.navigation.BupasTransitions

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onNavigateToRecoverPassword: () -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginUiEffect.NavigateToOrdersInbox -> onLoginSuccess()
                LoginUiEffect.NavigateToRecoverPassword -> onNavigateToRecoverPassword()
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        themeMode = themeMode,
        onThemeModeSelected = onThemeModeSelected
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeSelected: (ThemeMode) -> Unit = {}
) {
    val cardTitle = when (uiState.authStep) {
        AuthStep.CREDENTIALS -> "Portal de Aplicaciones BUPAS"
        AuthStep.TWO_FACTOR -> "Verificación en Dos Pasos (2FA)"
    }
    val cardSubtitle = when (uiState.authStep) {
        AuthStep.CREDENTIALS -> "Coéxito Logística Reversiva • Conductor en Campo"
        AuthStep.TWO_FACTOR -> if (uiState.maskedEmail.isNotBlank()) {
            "Código de 6 dígitos enviado a ${uiState.maskedEmail}"
        } else {
            "Ingrese el código de 6 dígitos enviado a su correo"
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        CoeAuthSplitLayout(
            title = cardTitle,
            subtitle = cardSubtitle,
            modifier = Modifier.padding(innerPadding),
            footerContent = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
                ) {
                    Crossfade(
                        targetState = uiState.authStep,
                        animationSpec = tween(durationMillis = 250),
                        label = "DemoBannerCrossfade"
                    ) { step ->
                        DemoCredentialsInfoBanner(authStep = step)
                    }
                    BupasThemeModeSelector(
                        selectedMode = themeMode,
                        onModeSelected = onThemeModeSelected
                    )
                }
            }
        ) {
            AnimatedContent(
                targetState = uiState.authStep,
                transitionSpec = {
                    val isForward = targetState == AuthStep.TWO_FACTOR &&
                        initialState == AuthStep.CREDENTIALS
                    BupasTransitions.stepTransition(isForward = isForward)
                },
                label = "AuthStepTransition"
            ) { step ->
                when (step) {
                    AuthStep.CREDENTIALS -> CredentialsFormSection(
                        uiState = uiState,
                        onEvent = onEvent
                    )
                    AuthStep.TWO_FACTOR -> TwoFactorVerificationSection(
                        uiState = uiState,
                        onEvent = onEvent
                    )
                }
            }
        }
    }

    uiState.activeModal?.let { modal ->
        BupasAlertModal(
            title = modal.title,
            message = modal.message,
            isError = modal.isError,
            onDismiss = { onEvent(LoginUiEvent.OnDismissModal) }
        )
    }
}

@Composable
private fun CredentialsFormSection(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BupasTextField(
            value = uiState.username,
            onValueChange = { onEvent(LoginUiEvent.OnUsernameChanged(it)) },
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

        BupasPasswordField(
            value = uiState.password,
            onValueChange = { onEvent(LoginUiEvent.OnPasswordChanged(it)) },
            label = "Contraseña",
            isRequired = true,
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError,
            enabled = !uiState.isSubmitting,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            }
        )

        Spacer(modifier = Modifier.height(BupasTheme.spacing.xs))

        BupasPrimaryButton(
            text = "Iniciar Sesión",
            enabled = uiState.isLoginButtonEnabled,
            isLoading = uiState.isSubmitting,
            onClick = { onEvent(LoginUiEvent.OnSubmitCredentials) }
        )

        BupasTextButton(
            text = "¿Olvidaste tu contraseña?",
            onClick = { onEvent(LoginUiEvent.OnForgotPasswordClick) },
            enabled = !uiState.isSubmitting,
            underlined = false,
            leadingIcon = Icons.Default.Key
        )
    }
}

@Composable
private fun TwoFactorVerificationSection(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BupasOtpField(
            code = uiState.otpCode,
            onCodeChange = { onEvent(LoginUiEvent.OnOtpCodeChanged(it)) },
            remainingSeconds = uiState.remainingSeconds,
            isError = uiState.otpError != null,
            errorMessage = uiState.otpError,
            enabled = !uiState.isOtpExpired && !uiState.isSubmitting,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null
                )
            }
        )

        Spacer(modifier = Modifier.height(BupasTheme.spacing.xs))

        BupasPrimaryButton(
            text = "Verificar",
            enabled = uiState.isVerifyButtonEnabled,
            isLoading = uiState.isSubmitting,
            onClick = { onEvent(LoginUiEvent.OnSubmitOtpCode) }
        )

        BupasTextButton(
            text = "Volver al login",
            onClick = { onEvent(LoginUiEvent.OnBackToCredentials) },
            enabled = !uiState.isSubmitting,
            underlined = false
        )
    }
}

@Composable
private fun DemoCredentialsInfoBanner(authStep: AuthStep) {
    val helperMessage = when (authStep) {
        AuthStep.CREDENTIALS ->
            "Entorno de Pruebas BUPAS • Usuario: conductor.bupas | Clave: Coexito2026*"
        AuthStep.TWO_FACTOR ->
            "Entorno de Pruebas BUPAS • Código 2FA de verificación: 123456 (Vigencia: 3 min)"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = helperMessage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(BupasTheme.spacing.sm)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenCredentialsPreview() {
    BupasTheme {
        LoginScreen(
            uiState = LoginUiState(
                username = "",
                usernameTouched = true,
                usernameError = "Campo requerido"
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenTwoFactorPreview() {
    BupasTheme {
        LoginScreen(
            uiState = LoginUiState(
                authStep = AuthStep.TWO_FACTOR,
                maskedEmail = "con***@coexito.com.co",
                otpCode = "999999",
                otpError = "Código de verificación incorrecto",
                remainingSeconds = 142
            ),
            onEvent = {}
        )
    }
}
