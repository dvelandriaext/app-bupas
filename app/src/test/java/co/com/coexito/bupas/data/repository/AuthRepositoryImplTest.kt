package co.com.coexito.bupas.data.repository

import co.com.coexito.bupas.data.local.SessionLocalDataSource
import co.com.coexito.bupas.data.remote.FakeAuthApiService
import co.com.coexito.bupas.domain.model.AuthException
import co.com.coexito.bupas.domain.model.AuthMessages
import co.com.coexito.bupas.domain.model.BupasUserRole
import co.com.coexito.bupas.domain.model.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var fakeSessionDataSource: FakeSessionLocalDataSource
    private var fakeClockMillis: Long = 1_000_000L
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        fakeSessionDataSource = FakeSessionLocalDataSource()
        fakeClockMillis = 1_000_000L
        val apiService = FakeAuthApiService(
            simulatedDelayMs = 0L,
            clockMillis = { fakeClockMillis },
            otpCodeGenerator = { "654321" }
        )
        repository = AuthRepositoryImpl(
            authApiService = apiService,
            sessionLocalDataSource = fakeSessionDataSource,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `authenticateCredentials con credenciales validas genera reto 2FA de 180 segundos`() = runTest {
        val result = repository.authenticateCredentials("conductor.bupas", "Coexito2026*")

        assertTrue(result.isSuccess)
        val challenge = result.getOrThrow()
        assertEquals("conductor.bupas", challenge.username)
        assertEquals(180, challenge.validityDurationSeconds)
        assertTrue(challenge.maskedEmail.endsWith("@coexito.com.co"))
    }

    @Test
    fun `authenticateCredentials con clave erronea o usuario inactivo retorna InvalidCredentials`() = runTest {
        val wrongPasswordResult = repository.authenticateCredentials("conductor.bupas", "ClaveErronea")
        val inactiveUserResult = repository.authenticateCredentials("conductor.inactivo", "Coexito2026*")
        val unauthorizedRoleResult = repository.authenticateCredentials("usuario.sinrol", "Coexito2026*")

        assertTrue(wrongPasswordResult.exceptionOrNull() is AuthException.InvalidCredentials)
        assertEquals(AuthMessages.INVALID_CREDENTIALS, wrongPasswordResult.exceptionOrNull()?.message)
        assertTrue(inactiveUserResult.exceptionOrNull() is AuthException.InvalidCredentials)
        assertTrue(unauthorizedRoleResult.exceptionOrNull() is AuthException.InvalidCredentials)
    }

    @Test
    fun `verifyTwoFactorCode con codigo correcto dentro de 3 minutos autentica y persiste sesion`() = runTest {
        repository.authenticateCredentials("conductor.bupas", "Coexito2026*")
        fakeClockMillis += 60_000L // 1 minuto transcurrido (< 3 min)

        val verifyResult = repository.verifyTwoFactorCode("conductor.bupas", "654321")

        assertTrue(verifyResult.isSuccess)
        val session = verifyResult.getOrThrow()
        assertEquals(BupasUserRole.CONDUCTOR, session.role)
        assertTrue(session.isTwoFactorVerified)
        assertNotNull(repository.currentSession.first())
    }

    @Test
    fun `verifyTwoFactorCode con codigo equivocado retorna InvalidTwoFactorCode`() = runTest {
        repository.authenticateCredentials("conductor.bupas", "Coexito2026*")

        val verifyResult = repository.verifyTwoFactorCode("conductor.bupas", "000000")

        assertTrue(verifyResult.exceptionOrNull() is AuthException.InvalidTwoFactorCode)
        assertEquals(AuthMessages.INVALID_OTP_CODE, verifyResult.exceptionOrNull()?.message)
        assertNull(repository.currentSession.first())
    }

    @Test
    fun `verifyTwoFactorCode despues de 3 minutos retorna ExpiredTwoFactorCode`() = runTest {
        repository.authenticateCredentials("conductor.bupas", "Coexito2026*")
        fakeClockMillis += 181_000L // > 180 segundos (3 minutos)

        val verifyResult = repository.verifyTwoFactorCode("conductor.bupas", "654321")

        assertTrue(verifyResult.exceptionOrNull() is AuthException.ExpiredTwoFactorCode)
        assertEquals(AuthMessages.EXPIRED_OTP_CODE, verifyResult.exceptionOrNull()?.message)
        assertNull(repository.currentSession.first())
    }

    @Test
    fun `clearSession destruye la sesion activa en Salida segura`() = runTest {
        repository.authenticateCredentials("conductor.bupas", "Coexito2026*")
        repository.verifyTwoFactorCode("conductor.bupas", "654321")
        assertNotNull(repository.currentSession.first())

        repository.clearSession()

        assertNull(repository.currentSession.first())
    }

    private class FakeSessionLocalDataSource : SessionLocalDataSource {
        private val state = MutableStateFlow<UserSession?>(null)
        override val sessionFlow: Flow<UserSession?> = state

        override suspend fun saveSession(session: UserSession) {
            state.value = session
        }

        override suspend fun clearSession() {
            state.value = null
        }
    }
}
