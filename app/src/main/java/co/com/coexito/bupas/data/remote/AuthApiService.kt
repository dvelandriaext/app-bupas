package co.com.coexito.bupas.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Contrato HTTP Retrofit para autenticación contra Central de Accesos / Backend BUPAS (HU-001).
 * Actualmente respaldado por [FakeAuthApiService] y listo para conectarse a Retrofit sin
 * modificar Repository, UseCases ni ViewModels.
 */
interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<TwoFactorChallengeResponseDto>

    @POST("api/v1/auth/verify-2fa")
    suspend fun verifyTwoFactor(
        @Body request: VerifyTwoFactorRequestDto
    ): Response<UserSessionResponseDto>

    @POST("api/v1/auth/recover-password")
    suspend fun requestPasswordRecovery(
        @Body request: PasswordRecoveryRequestDto
    ): Response<PasswordRecoveryResponseDto>
}

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class TwoFactorChallengeResponseDto(
    val username: String,
    val maskedEmail: String,
    val validityDurationSeconds: Int
)

@Serializable
data class VerifyTwoFactorRequestDto(
    val username: String,
    val code: String
)

@Serializable
data class UserSessionResponseDto(
    val userId: String,
    val username: String,
    val fullName: String,
    val email: String,
    val role: String,
    val cedi: String,
    val accessToken: String,
    val isTwoFactorVerified: Boolean
)

@Serializable
data class PasswordRecoveryRequestDto(
    val username: String
)

@Serializable
data class PasswordRecoveryResponseDto(
    val message: String
)
