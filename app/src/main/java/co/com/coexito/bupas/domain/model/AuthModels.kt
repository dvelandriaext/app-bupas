package co.com.coexito.bupas.domain.model

/**
 * Roles autorizados dentro de la plataforma de Logística Reversiva BUPAS (HU-001).
 */
enum class BupasUserRole(val displayName: String) {
    CONDUCTOR("Conductor"),
    GERENTE_VENTAS("Gerente de Ventas"),
    COORDINADOR_LOGISTICO("Coordinador Logístico"),
    ADMINISTRADOR("Administrador")
}

/**
 * Sesión autenticada y verificada mediante Doble Factor (2FA) del usuario BUPAS.
 */
data class UserSession(
    val userId: String,
    val username: String,
    val fullName: String,
    val email: String,
    val role: BupasUserRole,
    val cedi: String,
    val accessToken: String,
    val isTwoFactorVerified: Boolean
)

/**
 * Reto de Doble Autenticación (2FA) generado tras validar credenciales en Central de Accesos (HU-001).
 * El código numérico de 6 dígitos enviado al correo tiene una vigencia estricta de 3 minutos (180 s).
 */
data class TwoFactorChallenge(
    val username: String,
    val maskedEmail: String,
    val validityDurationSeconds: Int = TWO_FACTOR_VALIDITY_SECONDS
) {
    companion object {
        const val TWO_FACTOR_VALIDITY_SECONDS: Int = 180
        const val TWO_FACTOR_CODE_LENGTH: Int = 6
    }
}

/**
 * Mensajes oficiales de validación y error definidos en los criterios de aceptación de la HU-001.
 */
object AuthMessages {
    const val REQUIRED_FIELD = "Campo requerido"
    const val INVALID_CREDENTIALS = "Usuario o contraseña inválido."
    const val EMAIL_CODE_SENT = "Se ha enviado un código de verificación a su correo electrónico"
    const val INVALID_OTP_CODE = "Código de verificación incorrecto"
    const val EXPIRED_OTP_CODE = "El Código de verificación ha expirado"
}

/**
 * Excepciones tipadas de negocio para el flujo de autenticación y 2FA (HU-001).
 */
sealed class AuthException(override val message: String) : Exception(message) {
    data object RequiredFieldMissing : AuthException(AuthMessages.REQUIRED_FIELD)
    data object InvalidCredentials : AuthException(AuthMessages.INVALID_CREDENTIALS)
    data object InvalidTwoFactorCode : AuthException(AuthMessages.INVALID_OTP_CODE)
    data object ExpiredTwoFactorCode : AuthException(AuthMessages.EXPIRED_OTP_CODE)
}
