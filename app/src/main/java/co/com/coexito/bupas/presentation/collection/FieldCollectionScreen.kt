package co.com.coexito.bupas.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import co.com.coexito.bupas.core.designsystem.components.BupasConfirmDialog
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasTopBar
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.usecase.CollectionTotalsSummary

@Composable
fun FieldCollectionRoute(
    onBackClick: () -> Unit,
    onNavigateToActa: (String) -> Unit = {},
    viewModel: FieldCollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FieldCollectionScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToActa = onNavigateToActa,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun FieldCollectionScreen(
    uiState: FieldCollectionUiState,
    onBackClick: () -> Unit,
    onEvent: (FieldCollectionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToActa: (String) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BupasTopBar(
                title = uiState.order?.numeroSolicitud ?: "Recolección en Campo",
                subtitle = uiState.order?.identificadorOm?.let { "Atención en Campo • $it" },
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
                uiState.isLoading -> CollectionLoadingState()
                uiState.errorMessage != null -> CollectionErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onEvent(FieldCollectionUiEvent.OnRetry) }
                )
                uiState.order != null -> FieldCollectionContent(
                    uiState = uiState,
                    onEvent = onEvent,
                    onNavigateToActa = onNavigateToActa
                )
            }
        }
    }

    FieldCollectionDialogs(
        uiState = uiState,
        onEvent = onEvent
    )
}

@Composable
private fun FieldCollectionContent(
    uiState: FieldCollectionUiState,
    onEvent: (FieldCollectionUiEvent) -> Unit,
    onNavigateToActa: (String) -> Unit
) {
    val order = uiState.order ?: return
    val showPhysicalSections = uiState.isCollectionSectionEnabled && !uiState.isNoRecolectadaSelected
    val showActions = !uiState.isReadOnly && (showPhysicalSections || uiState.isNoRecolectadaSelected)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(BupasTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
    ) {
        item { CollectionGeneralOrderSection(order = order) }
        item { CollectionClientInfoSection(order = order) }
        item {
            CollectionStatusSection(
                selectedStatus = uiState.selectedStatus,
                fechaHoraInicioAtencion = uiState.fechaHoraInicioAtencion,
                fechaHoraFinalizacion = uiState.fechaHoraFinalizacion,
                isOfflineDraftSaved = uiState.isOfflineDraftSaved,
                isReadOnly = uiState.isReadOnly,
                detalleNovedad = uiState.detalleNovedad,
                detalleNovedadError = uiState.validationErrors.detalleNovedadError,
                onStatusSelected = { onEvent(FieldCollectionUiEvent.OnStatusSelected(it)) },
                onDetalleNovedadChanged = { onEvent(FieldCollectionUiEvent.OnDetalleNovedadChanged(it)) }
            )
        }
        if (showPhysicalSections) {
            physicalCollectionItems(
                uiState = uiState,
                order = order,
                onEvent = onEvent,
                onNavigateToActa = onNavigateToActa
            )
        }
        if (showActions) {
            item {
                CollectionActionsSection(
                    isNoRecolectada = uiState.isNoRecolectadaSelected,
                    isReadOnly = uiState.isReadOnly,
                    isSaving = uiState.isSaving,
                    onSaveDraftClick = { onEvent(FieldCollectionUiEvent.OnSaveDraftClick) },
                    onFinalizeClick = { onEvent(FieldCollectionUiEvent.OnFinalizeClick) }
                )
            }
        }
    }
}

private fun LazyListScope.physicalCollectionItems(
    uiState: FieldCollectionUiState,
    order: OrdenRecoleccion,
    onEvent: (FieldCollectionUiEvent) -> Unit,
    onNavigateToActa: (String) -> Unit
) {
    item {
        CollectionArticlesSection(
            articulos = uiState.articulos,
            totalsSummary = uiState.totalsSummary,
            missingCodigos = uiState.validationErrors.missingCantidadActaCodigos,
            invalidCodigos = uiState.validationErrors.invalidCantidadActaCodigos,
            isReadOnly = uiState.isReadOnly,
            onQuantityChanged = { codigo, qty ->
                onEvent(FieldCollectionUiEvent.OnQuantityActaChanged(codigo, qty))
            },
            onRawQuantityChanged = { codigo, raw ->
                onEvent(FieldCollectionUiEvent.OnQuantityActaInputChanged(codigo, raw))
            }
        )
    }
    item {
        CollectionProvenientesSection(
            aplicaProvenientes = uiState.aplicaProvenientes,
            provenientes = uiState.provenientes,
            provenientesGateError = uiState.validationErrors.provenientesGateError,
            isReadOnly = uiState.isReadOnly,
            onAplicaChanged = { onEvent(FieldCollectionUiEvent.OnAplicaProvenientesChanged(it)) },
            onAddClick = { onEvent(FieldCollectionUiEvent.OnOpenAddProvenienteDialog) },
            onEditClick = { onEvent(FieldCollectionUiEvent.OnOpenEditProvenienteDialog(it)) },
            onRemoveClick = { onEvent(FieldCollectionUiEvent.OnRemoveProveniente(it)) }
        )
    }
    item {
        CollectionConfirmationSection(
            nombreResponsable = uiState.nombreResponsableEntrega,
            identificacionResponsable = uiState.identificacionResponsableEntrega,
            observacionesCampo = uiState.observacionesCampo,
            firmaDigitalBase64 = uiState.firmaDigitalBase64,
            numeroActaGenerada = order.numeroActaGenerada,
            nombreError = uiState.validationErrors.nombreResponsableError,
            identificacionError = uiState.validationErrors.identificacionResponsableError,
            firmaError = uiState.validationErrors.firmaDigitalError,
            isReadOnly = uiState.isReadOnly,
            onNombreChanged = { onEvent(FieldCollectionUiEvent.OnNombreResponsableChanged(it)) },
            onIdentificacionChanged = {
                onEvent(FieldCollectionUiEvent.OnIdentificacionResponsableChanged(it))
            },
            onObservacionesChanged = { onEvent(FieldCollectionUiEvent.OnObservacionesCampoChanged(it)) },
            onSignatureCaptured = { onEvent(FieldCollectionUiEvent.OnSignatureCaptured(it)) },
            onSignatureCleared = { onEvent(FieldCollectionUiEvent.OnSignatureCleared) },
            canViewActaPdf = order.canViewActaPdf,
            onViewActaClick = { onNavigateToActa(order.numeroSolicitud) }
        )
    }
}

