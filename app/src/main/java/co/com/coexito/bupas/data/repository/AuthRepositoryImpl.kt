package co.com.coexito.bupas.data.repository

import co.com.coexito.bupas.core.di.IoDispatcher
import co.com.coexito.bupas.data.local.SessionLocalDataSource
import co.com.coexito.bupas.data.remote.AuthApiService
import co.com.coexito.bupas.data.remote.FakeAuthApiService
import co.com.coexito.bupas.data.remote.LoginRequestDto
import co.com.coexito.bupas.data.remote.PasswordRecoveryRequestDto
import co.com.coexito.bupas.data.remote.UserSessionResponseDto
import co.com.coexito.bupas.data.remote.VerifyTwoFactorRequestDto
import co.com.coexito.bupas.domain.model.AuthException
import co.com.coexito.bupas.domain.model.BupasUserRole
import co.com.coexito.bupas.domain.model.TwoFactorChallenge
import co.com.coexito.bupas.domain.model.UserSession
import co.com.coexito.bupas.domain.repository.AuthRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementación del repositorio de autenticación que consume [AuthApiService] (HTTP DTOs)
 * y persiste la sesión verificada en [SessionLocalDataSource] (HU-001).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionLocalDataSource: SessionLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    override val currentSession: Flow<UserSession?> = sessionLocalDataSource.sessionFlow

    override suspend fun authenticateCredentials(
        username: String,
        password: String
    ): Result<TwoFactorChallenge> = withContext(ioDispatcher) {
        try {
            val response = authApiService.login(
                LoginRequestDto(username = username.trim(), password = password)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(
                    TwoFactorChallenge(
                        username = body.username,
                        maskedEmail = body.maskedEmail,
                        validityDurationSeconds = body.validityDurationSeconds
                    )
                )
            } else {
                Result.failure(AuthException.InvalidCredentials)
            }
        } catch (_: IOException) {
            Result.failure(AuthException.InvalidCredentials)
        }
    }

    override suspend fun verifyTwoFactorCode(
        username: String,
        code: String
    ): Result<UserSession> = withContext(ioDispatcher) {
        try {
            val response = authApiService.verifyTwoFactor(
                VerifyTwoFactorRequestDto(username = username.trim(), code = code.trim())
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val session = body.toDomain()
                sessionLocalDataSource.saveSession(session)
                Result.success(session)
            } else if (response.code() == HTTP_GONE) {
                Result.failure(AuthException.ExpiredTwoFactorCode)
            } else {
                Result.failure(AuthException.InvalidTwoFactorCode)
            }
        } catch (_: IOException) {
            Result.failure(AuthException.InvalidTwoFactorCode)
        }
    }

    override suspend fun requestPasswordRecovery(
        username: String
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val response = authApiService.requestPasswordRecovery(
                PasswordRecoveryRequestDto(username = username.trim())
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.message)
            } else {
                Result.failure(AuthException.InvalidCredentials)
            }
        } catch (_: IOException) {
            Result.failure(AuthException.InvalidCredentials)
        }
    }

    override suspend fun clearSession() = withContext(ioDispatcher) {
        if (authApiService is FakeAuthApiService) {
            authApiService.clearPendingChallenges()
        }
        sessionLocalDataSource.clearSession()
    }

    private fun UserSessionResponseDto.toDomain(): UserSession {
        val mappedRole = BupasUserRole.entries.firstOrNull { it.name == role }
            ?: BupasUserRole.CONDUCTOR
        return UserSession(
            userId = userId,
            username = username,
            fullName = fullName,
            email = email,
            role = mappedRole,
            cedi = cedi,
            accessToken = accessToken,
            isTwoFactorVerified = isTwoFactorVerified
        )
    }

    private companion object {
        const val HTTP_GONE = 410
    }
}
