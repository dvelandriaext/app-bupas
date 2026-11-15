package co.com.coexito.bupas.domain.usecase.collection

import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.FieldCollectionValidationErrors
import co.com.coexito.bupas.domain.model.OrdenStatus
import javax.inject.Inject

/**
 * Valida las reglas de negocio de HU-006 antes de guardar o finalizar la recolección en campo:
 * - Si el estado es NO_RECOLECTADA, exige obligatoriamente `detalleNovedad`.
 * - Al finalizar recolección (EN_ATENCION / RECOLECCION_PARCIAL), exige `cantidadActa != null`
 *   en todos los artículos y los datos del responsable que entrega.
 */
class ValidateFieldCollectionUseCase @Inject constructor() {

    operator fun invoke(
        estado: OrdenStatus,
        articulos: List<ArticuloBupas>,
        detalleNovedad: String,
        nombreResponsable: String,
        identificacionResponsable: String,
        isFinalizing: Boolean
    ): FieldCollectionValidationErrors {
        if (estado == OrdenStatus.NO_RECOLECTADA) {
            val novedadError = if (detalleNovedad.isBlank()) REQUIRED_FIELD_ERROR else null
            return FieldCollectionValidationErrors(detalleNovedadError = novedadError)
        }

        if (!isFinalizing) {
            return FieldCollectionValidationErrors()
        }

        val missingCodigos = articulos
            .filter { it.cantidadActa == null }
            .map { it.codigoArticulo }
            .toSet()

        val nombreError = if (nombreResponsable.isBlank()) REQUIRED_FIELD_ERROR else null
        val idError = if (identificacionResponsable.isBlank()) REQUIRED_FIELD_ERROR else null

        return FieldCollectionValidationErrors(
            missingCantidadActaCodigos = missingCodigos,
            nombreResponsableError = nombreError,
            identificacionResponsableError = idError
        )
    }

    companion object {
        const val REQUIRED_FIELD_ERROR = "Campo requerido"
    }
}
