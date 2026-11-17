package co.com.coexito.bupas.data.repository

import co.com.coexito.bupas.core.common.TimeProvider
import co.com.coexito.bupas.data.local.InMemoryFieldCollectionLocalDataSource
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.AssignedOrdersResponseDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.PrecioArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente
import co.com.coexito.bupas.domain.usecase.collection.PrepareFieldCollectionOrderUseCase
import co.com.coexito.bupas.domain.usecase.collection.SaveFieldCollectionUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de repositorio y casos de uso de persistencia alineadas con:
 * - HU-005 (CA 1, CA 2 y CA 3)
 * - HU-006 (CA 3, CA 4, CA 7, CA 8 y Casos Borde R2.4, R2.5)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeApiService = FakeOrderApiService(networkDelayMillis = 0L)
    private val repository = OrderRepositoryImpl(
        orderApiService = fakeApiService,
        ioDispatcher = testDispatcher
    )

    @Test
    fun `CA1 carga inicial retorna ordenes ordenadas por fecha de recoleccion descendente`() = runTest(testDispatcher) {
        val result = repository.getAssignedOrders(OrderSearchFilter()).getOrThrow()

        assertEquals(6, result.totalCount)
        assertEquals(6, result.orders.size)
        val dates = result.orders.map { it.fechaRecoleccion }
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun `CA1 CA2 filtrado por rango de fechas estado cliente solicitud y OM retorna coincidencias exactas`() = runTest(testDispatcher) {
        val byDate = repository.getAssignedOrders(
            OrderSearchFilter(startDate = "2026-11-10", endDate = "2026-11-15")
        ).getOrThrow()
        assertEquals(3, byDate.orders.size)

        val byStatus = repository.getAssignedOrders(
            OrderSearchFilter(status = OrdenStatus.RECOLECCION_REALIZADA)
        ).getOrThrow()
        assertEquals(2, byStatus.orders.size)
        assertTrue(byStatus.orders.all { it.estado == OrdenStatus.RECOLECCION_REALIZADA })

        val byNit = repository.getAssignedOrders(
            OrderSearchFilter(clientQuery = "890.300.211-4")
        ).getOrThrow()
        assertEquals(1, byNit.orders.size)
        assertEquals("Serviteca El Dorado S.A.S.", byNit.orders.first().razonSocialCliente)

        val bySolicitudAndOm = repository.getAssignedOrders(
            OrderSearchFilter(numeroSolicitud = "SOL-2026-00145", identificadorOm = "OM-889431")
        ).getOrThrow()
        assertEquals(1, bySolicitudAndOm.orders.size)
    }

    @Test
    fun `CA3 consulta de detalle por numeroSolicitud retorna orden completa`() = runTest(testDispatcher) {
        val order = repository.getOrderBySolicitud("SOL-2026-00148").getOrThrow()

        assertEquals("OM-889450", order.identificadorOm)
        assertEquals(OrdenStatus.ORDEN_ASIGNADA, order.estado)
        assertEquals(2, order.articulos.size)
    }

    @Test
    fun `caso borde error de red simulado propaga Result failure`() = runTest(testDispatcher) {
        val failingApi = object : OrderApiService {
            override suspend fun getAssignedOrders(
                startDate: String?,
                endDate: String?,
                status: String?,
                clientQuery: String?,
                numeroSolicitud: String?,
                identificadorOm: String?
            ): AssignedOrdersResponseDto = throw IllegalStateException("Sin conexión al servidor")

            override suspend fun getOrderBySolicitud(solicitudId: String): OrderDto =
                throw IllegalStateException("Sin conexión al servidor")

            override suspend fun saveFieldCollection(
                solicitudId: String,
                request: SaveFieldCollectionRequestDto
            ): OrderDto = throw IllegalStateException("Sin conexión al servidor")
        }
        val failingRepo = OrderRepositoryImpl(failingApi, testDispatcher)

        val result = failingRepo.getAssignedOrders()
        assertTrue(result.isFailure)
    }

    // HU-006 - CA 3, CA 4 y CA 7: Carga de catálogo con referencia inicial 0 y persistencia de timestamps, entrega y firma
    @Test
    fun `HU006 CA3 CA4 CA7 persiste inicio de atencion referencias con cantidad inicial cero datos de entrega y firma`() =
        runTest(testDispatcher) {
            val isolatedRepo = OrderRepositoryImpl(FakeOrderApiService(0L), testDispatcher)
            val prepareUseCase = PrepareFieldCollectionOrderUseCase(isolatedRepo)
            val saveUseCase = SaveFieldCollectionUseCase(
                orderRepository = isolatedRepo,
                timeProvider = FixedTimeProvider("2026-11-15 08:20:00")
            )

            val prepared = prepareUseCase("SOL-2026-00148").getOrThrow()
            assertEquals(4, prepared.articulos.size)
            val zeroInitialCodigos = prepared.articulos.filter { it.cantidadInicial == 0 }.map { it.codigoArticulo }.toSet()
            assertEquals(setOf("BUP-AUTO-34", "BUP-PESADA-31"), zeroInitialCodigos)
            assertTrue(prepared.articulos.all { it.cantidadActa == null })

            val updatedArticulos = prepared.articulos.map { item ->
                when (item.codigoArticulo) {
                    "BUP-AUTO-42" -> item.copy(cantidadActa = 12)
                    "BUP-MOTO-12" -> item.copy(cantidadActa = 8)
                    "BUP-AUTO-34" -> item.copy(cantidadActa = 4)
                    else -> item.copy(cantidadActa = 0)
                }
            }

            saveUseCase(
                FieldCollectionSubmission(
                    numeroSolicitud = "SOL-2026-00148",
                    estado = OrdenStatus.EN_ATENCION,
                    articulos = updatedArticulos,
                    aplicaProvenientes = false,
                    provenientes = emptyList(),
                    detalleNovedad = null,
                    observacionesCampo = "Entrega verificada en muelle secundario",
                    nombreResponsableEntrega = "Jorge Eliécer Ramírez",
                    identificacionResponsableEntrega = "79456123",
                    finalizeCollection = false,
                    firmaDigitalBase64 = "RklSTUFfQ0E3X1Rlc3Q="
                )
            ).getOrThrow()

            val reloaded = isolatedRepo.getOrderBySolicitud("SOL-2026-00148").getOrThrow()
            assertEquals(OrdenStatus.EN_ATENCION, reloaded.estado)
            assertEquals("2026-11-15 08:20:00", reloaded.fechaHoraInicioAtencion)
            assertNull(reloaded.fechaHoraFinalizacion)
            assertEquals("Entrega verificada en muelle secundario", reloaded.observacionesCampo)
            assertEquals("Jorge Eliécer Ramírez", reloaded.nombreResponsableEntrega)
            assertEquals("79456123", reloaded.identificacionResponsableEntrega)
            assertEquals("RklSTUFfQ0E3X1Rlc3Q=", reloaded.firmaDigitalBase64)

            val persistedPriceListItem = reloaded.articulos.first { it.codigoArticulo == "BUP-AUTO-34" }
            assertEquals(0, persistedPriceListItem.cantidadInicial)
            assertEquals(4, persistedPriceListItem.cantidadActa)
        }

    // HU-006 - CA 3 y CA 8: Finalizar genera Acta POS-XXXX o INS-XXXX cuando cantidad > 0, omite Acta si cantidad == 0 y registra fecha fin
    @Test
    fun `HU006 CA3 CA8 finalizar con unidades mayores a cero cambia a Recoleccion Realizada registra fecha fin y genera acta POS o INS`() =
        runTest(testDispatcher) {
            val isolatedRepo = OrderRepositoryImpl(FakeOrderApiService(0L), testDispatcher)
            val prepareUseCase = PrepareFieldCollectionOrderUseCase(isolatedRepo)
            val saveUseCase = SaveFieldCollectionUseCase(
                orderRepository = isolatedRepo,
                timeProvider = FixedTimeProvider("2026-11-15 11:45:00")
            )

            val posOrder = prepareUseCase("SOL-2026-00148").getOrThrow()
            val finalizedPos = saveUseCase(
                FieldCollectionSubmission(
                    numeroSolicitud = "SOL-2026-00148",
                    estado = OrdenStatus.EN_ATENCION,
                    articulos = posOrder.articulos.map { it.copy(cantidadActa = if (it.codigoArticulo == "BUP-AUTO-42") 10 else 0) },
                    aplicaProvenientes = false,
                    provenientes = emptyList(),
                    detalleNovedad = null,
                    observacionesCampo = "Cierre POS",
                    nombreResponsableEntrega = "Ana Milena Soto",
                    identificacionResponsableEntrega = "66891230",
                    finalizeCollection = true,
                    firmaDigitalBase64 = "RklSTUFfUE9T"
                )
            ).getOrThrow()

            assertEquals(OrdenStatus.RECOLECCION_REALIZADA, finalizedPos.estado)
            assertEquals("POS-0148", finalizedPos.numeroActaGenerada)
            assertEquals("2026-11-15 11:45:00", finalizedPos.fechaHoraInicioAtencion)
            assertEquals("2026-11-15 11:45:00", finalizedPos.fechaHoraFinalizacion)

            val insOrder = prepareUseCase("SOL-2026-00142").getOrThrow()
            val finalizedIns = saveUseCase(
                FieldCollectionSubmission(
                    numeroSolicitud = "SOL-2026-00142",
                    estado = OrdenStatus.RECOLECCION_PARCIAL,
                    articulos = insOrder.articulos.map { it.copy(cantidadActa = if (it.codigoArticulo == "BUP-AUTO-34") 12 else 0) },
                    aplicaProvenientes = false,
                    provenientes = emptyList(),
                    detalleNovedad = null,
                    observacionesCampo = "Cierre INS",
                    nombreResponsableEntrega = "Pedro Nel Ospina",
                    identificacionResponsableEntrega = "16784321",
                    finalizeCollection = true,
                    firmaDigitalBase64 = "RklSTUFfSU5T"
                )
            ).getOrThrow()

            assertEquals(OrdenStatus.RECOLECCION_REALIZADA, finalizedIns.estado)
            assertEquals("INS-0142", finalizedIns.numeroActaGenerada)
            assertEquals("2026-11-11 10:15:00", finalizedIns.fechaHoraInicioAtencion)
            assertEquals("2026-11-15 11:45:00", finalizedIns.fechaHoraFinalizacion)

            val zeroOrder = prepareUseCase("SOL-2026-00145").getOrThrow()
            val finalizedZero = saveUseCase(
                FieldCollectionSubmission(
                    numeroSolicitud = "SOL-2026-00145",
                    estado = OrdenStatus.RECOLECCION_PARCIAL,
                    articulos = zeroOrder.articulos.map { it.copy(cantidadActa = 0) },
                    aplicaProvenientes = false,
                    provenientes = emptyList(),
                    detalleNovedad = null,
                    observacionesCampo = "Sin unidades físicas aptas",
                    nombreResponsableEntrega = "Luis Fernando Paz",
                    identificacionResponsableEntrega = "94321678",
                    finalizeCollection = true,
                    firmaDigitalBase64 = "RklSTUFfWkVSTw=="
                )
            ).getOrThrow()

            assertEquals(OrdenStatus.RECOLECCION_REALIZADA, finalizedZero.estado)
            assertNull(finalizedZero.numeroActaGenerada)
            assertEquals("2026-11-15 11:45:00", finalizedZero.fechaHoraFinalizacion)
        }

    // HU-006 - CA 8 y Caso Borde R2.4: Repositorio y SaveFieldCollectionUseCase rechazan modificar o reabrir órdenes cerradas
    @Test
    fun `HU006 CA8 R2 rechaza actualizar o reabrir orden ya cerrada en Recoleccion Realizada o No Recolectada`() =
        runTest(testDispatcher) {
            val isolatedRepo = OrderRepositoryImpl(FakeOrderApiService(0L), testDispatcher)
            val saveUseCase = SaveFieldCollectionUseCase(isolatedRepo)

            listOf("SOL-2026-00139", "SOL-2026-00133").forEach { closedSolicitud ->
                val snapshotBefore = isolatedRepo.getOrderBySolicitud(closedSolicitud).getOrThrow()

                val reopenAttempt = FieldCollectionSubmission(
                    numeroSolicitud = closedSolicitud,
                    estado = OrdenStatus.EN_ATENCION,
                    articulos = snapshotBefore.articulos.map { it.copy(cantidadActa = 99) },
                    aplicaProvenientes = false,
                    provenientes = emptyList(),
                    detalleNovedad = null,
                    observacionesCampo = "Intento indebido de reapertura",
                    nombreResponsableEntrega = "Intruso",
                    identificacionResponsableEntrega = "000000",
                    finalizeCollection = false
                )

                val useCaseResult = saveUseCase(reopenAttempt)
                val repoSaveResult = isolatedRepo.saveFieldCollection(reopenAttempt)
                val repoStatusResult = isolatedRepo.updateOrderStatus(closedSolicitud, OrdenStatus.EN_ATENCION, "Reabrir")

                assertTrue(useCaseResult.isFailure)
                assertTrue(useCaseResult.exceptionOrNull() is IllegalStateException)
                assertTrue(repoSaveResult.isFailure)
                assertTrue(repoStatusResult.isFailure)

                val snapshotAfter = isolatedRepo.getOrderBySolicitud(closedSolicitud).getOrThrow()
                assertEquals(snapshotBefore, snapshotAfter)
            }
        }

    // HU-006 - Caso Borde R2.5: Persistencia local conserva el registro en sitio ante pérdida de conectividad
    @Test
    fun `HU006 R2 conserva borrador y avance local cuando falla conectividad remota y lo recupera al recargar`() =
        runTest(testDispatcher) {
            val toggleableApi = ToggleableOfflineApiService(FakeOrderApiService(networkDelayMillis = 0L))
            val localDataSource = InMemoryFieldCollectionLocalDataSource()
            val offlineCapableRepo = OrderRepositoryImpl(
                orderApiService = toggleableApi,
                localDataSource = localDataSource,
                ioDispatcher = testDispatcher
            )
            val prepareUseCase = PrepareFieldCollectionOrderUseCase(offlineCapableRepo)
            val saveUseCase = SaveFieldCollectionUseCase(
                orderRepository = offlineCapableRepo,
                timeProvider = FixedTimeProvider("2026-11-15 09:05:00")
            )

            val initialLoaded = prepareUseCase("SOL-2026-00148").getOrThrow()
            assertFalse(offlineCapableRepo.hasPendingOfflineDraft("SOL-2026-00148"))

            toggleableApi.isOffline = true

            val provenienteOffline = TerceroProveniente(
                id = "PROV-OFF-01",
                nit = "901.222.333-4",
                razonSocial = "Baterías del Pacífico S.A.S.",
                tipoBateria = TipoBateriaProveniente.SCRAP,
                cantidadBaterias = 5,
                telefono = "3167890123",
                correoElectronico = "logistica@pacifico.co"
            )
            val offlineArticulos = initialLoaded.articulos.map { articulo ->
                when (articulo.codigoArticulo) {
                    "BUP-AUTO-42" -> articulo.copy(cantidadActa = 14)
                    "BUP-AUTO-34" -> articulo.copy(cantidadActa = 3)
                    else -> articulo.copy(cantidadActa = 0)
                }
            }

            val savedOffline = saveUseCase(
                FieldCollectionSubmission(
                    numeroSolicitud = "SOL-2026-00148",
                    estado = OrdenStatus.EN_ATENCION,
                    articulos = offlineArticulos,
                    aplicaProvenientes = true,
                    provenientes = listOf(provenienteOffline),
                    detalleNovedad = null,
                    observacionesCampo = "Captura sin red móvil en sótano",
                    nombreResponsableEntrega = "Camilo Andrés Vélez",
                    identificacionResponsableEntrega = "1144098765",
                    finalizeCollection = false,
                    firmaDigitalBase64 = "RklSTUFfT0ZGTElORQ=="
                )
            ).getOrThrow()

            assertTrue(offlineCapableRepo.hasPendingOfflineDraft("SOL-2026-00148"))
            assertEquals(OrdenStatus.EN_ATENCION, savedOffline.estado)

            val recoveredWhileOffline = prepareUseCase("SOL-2026-00148").getOrThrow()
            assertEquals(OrdenStatus.EN_ATENCION, recoveredWhileOffline.estado)
            assertEquals("2026-11-15 09:05:00", recoveredWhileOffline.fechaHoraInicioAtencion)
            assertEquals(17, recoveredWhileOffline.totalUnidadesActa)
            assertTrue(recoveredWhileOffline.aplicaProvenientes)
            assertEquals(listOf(provenienteOffline), recoveredWhileOffline.provenientes)
            assertEquals("Captura sin red móvil en sótano", recoveredWhileOffline.observacionesCampo)
            assertEquals("Camilo Andrés Vélez", recoveredWhileOffline.nombreResponsableEntrega)
            assertEquals("1144098765", recoveredWhileOffline.identificacionResponsableEntrega)
            assertEquals("RklSTUFfT0ZGTElORQ==", recoveredWhileOffline.firmaDigitalBase64)
        }
}

