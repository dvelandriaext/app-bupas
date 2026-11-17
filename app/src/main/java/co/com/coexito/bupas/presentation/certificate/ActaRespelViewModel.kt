package co.com.coexito.bupas.presentation.certificate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.domain.usecase.certificate.GetActaRespelUseCase
import co.com.coexito.bupas.domain.usecase.certificate.RetryActaContingencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel con flujo de datos unidireccional (UDF) para consulta, trazabilidad, visualización
 * del PDF y contingencia del Acta Respel (HU-007 CA 1, CA 2 y CA 3).
 */
@HiltViewModel
class ActaRespelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getActaRespelUseCase: GetActaRespelUseCase,
    private val retryActaContingencyUseCase: RetryActaContingencyUseCase
) : ViewModel() {

    private val numeroSolicitud: String = savedStateHandle["numeroSolicitud"] ?: ""

    private val _uiState = MutableStateFlow(ActaRespelUiState())
    val uiState: StateFlow<ActaRespelUiState> = _uiState.asStateFlow()

    init {
        loadActa()
    }

    fun onEvent(event: ActaRespelUiEvent) {
        when (event) {
            ActaRespelUiEvent.OnRetryLoad -> loadActa()
            ActaRespelUiEvent.OnViewActaPdfClick -> handleViewPdfClick()
            ActaRespelUiEvent.OnDownloadPdfClick -> handleDownloadPdfClick()
            ActaRespelUiEvent.OnRetryContingencyClick -> executeContingencyRetry()
            ActaRespelUiEvent.OnDismissFeedbackAlert -> dismissFeedbackAlert()
        }
    }

    private fun loadActa() {
        if (numeroSolicitud.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Identificador de solicitud no válido para consultar el Acta Respel."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getActaRespelUseCase(numeroSolicitud)
                .onSuccess { acta ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            acta = acta,
                            isPdfDocumentVisible = acta.canViewPdf,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible consultar el Acta Respel."
                        )
                    }
                }
        }
    }

    private fun handleViewPdfClick() {
        val currentActa = _uiState.value.acta ?: return
        if (!currentActa.canViewPdf) {
            _uiState.update {
                it.copy(
                    isPdfDocumentVisible = false,
                    feedbackAlertTitle = "Error de generación",
                    feedbackAlertMessage = "El documento ${currentActa.nombreArchivoPdf} no se encuentra disponible " +
                        "porque el estado del acta es '${currentActa.estadoGeneracion.displayName}'."
                )
            }
            return
        }
        _uiState.update { it.copy(isPdfDocumentVisible = true) }
    }

    private fun handleDownloadPdfClick() {
        val currentActa = _uiState.value.acta ?: return
        if (!currentActa.canViewPdf) return

        _uiState.update {
            it.copy(
                feedbackAlertTitle = "Descarga de Comprobante PDF",
                feedbackAlertMessage = "Archivo oficial ${currentActa.nombreArchivoPdf} verificado y listo " +
                    "(Solicitud ${currentActa.numeroSolicitud} • OM ${currentActa.identificadorOm})."
            )
        }
    }

    private fun executeContingencyRetry() {
        if (_uiState.value.isRetryingContingency || numeroSolicitud.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRetryingContingency = true) }
            retryActaContingencyUseCase(numeroSolicitud)
                .onSuccess { updatedActa ->
                    _uiState.update {
                        it.copy(
                            isRetryingContingency = false,
                            acta = updatedActa,
                            isPdfDocumentVisible = updatedActa.canViewPdf,
                            feedbackAlertTitle = "Contingencia procesada",
                            feedbackAlertMessage = "El Acta ${updatedActa.numeroActa} (${updatedActa.nombreArchivoPdf}) " +
                                "fue generada y enviada a: ${updatedActa.notificacionCorreo.correosDestino.joinToString(", ")}."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRetryingContingency = false,
                            feedbackAlertTitle = "Error en reintento",
                            feedbackAlertMessage = error.message
                                ?: "No fue posible completar el reintento de contingencia."
                        )
                    }
                }
        }
    }

    private fun dismissFeedbackAlert() {
        _uiState.update {
            it.copy(
                feedbackAlertTitle = null,
                feedbackAlertMessage = null
            )
        }
    }
}
