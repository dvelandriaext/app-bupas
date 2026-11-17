package co.com.coexito.bupas.presentation.certificate

import androidx.compose.runtime.Immutable
import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa

@Immutable
data class ActaRespelUiState(
    val isLoading: Boolean = true,
    val acta: ActaRespel? = null,
    val isPdfDocumentVisible: Boolean = true,
    val isRetryingContingency: Boolean = false,
    val errorMessage: String? = null,
    val feedbackAlertTitle: String? = null,
    val feedbackAlertMessage: String? = null
) {
    val canViewPdf: Boolean
        get() = acta?.canViewPdf == true

    val requiresContingencyRetry: Boolean
        get() = acta != null && (
            acta.estadoGeneracion == EstadoGeneracionActa.ERROR_GENERACION ||
                acta.notificacionCorreo.estadoEnvio == EstadoEnvioActa.ERROR_ENVIO
            )
}

sealed interface ActaRespelUiEvent {
    data object OnRetryLoad : ActaRespelUiEvent
    data object OnViewActaPdfClick : ActaRespelUiEvent
    data object OnDownloadPdfClick : ActaRespelUiEvent
    data object OnRetryContingencyClick : ActaRespelUiEvent
    data object OnSimulateGenerationErrorClick : ActaRespelUiEvent
    data object OnSimulateEmailErrorClick : ActaRespelUiEvent
    data object OnDismissFeedbackAlert : ActaRespelUiEvent
}
