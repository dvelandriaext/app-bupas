package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import java.text.NumberFormat
import java.util.Locale

/**
 * Tarjeta reutilizable para la Bandeja de Órdenes Asignadas al Conductor (HU-005).
 */
@Composable
fun BupasOrderCard(
    order: OrdenRecoleccion,
    onViewDetailClick: (OrdenRecoleccion) -> Unit,
    modifier: Modifier = Modifier,
    onViewActaClick: ((String) -> Unit)? = null
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, BupasTheme.extendedColors.borderDisabled),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(BupasTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Solicitud: ${order.numeroSolicitud}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "OM Oracle: ${order.identificadorOm}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BupasStatusBadge(status = order.estado)
            }

            Spacer(modifier = Modifier.height(BupasTheme.spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(BupasTheme.spacing.sm))

            Text(
                text = order.razonSocialCliente,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "NIT: ${order.nitCliente} • Fecha: ${order.fechaRecoleccion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${order.direccionRecogida} (${order.ciudad}, ${order.departamento})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = BupasTheme.spacing.xs)
            )

            Spacer(modifier = Modifier.height(BupasTheme.spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Peso total: ${String.format(Locale.US, "%.1f", order.pesoTotalEstimadoKg)} kg",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Valor compra: ${currencyFormatter.format(order.valorCompraTotalEstimado)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)) {
                    if (order.numeroActaGenerada != null && onViewActaClick != null) {
                        BupasTextButton(
                            text = "Ver Acta",
                            onClick = { onViewActaClick(order.numeroActaGenerada) }
                        )
                    }
                    BupasTextButton(
                        text = "Ver detalle",
                        onClick = { onViewDetailClick(order) }
                    )
                }
            }
        }
    }
}

/**
 * Contenedor visual por sección para el flujo de recolección en campo (HU-006).
 */
@Composable
fun BupasSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, BupasTheme.extendedColors.borderDisabled),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(BupasTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            content()
        }
    }
}

/**
 * Barra de totales recalculados dinámicamente en tiempo real para HU-006:
 * Total Unidades, Total (kg) y Total ($).
 */
@Composable
fun BupasSummaryTotalsBar(
    totalUnidades: Int,
    totalPesoKg: Double,
    totalValorCompra: Double,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BupasTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryMetricItem(
                label = "Total Unidades",
                value = totalUnidades.toString()
            )
            SummaryMetricItem(
                label = "Total (kg)",
                value = "${String.format(Locale.US, "%.1f", totalPesoKg)} kg"
            )
            SummaryMetricItem(
                label = "Total ($)",
                value = currencyFormatter.format(totalValorCompra)
            )
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BupasCardsPreview() {
    BupasTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BupasOrderCard(
                order = OrdenRecoleccion(
                    numeroSolicitud = "SOL-2026-00142",
                    identificadorOm = "OM-889412",
                    cedi = "CEDI Cali",
                    fechaRecoleccion = "2026-11-09",
                    nitCliente = "890.300.211-4",
                    razonSocialCliente = "Serviteca El Dorado S.A.S.",
                    direccionRecogida = "Av. 3N # 45-12",
                    departamento = "Valle del Cauca",
                    ciudad = "Cali",
                    vendedor = "Carlos Pérez",
                    tipoDevolucion = "POS-Posconsumo",
                    observacionesIniciales = "Recoger en bodega posterior",
                    estado = OrdenStatus.ORDEN_ASIGNADA
                ),
                onViewDetailClick = {}
            )
            BupasSummaryTotalsBar(
                totalUnidades = 24,
                totalPesoKg = 360.5,
                totalValorCompra = 1440000.0
            )
        }
    }
}
