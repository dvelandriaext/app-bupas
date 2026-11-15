package co.com.coexito.bupas.presentation.auth

import androidx.compose.runtime.Immutable
import co.com.coexito.bupas.domain.model.AuthMessages
import co.com.coexito.bupas.domain.model.TwoFactorChallenge

enum class AuthStep {
    CREDENTIALS,
    TWO_FACTOR
}

enum class AuthAlertModal(
    val title: String,
    val message: String,
    val isError: Boolean
) {
    INVALID_CREDENTIALS(
        title = "Credenciales Inválidas",
        message = AuthMessages.INVALID_CREDENTIALS,
        isError = true
    ),
    EMAIL_CODE_SENT(
        title = "Código de Verificación",
        message = AuthMessages.EMAIL_CODE_SENT,
        isError = false
    )
}

@Immutable
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val usernameTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val authStep: AuthStep = AuthStep.CREDENTIALS,
    val maskedEmail: String = "",
    val otpCode: String = "",
    val otpError: String? = null,
    val remainingSeconds: Int = TwoFactorChallenge.TWO_FACTOR_VALIDITY_SECONDS,
    val isOtpExpired: Boolean = false,
    val activeModal: AuthAlertModal? = null
) {
    val isLoginButtonEnabled: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !isSubmitting

    val isVerifyButtonEnabled: Boolean
        get() = otpCode.length == TwoFactorChallenge.TWO_FACTOR_CODE_LENGTH &&
            !isOtpExpired &&
            !isSubmitting
}

sealed interface LoginUiEvent {
    data class OnUsernameChanged(val username: String) : LoginUiEvent
    data class OnPasswordChanged(val password: String) : LoginUiEvent
    data object OnSubmitCredentials : LoginUiEvent
    data object OnDismissModal : LoginUiEvent
    data class OnOtpCodeChanged(val code: String) : LoginUiEvent
    data object OnSubmitOtpCode : LoginUiEvent
    data object OnBackToCredentials : LoginUiEvent
    data object OnForgotPasswordClick : LoginUiEvent
}

sealed interface LoginUiEffect {
    data object NavigateToOrdersInbox : LoginUiEffect
    data object NavigateToRecoverPassword : LoginUiEffect
}
