package co.com.coexito.bupas.presentation.certificate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.com.coexito.bupas.BuildConfig
import co.com.coexito.bupas.core.config.AppEnvironment
import co.com.coexito.bupas.core.designsystem.components.BupasAlertModal
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasSectionCard
import co.com.coexito.bupas.core.designsystem.components.BupasTextButton
import co.com.coexito.bupas.core.designsystem.components.BupasTopBar
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.usecase.certificate.BuildActaRespelUseCase
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun ActaRespelRoute(
    onBackClick: () -> Unit,
    viewModel: ActaRespelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ActaRespelScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun ActaRespelScreen(
    uiState: ActaRespelUiState,
    onBackClick: () -> Unit,
    onEvent: (ActaRespelUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BupasTopBar(
                title = uiState.acta?.let { "Acta Respel ${it.numeroActa}" } ?: "Acta Respel",
                subtitle = uiState.acta?.let { "${it.nombreArchivoPdf} • Solicitud ${it.numeroSolicitud}" },
                environmentLabel = AppEnvironment.fromRaw(BuildConfig.ENVIRONMENT).badgeLabel,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> ActaLoadingState()
                uiState.errorMessage != null -> ActaErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onEvent(ActaRespelUiEvent.OnRetryLoad) }
                )
                uiState.acta != null -> ActaRespelContent(
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }

    val alertTitle = uiState.feedbackAlertTitle
    val alertMessage = uiState.feedbackAlertMessage
    if (alertTitle != null && alertMessage != null) {
        BupasAlertModal(
            title = alertTitle,
            message = alertMessage,
            isError = alertTitle.startsWith("Error"),
            onDismiss = { onEvent(ActaRespelUiEvent.OnDismissFeedbackAlert) }
        )
    }
}

@Composable
private fun ActaRespelContent(
    uiState: ActaRespelUiState,
    onEvent: (ActaRespelUiEvent) -> Unit
) {
    val acta = uiState.acta ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(BupasTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
    ) {
        item {
            ActaTraceabilitySection(
                acta = acta,
                requiresContingencyRetry = uiState.requiresContingencyRetry,
                isRetryingContingency = uiState.isRetryingContingency,
                onViewPdfClick = { onEvent(ActaRespelUiEvent.OnViewActaPdfClick) },
                onDownloadPdfClick = { onEvent(ActaRespelUiEvent.OnDownloadPdfClick) },
                onRetryContingencyClick = { onEvent(ActaRespelUiEvent.OnRetryContingencyClick) }
            )
        }
        if (BuildConfig.USE_FAKE_DATA) {
            item {
                ActaDemoContingencyControls(
                    onSimulateGenerationError = {
                        onEvent(ActaRespelUiEvent.OnSimulateGenerationErrorClick)
                    },
                    onSimulateEmailError = {
                        onEvent(ActaRespelUiEvent.OnSimulateEmailErrorClick)
                    }
                )
            }
        }
        if (uiState.isPdfDocumentVisible && acta.canViewPdf) {
            item {
                ActaOfficialPdfDocumentCard(acta = acta)
            }
        }
    }
}

@Composable
private fun ActaDemoContingencyControls(
    onSimulateGenerationError: () -> Unit,
    onSimulateEmailError: () -> Unit
) {
    BupasSectionCard(
        title = "Simulador de Contingencia (Demo HU-007)",
        subtitle = "Pruebe los escenarios de fallo de generación PDF (CA 3) y fallo de envío por correo con reintentos 2h/4h/6h (CA 2)"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BupasTextButton(
                text = "Simular Error Generación (CA 3)",
                onClick = onSimulateGenerationError,
                modifier = Modifier.weight(1f)
            )
            BupasTextButton(
                text = "Simular Error Envío (CA 2)",
                onClick = onSimulateEmailError,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActaLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ActaErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(BupasTheme.spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            BupasPrimaryButton(
                text = "Reintentar",
                onClick = onRetry
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActaRespelScreenPreview() {
    val sampleOrder = OrdenRecoleccion(
        numeroSolicitud = "SOL-2026-00139",
        identificadorOm = "OM-889390",
        cedi = "CEDI Cali",
        fechaRecoleccion = "2026-11-09",
        nitCliente = "900.412.889-1",
        razonSocialCliente = "Autocentro del Valle Ltda.",
        direccionRecogida = "Cra. 15 # 22-40",
        departamento = "Valle del Cauca",
        ciudad = "Palmira",
        vendedor = "Laura Gómez",
        tipoDevolucion = "INS-Institucional",
        observacionesIniciales = "Entrega con encargado de almacén",
        estado = OrdenStatus.RECOLECCION_REALIZADA,
        numeroActaGenerada = "INS-0048",
        observacionesCampo = "Recolección completa verificada en almacén.",
        nombreResponsableEntrega = "Andrés Felipe Mora",
        identificacionResponsableEntrega = "1113645890",
        fechaHoraFinalizacion = "2026-11-09 09:52:00",
        firmaDigitalBase64 = "U0lHTkFUVVJFX0lOU18wMDQ4",
        articulos = listOf(
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-34",
                descripcion = "Batería Usada Auto 34",
                cantidadInicial = 20,
                cantidadActa = 20,
                pesoUnitarioKg = 16.0,
                valorCompraUnitario = 62000.0
            )
        )
    )
    val acta = BuildActaRespelUseCase()(sampleOrder)
    BupasTheme {
        ActaRespelScreen(
            uiState = ActaRespelUiState(
                isLoading = false,
                acta = acta,
                isPdfDocumentVisible = true
            ),
            onBackClick = {},
            onEvent = {}
        )
    }
}
