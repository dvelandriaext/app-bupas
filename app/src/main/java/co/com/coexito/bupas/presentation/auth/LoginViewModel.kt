package co.com.coexito.bupas.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.domain.model.AuthException
import co.com.coexito.bupas.domain.model.AuthMessages
import co.com.coexito.bupas.domain.model.TwoFactorChallenge
import co.com.coexito.bupas.domain.usecase.auth.AuthenticateCredentialsUseCase
import co.com.coexito.bupas.domain.usecase.auth.VerifyTwoFactorCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authenticateCredentialsUseCase: AuthenticateCredentialsUseCase,
    private val verifyTwoFactorCodeUseCase: VerifyTwoFactorCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LoginUiEffect>(Channel.BUFFERED)
    val effects: Flow<LoginUiEffect> = _effects.receiveAsFlow()

    private var countdownJob: Job? = null

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.OnUsernameChanged -> handleUsernameChanged(event.username)
            is LoginUiEvent.OnPasswordChanged -> handlePasswordChanged(event.password)
            LoginUiEvent.OnSubmitCredentials -> submitCredentials()
            LoginUiEvent.OnDismissModal -> dismissModal()
            is LoginUiEvent.OnOtpCodeChanged -> handleOtpCodeChanged(event.code)
            LoginUiEvent.OnSubmitOtpCode -> submitOtpCode()
            LoginUiEvent.OnBackToCredentials -> returnToCredentialsStep()
            LoginUiEvent.OnForgotPasswordClick -> navigateToRecoverPassword()
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

    private fun handlePasswordChanged(newValue: String) {
        val error = if (newValue.isBlank()) AuthMessages.REQUIRED_FIELD else null
        _uiState.update { state ->
            state.copy(
                password = newValue,
                passwordTouched = true,
                passwordError = error
            )
        }
    }

    private fun submitCredentials() {
        val currentState = _uiState.value
        if (!validateCredentialsLocally(currentState)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = authenticateCredentialsUseCase(
                username = currentState.username,
                password = currentState.password
            )
            result.fold(
                onSuccess = { challenge -> onCredentialsAuthenticated(challenge) },
                onFailure = { onCredentialsFailed() }
            )
        }
    }

    private fun validateCredentialsLocally(state: LoginUiState): Boolean {
        val userBlank = state.username.isBlank()
        val passBlank = state.password.isBlank()
        if (userBlank || passBlank) {
            _uiState.update {
                it.copy(
                    usernameTouched = true,
                    passwordTouched = true,
                    usernameError = if (userBlank) AuthMessages.REQUIRED_FIELD else null,
                    passwordError = if (passBlank) AuthMessages.REQUIRED_FIELD else null
                )
            }
            return false
        }
        return true
    }

    private fun onCredentialsAuthenticated(challenge: TwoFactorChallenge) {
        _uiState.update { state ->
            state.copy(
                isSubmitting = false,
                maskedEmail = challenge.maskedEmail,
                remainingSeconds = challenge.validityDurationSeconds,
                isOtpExpired = false,
                otpCode = "",
                otpError = null,
                activeModal = AuthAlertModal.EMAIL_CODE_SENT
            )
        }
    }

    private fun onCredentialsFailed() {
        _uiState.update { state ->
            state.copy(
                isSubmitting = false,
                activeModal = AuthAlertModal.INVALID_CREDENTIALS
            )
        }
    }

    private fun dismissModal() {
        val currentModal = _uiState.value.activeModal ?: return
        if (currentModal == AuthAlertModal.EMAIL_CODE_SENT) {
            _uiState.update { state ->
                state.copy(
                    activeModal = null,
                    authStep = AuthStep.TWO_FACTOR
                )
            }
            startTwoFactorCountdown(_uiState.value.remainingSeconds)
        } else {
            _uiState.update { it.copy(activeModal = null) }
        }
    }

    private fun startTwoFactorCountdown(durationSeconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var secondsLeft = durationSeconds
            while (secondsLeft > 0) {
                delay(ONE_SECOND_MILLIS)
                secondsLeft -= 1
                _uiState.update { state ->
                    val expired = secondsLeft <= 0
                    state.copy(
                        remainingSeconds = secondsLeft,
                        isOtpExpired = expired,
                        otpError = if (expired) AuthMessages.EXPIRED_OTP_CODE else state.otpError
                    )
                }
            }
        }
    }

    private fun handleOtpCodeChanged(newCode: String) {
        val sanitized = newCode.filter { it.isDigit() }
            .take(TwoFactorChallenge.TWO_FACTOR_CODE_LENGTH)
        _uiState.update { state ->
            val preservedError = if (state.isOtpExpired) AuthMessages.EXPIRED_OTP_CODE else null
            state.copy(
                otpCode = sanitized,
                otpError = preservedError
            )
        }
    }

    private fun submitOtpCode() {
        val currentState = _uiState.value
        if (currentState.isOtpExpired || currentState.remainingSeconds <= 0) {
            _uiState.update {
                it.copy(
                    isOtpExpired = true,
                    otpError = AuthMessages.EXPIRED_OTP_CODE
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = verifyTwoFactorCodeUseCase(
                username = currentState.username,
                code = currentState.otpCode,
                isTimerExpired = currentState.isOtpExpired
            )
            result.fold(
                onSuccess = {
                    countdownJob?.cancel()
                    _uiState.update { it.copy(isSubmitting = false, otpError = null) }
                    _effects.send(LoginUiEffect.NavigateToOrdersInbox)
                },
                onFailure = { throwable ->
                    handleOtpVerificationFailure(throwable)
                }
            )
        }
    }

    private fun handleOtpVerificationFailure(throwable: Throwable) {
        val isExpiredError = throwable is AuthException.ExpiredTwoFactorCode
        val errorMessage = when (throwable) {
            is AuthException -> throwable.message
            else -> AuthMessages.INVALID_OTP_CODE
        }
        if (isExpiredError) {
            countdownJob?.cancel()
        }
        _uiState.update { state ->
            state.copy(
                isSubmitting = false,
                isOtpExpired = state.isOtpExpired || isExpiredError,
                remainingSeconds = if (isExpiredError) 0 else state.remainingSeconds,
                otpError = errorMessage
            )
        }
    }

    private fun returnToCredentialsStep() {
        countdownJob?.cancel()
        _uiState.update { state ->
            state.copy(
                authStep = AuthStep.CREDENTIALS,
                otpCode = "",
                otpError = null,
                isOtpExpired = false,
                remainingSeconds = TwoFactorChallenge.TWO_FACTOR_VALIDITY_SECONDS
            )
        }
    }

    private fun navigateToRecoverPassword() {
        viewModelScope.launch {
            _effects.send(LoginUiEffect.NavigateToRecoverPassword)
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
    }
}
