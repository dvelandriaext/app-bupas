package co.com.coexito.bupas.domain.usecase.auth

import co.com.coexito.bupas.domain.model.AuthException
import co.com.coexito.bupas.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para solicitar código de recuperación de contraseña desde la pantalla
 * "¿Olvidaste tu contraseña?" (HU-001).
 */
class RequestPasswordRecoveryUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String): Result<String> {
        val sanitizedUser = username.trim()
        if (sanitizedUser.isBlank()) {
            return Result.failure(AuthException.RequiredFieldMissing)
        }
        return authRepository.requestPasswordRecovery(sanitizedUser)
    }
}
