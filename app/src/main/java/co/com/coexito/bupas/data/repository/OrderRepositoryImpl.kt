package co.com.coexito.bupas.data.repository

import co.com.coexito.bupas.core.di.IoDispatcher
import co.com.coexito.bupas.data.local.FieldCollectionLocalDataSource
import co.com.coexito.bupas.data.local.InMemoryFieldCollectionLocalDataSource
import co.com.coexito.bupas.data.local.StoredFieldCollectionDraftDto
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.ArticuloCantidadDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.data.remote.mapper.parseOrdenStatus
import co.com.coexito.bupas.data.remote.mapper.toDomain
import co.com.coexito.bupas.data.remote.mapper.toDto
import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.model.AssignedOrdersResult
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import co.com.coexito.bupas.domain.model.PrecioArticuloBupas
import co.com.coexito.bupas.domain.repository.OrderRepository
import co.com.coexito.bupas.domain.usecase.certificate.BuildActaRespelUseCase
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class OrderRepositoryImpl @Inject constructor(
    private val orderApiService: OrderApiService,
    private val localDataSource: FieldCollectionLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OrderRepository {

    constructor(
        orderApiService: OrderApiService,
        ioDispatcher: CoroutineDispatcher
    ) : this(
        orderApiService = orderApiService,
        localDataSource = InMemoryFieldCollectionLocalDataSource(),
        ioDispatcher = ioDispatcher
    )

    override suspend fun getAssignedOrders(
        filter: OrderSearchFilter
    ): Result<AssignedOrdersResult> = withContext(ioDispatcher) {
        runCatching {
            val response = orderApiService.getAssignedOrders(
                startDate = filter.startDate,
                endDate = filter.endDate,
                status = filter.status?.name,
                clientQuery = filter.clientQuery.takeIf { it.isNotBlank() },
                numeroSolicitud = filter.numeroSolicitud.takeIf { it.isNotBlank() },
                identificadorOm = filter.identificadorOm.takeIf { it.isNotBlank() }
            )
            response.orders.forEach { localDataSource.cacheOrder(it) }
            response.toDomain()
        }
    }

    override suspend fun getOrderBySolicitud(
        numeroSolicitud: String
    ): Result<OrdenRecoleccion> = withContext(ioDispatcher) {
        runCatching {
            fetchAndResolveOrder(numeroSolicitud).toDomain()
        }
    }

    override suspend fun getClientPriceList(
        nitCliente: String
    ): Result<List<PrecioArticuloBupas>> = withContext(ioDispatcher) {
        runCatching {
            try {
                val remoteItems = orderApiService.getClientPriceList(nitCliente)
                localDataSource.cacheClientPriceList(nitCliente, remoteItems)
                remoteItems.map { it.toDomain() }
            } catch (ioError: IOException) {
                val cached = localDataSource.getCachedClientPriceList(nitCliente)
                    ?: throw ioError
                cached.map { it.toDomain() }
            }
        }
    }

    override suspend fun updateOrderStatus(
        numeroSolicitud: String,
        newStatus: OrdenStatus,
        observation: String?
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            ensureNotLockedCached(numeroSolicitud)
            val current = orderApiService.getOrderBySolicitud(numeroSolicitud)
            ensureNotLockedDto(current)
            val updated = orderApiService.saveFieldCollection(
                solicitudId = numeroSolicitud,
                request = SaveFieldCollectionRequestDto(
                    estado = newStatus.name,
                    articulos = current.articulos.map {
                        ArticuloCantidadDto(it.codigoArticulo, it.cantidadActa)
                    },
                    aplicaProvenientes = current.aplicaProvenientes,
                    provenientes = current.provenientes,
                    detalleNovedad = if (newStatus == OrdenStatus.NO_RECOLECTADA) observation else current.detalleNovedad,
                    observacionesCampo = observation ?: current.observacionesCampo,
                    nombreResponsableEntrega = current.nombreResponsableEntrega,
                    identificacionResponsableEntrega = current.identificacionResponsableEntrega,
                    finalizeCollection = newStatus == OrdenStatus.RECOLECCION_REALIZADA,
                    fechaHoraInicioAtencion = current.fechaHoraInicioAtencion,
                    fechaHoraFinalizacion = current.fechaHoraFinalizacion,
                    firmaDigitalBase64 = current.firmaDigitalBase64
                )
            )
            localDataSource.cacheOrder(updated)
            Unit
        }
    }

    override suspend fun saveFieldCollection(
        submission: FieldCollectionSubmission
    ): Result<OrdenRecoleccion> = withContext(ioDispatcher) {
        runCatching {
            ensureNotLockedCached(submission.numeroSolicitud)
            val baselineOrder = loadBaselineAndVerifyOpen(submission.numeroSolicitud)
            val requestDto = submission.toDto()

            try {
                val updated = orderApiService.saveFieldCollection(
                    solicitudId = submission.numeroSolicitud,
                    request = requestDto
                )
                localDataSource.clearDraft(submission.numeroSolicitud)
                localDataSource.cacheOrder(updated)
                updated.toDomain()
            } catch (ioError: IOException) {
                handleOfflineSaveFailure(submission, requestDto, baselineOrder, ioError)
            }
        }
    }

    private val buildActaRespelUseCase = BuildActaRespelUseCase()

    override suspend fun hasPendingOfflineDraft(
        numeroSolicitud: String
    ): Boolean = withContext(ioDispatcher) {
        localDataSource.getDraft(numeroSolicitud)?.pendingSync == true
    }

    override suspend fun getActaBySolicitud(
        numeroSolicitud: String
    ): Result<ActaRespel> = withContext(ioDispatcher) {
        runCatching {
            val order = fetchAndResolveOrder(numeroSolicitud).toDomain()
            buildActaRespelUseCase(order)
                ?: throw IllegalStateException(
                    "La orden ${order.numeroSolicitud} no cuenta con Acta Respel emitida (requiere Recolección Realizada con cantidades > 0)."
                )
        }
    }

    override suspend fun retryActaContingency(
        numeroSolicitud: String
    ): Result<ActaRespel> = withContext(ioDispatcher) {
        runCatching {
            val updatedDto = orderApiService.retryActaContingency(numeroSolicitud)
            localDataSource.cacheOrder(updatedDto)
            val order = updatedDto.toDomain()
            buildActaRespelUseCase(order)
                ?: throw IllegalStateException(
                    "No fue posible regenerar el Acta Respel para la solicitud ${order.numeroSolicitud}."
                )
        }
    }

    private suspend fun fetchAndResolveOrder(numeroSolicitud: String): OrderDto {
        val remoteOrder = try {
            val fetched = orderApiService.getOrderBySolicitud(numeroSolicitud)
            localDataSource.cacheOrder(fetched)
            fetched
        } catch (ioError: IOException) {
            localDataSource.getCachedOrder(numeroSolicitud) ?: throw ioError
        }

        if (parseOrdenStatus(remoteOrder.estado).isReadOnlyLocked) {
            localDataSource.clearDraft(numeroSolicitud)
            return remoteOrder
        }

        val pendingDraft = localDataSource.getDraft(numeroSolicitud)
        return if (pendingDraft != null && pendingDraft.pendingSync) {
            mergeOrderWithDraft(remoteOrder, pendingDraft)
        } else {
            remoteOrder
        }
    }

    private suspend fun loadBaselineAndVerifyOpen(numeroSolicitud: String): OrderDto? {
        val remoteCurrent = try {
            val fetched = orderApiService.getOrderBySolicitud(numeroSolicitud)
            localDataSource.cacheOrder(fetched)
            fetched
        } catch (_: IOException) {
            localDataSource.getCachedOrder(numeroSolicitud)
        }
        if (remoteCurrent != null) {
            ensureNotLockedDto(remoteCurrent)
        }
        return remoteCurrent
    }

    private suspend fun handleOfflineSaveFailure(
        submission: FieldCollectionSubmission,
        requestDto: SaveFieldCollectionRequestDto,
        baselineOrder: OrderDto?,
        ioError: IOException
    ): OrdenRecoleccion {
        val draft = StoredFieldCollectionDraftDto(
            numeroSolicitud = submission.numeroSolicitud,
            request = requestDto.copy(finalizeCollection = false),
            fullArticulos = submission.articulos.map { it.toDto() },
            pendingSync = true
        )

        if (submission.finalizeCollection || submission.estado == OrdenStatus.NO_RECOLECTADA) {
            if (submission.estado != OrdenStatus.NO_RECOLECTADA) {
                localDataSource.saveDraft(draft)
            }
            throw ioError
        }

        localDataSource.saveDraft(draft)
        val baseOrder = baselineOrder ?: buildFallbackOrderDto(submission).also {
            localDataSource.cacheOrder(it)
        }
        return mergeOrderWithDraft(baseOrder, draft).toDomain()
    }

    private suspend fun ensureNotLockedCached(numeroSolicitud: String) {
        val cached = localDataSource.getCachedOrder(numeroSolicitud) ?: return
        ensureNotLockedDto(cached)
    }

    private fun ensureNotLockedDto(order: OrderDto) {
        if (parseOrdenStatus(order.estado).isReadOnlyLocked) {
            throw IllegalStateException(CLOSED_ORDER_ERROR)
        }
    }

    private fun mergeOrderWithDraft(
        baseOrder: OrderDto,
        draft: StoredFieldCollectionDraftDto
    ): OrderDto {
        val baseByCode = baseOrder.articulos.associateBy { it.codigoArticulo.uppercase() }
        val draftByCode = draft.fullArticulos.associateBy { it.codigoArticulo.uppercase() }
        val submittedQtyByCode = draft.request.articulos.associateBy { it.codigoArticulo.uppercase() }

        val updatedBaseArticulos = baseOrder.articulos.map { item ->
            val key = item.codigoArticulo.uppercase()
            when {
                key in draftByCode -> item.copy(cantidadActa = draftByCode.getValue(key).cantidadActa)
                key in submittedQtyByCode -> item.copy(cantidadActa = submittedQtyByCode.getValue(key).cantidadActa)
                else -> item
            }
        }
        val extraDraftArticulos = draft.fullArticulos.filter {
            it.codigoArticulo.uppercase() !in baseByCode
        }

        return baseOrder.copy(
            estado = draft.request.estado,
            articulos = updatedBaseArticulos + extraDraftArticulos,
            aplicaProvenientes = draft.request.aplicaProvenientes,
            provenientes = if (draft.request.aplicaProvenientes) draft.request.provenientes else emptyList(),
            detalleNovedad = draft.request.detalleNovedad,
            observacionesCampo = draft.request.observacionesCampo,
            nombreResponsableEntrega = draft.request.nombreResponsableEntrega,
            identificacionResponsableEntrega = draft.request.identificacionResponsableEntrega,
            fechaHoraInicioAtencion = baseOrder.fechaHoraInicioAtencion ?: draft.request.fechaHoraInicioAtencion,
            fechaHoraFinalizacion = draft.request.fechaHoraFinalizacion ?: baseOrder.fechaHoraFinalizacion,
            firmaDigitalBase64 = draft.request.firmaDigitalBase64 ?: baseOrder.firmaDigitalBase64
        )
    }

    private fun buildFallbackOrderDto(submission: FieldCollectionSubmission): OrderDto = OrderDto(
        numeroSolicitud = submission.numeroSolicitud,
        identificadorOm = "",
        cedi = "",
        fechaRecoleccion = "",
        nitCliente = "",
        razonSocialCliente = "",
        direccionRecogida = "",
        departamento = "",
        ciudad = "",
        vendedor = "",
        tipoDevolucion = "",
        observacionesIniciales = "",
        estado = submission.estado.name,
        articulos = submission.articulos.map { it.toDto() }
    )

    companion object {
        const val CLOSED_ORDER_ERROR =
            "La orden ya fue cerrada y se encuentra bloqueada en modo solo lectura."
    }
}
