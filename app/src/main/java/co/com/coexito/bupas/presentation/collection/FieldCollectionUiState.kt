package co.com.coexito.bupas.presentation.collection

import androidx.compose.runtime.Immutable
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.FieldCollectionValidationErrors
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente
import co.com.coexito.bupas.domain.usecase.CollectionTotalsSummary

@Immutable
data class TerceroProvenienteFormState(
    val editingId: String? = null,
    val nit: String = "",
    val razonSocial: String = "",
    val tipoBateria: TipoBateriaProveniente = TipoBateriaProveniente.AUTO,
    val cantidadBaterias: String = "",
    val telefono: String = "",
    val correoElectronico: String = "",
    val nitError: String? = null,
    val razonSocialError: String? = null,
    val cantidadError: String? = null,
    val telefonoError: String? = null,
    val correoError: String? = null
) {
    val isEditMode: Boolean
        get() = editingId != null
}

@Immutable
data class FieldCollectionUiState(
    val order: OrdenRecoleccion? = null,
    val selectedStatus: OrdenStatus = OrdenStatus.ORDEN_ASIGNADA,
    val fechaHoraInicioAtencion: String? = null,
    val fechaHoraFinalizacion: String? = null,
    val articulos: List<ArticuloBupas> = emptyList(),
    val totalsSummary: CollectionTotalsSummary = CollectionTotalsSummary(
        totalUnidades = 0,
        totalPesoKg = 0.0,
        totalValorCompra = 0.0,
        hasMissingCantidadActa = true
    ),
    val aplicaProvenientes: Boolean = false,
    val provenientes: List<TerceroProveniente> = emptyList(),
    val detalleNovedad: String = "",
    val observacionesCampo: String = "",
    val nombreResponsableEntrega: String = "",
    val identificacionResponsableEntrega: String = "",
    val firmaDigitalBase64: String? = null,
    val isOfflineDraftSaved: Boolean = false,
    val validationErrors: FieldCollectionValidationErrors = FieldCollectionValidationErrors(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showFinalizeConfirmDialog: Boolean = false,
    val showNoRecolectadaConfirmDialog: Boolean = false,
    val showAddProvenienteDialog: Boolean = false,
    val provenienteForm: TerceroProvenienteFormState = TerceroProvenienteFormState(),
    val completionAlertTitle: String? = null,
    val completionAlertMessage: String? = null
) {
    val isReadOnly: Boolean
        get() = order?.estado?.isReadOnlyLocked == true

    val isNoRecolectadaSelected: Boolean
        get() = selectedStatus == OrdenStatus.NO_RECOLECTADA

    val isCollectionSectionEnabled: Boolean
        get() = selectedStatus == OrdenStatus.EN_ATENCION ||
            selectedStatus == OrdenStatus.RECOLECCION_PARCIAL ||
            order?.estado == OrdenStatus.RECOLECCION_REALIZADA
}

sealed interface FieldCollectionUiEvent {
    data object OnRetry : FieldCollectionUiEvent
    data class OnStatusSelected(val status: OrdenStatus) : FieldCollectionUiEvent
    data class OnQuantityActaChanged(val codigoArticulo: String, val quantity: Int?) : FieldCollectionUiEvent
    data class OnQuantityActaInputChanged(val codigoArticulo: String, val rawInput: String) : FieldCollectionUiEvent
    data class OnAplicaProvenientesChanged(val applies: Boolean) : FieldCollectionUiEvent
    data object OnOpenAddProvenienteDialog : FieldCollectionUiEvent
    data class OnOpenEditProvenienteDialog(val id: String) : FieldCollectionUiEvent
    data object OnDismissAddProvenienteDialog : FieldCollectionUiEvent
    data class OnProvenienteNitChanged(val value: String) : FieldCollectionUiEvent
    data class OnProvenienteRazonSocialChanged(val value: String) : FieldCollectionUiEvent
    data class OnProvenienteTipoChanged(val value: TipoBateriaProveniente) : FieldCollectionUiEvent
    data class OnProvenienteCantidadChanged(val value: String) : FieldCollectionUiEvent
    data class OnProvenienteTelefonoChanged(val value: String) : FieldCollectionUiEvent
    data class OnProvenienteCorreoChanged(val value: String) : FieldCollectionUiEvent
    data object OnConfirmAddProveniente : FieldCollectionUiEvent
    data class OnRemoveProveniente(val id: String) : FieldCollectionUiEvent
    data class OnDetalleNovedadChanged(val value: String) : FieldCollectionUiEvent
    data class OnObservacionesCampoChanged(val value: String) : FieldCollectionUiEvent
    data class OnNombreResponsableChanged(val value: String) : FieldCollectionUiEvent
    data class OnIdentificacionResponsableChanged(val value: String) : FieldCollectionUiEvent
    data class OnSignatureCaptured(val base64: String) : FieldCollectionUiEvent
    data object OnSignatureCleared : FieldCollectionUiEvent
    data object OnSaveDraftClick : FieldCollectionUiEvent
    data object OnFinalizeClick : FieldCollectionUiEvent
    data object OnConfirmFinalize : FieldCollectionUiEvent
    data object OnDismissFinalizeConfirm : FieldCollectionUiEvent
    data object OnConfirmNoRecolectada : FieldCollectionUiEvent
    data object OnDismissNoRecolectadaConfirm : FieldCollectionUiEvent
    data object OnDismissCompletionAlert : FieldCollectionUiEvent
}
