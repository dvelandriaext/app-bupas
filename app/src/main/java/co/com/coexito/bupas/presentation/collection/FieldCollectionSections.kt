package co.com.coexito.bupas.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasQuantityActaField
import co.com.coexito.bupas.core.designsystem.components.BupasSecondaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasSectionCard
import co.com.coexito.bupas.core.designsystem.components.BupasStatusBadge
import co.com.coexito.bupas.core.designsystem.components.BupasSummaryTotalsBar
import co.com.coexito.bupas.core.designsystem.components.BupasTextButton
import co.com.coexito.bupas.core.designsystem.components.BupasTextField
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente
import co.com.coexito.bupas.domain.usecase.CollectionTotalsSummary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CollectionClientSummaryCard(order: OrdenRecoleccion) {
    BupasSectionCard(
        title = order.razonSocialCliente,
        subtitle = "NIT: ${order.nitCliente} • ${order.numeroSolicitud}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${order.direccionRecogida} (${order.ciudad})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            BupasStatusBadge(status = order.estado)
        }
        if (order.estado.isReadOnlyLocked) {
            ReadOnlyOrderBanner(order = order)
        }
    }
}

@Composable
private fun ReadOnlyOrderBanner(order: OrdenRecoleccion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = BupasTheme.spacing.xs),
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
                append("Orden en modo solo lectura (${order.estado.displayName}).")
                order.numeroActaGenerada?.let { append(" Acta Respel: $it.") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionStatusSection(
    selectedStatus: OrdenStatus,
    isReadOnly: Boolean,
    detalleNovedad: String,
    detalleNovedadError: String?,
    onStatusSelected: (OrdenStatus) -> Unit,
    onDetalleNovedadChanged: (String) -> Unit
) {
    BupasSectionCard(
        title = "Estado de atención en campo",
        subtitle = if (isReadOnly) "Estado final bloqueado" else "Seleccione el resultado de la visita"
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
                    label = {
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
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
fun CollectionArticlesSection(
    articulos: List<ArticuloBupas>,
    totalsSummary: CollectionTotalsSummary,
    missingCodigos: Set<String>,
    isReadOnly: Boolean,
    onQuantityChanged: (String, Int?) -> Unit
) {
    BupasSectionCard(
        title = "Artículos BUPAS",
        subtitle = "Registre la Cantidad Acta (0 si no se recolectó)"
    ) {
        articulos.forEachIndexed { index, articulo ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            ArticleCollectionRow(
                articulo = articulo,
                isError = articulo.codigoArticulo in missingCodigos,
                isReadOnly = isReadOnly,
                onQuantityChanged = { qty -> onQuantityChanged(articulo.codigoArticulo, qty) }
            )
        }

        BupasSummaryTotalsBar(
            totalUnidades = totalsSummary.totalUnidades,
            totalPesoKg = totalsSummary.totalPesoKg,
            totalValorCompra = totalsSummary.totalValorCompra
        )
    }
}

@Composable
private fun ArticleCollectionRow(
    articulo: ArticuloBupas,
    isError: Boolean,
    isReadOnly: Boolean,
    onQuantityChanged: (Int?) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = BupasTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        Text(
            text = "${articulo.codigoArticulo} • ${articulo.descripcion}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Solicitado: ${articulo.cantidadInicial} uds • " +
                "Peso unit.: ${String.format(Locale.US, "%.1f", articulo.pesoUnitarioKg)} kg • " +
                "Valor unit.: ${currencyFormat.format(articulo.valorCompraUnitario)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cantidad Acta *",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", articulo.pesoTotalKg)} kg • " +
                        currencyFormat.format(articulo.valorCompraTotal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            BupasQuantityActaField(
                quantityActa = articulo.cantidadActa,
                onQuantityChange = onQuantityChanged,
                readOnly = isReadOnly,
                isError = isError
            )
        }
        if (isError) {
            Text(
                text = "Campo requerido (ingrese 0 si no recolectó)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun CollectionProvenientesSection(
    aplicaProvenientes: Boolean,
    provenientes: List<TerceroProveniente>,
    isReadOnly: Boolean,
    onAplicaChanged: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: (String) -> Unit
) {
    BupasSectionCard(
        title = "Terceros Provenientes",
        subtitle = if (aplicaProvenientes) "${provenientes.size} tercero(s) registrado(s)" else "Opcional según origen"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿Aplica registro de provenientes?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = aplicaProvenientes,
                onCheckedChange = onAplicaChanged,
                enabled = !isReadOnly
            )
        }

        if (aplicaProvenientes) {
            provenientes.forEach { prov ->
                ProvenienteItemRow(
                    proveniente = prov,
                    isReadOnly = isReadOnly,
                    onRemoveClick = { onRemoveClick(prov.id) }
                )
            }
            if (!isReadOnly) {
                BupasSecondaryButton(
                    text = "+ Agregar Provenientes",
                    onClick = onAddClick,
                    leadingIcon = Icons.Default.Add
                )
            }
        }
    }
}

@Composable
private fun ProvenienteItemRow(
    proveniente: TerceroProveniente,
    isReadOnly: Boolean,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = BupasTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${proveniente.razonSocial} (${proveniente.nit})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${proveniente.tipoBateria.label}: ${proveniente.cantidadBaterias} uds • " +
                    "${proveniente.telefono} • ${proveniente.correoElectronico}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isReadOnly) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar proveniente",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CollectionResponsibleSection(
    nombreResponsable: String,
    identificacionResponsable: String,
    observacionesCampo: String,
    nombreError: String?,
    identificacionError: String?,
    isReadOnly: Boolean,
    onNombreChanged: (String) -> Unit,
    onIdentificacionChanged: (String) -> Unit,
    onObservacionesChanged: (String) -> Unit
) {
    BupasSectionCard(
        title = "Responsable que entrega",
        subtitle = "Datos obligatorios para el cierre y Acta Respel"
    ) {
        BupasTextField(
            value = nombreResponsable,
            onValueChange = onNombreChanged,
            label = "Nombre completo",
            placeholder = "Ej. Juan Carlos Gómez",
            isRequired = true,
            isError = !nombreError.isNullOrBlank(),
            errorMessage = nombreError,
            readOnly = isReadOnly
        )
        BupasTextField(
            value = identificacionResponsable,
            onValueChange = onIdentificacionChanged,
            label = "Documento / Cédula",
            placeholder = "Ej. 1020304050",
            isRequired = true,
            isError = !identificacionError.isNullOrBlank(),
            errorMessage = identificacionError,
            readOnly = isReadOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        BupasTextField(
            value = observacionesCampo,
            onValueChange = onObservacionesChanged,
            label = "Observaciones en campo",
            placeholder = "Novedades o notas adicionales de la recolección",
            readOnly = isReadOnly,
            singleLine = false,
            minLines = 2
        )
    }
}

@Composable
fun CollectionActionsSection(
    isNoRecolectada: Boolean,
    isReadOnly: Boolean,
    isSaving: Boolean,
    onSaveDraftClick: () -> Unit,
    onFinalizeClick: () -> Unit
) {
    if (isReadOnly) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
    ) {
        if (!isNoRecolectada) {
            BupasSecondaryButton(
                text = "Guardar avance",
                onClick = onSaveDraftClick,
                enabled = !isSaving
            )
        }
        BupasPrimaryButton(
            text = if (isNoRecolectada) "Registrar No Recolectada" else "Finalizar Recolección",
            onClick = onFinalizeClick,
            isLoading = isSaving,
            enabled = !isSaving
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddProvenienteDialog(
    formState: TerceroProvenienteFormState,
    onEvent: (FieldCollectionUiEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(FieldCollectionUiEvent.OnDismissAddProvenienteDialog) },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Agregar tercero proveniente",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
            ) {
                BupasTextField(
                    value = formState.nit,
                    onValueChange = { onEvent(FieldCollectionUiEvent.OnProvenienteNitChanged(it)) },
                    label = "NIT / Cédula",
                    isRequired = true,
                    isError = !formState.nitError.isNullOrBlank(),
                    errorMessage = formState.nitError
                )
                BupasTextField(
                    value = formState.razonSocial,
                    onValueChange = { onEvent(FieldCollectionUiEvent.OnProvenienteRazonSocialChanged(it)) },
                    label = "Razón Social / Nombre",
                    isRequired = true,
                    isError = !formState.razonSocialError.isNullOrBlank(),
                    errorMessage = formState.razonSocialError
                )
                Text(
                    text = "Tipo de batería *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)) {
                    TipoBateriaProveniente.entries.forEach { tipo ->
                        FilterChip(
                            selected = formState.tipoBateria == tipo,
                            onClick = { onEvent(FieldCollectionUiEvent.OnProvenienteTipoChanged(tipo)) },
                            label = { Text(text = tipo.label) }
                        )
                    }
                }
                BupasTextField(
                    value = formState.cantidadBaterias,
                    onValueChange = { onEvent(FieldCollectionUiEvent.OnProvenienteCantidadChanged(it)) },
                    label = "Cantidad de baterías",
                    isRequired = true,
                    isError = !formState.cantidadError.isNullOrBlank(),
                    errorMessage = formState.cantidadError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                BupasTextField(
                    value = formState.telefono,
                    onValueChange = { onEvent(FieldCollectionUiEvent.OnProvenienteTelefonoChanged(it)) },
                    label = "Teléfono",
                    isRequired = true,
                    isError = !formState.telefonoError.isNullOrBlank(),
                    errorMessage = formState.telefonoError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                BupasTextField(
                    value = formState.correoElectronico,
                    onValueChange = { onEvent(FieldCollectionUiEvent.OnProvenienteCorreoChanged(it)) },
                    label = "Correo electrónico",
                    isRequired = true,
                    isError = !formState.correoError.isNullOrBlank(),
                    errorMessage = formState.correoError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        },
        confirmButton = {
            BupasTextButton(
                text = "Agregar",
                onClick = { onEvent(FieldCollectionUiEvent.OnConfirmAddProveniente) }
            )
        },
        dismissButton = {
            BupasTextButton(
                text = "Cancelar",
                onClick = { onEvent(FieldCollectionUiEvent.OnDismissAddProvenienteDialog) }
            )
        }
    )
}