@Composable
private fun FieldCollectionDialogs(
    uiState: FieldCollectionUiState,
    onEvent: (FieldCollectionUiEvent) -> Unit
) {
    if (uiState.showFinalizeConfirmDialog) {
        BupasConfirmDialog(
            title = "Finalizar recolección",
            message = "¿Está seguro de finalizar la recolección? Una vez finalizada, no podrá modificar la información registrada.",
            onConfirm = { onEvent(FieldCollectionUiEvent.OnConfirmFinalize) },
            onDismiss = { onEvent(FieldCollectionUiEvent.OnDismissFinalizeConfirm) }
        )
    }

    if (uiState.showNoRecolectadaConfirmDialog) {
        BupasConfirmDialog(
            title = "Confirmar No Recolectada",
            message = "¿Está seguro de registrar la orden como No Recolectada? Una vez guardada, quedará en modo solo lectura.",
            onConfirm = { onEvent(FieldCollectionUiEvent.OnConfirmNoRecolectada) },
            onDismiss = { onEvent(FieldCollectionUiEvent.OnDismissNoRecolectadaConfirm) }
        )
    }

    if (uiState.showAddProvenienteDialog) {
        ProvenienteFormDialog(
            formState = uiState.provenienteForm,
            onEvent = onEvent
        )
    }

    val alertTitle = uiState.completionAlertTitle
    val alertMessage = uiState.completionAlertMessage
    if (alertTitle != null && alertMessage != null) {
        BupasAlertModal(
            title = alertTitle,
            message = alertMessage,
            isError = alertTitle.startsWith("Error"),
            onDismiss = { onEvent(FieldCollectionUiEvent.OnDismissCompletionAlert) }
        )
    }
}

@Composable
private fun CollectionLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CollectionErrorState(
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
private fun FieldCollectionScreenPreview() {
    val articulos = listOf(
        ArticuloBupas(
            codigoArticulo = "BUP-AUTO-42",
            descripcion = "Batería Usada Auto 42R",
            cantidadInicial = 15,
            cantidadActa = 12,
            pesoUnitarioKg = 14.5,
            valorCompraUnitario = 55000.0
        ),
        ArticuloBupas(
            codigoArticulo = "BUP-AUTO-34",
            descripcion = "Batería Usada Auto 34",
            cantidadInicial = 0,
            cantidadActa = 0,
            pesoUnitarioKg = 16.0,
            valorCompraUnitario = 62000.0
        )
    )
    BupasTheme {
        FieldCollectionScreen(
            uiState = FieldCollectionUiState(
                isLoading = false,
                order = OrdenRecoleccion(
                    numeroSolicitud = "SOL-2026-00148",
                    identificadorOm = "OM-889450",
                    cedi = "CEDI Cali",
                    fechaRecoleccion = "2026-11-15",
                    nitCliente = "890.300.211-4",
                    razonSocialCliente = "Serviteca El Dorado S.A.S.",
                    direccionRecogida = "Av. 3N # 45-12",
                    departamento = "Valle del Cauca",
                    ciudad = "Cali",
                    vendedor = "Carlos Pérez",
                    tipoDevolucion = "POS-Posconsumo",
                    observacionesIniciales = "Recoger en bodega posterior.",
                    estado = OrdenStatus.EN_ATENCION,
                    fechaHoraInicioAtencion = "2026-11-15 09:30:00",
                    articulos = articulos
                ),
                selectedStatus = OrdenStatus.EN_ATENCION,
                fechaHoraInicioAtencion = "2026-11-15 09:30:00",
                articulos = articulos,
                totalsSummary = CollectionTotalsSummary(
                    totalUnidades = 12,
                    totalPesoKg = 174.0,
                    totalValorCompra = 660000.0,
                    hasMissingCantidadActa = false
                )
            ),
            onBackClick = {},
            onEvent = {}
        )
    }
}
