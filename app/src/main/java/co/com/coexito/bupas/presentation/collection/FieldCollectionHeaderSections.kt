package co.com.coexito.bupas.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.com.coexito.bupas.core.designsystem.components.BupasSectionCard
import co.com.coexito.bupas.core.designsystem.components.BupasStatusBadge
import co.com.coexito.bupas.core.designsystem.components.BupasTextField
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus

/**
 * Sección de datos generales de la orden en modo solo lectura (HU-006 CA 1 y CA 8).
 */
@Composable
fun CollectionGeneralOrderSection(
    order: OrdenRecoleccion,
    modifier: Modifier = Modifier
) {
    BupasSectionCard(
        title = "Datos generales de la orden",
        subtitle = "Fecha programada: ${order.fechaRecoleccion}",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estado registrado",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BupasStatusBadge(status = order.estado)
        }
        ReadOnlyKeyValueGrid(
            leftLabel = "CEDI",
            leftValue = order.cedi,
            rightLabel = "Nro. Solicitud",
            rightValue = order.numeroSolicitud
        )
        ReadOnlyKeyValueGrid(
            leftLabel = "Identificador OM",
            leftValue = order.identificadorOm,
            rightLabel = "Vendedor",
            rightValue = order.vendedor
        )
        order.numeroActaGenerada?.let { acta ->
            ReadOnlyFieldBlock(
                label = "Acta Respel generada",
                value = acta,
                highlightPrimary = true
            )
        }
        if (order.estado.isReadOnlyLocked) {
            ReadOnlyOrderBanner(order = order)
        }
    }
}

/**
 * Sección de datos del cliente y ubicación de recogida en modo solo lectura (HU-006 CA 2).
 */
@Composable
fun CollectionClientInfoSection(
    order: OrdenRecoleccion,
    modifier: Modifier = Modifier
) {
    BupasSectionCard(
        title = "Información del cliente",
        subtitle = "NIT: ${order.nitCliente}",
        modifier = modifier
    ) {
        ReadOnlyFieldBlock(label = "Cliente", value = order.razonSocialCliente)
        ReadOnlyFieldBlock(label = "Dirección de recogida", value = order.direccionRecogida)
        ReadOnlyKeyValueGrid(
            leftLabel = "Departamento",
            leftValue = order.departamento,
            rightLabel = "Ciudad",
            rightValue = order.ciudad
        )
        ReadOnlyFieldBlock(label = "Tipo de Devolución", value = order.tipoDevolucion)
        ReadOnlyFieldBlock(
            label = "Observaciones",
            value = order.observacionesIniciales.ifBlank { "Sin observaciones iniciales" }
        )
    }
}

/**
 * Selector editable de Estado Orden, trazabilidad de inicio/fin de atención,
 * indicador de borrador offline y captura obligatoria de Detalle Novedad (CA 2, CA 3, R2.1, R2.5).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionStatusSection(
    selectedStatus: OrdenStatus,
    fechaHoraInicioAtencion: String?,
    fechaHoraFinalizacion: String?,
    isOfflineDraftSaved: Boolean,
    isReadOnly: Boolean,
    detalleNovedad: String,
    detalleNovedadError: String?,
    onStatusSelected: (OrdenStatus) -> Unit,
    onDetalleNovedadChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BupasSectionCard(
        title = "Estado Orden",
        subtitle = if (isReadOnly) "Estado definitivo bloqueado" else "Seleccione el estado de atención en campo",
        modifier = modifier
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
        ) {
            OrdenStatus.selectableByDriver().forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusSelected(status) },
                    enabled = !isReadOnly,
                    modifier = Modifier.defaultMinSize(minHeight = BupasTheme.spacing.minTouchTarget),
                    label = {
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }
        if (selectedStatus == OrdenStatus.ORDEN_ASIGNADA && !isReadOnly) {
            Text(
                text = "Seleccione En Atención o Recolección Parcial para habilitar el registro en campo, o No Recolectada para reportar novedad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AttentionTimestampsBlock(
            fechaHoraInicio = fechaHoraInicioAtencion,
            fechaHoraFinalizacion = fechaHoraFinalizacion
        )
        if (isOfflineDraftSaved) {
            OfflineDraftBanner()
        }
        if (selectedStatus == OrdenStatus.NO_RECOLECTADA) {
            BupasTextField(
                value = detalleNovedad,
                onValueChange = onDetalleNovedadChanged,
                label = "Detalle Novedad",
                placeholder = "Describa el motivo por el cual no se realizó la recolección",
                isRequired = true,
                isError = !detalleNovedadError.isNullOrBlank(),
                errorMessage = detalleNovedadError,
                readOnly = isReadOnly,
                singleLine = false,
                minLines = 3
            )
        }
    }
}

@Composable
private fun AttentionTimestampsBlock(
    fechaHoraInicio: String?,
    fechaHoraFinalizacion: String?
) {
    if (fechaHoraInicio.isNullOrBlank() && fechaHoraFinalizacion.isNullOrBlank()) return

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)) {
        if (!fechaHoraInicio.isNullOrBlank()) {
            TimestampRow(label = "Inicio de atención:", value = fechaHoraInicio)
        }
        if (!fechaHoraFinalizacion.isNullOrBlank()) {
            TimestampRow(label = "Finalización de atención:", value = fechaHoraFinalizacion)
        }
    }
}

@Composable
private fun TimestampRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OfflineDraftBanner() {
    Surface(
        color = BupasTheme.extendedColors.enAtencion.containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(BupasTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = BupasTheme.extendedColors.enAtencion.contentColor
            )
            Text(
                text = "Avance resguardado localmente sin conexión (pendiente de sincronizar).",
                style = MaterialTheme.typography.bodySmall,
                color = BupasTheme.extendedColors.enAtencion.contentColor
            )
        }
    }
}

@Composable
private fun ReadOnlyOrderBanner(order: OrdenRecoleccion) {
    Surface(
        color = BupasTheme.extendedColors.readOnlySurface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(BupasTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = buildString {
                    append("Orden bloqueada en modo solo lectura (${order.estado.displayName}).")
                    order.numeroActaGenerada?.let { append(" Acta: $it.") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadOnlyKeyValueGrid(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
    ) {
        ReadOnlyFieldBlock(
            label = leftLabel,
            value = leftValue,
            modifier = Modifier.weight(1f)
        )
        ReadOnlyFieldBlock(
            label = rightLabel,
            value = rightValue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReadOnlyFieldBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightPrimary: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (highlightPrimary) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (highlightPrimary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