private class FixedTimeProvider(
    private val fixedTimestamp: String
) : TimeProvider {
    override fun nowFormatted(): String = fixedTimestamp
}

private class ToggleableOfflineApiService(
    private val delegate: OrderApiService,
    var isOffline: Boolean = false
) : OrderApiService {

    override suspend fun getAssignedOrders(
        startDate: String?,
        endDate: String?,
        status: String?,
        clientQuery: String?,
        numeroSolicitud: String?,
        identificadorOm: String?
    ): AssignedOrdersResponseDto {
        if (isOffline) throw IOException("Sin conexión de red en campo")
        return delegate.getAssignedOrders(startDate, endDate, status, clientQuery, numeroSolicitud, identificadorOm)
    }

    override suspend fun getOrderBySolicitud(solicitudId: String): OrderDto {
        if (isOffline) throw IOException("Sin conexión de red en campo")
        return delegate.getOrderBySolicitud(solicitudId)
    }

    override suspend fun getClientPriceList(nitCliente: String): List<PrecioArticuloBupasDto> {
        if (isOffline) throw IOException("Sin conexión de red en campo")
        return delegate.getClientPriceList(nitCliente)
    }

    override suspend fun saveFieldCollection(
        solicitudId: String,
        request: SaveFieldCollectionRequestDto
    ): OrderDto {
        if (isOffline) throw IOException("Sin conexión de red en campo")
        return delegate.saveFieldCollection(solicitudId, request)
    }
}
