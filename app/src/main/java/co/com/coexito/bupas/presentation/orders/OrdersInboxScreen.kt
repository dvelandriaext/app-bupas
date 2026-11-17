package co.com.coexito.bupas.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.com.coexito.bupas.BuildConfig
import co.com.coexito.bupas.core.config.AppEnvironment
import co.com.coexito.bupas.core.designsystem.components.BupasAlertModal
import co.com.coexito.bupas.core.designsystem.components.BupasOrderCard
import co.com.coexito.bupas.core.designsystem.components.BupasOrderFilterSheet
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasTextButton
import co.com.coexito.bupas.core.designsystem.components.BupasThemeSettingsDialog
import co.com.coexito.bupas.core.designsystem.components.BupasTopBar
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.core.designsystem.theme.ThemeMode
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus

@Composable
fun OrdersInboxRoute(
    driverSubtitle: String,
    onNavigateToDetail: (String) -> Unit,
    onSafeLogout: () -> Unit,
    onNavigateToActa: (String) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    viewModel: OrdersInboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!uiState.isLoading) {
            viewModel.onEvent(OrdersInboxUiEvent.OnRefreshOrders)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.navigationEffects.collect { effect ->
            when (effect) {
                is OrdersInboxNavigationEffect.NavigateToOrderDetail -> {
                    onNavigateToDetail(effect.numeroSolicitud)
                }
                is OrdersInboxNavigationEffect.NavigateToActaCertificate -> {
                    onNavigateToActa(effect.numeroSolicitud)
                }
            }
        }
    }

    OrdersInboxScreen(
        uiState = uiState,
        driverSubtitle = driverSubtitle,
        onEvent = viewModel::onEvent,
        onThemeSettingsClick = { showThemeDialog = true },
        onSafeLogout = onSafeLogout
    )

    if (showThemeDialog) {
        BupasThemeSettingsDialog(
            selectedMode = themeMode,
            onModeSelected = onThemeModeSelected,
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
fun OrdersInboxScreen(
    uiState: OrdersInboxUiState,
    driverSubtitle: String,
    onEvent: (OrdersInboxUiEvent) -> Unit,
    onSafeLogout: () -> Unit,
    modifier: Modifier = Modifier,
    onThemeSettingsClick: (() -> Unit)? = null
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BupasTopBar(
                title = "Órdenes Asignadas BUPAS",
                subtitle = driverSubtitle,
                environmentLabel = AppEnvironment.fromRaw(BuildConfig.ENVIRONMENT).badgeLabel,
                onThemeSettingsClick = onThemeSettingsClick,
                onLogoutClick = onSafeLogout
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (BuildConfig.USE_FAKE_DATA || uiState.pendingSyncCount > 0) {
                OrdersConnectivityAndSyncBanner(
                    isOfflineMode = uiState.isOfflineMode,
                    pendingSyncCount = uiState.pendingSyncCount,
                    isSyncingPending = uiState.isSyncingPending,
                    onToggleOffline = { onEvent(OrdersInboxUiEvent.OnToggleOfflineMode(it)) },
                    onSyncNowClick = { onEvent(OrdersInboxUiEvent.OnSyncPendingClick) }
                )
            }
            OrdersFilterSummaryHeader(
                visibleCount = uiState.orders.size,
                totalCount = uiState.totalCount,
                hasActiveFilters = uiState.appliedFilter.hasActiveFilters,
                onOpenFilters = { onEvent(OrdersInboxUiEvent.OnOpenFilters) },
                onClearFilters = { onEvent(OrdersInboxUiEvent.OnClearFilters) }
            )
            OrdersInboxContent(
                uiState = uiState,
                onEvent = onEvent
            )
        }
    }

    OrdersInboxOverlays(
        uiState = uiState,
        onEvent = onEvent
    )
}

@Composable
private fun OrdersConnectivityAndSyncBanner(
    isOfflineMode: Boolean,
    pendingSyncCount: Int,
    isSyncingPending: Boolean,
    onToggleOffline: (Boolean) -> Unit,
    onSyncNowClick: () -> Unit
) {
    val containerColor = if (isOfflineMode || pendingSyncCount > 0) {
        BupasTheme.extendedColors.enAtencion.containerColor
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    }
    val contentColor = if (isOfflineMode || pendingSyncCount > 0) {
        BupasTheme.extendedColors.enAtencion.contentColor
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BupasTheme.spacing.md,
                    vertical = BupasTheme.spacing.xs
                ),
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = if (isOfflineMode) {
                                "Modo Sin Conexión (Demo Offline)"
                            } else {
                                "Conectado al Servidor (Online)"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (pendingSyncCount > 0) {
                                "$pendingSyncCount orden(es) en cola local (PENDING_SYNC)"
                            } else {
                                "Catálogo y listas de precios sincronizados en Room"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (BuildConfig.USE_FAKE_DATA) {
                    Switch(
                        checked = !isOfflineMode,
                        onCheckedChange = { isOnline -> onToggleOffline(!isOnline) }
                    )
                }
            }
            if (pendingSyncCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onSyncNowClick,
                        enabled = !isOfflineMode && !isSyncingPending
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.padding(end = BupasTheme.spacing.xs)
                        )
                        Text(
                            text = if (isSyncingPending) {
                                "Sincronizando..."
                            } else if (isOfflineMode) {
                                "Active red para sincronizar ($pendingSyncCount)"
                            } else {
                                "Sincronizar ahora ($pendingSyncCount)"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersFilterSummaryHeader(
    visibleCount: Int,
    totalCount: Int,
    hasActiveFilters: Boolean,
    onOpenFilters: () -> Unit,
    onClearFilters: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BupasTheme.spacing.md,
                    vertical = BupasTheme.spacing.sm
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$visibleCount de $totalCount órdenes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasActiveFilters) {
                    Text(
                        text = "Filtros aplicados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasActiveFilters) {
                    BupasTextButton(
                        text = "Limpiar",
                        onClick = onClearFilters
                    )
                }
                FilledTonalButton(onClick = onOpenFilters) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.padding(end = BupasTheme.spacing.xs)
                    )
                    Text(text = "Filtrar")
                }
            }
        }
    }
}

