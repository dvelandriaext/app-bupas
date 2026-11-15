package co.com.coexito.bupas.presentation.auth

import co.com.coexito.bupas.data.local.SessionLocalDataSource
import co.com.coexito.bupas.data.remote.FakeAuthApiService
import co.com.coexito.bupas.data.repository.AuthRepositoryImpl
import co.com.coexito.bupas.domain.model.AuthMessages
import co.com.coexito.bupas.domain.model.UserSession
import co.com.coexito.bupas.domain.usecase.auth.AuthenticateCredentialsUseCase
import co.com.coexito.bupas.domain.usecase.auth.VerifyTwoFactorCodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private var fakeClockMillis: Long = 5_000_000L
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val sessionDataSource = object : SessionLocalDataSource {
            private val state = MutableStateFlow<UserSession?>(null)
            override val sessionFlow: Flow<UserSession?> = state
            override suspend fun saveSession(session: UserSession) {
                state.value = session
            }
            override suspend fun clearSession() {
                state.value = null
            }
        }
        val apiService = FakeAuthApiService(
            simulatedDelayMs = 0L,
            clockMillis = { fakeClockMillis },
            otpCodeGenerator = { "123456" }
        )
        val repository = AuthRepositoryImpl(
            authApiService = apiService,
            sessionLocalDataSource = sessionDataSource,
            ioDispatcher = testDispatcher
        )
        viewModel = LoginViewModel(
            authenticateCredentialsUseCase = AuthenticateCredentialsUseCase(repository),
            verifyTwoFactorCodeUseCase = VerifyTwoFactorCodeUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `campos vacios muestran Campo requerido y mantienen boton Iniciar Sesion deshabilitado`() {
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged(""))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged(""))

        val state = viewModel.uiState.value
        assertEquals(AuthMessages.REQUIRED_FIELD, state.usernameError)
        assertEquals(AuthMessages.REQUIRED_FIELD, state.passwordError)
        assertFalse(state.isLoginButtonEnabled)
    }

    @Test
    fun `credenciales invalidas muestran modal emergente Usuario o contrasena invalido`() = runTest {
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged("conductor.bupas"))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged("ClaveIncorrecta"))
        viewModel.onEvent(LoginUiEvent.OnSubmitCredentials)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthAlertModal.INVALID_CREDENTIALS, state.activeModal)
        assertEquals(AuthMessages.INVALID_CREDENTIALS, state.activeModal?.message)
    }

    @Test
    fun `credenciales validas muestran modal de envio de codigo 2FA y al cerrar avanza a paso 2FA`() = runTest {
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged("conductor.bupas"))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged("Coexito2026*"))
        viewModel.onEvent(LoginUiEvent.OnSubmitCredentials)
        runCurrent()

        assertEquals(AuthAlertModal.EMAIL_CODE_SENT, viewModel.uiState.value.activeModal)
        assertEquals(AuthMessages.EMAIL_CODE_SENT, viewModel.uiState.value.activeModal?.message)

        viewModel.onEvent(LoginUiEvent.OnDismissModal)

        val stateAfterDismiss = viewModel.uiState.value
        assertNull(stateAfterDismiss.activeModal)
        assertEquals(AuthStep.TWO_FACTOR, stateAfterDismiss.authStep)
        assertEquals(180, stateAfterDismiss.remainingSeconds)
    }

    @Test
    fun `codigo 2FA incorrecto muestra error en rojo Codigo de verificacion incorrecto`() = runTest {
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged("conductor.bupas"))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged("Coexito2026*"))
        viewModel.onEvent(LoginUiEvent.OnSubmitCredentials)
        runCurrent()
        viewModel.onEvent(LoginUiEvent.OnDismissModal)

        viewModel.onEvent(LoginUiEvent.OnOtpCodeChanged("999999"))
        viewModel.onEvent(LoginUiEvent.OnSubmitOtpCode)
        runCurrent()

        assertEquals(AuthMessages.INVALID_OTP_CODE, viewModel.uiState.value.otpError)
    }

    @Test
    fun `temporizador de 3 minutos expira el codigo 2FA y bloquea la verificacion`() = runTest {
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged("conductor.bupas"))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged("Coexito2026*"))
        viewModel.onEvent(LoginUiEvent.OnSubmitCredentials)
        runCurrent()
        viewModel.onEvent(LoginUiEvent.OnDismissModal)

        advanceTimeBy(180_000L)
        runCurrent()

        val expiredState = viewModel.uiState.value
        assertEquals(0, expiredState.remainingSeconds)
        assertTrue(expiredState.isOtpExpired)
        assertEquals(AuthMessages.EXPIRED_OTP_CODE, expiredState.otpError)
        assertFalse(expiredState.isVerifyButtonEnabled)
    }

    @Test
    fun `codigo 2FA valido dentro de vigencia navega hacia bandeja de ordenes`() = runTest {
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged("conductor.bupas"))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged("Coexito2026*"))
        viewModel.onEvent(LoginUiEvent.OnSubmitCredentials)
        runCurrent()
        viewModel.onEvent(LoginUiEvent.OnDismissModal)

        viewModel.onEvent(LoginUiEvent.OnOtpCodeChanged("123456"))
        viewModel.onEvent(LoginUiEvent.OnSubmitOtpCode)
        runCurrent()

        val effect = viewModel.effects.first()
        assertEquals(LoginUiEffect.NavigateToOrdersInbox, effect)
    }
}
