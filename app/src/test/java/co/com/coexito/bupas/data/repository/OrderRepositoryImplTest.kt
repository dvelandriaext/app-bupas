package co.com.coexito.bupas.data.repository

import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.AssignedOrdersResponseDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de repositorio y servicio remoto simulado alineadas con HU-005 CA 1, CA 2 y CA 3.
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
}
