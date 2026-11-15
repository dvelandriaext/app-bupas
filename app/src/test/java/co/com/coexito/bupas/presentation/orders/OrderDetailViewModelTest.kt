package co.com.coexito.bupas.presentation.orders

import androidx.lifecycle.SavedStateHandle
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.usecase.orders.GetOrderBySolicitudUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de OrderDetailViewModel alineadas con HU-005 CA 3 y casos borde.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: GetOrderBySolicitudUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repository = OrderRepositoryImpl(
            orderApiService = FakeOrderApiService(networkDelayMillis = 0L),
            ioDispatcher = testDispatcher
        )
        useCase = GetOrderBySolicitudUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `CA3 orden en estado Orden Asignada habilita Iniciar atencion`() = runTest(testDispatcher) {
        val viewModel = OrderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("numeroSolicitud" to "SOL-2026-00148")),
            getOrderBySolicitudUseCase = useCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(OrdenStatus.ORDEN_ASIGNADA, state.order?.estado)
        assertTrue(state.canStartAttention)
    }

    @Test
    fun `CA3 caso borde orden en Recoleccion Realizada o No Recolectada inhabilita Iniciar atencion`() = runTest(testDispatcher) {
        val realizedViewModel = OrderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("numeroSolicitud" to "SOL-2026-00139")),
            getOrderBySolicitudUseCase = useCase
        )
        advanceUntilIdle()
        assertEquals(OrdenStatus.RECOLECCION_REALIZADA, realizedViewModel.uiState.value.order?.estado)
        assertFalse(realizedViewModel.uiState.value.canStartAttention)

        val notCollectedViewModel = OrderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("numeroSolicitud" to "SOL-2026-00133")),
            getOrderBySolicitudUseCase = useCase
        )
        advanceUntilIdle()
        assertEquals(OrdenStatus.NO_RECOLECTADA, notCollectedViewModel.uiState.value.order?.estado)
        assertFalse(notCollectedViewModel.uiState.value.canStartAttention)
    }

    @Test
    fun `caso borde orden no encontrada expone mensaje de error`() = runTest(testDispatcher) {
        val viewModel = OrderDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("numeroSolicitud" to "SOL-INEXISTENTE")),
            getOrderBySolicitudUseCase = useCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }
}
