package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class ActiveDateTarget {
    START,
    END
}

/**
 * Panel modal de filtros de búsqueda para la Bandeja de Órdenes Asignadas (HU-005 CA 1).
 * Soporta rango de fechas (máximo 1 mes), estado, cliente (NIT/Razón Social), solicitud y OM.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BupasOrderFilterSheet(
    filter: OrderSearchFilter,
    dateRangeError: String?,
    onStartDateSelected: (String?) -> Unit,
    onEndDateSelected: (String?) -> Unit,
    onStatusSelected: (OrdenStatus?) -> Unit,
    onClientQueryChanged: (String) -> Unit,
    onSolicitudChanged: (String) -> Unit,
    onOmChanged: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeDateTarget by remember { mutableStateOf<ActiveDateTarget?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        BupasOrderFilterSheetContent(
            filter = filter,
            dateRangeError = dateRangeError,
            onPickStartDate = { activeDateTarget = ActiveDateTarget.START },
            onPickEndDate = { activeDateTarget = ActiveDateTarget.END },
            onStatusSelected = onStatusSelected,
            onClientQueryChanged = onClientQueryChanged,
            onSolicitudChanged = onSolicitudChanged,
            onOmChanged = onOmChanged,
            onApplyFilters = onApplyFilters,
            onClearFilters = onClearFilters,
            onDismiss = onDismiss
        )
    }

    activeDateTarget?.let { target ->
        val initialIso = when (target) {
            ActiveDateTarget.START -> filter.startDate
            ActiveDateTarget.END -> filter.endDate
        }
        BupasDatePickerModal(
            initialDateIso = initialIso,
            onDateConfirmed = { selectedIso ->
                when (target) {
                    ActiveDateTarget.START -> onStartDateSelected(selectedIso)
                    ActiveDateTarget.END -> onEndDateSelected(selectedIso)
                }
                activeDateTarget = null
            },
            onDismiss = { activeDateTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BupasOrderFilterSheetContent(
    filter: OrderSearchFilter,
    dateRangeError: String?,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit,
    onStatusSelected: (OrdenStatus?) -> Unit,
    onClientQueryChanged: (String) -> Unit,
    onSolicitudChanged: (String) -> Unit,
    onOmChanged: (String) -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = BupasTheme.spacing.md,
                vertical = BupasTheme.spacing.sm
            ),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.md)
    ) {
        FilterHeaderRow(onDismiss = onDismiss)
        DateRangeSection(
            startDate = filter.startDate,
            endDate = filter.endDate,
            dateRangeError = dateRangeError,
            onPickStartDate = onPickStartDate,
            onPickEndDate = onPickEndDate
        )
        StatusFilterSection(
            selectedStatus = filter.status,
            onStatusSelected = onStatusSelected
        )
        TextFilterFieldsSection(
            filter = filter,
            onClientQueryChanged = onClientQueryChanged,
            onSolicitudChanged = onSolicitudChanged,
            onOmChanged = onOmChanged
        )
        FilterActionButtonsRow(
            isApplyEnabled = dateRangeError == null,
            onClearFilters = onClearFilters,
            onApplyFilters = onApplyFilters
        )
        Spacer(modifier = Modifier.height(BupasTheme.spacing.md))
    }
}

@Composable
private fun FilterHeaderRow(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filtrar órdenes asignadas",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar filtros"
            )
        }
    }
}

@Composable
private fun DateRangeSection(
    startDate: String?,
    endDate: String?,
    dateRangeError: String?,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)) {
        Text(
            text = "Rango de fechas (máximo 1 mes)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)
        ) {
            DatePickerTriggerButton(
                label = "Desde",
                dateValue = startDate,
                onClick = onPickStartDate,
                modifier = Modifier.weight(1f)
            )
            DatePickerTriggerButton(
                label = "Hasta",
                dateValue = endDate,
                onClick = onPickEndDate,
                modifier = Modifier.weight(1f)
            )
        }
        if (!dateRangeError.isNullOrBlank()) {
            Text(
                text = dateRangeError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DatePickerTriggerButton(
    label: String,
    dateValue: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = BupasTheme.spacing.minTouchTarget),
        shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.padding(end = BupasTheme.spacing.xs)
        )
        Text(
            text = dateValue ?: label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusFilterSection(
    selectedStatus: OrdenStatus?,
    onStatusSelected: (OrdenStatus?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)) {
        Text(
            text = "Estado de la orden",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
        ) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                label = { Text("Todos") }
            )
            OrdenStatus.entries.forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = {
                        onStatusSelected(if (selectedStatus == status) null else status)
                    },
                    label = { Text(status.displayName) }
                )
            }
        }
    }
}

@Composable
private fun TextFilterFieldsSection(
    filter: OrderSearchFilter,
    onClientQueryChanged: (String) -> Unit,
    onSolicitudChanged: (String) -> Unit,
    onOmChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm)) {
        BupasTextField(
            value = filter.clientQuery,
            onValueChange = onClientQueryChanged,
            label = "Cliente (NIT o Razón Social)",
            placeholder = "Ej: Serviteca o 890.300"
        )
        BupasTextField(
            value = filter.numeroSolicitud,
            onValueChange = onSolicitudChanged,
            label = "Número de solicitud",
            placeholder = "Ej: SOL-2026-00148"
        )
        BupasTextField(
            value = filter.identificadorOm,
            onValueChange = onOmChanged,
            label = "Identificador OM",
            placeholder = "Ej: OM-889450"
        )
    }
}

@Composable
private fun FilterActionButtonsRow(
    isApplyEnabled: Boolean,
    onClearFilters: () -> Unit,
    onApplyFilters: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BupasTextButton(
            text = "Limpiar",
            onClick = onClearFilters,
            modifier = Modifier.weight(0.4f)
        )
        BupasPrimaryButton(
            text = "Aplicar filtros",
            onClick = onApplyFilters,
            enabled = isApplyEnabled,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BupasDatePickerModal(
    initialDateIso: String?,
    onDateConfirmed: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDateIso) {
        initialDateIso?.let {
            runCatching {
                LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            BupasTextButton(
                text = "Aceptar",
                onClick = {
                    val selected = datePickerState.selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                    }
                    onDateConfirmed(selected)
                }
            )
        },
        dismissButton = {
            BupasTextButton(
                text = "Cancelar",
                onClick = onDismiss
            )
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Preview(showBackground = true)
@Composable
private fun BupasOrderFilterSheetContentPreview() {
    BupasTheme {
        BupasOrderFilterSheetContent(
            filter = OrderSearchFilter(
                startDate = "2026-10-01",
                endDate = "2026-11-15",
                status = OrdenStatus.ORDEN_ASIGNADA
            ),
            dateRangeError = "El rango de fechas no puede superar 1 mes.",
            onPickStartDate = {},
            onPickEndDate = {},
            onStatusSelected = {},
            onClientQueryChanged = {},
            onSolicitudChanged = {},
            onOmChanged = {},
            onApplyFilters = {},
            onClearFilters = {},
            onDismiss = {}
        )
    }
}
