package co.com.coexito.bupas.presentation.auth

import co.com.coexito.bupas.data.local.SessionLocalDataSource
import co.com.coexito.bupas.data.remote.FakeAuthApiService
import co.com.coexito.bupas.data.repository.AuthRepositoryImpl
import co.com.coexito.bupas.domain.model.AuthMessages
import co.com.coexito.bupas.domain.model.UserSession
import co.com.coexito.bupas.domain.usecase.auth.RequestPasswordRecoveryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoverPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RecoverPasswordViewModel

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
        val repository = AuthRepositoryImpl(
            authApiService = FakeAuthApiService(simulatedDelayMs = 0L),
            sessionLocalDataSource = sessionDataSource,
            ioDispatcher = testDispatcher
        )
        viewModel = RecoverPasswordViewModel(
            requestPasswordRecoveryUseCase = RequestPasswordRecoveryUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `usuario vacio en recuperar contrasena muestra Campo requerido`() {
        viewModel.onEvent(RecoverPasswordUiEvent.OnUsernameChanged(""))

        val state = viewModel.uiState.value
        assertEquals(AuthMessages.REQUIRED_FIELD, state.usernameError)
        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `solicitar codigo para usuario activo muestra modal de confirmacion`() = runTest {
        viewModel.onEvent(RecoverPasswordUiEvent.OnUsernameChanged("conductor.bupas"))
        viewModel.onEvent(RecoverPasswordUiEvent.OnRequestRecoveryCode)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isModalError)
        assertNotNull(state.modalMessage)
        assertTrue(state.shouldReturnToLoginOnModalDismiss)
    }
}
