package co.com.coexito.bupas.domain.repository

import co.com.coexito.bupas.domain.model.AssignedOrdersResult
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import co.com.coexito.bupas.domain.model.PrecioArticuloBupas

interface OrderRepository {
    suspend fun getAssignedOrders(
        filter: OrderSearchFilter = OrderSearchFilter()
    ): Result<AssignedOrdersResult>

    suspend fun getOrderBySolicitud(numeroSolicitud: String): Result<OrdenRecoleccion>

    suspend fun getClientPriceList(
        nitCliente: String
    ): Result<List<PrecioArticuloBupas>> = Result.success(emptyList())

    suspend fun updateOrderStatus(
        numeroSolicitud: String,
        newStatus: OrdenStatus,
        observation: String? = null
    ): Result<Unit>

    suspend fun saveFieldCollection(
        submission: FieldCollectionSubmission
    ): Result<OrdenRecoleccion>

    suspend fun hasPendingOfflineDraft(
        numeroSolicitud: String
    ): Boolean = false
}
