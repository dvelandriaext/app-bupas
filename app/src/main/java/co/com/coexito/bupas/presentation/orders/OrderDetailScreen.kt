package co.com.coexito.bupas.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.com.coexito.bupas.BuildConfig
import co.com.coexito.bupas.core.config.AppEnvironment
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasSectionCard
import co.com.coexito.bupas.core.designsystem.components.BupasStatusBadge
import co.com.coexito.bupas.core.designsystem.components.BupasSummaryTotalsBar
import co.com.coexito.bupas.core.designsystem.components.BupasTopBar
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import java.util.Locale

@Composable
fun OrderDetailRoute(
    onBackClick: () -> Unit,
    onNavigateToFieldCollection: (String) -> Unit = {},
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!uiState.isLoading) {
            viewModel.onEvent(OrderDetailUiEvent.OnRetry)
        }
    }

    OrderDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onNavigateToFieldCollection = onNavigateToFieldCollection,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun OrderDetailScreen(
    uiState: OrderDetailUiState,
    onBackClick: () -> Unit,
    onEvent: (OrderDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToFieldCollection: (String) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BupasTopBar(
                title = uiState.order?.numeroSolicitud ?: "Detalle de Orden",
                subtitle = uiState.order?.identificadorOm?.let { "OM Oracle: $it" },
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
                uiState.isLoading -> DetailLoadingState()
                uiState.errorMessage != null -> DetailErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onEvent(OrderDetailUiEvent.OnRetry) }
                )
                uiState.order != null -> OrderDetailSectionsContent(
                    order = uiState.order,
                    canStartAttention = uiState.canStartAttention,
                    onStartAttentionClick = {
                        onNavigateToFieldCollection(uiState.order.numeroSolicitud)
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderDetailSectionsContent(
    order: OrdenRecoleccion,
    canStartAttention: Boolean,
    onStartAttentionClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(BupasTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
    ) {
        item {
            GeneralInfoSection(order = order)
        }
        item {
            FieldCollectionSection(order = order)
        }
        item {
            ProvenientesSection(order = order)
        }
        item {
            ConfirmationSection(
                order = order,
                canStartAttention = canStartAttention,
                onStartAttentionClick = onStartAttentionClick
            )
        }
    }
}

@Composable
private fun GeneralInfoSection(order: OrdenRecoleccion) {
    BupasSectionCard(
        title = "Información general",
        subtitle = "CEDI: ${order.cedi} • Fecha: ${order.fechaRecoleccion}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estado actual",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BupasStatusBadge(status = order.estado)
        }
        DetailLine(label = "Cliente", value = order.razonSocialCliente)
        DetailLine(label = "NIT", value = order.nitCliente)
        DetailLine(label = "Dirección", value = "${order.direccionRecogida} (${order.ciudad}, ${order.departamento})")
        DetailLine(label = "Tipo devolución", value = order.tipoDevolucion)
        DetailLine(label = "Vendedor", value = order.vendedor)
        DetailLine(label = "Observaciones", value = order.observacionesIniciales)
    }
}

@Composable
private fun FieldCollectionSection(order: OrdenRecoleccion) {
    BupasSectionCard(
        title = "Registro de recolección en campo",
        subtitle = "${order.articulos.size} artículo(s) asociado(s)"
    ) {
        order.articulos.forEach { articulo ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BupasTheme.spacing.xs)
            ) {
                Text(
                    text = "${articulo.codigoArticulo} • ${articulo.descripcion}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Cantidad inicial: ${articulo.cantidadInicial} • " +
                        "Cantidad Acta: ${articulo.cantidadActa ?: "Pendiente"} • " +
                        "Peso unit.: ${String.format(Locale.US, "%.1f", articulo.pesoUnitarioKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BupasSummaryTotalsBar(
            totalUnidades = order.totalUnidadesActa,
            totalPesoKg = order.pesoTotalEstimadoKg,
            totalValorCompra = order.valorCompraTotalEstimado
        )
    }
}

@Composable
private fun ProvenientesSection(order: OrdenRecoleccion) {
    BupasSectionCard(
        title = "Provenientes",
        subtitle = if (order.aplicaProvenientes) "Aplica registro de terceros" else "No aplica terceros"
    ) {
        if (order.provenientes.isEmpty()) {
            Text(
                text = "Sin terceros provenientes registrados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            order.provenientes.forEach { prov ->
                DetailLine(
                    label = "${prov.razonSocial} (${prov.nit})",
                    value = "${prov.tipoBateria.label}: ${prov.cantidadBaterias} uds"
                )
            }
        }
    }
}

@Composable
private fun ConfirmationSection(
    order: OrdenRecoleccion,
    canStartAttention: Boolean,
    onStartAttentionClick: () -> Unit
) {
    val actionLabel = when (order.estado) {
        OrdenStatus.ORDEN_ASIGNADA -> "Iniciar atención"
        OrdenStatus.EN_ATENCION, OrdenStatus.RECOLECCION_PARCIAL -> "Continuar atención"
        OrdenStatus.NO_RECOLECTADA, OrdenStatus.RECOLECCION_REALIZADA -> "Ver registro en campo"
    }

    BupasSectionCard(
        title = "Confirmación Recolección",
        subtitle = order.numeroActaGenerada?.let { "Acta generada: $it" } ?: "Pendiente de cierre en campo"
    ) {
        BupasPrimaryButton(
            text = actionLabel,
            onClick = onStartAttentionClick,
            enabled = true
        )
        if (!canStartAttention) {
            Text(
                text = if (order.estado.isReadOnlyLocked) {
                    "Orden en modo solo lectura (${order.estado.displayName})."
                } else {
                    "Orden en curso (${order.estado.displayName}). Puede continuar el registro en campo."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DetailErrorState(
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
private fun OrderDetailScreenPreview() {
    BupasTheme {
        OrderDetailScreen(
            uiState = OrderDetailUiState(
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
            ),
            onBackClick = {},
            onEvent = {}
        )
    }
}
