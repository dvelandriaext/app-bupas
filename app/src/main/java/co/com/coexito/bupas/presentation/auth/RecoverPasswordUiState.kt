package co.com.coexito.bupas.presentation.auth

import androidx.compose.runtime.Immutable

@Immutable
data class RecoverPasswordUiState(
    val username: String = "",
    val usernameTouched: Boolean = false,
    val usernameError: String? = null,
    val isSubmitting: Boolean = false,
    val modalTitle: String? = null,
    val modalMessage: String? = null,
    val isModalError: Boolean = false,
    val shouldReturnToLoginOnModalDismiss: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = username.isNotBlank() && !isSubmitting
}

sealed interface RecoverPasswordUiEvent {
    data class OnUsernameChanged(val username: String) : RecoverPasswordUiEvent
    data object OnRequestRecoveryCode : RecoverPasswordUiEvent
    data object OnDismissModal : RecoverPasswordUiEvent
    data object OnBackToLoginClick : RecoverPasswordUiEvent
}

sealed interface RecoverPasswordUiEffect {
    data object NavigateBackToLogin : RecoverPasswordUiEffect
}
