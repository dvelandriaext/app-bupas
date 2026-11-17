package co.com.coexito.bupas.domain.model

import androidx.compose.runtime.Immutable

/**
 * Solicitud de guardado o cierre definitivo de recolección en campo (HU-006).
 */
@Immutable
data class FieldCollectionSubmission(
    val numeroSolicitud: String,
    val estado: OrdenStatus,
    val articulos: List<ArticuloBupas>,
    val aplicaProvenientes: Boolean,
    val provenientes: List<TerceroProveniente>,
    val detalleNovedad: String?,
    val observacionesCampo: String,
    val nombreResponsableEntrega: String,
    val identificacionResponsableEntrega: String,
    val finalizeCollection: Boolean,
    val fechaHoraInicioAtencion: String? = null,
    val fechaHoraFinalizacion: String? = null,
    val firmaDigitalBase64: String? = null
)

/**
 * Errores de validación estructurados por campo para la pantalla de recolección (HU-006).
 */
@Immutable
data class FieldCollectionValidationErrors(
    val detalleNovedadError: String? = null,
    val missingCantidadActaCodigos: Set<String> = emptySet(),
    val invalidCantidadActaCodigos: Map<String, String> = emptyMap(),
    val provenientesGateError: String? = null,
    val nombreResponsableError: String? = null,
    val identificacionResponsableError: String? = null,
    val firmaDigitalError: String? = null
) {
    val isValid: Boolean
        get() = detalleNovedadError == null &&
            missingCantidadActaCodigos.isEmpty() &&
            invalidCantidadActaCodigos.isEmpty() &&
            provenientesGateError == null &&
            nombreResponsableError == null &&
            identificacionResponsableError == null &&
            firmaDigitalError == null
}

/**
 * Resumen de ejecución de la sincronización ascendente (App -> Backend)
 * de avances y cierres registrados sin conexión (HU-006 y HU-007).
 */
@Immutable
data class OfflineSyncSummary(
    val totalPendingBeforeSync: Int = 0,
    val syncedCount: Int = 0,
    val remainingPendingCount: Int = 0,
    val generatedActas: List<String> = emptyList()
)
