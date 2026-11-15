package co.com.coexito.bupas.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.domain.model.AuthMessages
import co.com.coexito.bupas.domain.usecase.auth.RequestPasswordRecoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecoverPasswordViewModel @Inject constructor(
    private val requestPasswordRecoveryUseCase: RequestPasswordRecoveryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoverPasswordUiState())
    val uiState: StateFlow<RecoverPasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecoverPasswordUiEffect>(Channel.BUFFERED)
    val effects: Flow<RecoverPasswordUiEffect> = _effects.receiveAsFlow()

    fun onEvent(event: RecoverPasswordUiEvent) {
        when (event) {
            is RecoverPasswordUiEvent.OnUsernameChanged -> handleUsernameChanged(event.username)
            RecoverPasswordUiEvent.OnRequestRecoveryCode -> submitRecoveryRequest()
            RecoverPasswordUiEvent.OnDismissModal -> handleModalDismiss()
            RecoverPasswordUiEvent.OnBackToLoginClick -> navigateBackToLogin()
        }
    }

    private fun handleUsernameChanged(newValue: String) {
        val error = if (newValue.isBlank()) AuthMessages.REQUIRED_FIELD else null
        _uiState.update { state ->
            state.copy(
                username = newValue,
                usernameTouched = true,
                usernameError = error
            )
        }
    }

    private fun submitRecoveryRequest() {
        val currentUsername = _uiState.value.username
        if (currentUsername.isBlank()) {
            _uiState.update {
                it.copy(
                    usernameTouched = true,
                    usernameError = AuthMessages.REQUIRED_FIELD
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = requestPasswordRecoveryUseCase(currentUsername)
            result.fold(
                onSuccess = { confirmationMessage ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            modalTitle = "Recuperación de Contraseña",
                            modalMessage = confirmationMessage,
                            isModalError = false,
                            shouldReturnToLoginOnModalDismiss = true
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            modalTitle = "Usuario No Encontrado",
                            modalMessage = throwable.message ?: AuthMessages.INVALID_CREDENTIALS,
                            isModalError = true,
                            shouldReturnToLoginOnModalDismiss = false
                        )
                    }
                }
            )
        }
    }

    private fun handleModalDismiss() {
        val shouldReturn = _uiState.value.shouldReturnToLoginOnModalDismiss
        _uiState.update {
            it.copy(
                modalTitle = null,
                modalMessage = null,
                isModalError = false,
                shouldReturnToLoginOnModalDismiss = false
            )
        }
        if (shouldReturn) {
            navigateBackToLogin()
        }
    }

    private fun navigateBackToLogin() {
        viewModelScope.launch {
            _effects.send(RecoverPasswordUiEffect.NavigateBackToLogin)
        }
    }
}
