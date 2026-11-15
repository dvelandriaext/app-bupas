package co.com.coexito.bupas.data.repository

import co.com.coexito.bupas.core.di.IoDispatcher
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.ArticuloCantidadDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.data.remote.mapper.toDomain
import co.com.coexito.bupas.data.remote.mapper.toDto
import co.com.coexito.bupas.domain.model.AssignedOrdersResult
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class OrderRepositoryImpl @Inject constructor(
    private val orderApiService: OrderApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OrderRepository {

    override suspend fun getAssignedOrders(
        filter: OrderSearchFilter
    ): Result<AssignedOrdersResult> = withContext(ioDispatcher) {
        runCatching {
            orderApiService.getAssignedOrders(
                startDate = filter.startDate,
                endDate = filter.endDate,
                status = filter.status?.name,
                clientQuery = filter.clientQuery.takeIf { it.isNotBlank() },
                numeroSolicitud = filter.numeroSolicitud.takeIf { it.isNotBlank() },
                identificadorOm = filter.identificadorOm.takeIf { it.isNotBlank() }
            ).toDomain()
        }
    }

    override suspend fun getOrderBySolicitud(
        numeroSolicitud: String
    ): Result<OrdenRecoleccion> = withContext(ioDispatcher) {
        runCatching {
            orderApiService.getOrderBySolicitud(numeroSolicitud).toDomain()
        }
    }

    override suspend fun updateOrderStatus(
        numeroSolicitud: String,
        newStatus: OrdenStatus,
        observation: String?
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val current = orderApiService.getOrderBySolicitud(numeroSolicitud)
            orderApiService.saveFieldCollection(
                solicitudId = numeroSolicitud,
                request = SaveFieldCollectionRequestDto(
                    estado = newStatus.name,
                    articulos = current.articulos.map { ArticuloCantidadDto(it.codigoArticulo, it.cantidadActa) },
                    aplicaProvenientes = current.aplicaProvenientes,
                    provenientes = current.provenientes,
                    detalleNovedad = if (newStatus == OrdenStatus.NO_RECOLECTADA) observation else current.detalleNovedad,
                    observacionesCampo = observation ?: current.observacionesCampo,
                    nombreResponsableEntrega = current.nombreResponsableEntrega,
                    identificacionResponsableEntrega = current.identificacionResponsableEntrega,
                    finalizeCollection = newStatus == OrdenStatus.RECOLECCION_REALIZADA
                )
            )
            Unit
        }
    }

    override suspend fun saveFieldCollection(
        submission: FieldCollectionSubmission
    ): Result<OrdenRecoleccion> = withContext(ioDispatcher) {
        runCatching {
            orderApiService.saveFieldCollection(
                solicitudId = submission.numeroSolicitud,
                request = submission.toDto()
            ).toDomain()
        }
    }
}
