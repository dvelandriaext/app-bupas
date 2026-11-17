package co.com.coexito.bupas.presentation.orders

import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.usecase.collection.SyncPendingCollectionsUseCase
import co.com.coexito.bupas.domain.usecase.orders.GetAssignedOrdersUseCase
import co.com.coexito.bupas.domain.usecase.orders.ValidateDateRangeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de OrdersInboxViewModel alineadas con HU-005 CA 1, CA 4, CA 5 y sincronización offline/online.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrdersInboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: OrderRepositoryImpl
    private lateinit var viewModel: OrdersInboxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = OrderRepositoryImpl(
            orderApiService = FakeOrderApiService(networkDelayMillis = 0L),
            ioDispatcher = testDispatcher
        )
        viewModel = OrdersInboxViewModel(
            getAssignedOrdersUseCase = GetAssignedOrdersUseCase(repository),
            validateDateRangeUseCase = ValidateDateRangeUseCase(),
            syncPendingCollectionsUseCase = SyncPendingCollectionsUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `CA1 carga automatica inicial trae ordenes ordenadas por fecha descendente`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(6, state.orders.size)
        assertEquals(6, state.totalCount)
        assertEquals("SOL-2026-00148", state.orders.first().numeroSolicitud)
    }

    @Test
    fun `CA1 caso borde rango de fechas mayor a 1 mes muestra error y bloquea aplicacion de filtro`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(OrdersInboxUiEvent.OnOpenFilters)
        viewModel.onEvent(OrdersInboxUiEvent.OnStartDateSelected("2026-10-01"))
        viewModel.onEvent(OrdersInboxUiEvent.OnEndDateSelected("2026-11-15"))
        viewModel.onEvent(OrdersInboxUiEvent.OnApplyFilters)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ValidateDateRangeUseCase.MAX_RANGE_EXCEEDED_MESSAGE, state.dateRangeError)
        assertTrue(state.isFilterSheetVisible)
        assertEquals(6, state.orders.size)
    }

    @Test
    fun `CA1 caso borde filtros sin coincidencias muestran 0 resultados manteniendo totalCount`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(OrdersInboxUiEvent.OnOpenFilters)
        viewModel.onEvent(OrdersInboxUiEvent.OnClientQueryChanged("Cliente Inexistente XYZ"))
        viewModel.onEvent(OrdersInboxUiEvent.OnApplyFilters)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isFilterSheetVisible)
        assertEquals(0, state.orders.size)
        assertEquals(6, state.totalCount)
        assertTrue(state.appliedFilter.hasActiveFilters)
    }

    @Test
    fun `CA4 CA5 navegar a detalle emite efecto y conserva los filtros aplicados en la bandeja`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(OrdersInboxUiEvent.OnOpenFilters)
        viewModel.onEvent(OrdersInboxUiEvent.OnStatusSelected(OrdenStatus.ORDEN_ASIGNADA))
        viewModel.onEvent(OrdersInboxUiEvent.OnApplyFilters)
        advanceUntilIdle()

        var emittedEffect: OrdersInboxNavigationEffect? = null
        val job = launch {
            emittedEffect = viewModel.navigationEffects.first()
        }
        viewModel.onEvent(OrdersInboxUiEvent.OnOrderDetailClick("SOL-2026-00148"))
        advanceUntilIdle()
        job.cancel()

        assertEquals(
            OrdersInboxNavigationEffect.NavigateToOrderDetail("SOL-2026-00148"),
            emittedEffect
        )
        val state = viewModel.uiState.value
        assertEquals(OrdenStatus.ORDEN_ASIGNADA, state.appliedFilter.status)
        assertEquals(1, state.orders.size)
    }

    @Test
    fun `Sync bandeja en modo offline muestra ordenes cacheadas con conteo PENDING_SYNC y sincroniza automaticamente al reconectar`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            // Conmutar a modo Offline desde la bandeja
            viewModel.onEvent(OrdersInboxUiEvent.OnToggleOfflineMode(offline = true))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isOfflineMode)
            assertEquals(6, viewModel.uiState.value.orders.size)

            // El conductor cierra SOL-2026-00148 sin conexión
            val order148 = repository.getOrderBySolicitud("SOL-2026-00148").getOrThrow()
            repository.saveFieldCollection(
                FieldCollectionSubmission(
                    numeroSolicitud = "SOL-2026-00148",
                    estado = OrdenStatus.EN_ATENCION,
                    articulos = order148.articulos.map { it.copy(cantidadActa = 10) },
                    aplicaProvenientes = false,
                    provenientes = emptyList(),
                    detalleNovedad = null,
                    observacionesCampo = "Cierre en campo sin red",
                    nombreResponsableEntrega = "Mario Montoya",
                    identificacionResponsableEntrega = "10203040",
                    finalizeCollection = true,
                    fechaHoraInicioAtencion = "2026-11-15 08:30:00",
                    fechaHoraFinalizacion = "2026-11-15 09:00:00",
                    firmaDigitalBase64 = "RklSTUFfSU5CT1hfT0ZGTElORQ=="
                )
            ).getOrThrow()

            // Al volver a la bandeja sin red, refleja el estado cerrado local y pendingSyncCount = 1
            viewModel.onEvent(OrdersInboxUiEvent.OnRefreshOrders)
            advanceUntilIdle()
            val offlineInboxState = viewModel.uiState.value
            assertEquals(1, offlineInboxState.pendingSyncCount)
            val cardOffline = offlineInboxState.orders.first { it.numeroSolicitud == "SOL-2026-00148" }
            assertEquals(OrdenStatus.RECOLECCION_REALIZADA, cardOffline.estado)
            assertTrue(cardOffline.isPendingSync)

            // Al reconectar (OnToggleOfflineMode(false)), sincroniza automáticamente hacia el backend y genera Acta POS-0148
            viewModel.onEvent(OrdersInboxUiEvent.OnToggleOfflineMode(offline = false))
            advanceUntilIdle()

            val onlineSyncedState = viewModel.uiState.value
            assertFalse(onlineSyncedState.isOfflineMode)
            assertEquals(0, onlineSyncedState.pendingSyncCount)
            assertNotNull(onlineSyncedState.syncFeedbackMessage)
            assertTrue(onlineSyncedState.syncFeedbackMessage!!.contains("POS-0148"))

            val cardSynced = onlineSyncedState.orders.first { it.numeroSolicitud == "SOL-2026-00148" }
            assertFalse(cardSynced.isPendingSync)
            assertEquals("POS-0148", cardSynced.numeroActaGenerada)
        }
}
