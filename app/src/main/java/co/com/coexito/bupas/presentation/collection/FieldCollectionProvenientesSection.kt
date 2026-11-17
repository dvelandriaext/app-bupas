package co.com.coexito.bupas.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import co.com.coexito.bupas.core.designsystem.components.BupasSecondaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasSectionCard
import co.com.coexito.bupas.core.designsystem.components.BupasTextButton
import co.com.coexito.bupas.core.designsystem.components.BupasTextField
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.TerceroProveniente
import co.com.coexito.bupas.domain.model.TipoBateriaProveniente

/**
 * Sección de gestión de terceros Provenientes (HU-006 CA 5, CA 6 y caso borde R2.2):
 * selector explícito `Sí`/`No`, validación de bloqueo cuando faltan Cantidades Acta,
 * y acciones de agregar, editar y eliminar provenientes antes del cierre.
 */
@Composable
fun CollectionProvenientesSection(
    aplicaProvenientes: Boolean,
    provenientes: List<TerceroProveniente>,
    provenientesGateError: String?,
    isReadOnly: Boolean,
    onAplicaChanged: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BupasSectionCard(
        title = "Terceros Provenientes",
        subtitle = if (aplicaProvenientes) {
            "${provenientes.size} tercero(s) registrado(s)"
        } else {
            "Indique si las baterías provienen de terceros"
        },
        modifier = modifier
    ) {
        ProvenientesSelectorRow(
            aplicaProvenientes = aplicaProvenientes,
            isReadOnly = isReadOnly,
            onAplicaChanged = onAplicaChanged
        )
        if (!provenientesGateError.isNullOrBlank()) {
            Text(
                text = provenientesGateError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (aplicaProvenientes) {
            ProvenientesListContent(
                provenientes = provenientes,
                isReadOnly = isReadOnly,
                onAddClick = onAddClick,
                onEditClick = onEditClick,
                onRemoveClick = onRemoveClick
            )
        }
    }
}

@Composable
private fun ProvenientesSelectorRow(
    aplicaProvenientes: Boolean,
    isReadOnly: Boolean,
    onAplicaChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "¿Aplica provenientes?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)) {
            FilterChip(
                selected = aplicaProvenientes,
                onClick = { onAplicaChanged(true) },
                enabled = !isReadOnly,
                modifier = Modifier.defaultMinSize(minHeight = BupasTheme.spacing.minTouchTarget),
                label = { Text(text = "Sí", style = MaterialTheme.typography.labelLarge) }
            )
            FilterChip(
                selected = !aplicaProvenientes,
                onClick = { onAplicaChanged(false) },
                enabled = !isReadOnly,
                modifier = Modifier.defaultMinSize(minHeight = BupasTheme.spacing.minTouchTarget),
                label = { Text(text = "No", style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}

@Composable
private fun ProvenientesListContent(
    provenientes: List<TerceroProveniente>,
    isReadOnly: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit
) {
    provenientes.forEachIndexed { index, prov ->
        if (index > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        ProvenienteItemRow(
            proveniente = prov,
            isReadOnly = isReadOnly,
            onEditClick = { onEditClick(prov.id) },
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

@Composable
private fun ProvenienteItemRow(
    proveniente: TerceroProveniente,
    isReadOnly: Boolean,
    onEditClick: () -> Unit,
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
            Row {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.defaultMinSize(
                        minWidth = BupasTheme.spacing.minTouchTarget,
                        minHeight = BupasTheme.spacing.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar proveniente",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.defaultMinSize(
                        minWidth = BupasTheme.spacing.minTouchTarget,
                        minHeight = BupasTheme.spacing.minTouchTarget
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar proveniente",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ProvenienteFormDialog(
    formState: TerceroProvenienteFormState,
    onEvent: (FieldCollectionUiEvent) -> Unit
) {
    val isEditing = formState.isEditMode
    AlertDialog(
        onDismissRequest = { onEvent(FieldCollectionUiEvent.OnDismissAddProvenienteDialog) },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (isEditing) "Editar tercero proveniente" else "Agregar tercero proveniente",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = { ProvenienteFormFields(formState = formState, onEvent = onEvent) },
        confirmButton = {
            BupasTextButton(
                text = if (isEditing) "Guardar" else "Agregar",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProvenienteFormFields(
    formState: TerceroProvenienteFormState,
    onEvent: (FieldCollectionUiEvent) -> Unit
) {
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
                    modifier = Modifier.defaultMinSize(minHeight = BupasTheme.spacing.minTouchTarget),
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
}
