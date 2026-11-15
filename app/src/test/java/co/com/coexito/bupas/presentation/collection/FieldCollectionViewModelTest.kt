package co.com.coexito.bupas.presentation.collection

import androidx.lifecycle.SavedStateHandle
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente
import co.com.coexito.bupas.domain.usecase.CalculateCollectionTotalsUseCase
import co.com.coexito.bupas.domain.usecase.collection.SaveFieldCollectionUseCase
import co.com.coexito.bupas.domain.usecase.collection.ValidateFieldCollectionUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class FieldCollectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FieldCollectionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repository = OrderRepositoryImpl(
            orderApiService = FakeOrderApiService(networkDelayMillis = 0L),
            ioDispatcher = testDispatcher
        )
        viewModel = FieldCollectionViewModel(
            savedStateHandle = SavedStateHandle(mapOf("numeroSolicitud" to "SOL-2026-00148")),
            getOrderBySolicitudUseCase = GetOrderBySolicitudUseCase(repository),
            calculateCollectionTotalsUseCase = CalculateCollectionTotalsUseCase(),
            validateFieldCollectionUseCase = ValidateFieldCollectionUseCase(),
            saveFieldCollectionUseCase = SaveFieldCollectionUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ajuste de cantidad acta recalcula totales dinamicamente`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(OrdenStatus.EN_ATENCION, viewModel.uiState.value.selectedStatus)

        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-AUTO-42", 10))
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-MOTO-12", 5))

        val totals = viewModel.uiState.value.totalsSummary
        assertEquals(15, totals.totalUnidades)
        assertEquals(165.0, totals.totalPesoKg, 0.01)
        assertEquals(640000.0, totals.totalValorCompra, 0.01)
        assertFalse(totals.hasMissingCantidadActa)
    }

    @Test
    fun `agregar proveniente valido lo incluye en la lista`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(true))
        viewModel.onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog)
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteNitChanged("900123456-1"))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteRazonSocialChanged("Baterías del Valle"))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteTipoChanged(TipoBateriaProveniente.AUTO))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteCantidadChanged("4"))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteTelefonoChanged("3105551234"))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteCorreoChanged("contacto@valle.com"))
        viewModel.onEvent(FieldCollectionUiEvent.OnConfirmAddProveniente)

        val state = viewModel.uiState.value
        assertFalse(state.showAddProvenienteDialog)
        assertEquals(1, state.provenientes.size)
        assertEquals("Baterías del Valle", state.provenientes.first().razonSocial)
    }

    @Test
    fun `finalizar recoleccion con cantidades y responsable bloquea en solo lectura y genera acta`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-AUTO-42", 12))
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-MOTO-12", 0))
        viewModel.onEvent(FieldCollectionUiEvent.OnNombreResponsableChanged("María López"))
        viewModel.onEvent(FieldCollectionUiEvent.OnIdentificacionResponsableChanged("1098765432"))

        viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
        assertTrue(viewModel.uiState.value.showFinalizeConfirmDialog)

        viewModel.onEvent(FieldCollectionUiEvent.OnConfirmFinalize)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OrdenStatus.RECOLECCION_REALIZADA, state.order?.estado)
        assertTrue(state.isReadOnly)
        assertNotNull(state.order?.numeroActaGenerada)
        assertTrue(state.order?.numeroActaGenerada!!.startsWith("POS-"))

        // Verificar bloqueo de solo lectura: cambios posteriores no alteran cantidades
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-AUTO-42", 99))
        assertEquals(12, viewModel.uiState.value.articulos.first { it.codigoArticulo == "BUP-AUTO-42" }.cantidadActa)
    }

    @Test
    fun `registrar no recolectada exige detalle novedad y bloquea orden en solo lectura`() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.NO_RECOLECTADA))
        viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)

        assertFalse(viewModel.uiState.value.showNoRecolectadaConfirmDialog)
        assertNotNull(viewModel.uiState.value.validationErrors.detalleNovedadError)

        viewModel.onEvent(FieldCollectionUiEvent.OnDetalleNovedadChanged("Bodega cerrada sin personal"))
        viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
        assertTrue(viewModel.uiState.value.showNoRecolectadaConfirmDialog)

        viewModel.onEvent(FieldCollectionUiEvent.OnConfirmNoRecolectada)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OrdenStatus.NO_RECOLECTADA, state.order?.estado)
        assertTrue(state.isReadOnly)
    }
}
