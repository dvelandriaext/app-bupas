package co.com.coexito.bupas.domain.usecase.auth

import co.com.coexito.bupas.domain.model.UserSession
import co.com.coexito.bupas.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para observar el estado reactivo de la sesión autenticada y proteger rutas (HU-001).
 */
class ObserveUserSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserSession?> = authRepository.currentSession
}
