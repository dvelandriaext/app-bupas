package co.com.coexito.bupas.presentation.orders

import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.model.OrdenStatus
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de OrdersInboxViewModel alineadas con HU-005 CA 1, CA 4, CA 5 y casos borde.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrdersInboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OrdersInboxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repository = OrderRepositoryImpl(
            orderApiService = FakeOrderApiService(networkDelayMillis = 0L),
            ioDispatcher = testDispatcher
        )
        viewModel = OrdersInboxViewModel(
            getAssignedOrdersUseCase = GetAssignedOrdersUseCase(repository),
            validateDateRangeUseCase = ValidateDateRangeUseCase()
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
}
