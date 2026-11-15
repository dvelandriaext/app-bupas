package co.com.coexito.bupas.domain.repository

import co.com.coexito.bupas.domain.model.TwoFactorChallenge
import co.com.coexito.bupas.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de dominio para autenticación con Central de Accesos, verificación 2FA
 * por correo electrónico, recuperación de contraseña y cierre seguro de sesión (HU-001).
 */
interface AuthRepository {
    val currentSession: Flow<UserSession?>

    suspend fun authenticateCredentials(
        username: String,
        password: String
    ): Result<TwoFactorChallenge>

    suspend fun verifyTwoFactorCode(
        username: String,
        code: String
    ): Result<UserSession>

    suspend fun requestPasswordRecovery(
        username: String
    ): Result<String>

    suspend fun clearSession()
}
