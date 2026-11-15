package co.com.coexito.bupas.data.remote

import co.com.coexito.bupas.domain.model.BupasUserRole
import co.com.coexito.bupas.domain.model.TwoFactorChallenge
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Simulador HTTP de [AuthApiService] con latencia de red configurable y códigos HTTP estándar
 * (200, 401, 410, 422) para pruebas locales previas a la integración real con CDA.
 */
@Singleton
class FakeAuthApiService(
    private val simulatedDelayMs: Long = DEFAULT_NETWORK_DELAY_MS,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val otpCodeGenerator: () -> String = { DEFAULT_VERIFICATION_CODE }
) : AuthApiService {

    @Inject
    constructor() : this(
        simulatedDelayMs = DEFAULT_NETWORK_DELAY_MS,
        clockMillis = { System.currentTimeMillis() },
        otpCodeGenerator = { DEFAULT_VERIFICATION_CODE }
    )

    private val activeChallenges = ConcurrentHashMap<String, PendingTwoFactorChallenge>()

    override suspend fun login(
        request: LoginRequestDto
    ): Response<TwoFactorChallengeResponseDto> {
        simulateNetworkLatency()

        val normalizedKey = request.username.trim().lowercase()
        val account = directoryAccounts[normalizedKey]
            ?: return errorJsonResponse(HTTP_UNAUTHORIZED, "Usuario o contraseña inválido.")

        if (!isAccountAuthorized(account, request.password)) {
            return errorJsonResponse(HTTP_UNAUTHORIZED, "Usuario o contraseña inválido.")
        }

        val generatedCode = otpCodeGenerator()
        val expiresAtMillis = clockMillis() +
            (TwoFactorChallenge.TWO_FACTOR_VALIDITY_SECONDS * MILLIS_PER_SECOND)

        activeChallenges[normalizedKey] = PendingTwoFactorChallenge(
            account = account,
            verificationCode = generatedCode,
            expiresAtMillis = expiresAtMillis
        )

        return Response.success(
            TwoFactorChallengeResponseDto(
                username = account.username,
                maskedEmail = maskCorporateEmail(account.email),
                validityDurationSeconds = TwoFactorChallenge.TWO_FACTOR_VALIDITY_SECONDS
            )
        )
    }

    override suspend fun verifyTwoFactor(
        request: VerifyTwoFactorRequestDto
    ): Response<UserSessionResponseDto> {
        simulateNetworkLatency()

        val normalizedKey = request.username.trim().lowercase()
        val pendingChallenge = activeChallenges[normalizedKey]
            ?: return errorJsonResponse(HTTP_UNPROCESSABLE_ENTITY, "Código de verificación incorrecto")

        if (clockMillis() > pendingChallenge.expiresAtMillis) {
            activeChallenges.remove(normalizedKey)
            return errorJsonResponse(HTTP_GONE, "El Código de verificación ha expirado")
        }

        if (request.code.trim() != pendingChallenge.verificationCode) {
            return errorJsonResponse(HTTP_UNPROCESSABLE_ENTITY, "Código de verificación incorrecto")
        }

        activeChallenges.remove(normalizedKey)
        val account = pendingChallenge.account

        return Response.success(
            UserSessionResponseDto(
                userId = account.userId,
                username = account.username,
                fullName = account.fullName,
                email = account.email,
                role = requireNotNull(account.role).name,
                cedi = account.cedi,
                accessToken = "bupas-jwt-${UUID.randomUUID()}",
                isTwoFactorVerified = true
            )
        )
    }

    override suspend fun requestPasswordRecovery(
        request: PasswordRecoveryRequestDto
    ): Response<PasswordRecoveryResponseDto> {
        simulateNetworkLatency()

        val normalizedKey = request.username.trim().lowercase()
        val account = directoryAccounts[normalizedKey]
        if (account == null || !account.isActive) {
            return errorJsonResponse(HTTP_UNAUTHORIZED, "Usuario o contraseña inválido.")
        }

        val masked = maskCorporateEmail(account.email)
        return Response.success(
            PasswordRecoveryResponseDto(
                message = "Se ha enviado un código de recuperación al correo electrónico registrado ($masked)."
            )
        )
    }

    fun clearPendingChallenges() {
        activeChallenges.clear()
    }

    private suspend fun simulateNetworkLatency() {
        if (simulatedDelayMs > 0L) {
            delay(simulatedDelayMs)
        }
    }

    private fun isAccountAuthorized(
        account: CentralAccesosAccount,
        password: String
    ): Boolean {
        val passwordMatches = account.validPasswords.contains(password)
        val hasAuthorizedRole = account.role != null
        return account.isActive && passwordMatches && hasAuthorizedRole
    }

    private fun maskCorporateEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val local = parts[0]
        val domain = parts[1]
        return "${local.take(3)}***@$domain"
    }

    private fun <T> errorJsonResponse(statusCode: Int, message: String): Response<T> {
        val payload = """{"message":"$message"}"""
        return Response.error(
            statusCode,
            payload.toResponseBody("application/json".toMediaType())
        )
    }

    private data class CentralAccesosAccount(
        val userId: String,
        val username: String,
        val fullName: String,
        val email: String,
        val cedi: String,
        val validPasswords: Set<String>,
        val isActive: Boolean,
        val role: BupasUserRole?
    )

    private data class PendingTwoFactorChallenge(
        val account: CentralAccesosAccount,
        val verificationCode: String,
        val expiresAtMillis: Long
    )

    companion object {
        const val DEFAULT_VERIFICATION_CODE = "123456"
        private const val DEFAULT_NETWORK_DELAY_MS = 650L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_GONE = 410
        private const val HTTP_UNPROCESSABLE_ENTITY = 422

        private val directoryAccounts: Map<String, CentralAccesosAccount> = listOf(
            CentralAccesosAccount(
                userId = "USR-COND-001",
                username = "conductor.bupas",
                fullName = "Jorge Eliecer Rodríguez",
                email = "conductor.bupas@coexito.com.co",
                cedi = "CEDI Cali",
                validPasswords = setOf("Coexito2026*", "123456"),
                isActive = true,
                role = BupasUserRole.CONDUCTOR
            ),
            CentralAccesosAccount(
                userId = "USR-COND-002",
                username = "conductor",
                fullName = "Jorge Eliecer Rodríguez",
                email = "jrodriguez@coexito.com.co",
                cedi = "CEDI Cali",
                validPasswords = setOf("Coexito2026*", "123456"),
                isActive = true,
                role = BupasUserRole.CONDUCTOR
            ),
            CentralAccesosAccount(
                userId = "USR-COND-INACTIVO",
                username = "conductor.inactivo",
                fullName = "Usuario Bloqueado",
                email = "inactivo@coexito.com.co",
                cedi = "CEDI Bogotá",
                validPasswords = setOf("Coexito2026*"),
                isActive = false,
                role = BupasUserRole.CONDUCTOR
            ),
            CentralAccesosAccount(
                userId = "USR-SIN-ROL",
                username = "usuario.sinrol",
                fullName = "Usuario Sin Perfil BUPAS",
                email = "sinrol@coexito.com.co",
                cedi = "CEDI Medellín",
                validPasswords = setOf("Coexito2026*"),
                isActive = true,
                role = null
            )
        ).associateBy { it.username.lowercase() }
    }
}
