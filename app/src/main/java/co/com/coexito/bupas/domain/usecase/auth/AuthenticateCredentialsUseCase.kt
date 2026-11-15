package co.com.coexito.bupas.domain.usecase.auth

import co.com.coexito.bupas.domain.model.AuthException
import co.com.coexito.bupas.domain.model.TwoFactorChallenge
import co.com.coexito.bupas.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para validar credenciales corporativas contra Central de Accesos y disparar
 * el envío del código 2FA de 6 dígitos al correo electrónico asociado (HU-001).
 */
class AuthenticateCredentialsUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        username: String,
        password: String
    ): Result<TwoFactorChallenge> {
        val sanitizedUser = username.trim()
        if (sanitizedUser.isBlank() || password.isBlank()) {
            return Result.failure(AuthException.RequiredFieldMissing)
        }
        return authRepository.authenticateCredentials(
            username = sanitizedUser,
            password = password
        )
    }
}