@Composable
private fun OrdersInboxContent(
    uiState: OrdersInboxUiState,
    onEvent: (OrdersInboxUiEvent) -> Unit
) {
    when {
        uiState.isLoading -> OrdersLoadingState()
        uiState.errorMessage != null -> OrdersErrorState(
            message = uiState.errorMessage,
            onRetry = { onEvent(OrdersInboxUiEvent.OnRefreshOrders) }
        )
        uiState.orders.isEmpty() -> OrdersEmptyState(
            hasActiveFilters = uiState.appliedFilter.hasActiveFilters,
            onClearFilters = { onEvent(OrdersInboxUiEvent.OnClearFilters) }
        )
        else -> OrdersList(
            orders = uiState.orders,
            onEvent = onEvent
        )
    }
}

@Composable
private fun OrdersList(
    orders: List<OrdenRecoleccion>,
    onEvent: (OrdersInboxUiEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(BupasTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
    ) {
        items(
            items = orders,
            key = { it.numeroSolicitud }
        ) { order ->
            BupasOrderCard(
                order = order,
                onViewDetailClick = {
                    onEvent(OrdersInboxUiEvent.OnOrderDetailClick(order.numeroSolicitud))
                },
                onViewActaClick = if (order.numeroActaGenerada != null) {
                    { onEvent(OrdersInboxUiEvent.OnViewActaClick(order)) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun OrdersLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Consultando órdenes asignadas...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrdersErrorState(
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

@Composable
private fun OrdersEmptyState(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(BupasTheme.spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasActiveFilters) {
                    "No se encontraron órdenes con los filtros seleccionados."
                } else {
                    "No tienes órdenes de recolección asignadas."
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(BupasTheme.spacing.xs))
                BupasPrimaryButton(
                    text = "Limpiar filtros",
                    onClick = onClearFilters
                )
            }
        }
    }
}

@Composable
private fun OrdersInboxOverlays(
    uiState: OrdersInboxUiState,
    onEvent: (OrdersInboxUiEvent) -> Unit
) {
    if (uiState.isFilterSheetVisible) {
        BupasOrderFilterSheet(
            filter = uiState.draftFilter,
            dateRangeError = uiState.dateRangeError,
            onStartDateSelected = { onEvent(OrdersInboxUiEvent.OnStartDateSelected(it)) },
            onEndDateSelected = { onEvent(OrdersInboxUiEvent.OnEndDateSelected(it)) },
            onStatusSelected = { onEvent(OrdersInboxUiEvent.OnStatusSelected(it)) },
            onClientQueryChanged = { onEvent(OrdersInboxUiEvent.OnClientQueryChanged(it)) },
            onSolicitudChanged = { onEvent(OrdersInboxUiEvent.OnSolicitudChanged(it)) },
            onOmChanged = { onEvent(OrdersInboxUiEvent.OnOmChanged(it)) },
            onApplyFilters = { onEvent(OrdersInboxUiEvent.OnApplyFilters) },
            onClearFilters = { onEvent(OrdersInboxUiEvent.OnClearFilters) },
            onDismiss = { onEvent(OrdersInboxUiEvent.OnDismissFilters) }
        )
    }

    uiState.selectedActaPreview?.let { order ->
        BupasAlertModal(
            title = "Acta Respel ${order.numeroActaGenerada.orEmpty()}",
            message = "Cliente: ${order.razonSocialCliente} (${order.nitCliente})\n" +
                "Solicitud: ${order.numeroSolicitud} • OM: ${order.identificadorOm}\n" +
                "Unidades recolectadas: ${order.totalUnidadesActa}\n\n" +
                "La descarga y visualización PDF completa estará disponible en HU-007.",
            onDismiss = { onEvent(OrdersInboxUiEvent.OnDismissActaPreview) }
        )
    }

    uiState.syncFeedbackMessage?.let { syncMessage ->
        BupasAlertModal(
            title = "Sincronización BUPAS",
            message = syncMessage,
            onDismiss = { onEvent(OrdersInboxUiEvent.OnDismissSyncFeedback) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrdersInboxScreenPreview() {
    BupasTheme {
        OrdersInboxScreen(
            uiState = OrdersInboxUiState(
                isLoading = false,
                totalCount = 1,
                orders = listOf(
                    OrdenRecoleccion(
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
                        observacionesIniciales = "Recoger en bodega posterior",
                        estado = OrdenStatus.ORDEN_ASIGNADA,
                        articulos = listOf(
                            ArticuloBupas(
                                codigoArticulo = "BUP-AUTO-42",
                                descripcion = "Batería Usada Auto 42R",
                                cantidadInicial = 15,
                                cantidadActa = null,
                                pesoUnitarioKg = 14.5,
                                valorCompraUnitario = 55000.0
                            )
                        )
                    )
                )
            ),
            driverSubtitle = "Juan Rodríguez • CEDI Cali (Conductor en Campo)",
            onEvent = {},
            onSafeLogout = {}
        )
    }
}
