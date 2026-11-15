package co.com.coexito.bupas.presentation.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.usecase.CalculateCollectionTotalsUseCase
import co.com.coexito.bupas.domain.usecase.collection.SaveFieldCollectionUseCase
import co.com.coexito.bupas.domain.usecase.collection.ValidateFieldCollectionUseCase
import co.com.coexito.bupas.domain.usecase.orders.GetOrderBySolicitudUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FieldCollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOrderBySolicitudUseCase: GetOrderBySolicitudUseCase,
    private val calculateCollectionTotalsUseCase: CalculateCollectionTotalsUseCase,
    private val validateFieldCollectionUseCase: ValidateFieldCollectionUseCase,
    private val saveFieldCollectionUseCase: SaveFieldCollectionUseCase
) : ViewModel() {

    private val numeroSolicitud: String = checkNotNull(savedStateHandle["numeroSolicitud"])

    private val _uiState = MutableStateFlow(FieldCollectionUiState())
    val uiState: StateFlow<FieldCollectionUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun onEvent(event: FieldCollectionUiEvent) {
        when (event) {
            FieldCollectionUiEvent.OnRetry -> loadOrder()
            is FieldCollectionUiEvent.OnStatusSelected -> handleStatusSelected(event.status)
            is FieldCollectionUiEvent.OnQuantityActaChanged -> handleQuantityChanged(event.codigoArticulo, event.quantity)
            is FieldCollectionUiEvent.OnAplicaProvenientesChanged -> updateIfEditable {
                it.copy(aplicaProvenientes = event.applies)
            }
            FieldCollectionUiEvent.OnOpenAddProvenienteDialog -> updateIfEditable {
                it.copy(showAddProvenienteDialog = true, provenienteForm = TerceroProvenienteFormState())
            }
            FieldCollectionUiEvent.OnDismissAddProvenienteDialog -> _uiState.update {
                it.copy(showAddProvenienteDialog = false)
            }
            is FieldCollectionUiEvent.OnProvenienteNitChanged -> updateForm {
                it.copy(nit = event.value, nitError = null)
            }
            is FieldCollectionUiEvent.OnProvenienteRazonSocialChanged -> updateForm {
                it.copy(razonSocial = event.value, razonSocialError = null)
            }
            is FieldCollectionUiEvent.OnProvenienteTipoChanged -> updateForm {
                it.copy(tipoBateria = event.value)
            }
            is FieldCollectionUiEvent.OnProvenienteCantidadChanged -> updateForm {
                it.copy(cantidadBaterias = event.value.filter { ch -> ch.isDigit() }, cantidadError = null)
            }
            is FieldCollectionUiEvent.OnProvenienteTelefonoChanged -> updateForm {
                it.copy(telefono = event.value, telefonoError = null)
            }
            is FieldCollectionUiEvent.OnProvenienteCorreoChanged -> updateForm {
                it.copy(correoElectronico = event.value, correoError = null)
            }
            FieldCollectionUiEvent.OnConfirmAddProveniente -> handleConfirmAddProveniente()
            is FieldCollectionUiEvent.OnRemoveProveniente -> updateIfEditable { state ->
                state.copy(provenientes = state.provenientes.filterNot { it.id == event.id })
            }
            is FieldCollectionUiEvent.OnDetalleNovedadChanged -> updateIfEditable { state ->
                state.copy(
                    detalleNovedad = event.value,
                    validationErrors = state.validationErrors.copy(detalleNovedadError = null)
                )
            }
            is FieldCollectionUiEvent.OnObservacionesCampoChanged -> updateIfEditable {
                it.copy(observacionesCampo = event.value)
            }
            is FieldCollectionUiEvent.OnNombreResponsableChanged -> updateIfEditable { state ->
                state.copy(
                    nombreResponsableEntrega = event.value,
                    validationErrors = state.validationErrors.copy(nombreResponsableError = null)
                )
            }
            is FieldCollectionUiEvent.OnIdentificacionResponsableChanged -> updateIfEditable { state ->
                state.copy(
                    identificacionResponsableEntrega = event.value,
                    validationErrors = state.validationErrors.copy(identificacionResponsableError = null)
                )
            }
            FieldCollectionUiEvent.OnSaveDraftClick -> handleSaveDraft()
            FieldCollectionUiEvent.OnFinalizeClick -> handleFinalizeRequest()
            FieldCollectionUiEvent.OnConfirmFinalize -> persistCollection(finalize = true)
            FieldCollectionUiEvent.OnDismissFinalizeConfirm -> _uiState.update {
                it.copy(showFinalizeConfirmDialog = false)
            }
            FieldCollectionUiEvent.OnConfirmNoRecolectada -> persistCollection(finalize = true)
            FieldCollectionUiEvent.OnDismissNoRecolectadaConfirm -> _uiState.update {
                it.copy(showNoRecolectadaConfirmDialog = false)
            }
            FieldCollectionUiEvent.OnDismissCompletionAlert -> _uiState.update {
                it.copy(completionAlertTitle = null, completionAlertMessage = null)
            }
        }
    }

    private fun loadOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getOrderBySolicitudUseCase(numeroSolicitud)
                .onSuccess { order -> applyLoadedOrder(order) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible cargar la orden."
                        )
                    }
                }
        }
    }

    private fun applyLoadedOrder(order: OrdenRecoleccion) {
        val initialStatus = if (order.estado == OrdenStatus.ORDEN_ASIGNADA) {
            OrdenStatus.EN_ATENCION
        } else {
            order.estado
        }
        val totals = calculateCollectionTotalsUseCase(order.articulos)
        _uiState.update {
            it.copy(
                order = order,
                selectedStatus = initialStatus,
                articulos = order.articulos,
                totalsSummary = totals,
                aplicaProvenientes = order.aplicaProvenientes,
                provenientes = order.provenientes,
                detalleNovedad = order.detalleNovedad.orEmpty(),
                observacionesCampo = order.observacionesCampo,
                nombreResponsableEntrega = order.nombreResponsableEntrega,
                identificacionResponsableEntrega = order.identificacionResponsableEntrega,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun handleStatusSelected(status: OrdenStatus) {
        if (!status.isSelectableByDriver) return
        updateIfEditable { state ->
            state.copy(
                selectedStatus = status,
                validationErrors = state.validationErrors.copy(detalleNovedadError = null)
            )
        }
    }

    private fun handleQuantityChanged(codigoArticulo: String, quantity: Int?) {
        updateIfEditable { state ->
            val updatedArticulos = state.articulos.map { articulo ->
                if (articulo.codigoArticulo == codigoArticulo) {
                    articulo.copy(cantidadActa = quantity?.coerceAtLeast(0))
                } else {
                    articulo
                }
            }
            state.copy(
                articulos = updatedArticulos,
                totalsSummary = calculateCollectionTotalsUseCase(updatedArticulos),
                validationErrors = state.validationErrors.copy(
                    missingCantidadActaCodigos = state.validationErrors.missingCantidadActaCodigos - codigoArticulo
                )
            )
        }
    }

    private fun handleConfirmAddProveniente() {
        val form = _uiState.value.provenienteForm
        val cantidad = form.cantidadBaterias.toIntOrNull()
        val validated = form.copy(
            nitError = if (form.nit.isBlank()) REQUIRED_ERROR else null,
            razonSocialError = if (form.razonSocial.isBlank()) REQUIRED_ERROR else null,
            cantidadError = if (cantidad == null || cantidad <= 0) "Ingrese una cantidad mayor a 0" else null,
            telefonoError = if (form.telefono.isBlank()) REQUIRED_ERROR else null,
            correoError = if (form.correoElectronico.isBlank() || !form.correoElectronico.contains("@")) {
                "Correo electrónico inválido"
            } else null
        )

        val hasErrors = listOf(
            validated.nitError,
            validated.razonSocialError,
            validated.cantidadError,
            validated.telefonoError,
            validated.correoError
        ).any { it != null }

        if (hasErrors) {
            _uiState.update { it.copy(provenienteForm = validated) }
            return
        }

        val nuevo = TerceroProveniente(
            id = UUID.randomUUID().toString(),
            nit = form.nit.trim(),
            razonSocial = form.razonSocial.trim(),
            tipoBateria = form.tipoBateria,
            cantidadBaterias = cantidad!!,
            telefono = form.telefono.trim(),
            correoElectronico = form.correoElectronico.trim()
        )
        _uiState.update {
            it.copy(
                provenientes = it.provenientes + nuevo,
                showAddProvenienteDialog = false,
                provenienteForm = TerceroProvenienteFormState()
            )
        }
    }

    private fun handleSaveDraft() {
        val state = _uiState.value
        if (state.isReadOnly) return
        if (state.selectedStatus == OrdenStatus.NO_RECOLECTADA) {
            handleFinalizeRequest()
            return
        }
        persistCollection(finalize = false)
    }

    private fun handleFinalizeRequest() {
        val state = _uiState.value
        if (state.isReadOnly) return

        val errors = validateFieldCollectionUseCase(
            estado = state.selectedStatus,
            articulos = state.articulos,
            detalleNovedad = state.detalleNovedad,
            nombreResponsable = state.nombreResponsableEntrega,
            identificacionResponsable = state.identificacionResponsableEntrega,
            isFinalizing = true
        )

        if (!errors.isValid) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        _uiState.update {
            it.copy(
                validationErrors = errors,
                showNoRecolectadaConfirmDialog = state.selectedStatus == OrdenStatus.NO_RECOLECTADA,
                showFinalizeConfirmDialog = state.selectedStatus != OrdenStatus.NO_RECOLECTADA
            )
        }
    }

    private fun persistCollection(finalize: Boolean) {
        val state = _uiState.value
        val submission = FieldCollectionSubmission(
            numeroSolicitud = numeroSolicitud,
            estado = state.selectedStatus,
            articulos = state.articulos,
            aplicaProvenientes = state.aplicaProvenientes,
            provenientes = if (state.aplicaProvenientes) state.provenientes else emptyList(),
            detalleNovedad = state.detalleNovedad.trim().takeIf { it.isNotBlank() },
            observacionesCampo = state.observacionesCampo.trim(),
            nombreResponsableEntrega = state.nombreResponsableEntrega.trim(),
            identificacionResponsableEntrega = state.identificacionResponsableEntrega.trim(),
            finalizeCollection = finalize
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    showFinalizeConfirmDialog = false,
                    showNoRecolectadaConfirmDialog = false,
                    errorMessage = null
                )
            }
            saveFieldCollectionUseCase(submission)
                .onSuccess { updatedOrder -> handleSaveSuccess(updatedOrder, finalize) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            completionAlertTitle = "Error al guardar",
                            completionAlertMessage = error.message ?: "No fue posible registrar la recolección."
                        )
                    }
                }
        }
    }

    private fun handleSaveSuccess(updatedOrder: OrdenRecoleccion, finalize: Boolean) {
        val totals = calculateCollectionTotalsUseCase(updatedOrder.articulos)
        val (alertTitle, alertMessage) = buildSuccessAlert(updatedOrder, finalize)

        _uiState.update {
            it.copy(
                order = updatedOrder,
                selectedStatus = updatedOrder.estado,
                articulos = updatedOrder.articulos,
                totalsSummary = totals,
                aplicaProvenientes = updatedOrder.aplicaProvenientes,
                provenientes = updatedOrder.provenientes,
                detalleNovedad = updatedOrder.detalleNovedad.orEmpty(),
                observacionesCampo = updatedOrder.observacionesCampo,
                nombreResponsableEntrega = updatedOrder.nombreResponsableEntrega,
                identificacionResponsableEntrega = updatedOrder.identificacionResponsableEntrega,
                isSaving = false,
                completionAlertTitle = alertTitle,
                completionAlertMessage = alertMessage
            )
        }
    }

    private fun buildSuccessAlert(order: OrdenRecoleccion, finalize: Boolean): Pair<String, String> {
        return when {
            order.estado == OrdenStatus.NO_RECOLECTADA ->
                "Novedad registrada" to "La orden ${order.numeroSolicitud} quedó cerrada en estado No Recolectada (solo lectura)."
            finalize && order.numeroActaGenerada != null ->
                "Recolección finalizada" to "La orden ${order.numeroSolicitud} fue cerrada como Recolección Realizada. " +
                    "Se generó el Acta Respel ${order.numeroActaGenerada}."
            finalize ->
                "Recolección finalizada" to "La orden ${order.numeroSolicitud} quedó cerrada en estado Recolección Realizada (solo lectura)."
            else ->
                "Avance guardado" to "Los datos de la orden ${order.numeroSolicitud} fueron guardados en estado ${order.estado.displayName}."
        }
    }

    private inline fun updateIfEditable(
        crossinline transform: (FieldCollectionUiState) -> FieldCollectionUiState
    ) {
        _uiState.update { current ->
            if (current.isReadOnly) current else transform(current)
        }
    }

    private inline fun updateForm(
        crossinline transform: (TerceroProvenienteFormState) -> TerceroProvenienteFormState
    ) {
        _uiState.update { current ->
            current.copy(provenienteForm = transform(current.provenienteForm))
        }
    }

    companion object {
        private const val REQUIRED_ERROR = ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR
    }
}
