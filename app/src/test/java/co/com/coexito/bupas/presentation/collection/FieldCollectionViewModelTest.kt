package co.com.coexito.bupas.presentation.collection

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.SavedStateHandle
import co.com.coexito.bupas.core.common.TimeProvider
import co.com.coexito.bupas.core.designsystem.components.decodeSignatureStrokesBase64
import co.com.coexito.bupas.core.designsystem.components.encodeSignatureStrokesBase64
import co.com.coexito.bupas.data.local.InMemoryFieldCollectionLocalDataSource
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.remote.dto.AssignedOrdersResponseDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.PrecioArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente
import co.com.coexito.bupas.domain.repository.OrderRepository
import co.com.coexito.bupas.domain.usecase.CalculateCollectionTotalsUseCase
import co.com.coexito.bupas.domain.usecase.collection.PrepareFieldCollectionOrderUseCase
import co.com.coexito.bupas.domain.usecase.collection.SaveFieldCollectionUseCase
import co.com.coexito.bupas.domain.usecase.collection.ValidateFieldCollectionUseCase
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
 * Suite de pruebas unitarias de presentación (UDF / StateFlow) para FieldCollectionViewModel
 * trazada estrictamente a HU-006 (CA 1 a CA 8 y Casos Borde R2.1 a R2.5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FieldCollectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTimeProvider: MutableFakeTimeProvider
    private lateinit var spyApiService: SpyOfflineOrderApiService
    private lateinit var repository: OrderRepositoryImpl
    private lateinit var viewModel: FieldCollectionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTimeProvider = MutableFakeTimeProvider("2026-11-15 08:10:00")
        spyApiService = SpyOfflineOrderApiService(FakeOrderApiService(networkDelayMillis = 0L))
        repository = OrderRepositoryImpl(
            orderApiService = spyApiService,
            localDataSource = InMemoryFieldCollectionLocalDataSource(),
            ioDispatcher = testDispatcher
        )
        viewModel = buildViewModel("SOL-2026-00148", repository, fakeTimeProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // HU-006 - CA 1, CA 2 y CA 4: Carga inicial expone datos generales, cliente y referencias (incluida cantidad inicial 0) en solo lectura
    @Test
    fun `CA1 CA2 CA4 carga inicial expone datos generales cliente y lista de precios con cantidad inicial cero sin mutarlos`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            val state = viewModel.uiState.value
            val order = checkNotNull(state.order)

            assertEquals("CEDI Cali", order.cedi)
            assertEquals("SOL-2026-00148", order.numeroSolicitud)
            assertEquals("OM-889450", order.identificadorOm)
            assertEquals("Carlos Pérez", order.vendedor)

            assertEquals("Serviteca El Dorado S.A.S.", order.razonSocialCliente)
            assertEquals("890.300.211-4", order.nitCliente)
            assertEquals("Av. 3N # 45-12", order.direccionRecogida)
            assertEquals("Valle del Cauca", order.departamento)
            assertEquals("Cali", order.ciudad)
            assertEquals("POS-Posconsumo", order.tipoDevolucion)
            assertEquals("Recoger en bodega posterior. Portería solicita cédula.", order.observacionesIniciales)

            assertEquals(OrdenStatus.ORDEN_ASIGNADA, state.selectedStatus)
            assertFalse(state.isCollectionSectionEnabled)
            assertFalse(state.isReadOnly)

            assertEquals(4, state.articulos.size)
            assertEquals(
                mapOf("BUP-AUTO-42" to 15, "BUP-MOTO-12" to 8, "BUP-AUTO-34" to 0, "BUP-PESADA-31" to 0),
                state.articulos.associate { it.codigoArticulo to it.cantidadInicial }
            )

            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.RECOLECCION_REALIZADA))
            assertEquals(OrdenStatus.ORDEN_ASIGNADA, viewModel.uiState.value.selectedStatus)

            viewModel.onEvent(FieldCollectionUiEvent.OnObservacionesCampoChanged("Observación en campo del conductor"))
            val updatedOrder = checkNotNull(viewModel.uiState.value.order)
            assertEquals("Recoger en bodega posterior. Portería solicita cédula.", updatedOrder.observacionesIniciales)
            assertEquals("CEDI Cali", updatedOrder.cedi)
        }

    // HU-006 - CA 2, CA 3 y Caso Borde R2.1: Flujo No Recolectada oculta secciones físicas, exige Detalle Novedad y registra cierre
    @Test
    fun `CA2 CA3 R2 seleccionar No Recolectada omite recoleccion exige detalle novedad y cierra con fecha finalizacion en solo lectura`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            fakeTimeProvider.currentTimestamp = "2026-11-15 09:30:00"

            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.NO_RECOLECTADA))
            assertTrue(viewModel.uiState.value.isNoRecolectadaSelected)
            assertFalse(viewModel.uiState.value.isCollectionSectionEnabled)

            viewModel.onEvent(FieldCollectionUiEvent.OnDetalleNovedadChanged("   "))
            viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)

            assertFalse(viewModel.uiState.value.showNoRecolectadaConfirmDialog)
            assertEquals(
                ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR,
                viewModel.uiState.value.validationErrors.detalleNovedadError
            )

            viewModel.onEvent(FieldCollectionUiEvent.OnDetalleNovedadChanged("Bodega cerrada sin personal autorizado"))
            assertNull(viewModel.uiState.value.validationErrors.detalleNovedadError)

            viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
            assertTrue(viewModel.uiState.value.showNoRecolectadaConfirmDialog)

            viewModel.onEvent(FieldCollectionUiEvent.OnConfirmNoRecolectada)
            advanceUntilIdle()

            val closedState = viewModel.uiState.value
            assertEquals(OrdenStatus.NO_RECOLECTADA, closedState.order?.estado)
            assertEquals("2026-11-15 09:30:00", closedState.fechaHoraFinalizacion)
            assertEquals("2026-11-15 09:30:00", closedState.order?.fechaHoraFinalizacion)
            assertNull(closedState.order?.numeroActaGenerada)
            assertTrue(closedState.isReadOnly)
        }

    // HU-006 - CA 3: Cambio a En Atención o Recolección Parcial registra y persiste automáticamente fechaHoraInicioAtencion
    @Test
    fun `CA3 cambiar estado a En Atencion o Recoleccion Parcial registra persiste fechaHoraInicioAtencion y habilita captura`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.fechaHoraInicioAtencion)
            assertFalse(viewModel.uiState.value.isCollectionSectionEnabled)

            fakeTimeProvider.currentTimestamp = "2026-11-15 08:15:00"
            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.EN_ATENCION))
            advanceUntilIdle()

            val inAttentionState = viewModel.uiState.value
            assertEquals(OrdenStatus.EN_ATENCION, inAttentionState.selectedStatus)
            assertTrue(inAttentionState.isCollectionSectionEnabled)
            assertEquals("2026-11-15 08:15:00", inAttentionState.fechaHoraInicioAtencion)
            assertEquals(
                "2026-11-15 08:15:00",
                repository.getOrderBySolicitud("SOL-2026-00148").getOrThrow().fechaHoraInicioAtencion
            )

            fakeTimeProvider.currentTimestamp = "2026-11-15 08:50:00"
            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.RECOLECCION_PARCIAL))
            advanceUntilIdle()

            val partialState = viewModel.uiState.value
            assertEquals(OrdenStatus.RECOLECCION_PARCIAL, partialState.selectedStatus)
            assertTrue(partialState.isCollectionSectionEnabled)
            assertEquals("2026-11-15 08:15:00", partialState.fechaHoraInicioAtencion)
            assertEquals(
                "2026-11-15 08:15:00",
                repository.getOrderBySolicitud("SOL-2026-00148").getOrThrow().fechaHoraInicioAtencion
            )
        }

    // HU-006 - CA 4, CA 5 y Caso Borde R2.3: Cantidad Acta acepta enteros >= 0, rechaza negativos y decimales y recalcula totales
    @Test
    fun `CA4 CA5 R2 cantidad acta acepta enteros mayores o iguales a cero rechaza negativos y decimales y actualiza totales`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.EN_ATENCION))
            advanceUntilIdle()

            viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged("BUP-AUTO-42", "10"))
            assertEquals(10, quantityOf("BUP-AUTO-42"))

            listOf("-5", "3.5", "3,5").forEach { invalidRaw ->
                viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged("BUP-AUTO-42", invalidRaw))
                assertEquals(10, quantityOf("BUP-AUTO-42"))
                assertEquals(
                    ValidateFieldCollectionUseCase.INVALID_CANTIDAD_ACTA_ERROR,
                    viewModel.uiState.value.validationErrors.invalidCantidadActaCodigos["BUP-AUTO-42"]
                )
            }

            viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-MOTO-12", -3))
            assertNull(quantityOf("BUP-MOTO-12"))
            assertNotNull(viewModel.uiState.value.validationErrors.invalidCantidadActaCodigos["BUP-MOTO-12"])

            fillAllArticleQuantities(auto42 = "12", moto12 = "8", auto34 = "3", pesada31 = "0")

            val state = viewModel.uiState.value
            assertTrue(state.validationErrors.invalidCantidadActaCodigos.isEmpty())
            assertFalse(state.totalsSummary.hasMissingCantidadActa)
            assertEquals(23, state.totalsSummary.totalUnidades)
            assertEquals(254.0, state.totalsSummary.totalPesoKg, 0.001)
            assertEquals(990000.0, state.totalsSummary.totalValorCompra, 0.001)
            assertEquals(listOf(15, 8, 0, 0), state.articulos.map { it.cantidadInicial })
        }

    // HU-006 - CA 5 y Caso Borde R2.2: Impide operar Provenientes mostrando validación obligatoria si alguna Cantidad Acta está vacía
    @Test
    fun `CA5 R2 impide activar u operar provenientes mostrando error obligatorio cuando falta diligenciar alguna cantidad acta`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.EN_ATENCION))
            advanceUntilIdle()

            viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-AUTO-42", 10))
            viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-MOTO-12", 5))

            viewModel.onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(true))
            assertFalse(viewModel.uiState.value.aplicaProvenientes)
            assertEquals(
                setOf("BUP-AUTO-34", "BUP-PESADA-31"),
                viewModel.uiState.value.validationErrors.missingCantidadActaCodigos
            )
            assertEquals(
                ValidateFieldCollectionUseCase.PROVENIENTES_GATE_ERROR,
                viewModel.uiState.value.validationErrors.provenientesGateError
            )

            viewModel.onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog)
            assertFalse(viewModel.uiState.value.showAddProvenienteDialog)

            viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-AUTO-34", 0))
            viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-PESADA-31", 2))

            assertTrue(viewModel.uiState.value.validationErrors.missingCantidadActaCodigos.isEmpty())
            assertNull(viewModel.uiState.value.validationErrors.provenientesGateError)

            viewModel.onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(true))
            viewModel.onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog)
            assertTrue(viewModel.uiState.value.aplicaProvenientes)
            assertTrue(viewModel.uiState.value.showAddProvenienteDialog)
        }

    // HU-006 - CA 6: Gestión de múltiples Provenientes (Auto, Moto, Scrap) permitiendo agregar, editar y eliminar antes del cierre
    @Test
    fun `CA6 permite activar provenientes agregar multiples tipos Auto Moto Scrap editar uno existente y eliminar antes del cierre`() =
        runTest(testDispatcher) {
            startAttentionWithQuantities()

            viewModel.onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(true))
            viewModel.onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog)
            viewModel.onEvent(FieldCollectionUiEvent.OnConfirmAddProveniente)

            val invalidForm = viewModel.uiState.value.provenienteForm
            assertTrue(viewModel.uiState.value.showAddProvenienteDialog)
            assertNotNull(invalidForm.nitError)
            assertNotNull(invalidForm.razonSocialError)
            assertNotNull(invalidForm.cantidadError)
            assertNotNull(invalidForm.telefonoError)
            assertNotNull(invalidForm.correoError)

            submitProvenienteForm("900123456-1", "Baterías del Valle", TipoBateriaProveniente.AUTO, "4", "3105551234", "contacto@valle.com")
            viewModel.onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog)
            submitProvenienteForm("805998877-2", "Reciclajes del Sur", TipoBateriaProveniente.SCRAP, "9", "3157778899", "admin@sur.co")

            assertEquals(2, viewModel.uiState.value.provenientes.size)
            val firstId = viewModel.uiState.value.provenientes[0].id
            val secondId = viewModel.uiState.value.provenientes[1].id

            viewModel.onEvent(FieldCollectionUiEvent.OnOpenEditProvenienteDialog(firstId))
            assertTrue(viewModel.uiState.value.provenienteForm.isEditMode)
            assertEquals(firstId, viewModel.uiState.value.provenienteForm.editingId)

            submitProvenienteForm(
                nit = "900123456-9",
                razonSocial = "Baterías del Valle S.A.S.",
                tipo = TipoBateriaProveniente.MOTO,
                cantidad = "7",
                telefono = "3110002233",
                correo = "gerencia@valle.com"
            )

            val afterEdit = viewModel.uiState.value.provenientes
            assertEquals(2, afterEdit.size)
            assertEquals(firstId, afterEdit[0].id)
            assertEquals("Baterías del Valle S.A.S.", afterEdit[0].razonSocial)
            assertEquals(TipoBateriaProveniente.MOTO, afterEdit[0].tipoBateria)
            assertEquals(7, afterEdit[0].cantidadBaterias)

            viewModel.onEvent(FieldCollectionUiEvent.OnRemoveProveniente(secondId))
            assertEquals(listOf(firstId), viewModel.uiState.value.provenientes.map { it.id })

            viewModel.onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(false))
            viewModel.onEvent(FieldCollectionUiEvent.OnSaveDraftClick)
            advanceUntilIdle()
            assertFalse(checkNotNull(viewModel.uiState.value.order).aplicaProvenientes)
            assertTrue(checkNotNull(viewModel.uiState.value.order).provenientes.isEmpty())
        }

    // HU-006 - CA 7: Confirmación exige Responsable, Documento y Firma Digital, persistiendo observaciones finales y firma
    @Test
    fun `CA7 exige responsable documento y firma digital bloqueando finalizacion si faltan y persiste firma con observaciones`() =
        runTest(testDispatcher) {
            startAttentionWithQuantities()

            viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
            val missingErrors = viewModel.uiState.value.validationErrors
            assertFalse(viewModel.uiState.value.showFinalizeConfirmDialog)
            assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, missingErrors.nombreResponsableError)
            assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, missingErrors.identificacionResponsableError)
            assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, missingErrors.firmaDigitalError)

            val encodedFirma = encodeSignatureStrokesBase64(
                strokes = listOf(listOf(Offset(12f, 18f), Offset(84f, 62f))),
                widthPx = 400,
                heightPx = 160
            )
            assertEquals(1, decodeSignatureStrokesBase64(encodedFirma).size)

            viewModel.onEvent(FieldCollectionUiEvent.OnNombreResponsableChanged("María Fernanda Ríos"))
            viewModel.onEvent(FieldCollectionUiEvent.OnIdentificacionResponsableChanged("1098765432"))
            viewModel.onEvent(FieldCollectionUiEvent.OnObservacionesCampoChanged("Entrega completa en estiba plástica"))
            viewModel.onEvent(FieldCollectionUiEvent.OnSignatureCaptured(encodedFirma))
            assertNull(viewModel.uiState.value.validationErrors.firmaDigitalError)

            viewModel.onEvent(FieldCollectionUiEvent.OnSignatureCleared)
            assertNull(viewModel.uiState.value.firmaDigitalBase64)
            viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
            assertEquals(
                ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR,
                viewModel.uiState.value.validationErrors.firmaDigitalError
            )

            viewModel.onEvent(FieldCollectionUiEvent.OnSignatureCaptured(encodedFirma))
            viewModel.onEvent(FieldCollectionUiEvent.OnSaveDraftClick)
            advanceUntilIdle()

            val persisted = checkNotNull(viewModel.uiState.value.order)
            assertEquals("María Fernanda Ríos", persisted.nombreResponsableEntrega)
            assertEquals("1098765432", persisted.identificacionResponsableEntrega)
            assertEquals("Entrega completa en estiba plástica", persisted.observacionesCampo)
            assertEquals(encodedFirma, persisted.firmaDigitalBase64)
        }

    // HU-006 - CA 8: Modal de confirmación de cierre, cancelación con No y transición a Recolección Realizada con Acta al confirmar Sí
    @Test
    fun `CA8 finalizar recoleccion despliega modal de confirmacion permite cancelar con No y al confirmar Si cierra orden y genera acta`() =
        runTest(testDispatcher) {
            startAttentionWithQuantities()
            fakeTimeProvider.currentTimestamp = "2026-11-15 10:40:00"

            viewModel.onEvent(FieldCollectionUiEvent.OnNombreResponsableChanged("María López"))
            viewModel.onEvent(FieldCollectionUiEvent.OnIdentificacionResponsableChanged("1098765432"))
            viewModel.onEvent(FieldCollectionUiEvent.OnSignatureCaptured("RklSTUFfQ0E4X1ZBTElEQQ=="))

            viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
            assertTrue(viewModel.uiState.value.showFinalizeConfirmDialog)

            viewModel.onEvent(FieldCollectionUiEvent.OnDismissFinalizeConfirm)
            assertFalse(viewModel.uiState.value.showFinalizeConfirmDialog)
            assertFalse(viewModel.uiState.value.isReadOnly)
            assertEquals(OrdenStatus.EN_ATENCION, viewModel.uiState.value.order?.estado)

            viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
            assertTrue(viewModel.uiState.value.showFinalizeConfirmDialog)

            viewModel.onEvent(FieldCollectionUiEvent.OnConfirmFinalize)
            advanceUntilIdle()

            val finalState = viewModel.uiState.value
            assertEquals(OrdenStatus.RECOLECCION_REALIZADA, finalState.order?.estado)
            assertEquals(OrdenStatus.RECOLECCION_REALIZADA, finalState.selectedStatus)
            assertEquals("2026-11-15 10:40:00", finalState.fechaHoraFinalizacion)
            assertEquals("POS-0148", finalState.order?.numeroActaGenerada)
            assertTrue(finalState.isReadOnly)
        }

    // HU-006 - CA 8 y Caso Borde R2.4: Bloqueo total en solo lectura ante intentos de edición o reapertura de órdenes cerradas
    @Test
    fun `CA8 R2 impide modificar campos provenientes estado o reabrir orden en estado Recoleccion Realizada o No Recolectada`() =
        runTest(testDispatcher) {
            listOf("SOL-2026-00139", "SOL-2026-00133").forEach { closedId ->
                val closedVm = buildViewModel(closedId, repository, fakeTimeProvider)
                advanceUntilIdle()

                val initialState = closedVm.uiState.value
                assertTrue("La orden $closedId debe iniciar en solo lectura", initialState.isReadOnly)
                val savesBefore = spyApiService.saveCallCount

                closedVm.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.EN_ATENCION))
                closedVm.onEvent(FieldCollectionUiEvent.OnQuantityActaChanged("BUP-AUTO-34", 99))
                closedVm.onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged("BUP-AUTO-34", "77"))
                closedVm.onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(true))
                closedVm.onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog)
                closedVm.onEvent(FieldCollectionUiEvent.OnProvenienteNitChanged("900000000-1"))
                closedVm.onEvent(FieldCollectionUiEvent.OnConfirmAddProveniente)
                closedVm.onEvent(FieldCollectionUiEvent.OnRemoveProveniente("PROV-001"))
                closedVm.onEvent(FieldCollectionUiEvent.OnDetalleNovedadChanged("Intento de cambio"))
                closedVm.onEvent(FieldCollectionUiEvent.OnObservacionesCampoChanged("Intento de cambio"))
                closedVm.onEvent(FieldCollectionUiEvent.OnNombreResponsableChanged("Otro Responsable"))
                closedVm.onEvent(FieldCollectionUiEvent.OnIdentificacionResponsableChanged("99999999"))
                closedVm.onEvent(FieldCollectionUiEvent.OnSignatureCaptured("RklSTUFfTlVFVkE="))
                closedVm.onEvent(FieldCollectionUiEvent.OnSignatureCleared)
                closedVm.onEvent(FieldCollectionUiEvent.OnSaveDraftClick)
                closedVm.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
                closedVm.onEvent(FieldCollectionUiEvent.OnConfirmFinalize)
                closedVm.onEvent(FieldCollectionUiEvent.OnConfirmNoRecolectada)
                advanceUntilIdle()

                assertEquals(initialState, closedVm.uiState.value)
                assertEquals(savesBefore, spyApiService.saveCallCount)
            }
        }

    // HU-006 - Caso Borde R2.5: Recuperación en ViewModel del avance persistido localmente tras recarga sin conexión
    @Test
    fun `R2 recarga de ViewModel en modo offline restaura el avance de recoleccion persistido localmente`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            spyApiService.isOffline = true
            fakeTimeProvider.currentTimestamp = "2026-11-15 09:12:00"

            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.EN_ATENCION))
            advanceUntilIdle()
            fillAllArticleQuantities(auto42 = "12", moto12 = "6", auto34 = "2", pesada31 = "0")

            viewModel.onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(true))
            viewModel.onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog)
            submitProvenienteForm("800444555-6", "Servibaterías Cali", TipoBateriaProveniente.MOTO, "3", "3189990011", "info@servibaterias.co")

            viewModel.onEvent(FieldCollectionUiEvent.OnNombreResponsableChanged("Hernán Darío Gómez"))
            viewModel.onEvent(FieldCollectionUiEvent.OnIdentificacionResponsableChanged("16450987"))
            viewModel.onEvent(FieldCollectionUiEvent.OnObservacionesCampoChanged("Avance guardado sin cobertura móvil"))
            viewModel.onEvent(FieldCollectionUiEvent.OnSignatureCaptured("RklSTUFfT0ZGTElORV9WTQ=="))
            viewModel.onEvent(FieldCollectionUiEvent.OnSaveDraftClick)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isOfflineDraftSaved)

            val reloadedOfflineVm = buildViewModel("SOL-2026-00148", repository, fakeTimeProvider)
            advanceUntilIdle()

            val restored = reloadedOfflineVm.uiState.value
            assertNull(restored.errorMessage)
            assertTrue(restored.isOfflineDraftSaved)
            assertEquals(OrdenStatus.EN_ATENCION, restored.selectedStatus)
            assertEquals("2026-11-15 09:12:00", restored.fechaHoraInicioAtencion)
            assertEquals(20, restored.totalsSummary.totalUnidades)
            assertTrue(restored.aplicaProvenientes)
            assertEquals(1, restored.provenientes.size)
            assertEquals("Servibaterías Cali", restored.provenientes.first().razonSocial)
            assertEquals("Hernán Darío Gómez", restored.nombreResponsableEntrega)
            assertEquals("16450987", restored.identificacionResponsableEntrega)
            assertEquals("Avance guardado sin cobertura móvil", restored.observacionesCampo)
            assertEquals("RklSTUFfT0ZGTElORV9WTQ==", restored.firmaDigitalBase64)
        }

    // HU-006 - Caso Borde 4 (Cierre Offline en Room PENDING_SYNC): Finalizar recolección sin red cierra en solo lectura y encola sincronización
    @Test
    fun `R2 finalizar recoleccion en modo offline cierra la orden en solo lectura bajo estado PENDING_SYNC lista para sincronizar`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            spyApiService.isOffline = true
            fakeTimeProvider.currentTimestamp = "2026-11-15 10:55:00"

            viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.EN_ATENCION))
            advanceUntilIdle()
            fillAllArticleQuantities(auto42 = "15", moto12 = "8", auto34 = "0", pesada31 = "0")
            viewModel.onEvent(FieldCollectionUiEvent.OnNombreResponsableChanged("Roberto Carlos Gómez"))
            viewModel.onEvent(FieldCollectionUiEvent.OnIdentificacionResponsableChanged("79888111"))
            viewModel.onEvent(FieldCollectionUiEvent.OnSignatureCaptured("RklSTUFfRklOQUxJWkFSX09GRkxJTkU="))

            viewModel.onEvent(FieldCollectionUiEvent.OnFinalizeClick)
            assertTrue(viewModel.uiState.value.showFinalizeConfirmDialog)

            viewModel.onEvent(FieldCollectionUiEvent.OnConfirmFinalize)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isReadOnly)
            assertTrue(state.isOfflineDraftSaved)
            assertEquals(OrdenStatus.RECOLECCION_REALIZADA, state.order?.estado)
            assertEquals("2026-11-15 10:55:00", state.fechaHoraFinalizacion)
            assertTrue(state.order?.isPendingSync == true)
            assertEquals("Cierre guardado sin conexión", state.completionAlertTitle)
        }

    private fun buildViewModel(
        numeroSolicitud: String,
        orderRepository: OrderRepository,
        timeProvider: TimeProvider
    ): FieldCollectionViewModel = FieldCollectionViewModel(
        savedStateHandle = SavedStateHandle(mapOf("numeroSolicitud" to numeroSolicitud)),
        prepareFieldCollectionOrderUseCase = PrepareFieldCollectionOrderUseCase(orderRepository),
        calculateCollectionTotalsUseCase = CalculateCollectionTotalsUseCase(),
        validateFieldCollectionUseCase = ValidateFieldCollectionUseCase(),
        saveFieldCollectionUseCase = SaveFieldCollectionUseCase(orderRepository, timeProvider),
        timeProvider = timeProvider
    )

    private fun TestScope.startAttentionWithQuantities() {
        advanceUntilIdle()
        viewModel.onEvent(FieldCollectionUiEvent.OnStatusSelected(OrdenStatus.EN_ATENCION))
        advanceUntilIdle()
        fillAllArticleQuantities(auto42 = "12", moto12 = "8", auto34 = "2", pesada31 = "0")
    }

    private fun fillAllArticleQuantities(auto42: String, moto12: String, auto34: String, pesada31: String) {
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged("BUP-AUTO-42", auto42))
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged("BUP-MOTO-12", moto12))
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged("BUP-AUTO-34", auto34))
        viewModel.onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged("BUP-PESADA-31", pesada31))
    }

    private fun submitProvenienteForm(
        nit: String,
        razonSocial: String,
        tipo: TipoBateriaProveniente,
        cantidad: String,
        telefono: String,
        correo: String
    ) {
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteNitChanged(nit))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteRazonSocialChanged(razonSocial))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteTipoChanged(tipo))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteCantidadChanged(cantidad))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteTelefonoChanged(telefono))
        viewModel.onEvent(FieldCollectionUiEvent.OnProvenienteCorreoChanged(correo))
        viewModel.onEvent(FieldCollectionUiEvent.OnConfirmAddProveniente)
    }

    private fun quantityOf(codigoArticulo: String): Int? =
        viewModel.uiState.value.articulos.first { it.codigoArticulo == codigoArticulo }.cantidadActa
}

