package co.com.coexito.bupas.domain.model

import androidx.compose.runtime.Immutable

/**
 * Criterios de búsqueda y filtrado para la Bandeja de Órdenes Asignadas (HU-005 CA 1).
 */
@Immutable
data class OrderSearchFilter(
    val startDate: String? = null,
    val endDate: String? = null,
    val status: OrdenStatus? = null,
    val clientQuery: String = "",
    val numeroSolicitud: String = "",
    val identificadorOm: String = ""
) {
    val hasActiveFilters: Boolean
        get() = !startDate.isNullOrBlank() ||
            !endDate.isNullOrBlank() ||
            status != null ||
            clientQuery.isNotBlank() ||
            numeroSolicitud.isNotBlank() ||
            identificadorOm.isNotBlank()
}

/**
 * Resultado paginado/contabilizado de órdenes asignadas al conductor.
 */
@Immutable
data class AssignedOrdersResult(
    val orders: List<OrdenRecoleccion>,
    val totalCount: Int
)
