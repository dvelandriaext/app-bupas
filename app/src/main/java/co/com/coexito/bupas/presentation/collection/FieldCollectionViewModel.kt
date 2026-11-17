package co.com.coexito.bupas.presentation.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.core.common.SystemTimeProvider
import co.com.coexito.bupas.core.common.TimeProvider
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.FieldCollectionValidationErrors
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.usecase.CalculateCollectionTotalsUseCase
import co.com.coexito.bupas.domain.usecase.collection.PrepareFieldCollectionOrderUseCase
import co.com.coexito.bupas.domain.usecase.collection.SaveFieldCollectionUseCase
import co.com.coexito.bupas.domain.usecase.collection.ValidateFieldCollectionUseCase
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
    private val prepareFieldCollectionOrderUseCase: PrepareFieldCollectionOrderUseCase,
    private val calculateCollectionTotalsUseCase: CalculateCollectionTotalsUseCase,
    private val validateFieldCollectionUseCase: ValidateFieldCollectionUseCase,
    private val saveFieldCollectionUseCase: SaveFieldCollectionUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    constructor(
        savedStateHandle: SavedStateHandle,
        prepareFieldCollectionOrderUseCase: PrepareFieldCollectionOrderUseCase,
        calculateCollectionTotalsUseCase: CalculateCollectionTotalsUseCase = CalculateCollectionTotalsUseCase(),
        validateFieldCollectionUseCase: ValidateFieldCollectionUseCase = ValidateFieldCollectionUseCase(),
        saveFieldCollectionUseCase: SaveFieldCollectionUseCase
    ) : this(
        savedStateHandle = savedStateHandle,
        prepareFieldCollectionOrderUseCase = prepareFieldCollectionOrderUseCase,
        calculateCollectionTotalsUseCase = calculateCollectionTotalsUseCase,
        validateFieldCollectionUseCase = validateFieldCollectionUseCase,
        saveFieldCollectionUseCase = saveFieldCollectionUseCase,
        timeProvider = SystemTimeProvider()
    )

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
            is FieldCollectionUiEvent.OnQuantityActaInputChanged -> handleRawQuantityChanged(event.codigoArticulo, event.rawInput)
            is FieldCollectionUiEvent.OnDetalleNovedadChanged -> updateIfEditable {
                it.copy(detalleNovedad = event.value, validationErrors = it.validationErrors.copy(detalleNovedadError = null))
            }
            is FieldCollectionUiEvent.OnObservacionesCampoChanged -> updateIfEditable { it.copy(observacionesCampo = event.value) }
            is FieldCollectionUiEvent.OnNombreResponsableChanged -> updateIfEditable {
                it.copy(nombreResponsableEntrega = event.value, validationErrors = it.validationErrors.copy(nombreResponsableError = null))
            }
            is FieldCollectionUiEvent.OnIdentificacionResponsableChanged -> updateIfEditable {
                it.copy(identificacionResponsableEntrega = event.value, validationErrors = it.validationErrors.copy(identificacionResponsableError = null))
            }
            is FieldCollectionUiEvent.OnSignatureCaptured -> handleSignatureCaptured(event.base64)
            FieldCollectionUiEvent.OnSignatureCleared -> updateIfEditable { it.copy(firmaDigitalBase64 = null) }
            else -> handleProvenienteOrActionEvent(event)
        }
    }

    private fun handleProvenienteOrActionEvent(event: FieldCollectionUiEvent) {
        when (event) {
            is FieldCollectionUiEvent.OnAplicaProvenientesChanged -> handleAplicaProvenientesChanged(event.applies)
            FieldCollectionUiEvent.OnOpenAddProvenienteDialog -> handleOpenAddProvenienteDialog()
            is FieldCollectionUiEvent.OnOpenEditProvenienteDialog -> handleOpenEditProvenienteDialog(event.id)
            FieldCollectionUiEvent.OnDismissAddProvenienteDialog -> _uiState.update { it.copy(showAddProvenienteDialog = false) }
            is FieldCollectionUiEvent.OnProvenienteNitChanged -> updateForm { it.copy(nit = event.value, nitError = null) }
            is FieldCollectionUiEvent.OnProvenienteRazonSocialChanged -> updateForm { it.copy(razonSocial = event.value, razonSocialError = null) }
            is FieldCollectionUiEvent.OnProvenienteTipoChanged -> updateForm { it.copy(tipoBateria = event.value) }
            is FieldCollectionUiEvent.OnProvenienteCantidadChanged -> updateForm { it.copy(cantidadBaterias = event.value, cantidadError = null) }
            is FieldCollectionUiEvent.OnProvenienteTelefonoChanged -> updateForm { it.copy(telefono = event.value, telefonoError = null) }
            is FieldCollectionUiEvent.OnProvenienteCorreoChanged -> updateForm { it.copy(correoElectronico = event.value, correoError = null) }
            FieldCollectionUiEvent.OnConfirmAddProveniente -> handleConfirmAddProveniente()
            is FieldCollectionUiEvent.OnRemoveProveniente -> updateIfEditable { state ->
                state.copy(provenientes = state.provenientes.filterNot { it.id == event.id })
            }
            else -> handleActionOrDialogEvent(event)
        }
    }

    private fun handleActionOrDialogEvent(event: FieldCollectionUiEvent) {
        when (event) {
            FieldCollectionUiEvent.OnSaveDraftClick -> handleSaveDraft()
            FieldCollectionUiEvent.OnFinalizeClick -> handleFinalizeRequest()
            FieldCollectionUiEvent.OnConfirmFinalize -> persistCollection(finalize = true)
            FieldCollectionUiEvent.OnDismissFinalizeConfirm -> _uiState.update { it.copy(showFinalizeConfirmDialog = false) }
            FieldCollectionUiEvent.OnConfirmNoRecolectada -> persistCollection(finalize = true)
            FieldCollectionUiEvent.OnDismissNoRecolectadaConfirm -> _uiState.update { it.copy(showNoRecolectadaConfirmDialog = false) }
            FieldCollectionUiEvent.OnDismissCompletionAlert -> _uiState.update {
                it.copy(completionAlertTitle = null, completionAlertMessage = null)
            }
            else -> Unit
        }
    }

    private fun loadOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            prepareFieldCollectionOrderUseCase(numeroSolicitud)
                .onSuccess { order ->
                    val hasDraft = prepareFieldCollectionOrderUseCase.hasPendingOfflineDraft(numeroSolicitud)
                    applyLoadedOrder(order, hasDraft)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "No fue posible cargar la orden.")
                    }
                }
        }
    }

    private fun applyLoadedOrder(order: OrdenRecoleccion, hasOfflineDraft: Boolean) {
        _uiState.update {
            it.copy(
                order = order,
                selectedStatus = order.estado,
                fechaHoraInicioAtencion = order.fechaHoraInicioAtencion,
                fechaHoraFinalizacion = order.fechaHoraFinalizacion,
                articulos = order.articulos,
                totalsSummary = calculateCollectionTotalsUseCase(order.articulos),
                aplicaProvenientes = order.aplicaProvenientes,
                provenientes = order.provenientes,
                detalleNovedad = order.detalleNovedad.orEmpty(),
                observacionesCampo = order.observacionesCampo,
                nombreResponsableEntrega = order.nombreResponsableEntrega,
                identificacionResponsableEntrega = order.identificacionResponsableEntrega,
                firmaDigitalBase64 = order.firmaDigitalBase64,
                isOfflineDraftSaved = hasOfflineDraft,
                validationErrors = FieldCollectionValidationErrors(),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun handleStatusSelected(status: OrdenStatus) {
        val current = _uiState.value
        if (current.isReadOnly || !status.isSelectableByDriver) return
        if (status == OrdenStatus.NO_RECOLECTADA) {
            _uiState.update { it.copy(selectedStatus = OrdenStatus.NO_RECOLECTADA, validationErrors = FieldCollectionValidationErrors()) }
            return
        }
        val resolvedInicio = current.fechaHoraInicioAtencion?.takeIf { it.isNotBlank() }
            ?: current.order?.fechaHoraInicioAtencion?.takeIf { it.isNotBlank() }
            ?: timeProvider.nowFormatted()
        _uiState.update {
            it.copy(
                selectedStatus = status,
                fechaHoraInicioAtencion = resolvedInicio,
                validationErrors = it.validationErrors.copy(detalleNovedadError = null)
            )
        }
        autoPersistAttentionStart()
    }

    private fun autoPersistAttentionStart() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            if (snapshot.isReadOnly) return@launch
            saveFieldCollectionUseCase(buildSubmission(snapshot, finalize = false))
                .onSuccess { updatedOrder ->
                    val hasDraft = saveFieldCollectionUseCase.hasPendingOfflineDraft(numeroSolicitud)
                    _uiState.update { current ->
                        current.copy(
                            order = updatedOrder,
                            fechaHoraInicioAtencion = updatedOrder.fechaHoraInicioAtencion ?: current.fechaHoraInicioAtencion,
                            isOfflineDraftSaved = hasDraft
                        )
                    }
                }
                .onFailure {
                    val hasDraft = saveFieldCollectionUseCase.hasPendingOfflineDraft(numeroSolicitud)
                    _uiState.update { it.copy(isOfflineDraftSaved = hasDraft) }
                }
        }
    }

    private fun handleRawQuantityChanged(codigoArticulo: String, rawInput: String) {
        if (_uiState.value.isReadOnly) return
        validateFieldCollectionUseCase.parseAndValidateCantidadActa(rawInput)
            .onSuccess { parsedQty -> applyValidQuantity(codigoArticulo, parsedQty) }
            .onFailure { error ->
                recordInvalidQuantity(codigoArticulo, error.message ?: ValidateFieldCollectionUseCase.INVALID_CANTIDAD_ACTA_ERROR)
            }
    }

    private fun handleQuantityChanged(codigoArticulo: String, quantity: Int?) {
        if (_uiState.value.isReadOnly) return
        if (quantity != null && quantity < 0) {
            recordInvalidQuantity(codigoArticulo, ValidateFieldCollectionUseCase.INVALID_CANTIDAD_ACTA_ERROR)
            return
        }
        applyValidQuantity(codigoArticulo, quantity)
    }

    private fun applyValidQuantity(codigoArticulo: String, quantity: Int?) {
        updateIfEditable { state ->
            val updatedArticulos = state.articulos.map { articulo ->
                if (articulo.codigoArticulo == codigoArticulo) articulo.copy(cantidadActa = quantity) else articulo
            }
            val updatedInvalid = state.validationErrors.invalidCantidadActaCodigos - codigoArticulo
            val updatedMissing = if (quantity != null) {
                state.validationErrors.missingCantidadActaCodigos - codigoArticulo
            } else {
                state.validationErrors.missingCantidadActaCodigos
            }
            val clearGate = updatedArticulos.all { it.cantidadActa != null } && updatedInvalid.isEmpty()
            state.copy(
                articulos = updatedArticulos,
                totalsSummary = calculateCollectionTotalsUseCase(updatedArticulos),
                validationErrors = state.validationErrors.copy(
                    missingCantidadActaCodigos = updatedMissing,
                    invalidCantidadActaCodigos = updatedInvalid,
                    provenientesGateError = if (clearGate) null else state.validationErrors.provenientesGateError
                )
            )
        }
    }

    private fun recordInvalidQuantity(codigoArticulo: String, message: String) {
        updateIfEditable { state ->
            state.copy(
                validationErrors = state.validationErrors.copy(
                    invalidCantidadActaCodigos = state.validationErrors.invalidCantidadActaCodigos + (codigoArticulo to message)
                )
            )
        }
    }

    private fun handleAplicaProvenientesChanged(applies: Boolean) {
        val state = _uiState.value
        if (state.isReadOnly) return
        if (!applies) {
            _uiState.update { it.copy(aplicaProvenientes = false, validationErrors = it.validationErrors.copy(provenientesGateError = null)) }
            return
        }
        if (!ensureProvenientesAllowed(state)) return
        _uiState.update { it.copy(aplicaProvenientes = true, validationErrors = it.validationErrors.copy(provenientesGateError = null)) }
    }

    private fun handleOpenAddProvenienteDialog() {
        val state = _uiState.value
        if (state.isReadOnly || !ensureProvenientesAllowed(state)) return
        _uiState.update {
            it.copy(aplicaProvenientes = true, showAddProvenienteDialog = true, provenienteForm = TerceroProvenienteFormState())
        }
    }

    private fun handleOpenEditProvenienteDialog(id: String) {
        val state = _uiState.value
        if (state.isReadOnly || !ensureProvenientesAllowed(state)) return
        val target = state.provenientes.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                showAddProvenienteDialog = true,
                provenienteForm = TerceroProvenienteFormState(
                    editingId = target.id,
                    nit = target.nit,
                    razonSocial = target.razonSocial,
                    tipoBateria = target.tipoBateria,
                    cantidadBaterias = target.cantidadBaterias.toString(),
                    telefono = target.telefono,
                    correoElectronico = target.correoElectronico
                )
            )
        }
    }

    private fun ensureProvenientesAllowed(state: FieldCollectionUiState): Boolean {
        val gateErrors = validateFieldCollectionUseCase.validateProvenientesAccess(
            articulos = state.articulos,
            invalidCantidadActaCodigos = state.validationErrors.invalidCantidadActaCodigos
        )
        if (gateErrors.isValid) return true
        _uiState.update { current ->
            current.copy(
                aplicaProvenientes = false,
                showAddProvenienteDialog = false,
                validationErrors = current.validationErrors.copy(
                    missingCantidadActaCodigos = gateErrors.missingCantidadActaCodigos,
                    invalidCantidadActaCodigos = gateErrors.invalidCantidadActaCodigos,
                    provenientesGateError = gateErrors.provenientesGateError
                )
            )
        }
        return false
    }

    private fun handleConfirmAddProveniente() {
        val state = _uiState.value
        if (state.isReadOnly || !ensureProvenientesAllowed(state)) return
        val form = state.provenienteForm
        val result = validateFieldCollectionUseCase.validateProveniente(
            nit = form.nit,
            razonSocial = form.razonSocial,
            cantidadRaw = form.cantidadBaterias,
            telefono = form.telefono,
            correo = form.correoElectronico
        )
        if (!result.isValid) {
            _uiState.update {
                it.copy(
                    provenienteForm = form.copy(
                        nitError = result.nitError,
                        razonSocialError = result.razonSocialError,
                        cantidadError = result.cantidadError,
                        telefonoError = result.telefonoError,
                        correoError = result.correoError
                    )
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                provenientes = upsertProvenienteList(state.provenientes, form),
                showAddProvenienteDialog = false,
                provenienteForm = TerceroProvenienteFormState()
            )
        }
    }

    private fun upsertProvenienteList(
        existing: List<TerceroProveniente>,
        form: TerceroProvenienteFormState
    ): List<TerceroProveniente> {
        val entry = TerceroProveniente(
            id = form.editingId ?: UUID.randomUUID().toString(),
            nit = form.nit.trim(),
            razonSocial = form.razonSocial.trim(),
            tipoBateria = form.tipoBateria,
            cantidadBaterias = form.cantidadBaterias.trim().toInt(),
            telefono = form.telefono.trim(),
            correoElectronico = form.correoElectronico.trim()
        )
        return if (form.editingId != null) existing.map { if (it.id == form.editingId) entry else it } else existing + entry
    }

    private fun handleSignatureCaptured(base64: String) {
        if (base64.isBlank()) return
        updateIfEditable { state ->
            state.copy(firmaDigitalBase64 = base64, validationErrors = state.validationErrors.copy(firmaDigitalError = null))
        }
    }

    private fun handleSaveDraft() {
        val state = _uiState.value
        if (state.isReadOnly || state.selectedStatus == OrdenStatus.ORDEN_ASIGNADA) return
        if (state.selectedStatus == OrdenStatus.NO_RECOLECTADA) {
            handleFinalizeRequest()
            return
        }
        persistCollection(finalize = false)
    }

    private fun handleFinalizeRequest() {
        val state = _uiState.value
        if (state.isReadOnly) return
        if (state.selectedStatus == OrdenStatus.ORDEN_ASIGNADA) {
            _uiState.update {
                it.copy(
                    completionAlertTitle = "Estado requerido",
                    completionAlertMessage = "Seleccione En Atención, Recolección Parcial o No Recolectada antes de finalizar."
                )
            }
            return
        }
        val errors = validateFieldCollectionUseCase(
            estado = state.selectedStatus,
            detalleNovedad = state.detalleNovedad,
            articulos = state.articulos,
            nombreResponsable = state.nombreResponsableEntrega,
            identificacionResponsable = state.identificacionResponsableEntrega,
            firmaDigitalBase64 = state.firmaDigitalBase64,
            isFinalizing = true,
            invalidCantidadActaCodigos = state.validationErrors.invalidCantidadActaCodigos
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
        if (state.isReadOnly) return
        val submission = buildSubmission(state, finalize)
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSaving = true, showFinalizeConfirmDialog = false, showNoRecolectadaConfirmDialog = false, errorMessage = null)
            }
            saveFieldCollectionUseCase(submission)
                .onSuccess { updated -> handleSaveSuccess(updated, finalize, saveFieldCollectionUseCase.hasPendingOfflineDraft(numeroSolicitud)) }
                .onFailure { err -> handleSaveFailure(err, saveFieldCollectionUseCase.hasPendingOfflineDraft(numeroSolicitud)) }
        }
    }

    private fun buildSubmission(state: FieldCollectionUiState, finalize: Boolean): FieldCollectionSubmission {
        val isClosing = finalize || state.selectedStatus == OrdenStatus.NO_RECOLECTADA
        val resolvedInicio = state.fechaHoraInicioAtencion?.takeIf { it.isNotBlank() }
            ?: state.order?.fechaHoraInicioAtencion?.takeIf { it.isNotBlank() }
            ?: if (state.selectedStatus != OrdenStatus.NO_RECOLECTADA && state.selectedStatus != OrdenStatus.ORDEN_ASIGNADA) {
                timeProvider.nowFormatted()
            } else {
                null
            }
        val resolvedFin = if (isClosing) {
            state.fechaHoraFinalizacion?.takeIf { it.isNotBlank() } ?: timeProvider.nowFormatted()
        } else {
            state.fechaHoraFinalizacion
        }
        return FieldCollectionSubmission(
            numeroSolicitud = numeroSolicitud,
            estado = state.selectedStatus,
            articulos = state.articulos,
            aplicaProvenientes = state.aplicaProvenientes,
            provenientes = if (state.aplicaProvenientes) state.provenientes else emptyList(),
            detalleNovedad = state.detalleNovedad.trim().takeIf { it.isNotBlank() },
            observacionesCampo = state.observacionesCampo.trim(),
            nombreResponsableEntrega = state.nombreResponsableEntrega.trim(),
            identificacionResponsableEntrega = state.identificacionResponsableEntrega.trim(),
            finalizeCollection = finalize,
            fechaHoraInicioAtencion = resolvedInicio,
            fechaHoraFinalizacion = resolvedFin,
            firmaDigitalBase64 = state.firmaDigitalBase64
        )
    }

    private fun handleSaveSuccess(updatedOrder: OrdenRecoleccion, finalize: Boolean, hasOfflineDraft: Boolean) {
        val (alertTitle, alertMessage) = buildSuccessAlert(updatedOrder, finalize, hasOfflineDraft)
        _uiState.update {
            it.copy(
                order = updatedOrder,
                selectedStatus = updatedOrder.estado,
                fechaHoraInicioAtencion = updatedOrder.fechaHoraInicioAtencion,
                fechaHoraFinalizacion = updatedOrder.fechaHoraFinalizacion,
                articulos = updatedOrder.articulos,
                totalsSummary = calculateCollectionTotalsUseCase(updatedOrder.articulos),
                aplicaProvenientes = updatedOrder.aplicaProvenientes,
                provenientes = updatedOrder.provenientes,
                detalleNovedad = updatedOrder.detalleNovedad.orEmpty(),
                observacionesCampo = updatedOrder.observacionesCampo,
                nombreResponsableEntrega = updatedOrder.nombreResponsableEntrega,
                identificacionResponsableEntrega = updatedOrder.identificacionResponsableEntrega,
                firmaDigitalBase64 = updatedOrder.firmaDigitalBase64,
                isOfflineDraftSaved = hasOfflineDraft,
                isSaving = false,
                completionAlertTitle = alertTitle,
                completionAlertMessage = alertMessage
            )
        }
    }

    private fun handleSaveFailure(error: Throwable, hasOfflineDraft: Boolean) {
        val message = if (hasOfflineDraft) {
            "Sin conexión para finalizar en servidor. Su avance quedó guardado localmente en el dispositivo."
        } else {
            error.message ?: "No fue posible registrar la recolección."
        }
        _uiState.update {
            it.copy(isSaving = false, isOfflineDraftSaved = hasOfflineDraft, completionAlertTitle = "Error al guardar", completionAlertMessage = message)
        }
    }

    private fun buildSuccessAlert(order: OrdenRecoleccion, finalize: Boolean, hasOfflineDraft: Boolean): Pair<String, String> = when {
        hasOfflineDraft ->
            "Avance guardado localmente" to "Sin conexión a internet. Los datos de la orden ${order.numeroSolicitud} se guardaron localmente en el dispositivo."
        order.estado == OrdenStatus.NO_RECOLECTADA ->
            "Novedad registrada" to "La orden ${order.numeroSolicitud} quedó cerrada en estado No Recolectada (solo lectura)."
        finalize && order.numeroActaGenerada != null ->
            "Recolección finalizada" to "La orden ${order.numeroSolicitud} fue cerrada como Recolección Realizada. Se generó el Acta Respel ${order.numeroActaGenerada}."
        finalize ->
            "Recolección finalizada" to "La orden ${order.numeroSolicitud} quedó cerrada en estado Recolección Realizada (solo lectura)."
        else ->
            "Avance guardado" to "Los datos de la orden ${order.numeroSolicitud} fueron guardados en estado ${order.estado.displayName}."
    }

    private inline fun updateIfEditable(crossinline transform: (FieldCollectionUiState) -> FieldCollectionUiState) {
        _uiState.update { current -> if (current.isReadOnly) current else transform(current) }
    }

    private inline fun updateForm(crossinline transform: (TerceroProvenienteFormState) -> TerceroProvenienteFormState) {
        _uiState.update { current -> if (current.isReadOnly) current else current.copy(provenienteForm = transform(current.provenienteForm)) }
    }
}
