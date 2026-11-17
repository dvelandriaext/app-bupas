package co.com.coexito.bupas.presentation.certificate

import androidx.lifecycle.SavedStateHandle
import co.com.coexito.bupas.data.local.InMemoryFieldCollectionLocalDataSource
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
import co.com.coexito.bupas.domain.usecase.certificate.GetActaRespelUseCase
import co.com.coexito.bupas.domain.usecase.certificate.RetryActaContingencyUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de presentación (UDF / StateFlow) para [ActaRespelViewModel]
 * trazadas estrictamente a HU-007 CA 2 y CA 3.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActaRespelViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeApiService: FakeOrderApiService
    private lateinit var repository: OrderRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApiService = FakeOrderApiService(networkDelayMillis = 0L)
        repository = OrderRepositoryImpl(
            orderApiService = fakeApiService,
            localDataSource = InMemoryFieldCollectionLocalDataSource(),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // HU-007 CA 3: Consulta de trazabilidad completa y habilitación de Ver Acta en estado Generada
    @Test
    fun `CA3 carga trazabilidad completa del acta generada y habilita visualizacion del PDF`() = runTest(testDispatcher) {
        val viewModel = buildViewModel("SOL-2026-00139")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val acta = checkNotNull(state.acta)

        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("INS-0048", acta.numeroActa)
        assertEquals("INS-0048.pdf", acta.nombreArchivoPdf)
        assertEquals("SOL-2026-00139", acta.numeroSolicitud)
        assertEquals("OM-889390", acta.identificadorOm)
        assertEquals("Autocentro del Valle Ltda.", acta.datosGenerador.razonSocial)
        assertEquals(EstadoGeneracionActa.GENERADA, acta.estadoGeneracion)
        assertEquals(EstadoEnvioActa.ENVIADA, acta.notificacionCorreo.estadoEnvio)
        assertEquals(
            listOf("ambiental@autocentrovalle.com.co", "gerencia@autocentrovalle.com.co"),
            acta.notificacionCorreo.correosDestino
        )
        assertTrue(state.canViewPdf)
        assertTrue(state.isPdfDocumentVisible)
        assertFalse(state.requiresContingencyRetry)
    }

    // HU-007 CA 3 y CA 2: Deshabilita Ver Acta cuando hay Error de generación y recupera estado mediante contingencia
    @Test
    fun `CA3 CA2 deshabilita Ver Acta en Error de generacion y recupera documento y envio tras contingencia`() =
        runTest(testDispatcher) {
            fakeApiService.simulateActaContingencyState(
                solicitudId = "SOL-2026-00139",
                estadoGeneracion = EstadoGeneracionActa.ERROR_GENERACION,
                estadoEnvio = EstadoEnvioActa.ERROR_ENVIO,
                detalleError = "Fallo temporal al firmar PDF en servicio documental."
            )

            val viewModel = buildViewModel("SOL-2026-00139")
            advanceUntilIdle()

            val failedState = viewModel.uiState.value
            val failedActa = checkNotNull(failedState.acta)
            assertEquals(EstadoGeneracionActa.ERROR_GENERACION, failedActa.estadoGeneracion)
            assertEquals(EstadoEnvioActa.ERROR_ENVIO, failedActa.notificacionCorreo.estadoEnvio)
            assertFalse(failedState.canViewPdf)
            assertFalse(failedState.isPdfDocumentVisible)
            assertTrue(failedState.requiresContingencyRetry)

            // Reintentar contingencia restaura estado a Generada y Enviada habilitando Ver Acta
            viewModel.onEvent(ActaRespelUiEvent.OnRetryContingencyClick)
            advanceUntilIdle()

            val recoveredState = viewModel.uiState.value
            val recoveredActa = checkNotNull(recoveredState.acta)
            assertEquals(EstadoGeneracionActa.GENERADA, recoveredActa.estadoGeneracion)
            assertEquals(EstadoEnvioActa.ENVIADA, recoveredActa.notificacionCorreo.estadoEnvio)
            assertTrue(recoveredState.canViewPdf)
            assertTrue(recoveredState.isPdfDocumentVisible)
            assertFalse(recoveredState.requiresContingencyRetry)
            assertNotNull(recoveredState.feedbackAlertTitle)
        }

    private fun buildViewModel(numeroSolicitud: String): ActaRespelViewModel =
        ActaRespelViewModel(
            savedStateHandle = SavedStateHandle(mapOf("numeroSolicitud" to numeroSolicitud)),
            getActaRespelUseCase = GetActaRespelUseCase(repository),
            retryActaContingencyUseCase = RetryActaContingencyUseCase(repository)
        )
}
