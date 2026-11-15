package co.com.coexito.bupas.domain.usecase.auth

import co.com.coexito.bupas.domain.model.AuthException
import co.com.coexito.bupas.domain.model.TwoFactorChallenge
import co.com.coexito.bupas.domain.model.UserSession
import co.com.coexito.bupas.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para verificar el código numérico de 6 dígitos de Doble Autenticación (2FA)
 * dentro de la ventana de vigencia de 3 minutos (HU-001).
 */
class VerifyTwoFactorCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        username: String,
        code: String,
        isTimerExpired: Boolean = false
    ): Result<UserSession> {
        if (isTimerExpired) {
            return Result.failure(AuthException.ExpiredTwoFactorCode)
        }
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) {
            return Result.failure(AuthException.RequiredFieldMissing)
        }
        val isSixDigits = cleanCode.length == TwoFactorChallenge.TWO_FACTOR_CODE_LENGTH &&
            cleanCode.all { it.isDigit() }
        if (!isSixDigits) {
            return Result.failure(AuthException.InvalidTwoFactorCode)
        }
        return authRepository.verifyTwoFactorCode(
            username = username.trim(),
            code = cleanCode
        )
    }
}