private class MutableFakeTimeProvider(
    var currentTimestamp: String
) : TimeProvider {
    override fun nowFormatted(): String = currentTimestamp
}

private class SpyOfflineOrderApiService(
    private val delegate: OrderApiService,
    var isOffline: Boolean = false
) : OrderApiService {

    var saveCallCount: Int = 0
        private set

    override suspend fun getAssignedOrders(
        startDate: String?,
        endDate: String?,
        status: String?,
        clientQuery: String?,
        numeroSolicitud: String?,
        identificadorOm: String?
    ): AssignedOrdersResponseDto {
        if (isOffline) throw IOException("Sin conexión móvil")
        return delegate.getAssignedOrders(startDate, endDate, status, clientQuery, numeroSolicitud, identificadorOm)
    }

    override suspend fun getOrderBySolicitud(solicitudId: String): OrderDto {
        if (isOffline) throw IOException("Sin conexión móvil")
        return delegate.getOrderBySolicitud(solicitudId)
    }

    override suspend fun getClientPriceList(nitCliente: String): List<PrecioArticuloBupasDto> {
        if (isOffline) throw IOException("Sin conexión móvil")
        return delegate.getClientPriceList(nitCliente)
    }

    override suspend fun saveFieldCollection(
        solicitudId: String,
        request: SaveFieldCollectionRequestDto
    ): OrderDto {
        if (isOffline) throw IOException("Sin conexión móvil")
        saveCallCount++
        return delegate.saveFieldCollection(solicitudId, request)
    }
}
