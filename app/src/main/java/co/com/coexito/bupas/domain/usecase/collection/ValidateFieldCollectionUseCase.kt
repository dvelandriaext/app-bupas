package co.com.coexito.bupas.domain.usecase.collection

import androidx.compose.runtime.Immutable
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.FieldCollectionValidationErrors
import co.com.coexito.bupas.domain.model.OrdenStatus
import javax.inject.Inject

@Immutable
data class ProvenienteValidationResult(
    val nitError: String? = null,
    val razonSocialError: String? = null,
    val cantidadError: String? = null,
    val telefonoError: String? = null,
    val correoError: String? = null
) {
    val isValid: Boolean
        get() = nitError == null &&
            razonSocialError == null &&
            cantidadError == null &&
            telefonoError == null &&
            correoError == null
}

/**
 * Valida las reglas de negocio de HU-006 (CA 2, CA 5, CA 6, CA 7 y casos borde R2.1, R2.2, R2.3):
 * - En `NO_RECOLECTADA`, exige obligatoriamente `detalleNovedad` e ignora campos físicos.
 * - Al finalizar recolección física, exige `cantidadActa >= 0` en todos los artículos,
 *   datos del responsable que entrega y firma digital (`firmaDigitalBase64`).
 * - Restringe `Cantidad Acta` a enteros `>= 0` (rechaza negativos y decimales).
 * - Bloquea `Provenientes` si falta diligenciar `Cantidad Acta` en cualquier referencia BUPAS.
 */
class ValidateFieldCollectionUseCase @Inject constructor() {

    operator fun invoke(
        estado: OrdenStatus,
        detalleNovedad: String = "",
        articulos: List<ArticuloBupas> = emptyList(),
        nombreResponsable: String = "",
        identificacionResponsable: String = "",
        firmaDigitalBase64: String? = null,
        isFinalizing: Boolean = true,
        invalidCantidadActaCodigos: Map<String, String> = emptyMap()
    ): FieldCollectionValidationErrors {
        if (estado == OrdenStatus.NO_RECOLECTADA) {
            val novedadError = if (detalleNovedad.isBlank()) REQUIRED_FIELD_ERROR else null
            return FieldCollectionValidationErrors(detalleNovedadError = novedadError)
        }

        if (!isFinalizing) {
            return FieldCollectionValidationErrors(
                invalidCantidadActaCodigos = invalidCantidadActaCodigos
            )
        }

        return validatePhysicalFinalization(
            articulos = articulos,
            nombreResponsable = nombreResponsable,
            identificacionResponsable = identificacionResponsable,
            firmaDigitalBase64 = firmaDigitalBase64,
            invalidCantidadActaCodigos = invalidCantidadActaCodigos
        )
    }

    fun validateProvenientesAccess(
        articulos: List<ArticuloBupas>,
        invalidCantidadActaCodigos: Map<String, String> = emptyMap()
    ): FieldCollectionValidationErrors {
        val missingCodigos = findMissingCantidadActaCodigos(articulos)
        val mergedInvalid = findNegativeCantidadActaCodigos(articulos) + invalidCantidadActaCodigos

        if (missingCodigos.isEmpty() && mergedInvalid.isEmpty()) {
            return FieldCollectionValidationErrors()
        }

        return FieldCollectionValidationErrors(
            missingCantidadActaCodigos = missingCodigos,
            invalidCantidadActaCodigos = mergedInvalid,
            provenientesGateError = PROVENIENTES_GATE_ERROR
        )
    }

    fun parseAndValidateCantidadActa(rawInput: String): Result<Int?> {
        if (rawInput.isEmpty()) {
            return Result.success(null)
        }
        val trimmed = rawInput.trim()
        if (!DIGITS_ONLY_REGEX.matches(trimmed)) {
            return Result.failure(IllegalArgumentException(INVALID_CANTIDAD_ACTA_ERROR))
        }
        val parsed = trimmed.toIntOrNull()
        return if (parsed != null && parsed >= 0) {
            Result.success(parsed)
        } else {
            Result.failure(IllegalArgumentException(INVALID_CANTIDAD_ACTA_ERROR))
        }
    }

    fun validateProveniente(
        nit: String,
        razonSocial: String,
        cantidadRaw: String,
        telefono: String,
        correo: String
    ): ProvenienteValidationResult {
        val trimmedCantidad = cantidadRaw.trim()
        val parsedCantidad = if (DIGITS_ONLY_REGEX.matches(trimmedCantidad)) {
            trimmedCantidad.toIntOrNull()
        } else {
            null
        }

        return ProvenienteValidationResult(
            nitError = if (nit.isBlank()) REQUIRED_FIELD_ERROR else null,
            razonSocialError = if (razonSocial.isBlank()) REQUIRED_FIELD_ERROR else null,
            cantidadError = if (parsedCantidad == null || parsedCantidad <= 0) {
                INVALID_PROVENIENTE_CANTIDAD_ERROR
            } else {
                null
            },
            telefonoError = if (telefono.isBlank()) REQUIRED_FIELD_ERROR else null,
            correoError = if (correo.isBlank() || !EMAIL_REGEX.matches(correo.trim())) {
                INVALID_EMAIL_ERROR
            } else {
                null
            }
        )
    }

    private fun validatePhysicalFinalization(
        articulos: List<ArticuloBupas>,
        nombreResponsable: String,
        identificacionResponsable: String,
        firmaDigitalBase64: String?,
        invalidCantidadActaCodigos: Map<String, String>
    ): FieldCollectionValidationErrors {
        val missingCodigos = findMissingCantidadActaCodigos(articulos)
        val mergedInvalid = findNegativeCantidadActaCodigos(articulos) + invalidCantidadActaCodigos

        return FieldCollectionValidationErrors(
            missingCantidadActaCodigos = missingCodigos,
            invalidCantidadActaCodigos = mergedInvalid,
            nombreResponsableError = if (nombreResponsable.isBlank()) REQUIRED_FIELD_ERROR else null,
            identificacionResponsableError = if (identificacionResponsable.isBlank()) REQUIRED_FIELD_ERROR else null,
            firmaDigitalError = if (firmaDigitalBase64.isNullOrBlank()) REQUIRED_FIELD_ERROR else null
        )
    }

    private fun findMissingCantidadActaCodigos(articulos: List<ArticuloBupas>): Set<String> =
        articulos.filter { it.cantidadActa == null }.map { it.codigoArticulo }.toSet()

    private fun findNegativeCantidadActaCodigos(articulos: List<ArticuloBupas>): Map<String, String> =
        articulos
            .filter { it.cantidadActa != null && it.cantidadActa < 0 }
            .associate { it.codigoArticulo to INVALID_CANTIDAD_ACTA_ERROR }

    companion object {
        const val REQUIRED_FIELD_ERROR = "Campo requerido"
        const val INVALID_CANTIDAD_ACTA_ERROR = "Ingrese un número entero mayor o igual a 0"
        const val PROVENIENTES_GATE_ERROR =
            "Debe diligenciar la Cantidad Acta en todos los artículos BUPAS antes de gestionar Provenientes"
        const val INVALID_PROVENIENTE_CANTIDAD_ERROR = "Ingrese una cantidad mayor a 0"
        const val INVALID_EMAIL_ERROR = "Correo electrónico inválido"

        private val DIGITS_ONLY_REGEX = Regex("^\\d+$")
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
