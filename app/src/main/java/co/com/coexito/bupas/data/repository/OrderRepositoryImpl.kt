package co.com.coexito.bupas.data.repository

import co.com.coexito.bupas.core.di.IoDispatcher
import co.com.coexito.bupas.data.local.FieldCollectionLocalDataSource
import co.com.coexito.bupas.data.local.InMemoryFieldCollectionLocalDataSource
import co.com.coexito.bupas.data.local.StoredFieldCollectionDraftDto
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.ArticuloCantidadDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.data.remote.mapper.parseOrdenStatus
import co.com.coexito.bupas.data.remote.mapper.toDomain
import co.com.coexito.bupas.data.remote.mapper.toDto
import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.model.AssignedOrdersResult
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OfflineSyncSummary
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

    private val buildActaRespelUseCase = BuildActaRespelUseCase()

    override suspend fun getAssignedOrders(
        filter: OrderSearchFilter
    ): Result<AssignedOrdersResult> = withContext(ioDispatcher) {
        runCatching {
            try {
                fetchRemoteAndMergeOrders(filter)
            } catch (ioError: IOException) {
                resolveAssignedOrdersFromLocalCache(filter, ioError)
            }
        }
    }

    override suspend fun getOrderBySolicitud(
        numeroSolicitud: String
    ): Result<OrdenRecoleccion> = withContext(ioDispatcher) {
        runCatching {
            val resolved = fetchAndResolveOrder(numeroSolicitud)
            val isPending = localDataSource.getDraft(numeroSolicitud)?.pendingSync == true
            resolved.toDomain(isPendingSync = isPending)
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
                updated.toDomain(isPendingSync = false)
            } catch (_: IOException) {
                handleOfflineSaveFailure(submission, requestDto, baselineOrder)
            }
        }
    }

    override suspend fun hasPendingOfflineDraft(
        numeroSolicitud: String
    ): Boolean = withContext(ioDispatcher) {
        localDataSource.getDraft(numeroSolicitud)?.pendingSync == true
    }

    override suspend fun getPendingSyncCount(): Int = withContext(ioDispatcher) {
        localDataSource.getAllPendingDrafts().size
    }

    override suspend fun syncPendingCollections(): Result<OfflineSyncSummary> =
        withContext(ioDispatcher) {
            runCatching {
                executeOutboxFlush()
            }
        }

    override fun isSimulatedOfflineMode(): Boolean =
        (orderApiService as? FakeOrderApiService)?.isOfflineSimulated ?: false

    override fun setSimulatedOfflineMode(offline: Boolean) {
        (orderApiService as? FakeOrderApiService)?.isOfflineSimulated = offline
    }

    override suspend fun getActaBySolicitud(
        numeroSolicitud: String
    ): Result<ActaRespel> = withContext(ioDispatcher) {
        runCatching {
            val resolved = fetchAndResolveOrder(numeroSolicitud)
            val isPending = localDataSource.getDraft(numeroSolicitud)?.pendingSync == true
            val order = resolved.toDomain(isPendingSync = isPending)
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

    override suspend fun simulateActaContingencyState(
        numeroSolicitud: String,
        estadoGeneracion: EstadoGeneracionActa,
        estadoEnvio: EstadoEnvioActa,
        detalleError: String?
    ): Result<ActaRespel> = withContext(ioDispatcher) {
        runCatching {
            (orderApiService as? FakeOrderApiService)?.simulateActaContingencyState(
                solicitudId = numeroSolicitud,
                estadoGeneracion = estadoGeneracion,
                estadoEnvio = estadoEnvio,
                detalleError = detalleError
            )
            val updatedDto = orderApiService.getOrderBySolicitud(numeroSolicitud)
            localDataSource.cacheOrder(updatedDto)
            buildActaRespelUseCase(updatedDto.toDomain())
                ?: throw IllegalStateException("No se encontró Acta Respel para $numeroSolicitud")
        }
    }

    private suspend fun fetchRemoteAndMergeOrders(
        filter: OrderSearchFilter
    ): AssignedOrdersResult {
        val fullRemoteResponse = orderApiService.getAssignedOrders()
        fullRemoteResponse.orders.forEach { localDataSource.cacheOrder(it) }
        preloadClientPriceLists(fullRemoteResponse.orders)

        val resolvedOrders = fullRemoteResponse.orders.map { remoteDto ->
            val resolvedDto = resolveOrderWithLocalDraft(remoteDto)
            val isPending = localDataSource.getDraft(remoteDto.numeroSolicitud)?.pendingSync == true
            resolvedDto.toDomain(isPendingSync = isPending)
        }
        val filtered = filterAndSortDomainOrders(resolvedOrders, filter)
        return AssignedOrdersResult(
            orders = filtered,
            totalCount = fullRemoteResponse.totalCount
        )
    }

    private suspend fun preloadClientPriceLists(orders: List<OrderDto>) {
        val activeNits = orders
            .filter { !parseOrdenStatus(it.estado).isReadOnlyLocked }
            .map { it.nitCliente.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        for (nit in activeNits) {
            if (localDataSource.getCachedClientPriceList(nit) == null) {
                runCatching {
                    val prices = orderApiService.getClientPriceList(nit)
                    localDataSource.cacheClientPriceList(nit, prices)
                }
            }
        }
    }

    private suspend fun resolveAssignedOrdersFromLocalCache(
        filter: OrderSearchFilter,
        ioError: IOException
    ): AssignedOrdersResult {
        val cachedOrders = localDataSource.getAllCachedOrders()
        if (cachedOrders.isEmpty()) {
            throw ioError
        }
        val resolvedOrders = cachedOrders.map { cachedDto ->
            val draft = localDataSource.getDraft(cachedDto.numeroSolicitud)
            val merged = if (draft != null && draft.pendingSync) {
                mergeOrderWithDraft(cachedDto, draft)
            } else {
                cachedDto
            }
            merged.toDomain(isPendingSync = draft?.pendingSync == true)
        }
        val filtered = filterAndSortDomainOrders(resolvedOrders, filter)
        return AssignedOrdersResult(
            orders = filtered,
            totalCount = cachedOrders.size
        )
    }

    private suspend fun executeOutboxFlush(): OfflineSyncSummary {
        val pendingDrafts = localDataSource.getAllPendingDrafts()
        if (pendingDrafts.isEmpty()) {
            return OfflineSyncSummary()
        }

        var syncedCount = 0
        val generatedActas = mutableListOf<String>()
        var firstIoError: IOException? = null

        for (draft in pendingDrafts) {
            try {
                val remoteCurrent = orderApiService.getOrderBySolicitud(draft.numeroSolicitud)
                if (parseOrdenStatus(remoteCurrent.estado).isReadOnlyLocked) {
                    localDataSource.cacheOrder(remoteCurrent)
                    localDataSource.clearDraft(draft.numeroSolicitud)
                    syncedCount++
                    continue
                }
                val updated = orderApiService.saveFieldCollection(
                    solicitudId = draft.numeroSolicitud,
                    request = draft.request
                )
                localDataSource.cacheOrder(updated)
                localDataSource.clearDraft(draft.numeroSolicitud)
                syncedCount++
                updated.numeroActaGenerada?.let { generatedActas.add(it) }
            } catch (ioError: IOException) {
                if (firstIoError == null) {
                    firstIoError = ioError
                }
            }
        }

        val remainingCount = localDataSource.getAllPendingDrafts().size
        if (syncedCount == 0 && firstIoError != null) {
            throw firstIoError
        }

        return OfflineSyncSummary(
            totalPendingBeforeSync = pendingDrafts.size,
            syncedCount = syncedCount,
            remainingPendingCount = remainingCount,
            generatedActas = generatedActas
        )
    }

    private suspend fun fetchAndResolveOrder(numeroSolicitud: String): OrderDto {
        val remoteOrder = try {
            val fetched = orderApiService.getOrderBySolicitud(numeroSolicitud)
            localDataSource.cacheOrder(fetched)
            fetched
        } catch (ioError: IOException) {
            localDataSource.getCachedOrder(numeroSolicitud) ?: throw ioError
        }
        return resolveOrderWithLocalDraft(remoteOrder)
    }

    private suspend fun resolveOrderWithLocalDraft(remoteOrder: OrderDto): OrderDto {
        if (parseOrdenStatus(remoteOrder.estado).isReadOnlyLocked) {
            localDataSource.clearDraft(remoteOrder.numeroSolicitud)
            return remoteOrder
        }
        val pendingDraft = localDataSource.getDraft(remoteOrder.numeroSolicitud)
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
        baselineOrder: OrderDto?
    ): OrdenRecoleccion {
        val normalizedArticulos = if (submission.estado == OrdenStatus.NO_RECOLECTADA) {
            submission.articulos.map { it.copy(cantidadActa = 0).toDto() }
        } else {
            submission.articulos.map { it.toDto() }
        }
        val draft = StoredFieldCollectionDraftDto(
            numeroSolicitud = submission.numeroSolicitud,
            request = requestDto,
            fullArticulos = normalizedArticulos,
            pendingSync = true
        )
        localDataSource.saveDraft(draft)
        val baseOrder = baselineOrder ?: buildFallbackOrderDto(submission).also {
            localDataSource.cacheOrder(it)
        }
        return mergeOrderWithDraft(baseOrder, draft).toDomain(isPendingSync = true)
    }

    private suspend fun ensureNotLockedCached(numeroSolicitud: String) {
        val cached = localDataSource.getCachedOrder(numeroSolicitud)
        if (cached != null) {
            ensureNotLockedDto(cached)
        }
        val draft = localDataSource.getDraft(numeroSolicitud)
        if (draft != null && draft.pendingSync) {
            val draftEffectiveStatus = parseOrdenStatus(resolveDraftEffectiveStatus(draft.request))
            if (draftEffectiveStatus.isReadOnlyLocked) {
                throw IllegalStateException(CLOSED_ORDER_ERROR)
            }
        }
    }

    private fun ensureNotLockedDto(order: OrderDto) {
        if (parseOrdenStatus(order.estado).isReadOnlyLocked) {
            throw IllegalStateException(CLOSED_ORDER_ERROR)
        }
    }

    private fun resolveDraftEffectiveStatus(request: SaveFieldCollectionRequestDto): String =
        if (request.finalizeCollection && request.estado != OrdenStatus.NO_RECOLECTADA.name) {
            OrdenStatus.RECOLECCION_REALIZADA.name
        } else {
            request.estado
        }

    private fun mergeOrderWithDraft(
        baseOrder: OrderDto,
        draft: StoredFieldCollectionDraftDto
    ): OrderDto {
        val effectiveStatus = resolveDraftEffectiveStatus(draft.request)
        if (effectiveStatus == OrdenStatus.NO_RECOLECTADA.name) {
            return baseOrder.copy(
                estado = OrdenStatus.NO_RECOLECTADA.name,
                articulos = baseOrder.articulos.map { it.copy(cantidadActa = 0) },
                aplicaProvenientes = false,
                provenientes = emptyList(),
                numeroActaGenerada = null,
                detalleNovedad = draft.request.detalleNovedad,
                observacionesCampo = draft.request.observacionesCampo,
                nombreResponsableEntrega = "",
                identificacionResponsableEntrega = "",
                fechaHoraInicioAtencion = baseOrder.fechaHoraInicioAtencion ?: draft.request.fechaHoraInicioAtencion,
                fechaHoraFinalizacion = draft.request.fechaHoraFinalizacion ?: baseOrder.fechaHoraFinalizacion,
                firmaDigitalBase64 = null
            )
        }

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
            estado = effectiveStatus,
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

    private fun filterAndSortDomainOrders(
        orders: List<OrdenRecoleccion>,
        filter: OrderSearchFilter
    ): List<OrdenRecoleccion> = orders
        .filter { order ->
            val afterStart = filter.startDate.isNullOrBlank() || order.fechaRecoleccion >= filter.startDate
            val beforeEnd = filter.endDate.isNullOrBlank() || order.fechaRecoleccion <= filter.endDate
            val statusMatches = filter.status == null || order.estado == filter.status
            val clientMatches = filter.clientQuery.isBlank() ||
                order.razonSocialCliente.contains(filter.clientQuery.trim(), ignoreCase = true) ||
                order.nitCliente.contains(filter.clientQuery.trim(), ignoreCase = true)
            val solicitudMatches = filter.numeroSolicitud.isBlank() ||
                order.numeroSolicitud.contains(filter.numeroSolicitud.trim(), ignoreCase = true)
            val omMatches = filter.identificadorOm.isBlank() ||
                order.identificadorOm.contains(filter.identificadorOm.trim(), ignoreCase = true)
            afterStart && beforeEnd && statusMatches && clientMatches && solicitudMatches && omMatches
        }
        .sortedByDescending { it.fechaRecoleccion }

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
