package co.com.coexito.bupas.domain.usecase.auth

import co.com.coexito.bupas.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para destruir la sesión activa al presionar "Salida segura" (HU-001).
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.clearSession()
    }
}
