package co.com.coexito.bupas.presentation.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasQuantityActaField
import co.com.coexito.bupas.core.designsystem.components.BupasSecondaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasSectionCard
import co.com.coexito.bupas.core.designsystem.components.BupasSummaryTotalsBar
import co.com.coexito.bupas.core.designsystem.components.BupasTextField
import co.com.coexito.bupas.core.designsystem.components.SignaturePadWidget
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.usecase.CollectionTotalsSummary
import java.text.NumberFormat
import java.util.Locale

/**
 * Grilla de artículos BUPAS de la lista de precios del cliente (incluyendo referencias con
 * cantidad inicial `0`), recálculo dinámico de peso/valor y captura de Cantidad Acta (CA 4, CA 5, R2.3).
 */
@Composable
fun CollectionArticlesSection(
    articulos: List<ArticuloBupas>,
    totalsSummary: CollectionTotalsSummary,
    missingCodigos: Set<String>,
    invalidCodigos: Map<String, String>,
    isReadOnly: Boolean,
    onQuantityChanged: (String, Int?) -> Unit,
    onRawQuantityChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    BupasSectionCard(
        title = "Registro de recolección en campo",
        subtitle = "Diligencie Cantidad Acta en cada referencia BUPAS (entero >= 0)",
        modifier = modifier
    ) {
        articulos.forEachIndexed { index, articulo ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            val rowError = invalidCodigos[articulo.codigoArticulo]
                ?: if (articulo.codigoArticulo in missingCodigos) {
                    "Campo requerido (ingrese 0 si no recolectó)"
                } else {
                    null
                }
            ArticleCollectionRow(
                articulo = articulo,
                errorMessage = rowError,
                isReadOnly = isReadOnly,
                onQuantityChanged = { qty -> onQuantityChanged(articulo.codigoArticulo, qty) },
                onRawQuantityChanged = { raw -> onRawQuantityChanged(articulo.codigoArticulo, raw) }
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
    errorMessage: String?,
    isReadOnly: Boolean,
    onQuantityChanged: (Int?) -> Unit,
    onRawQuantityChanged: (String) -> Unit
) {
    val isError = !errorMessage.isNullOrBlank()
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
            text = "Cantidad inicial: ${articulo.cantidadInicial} uds • " +
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cantidad Acta *",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
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
                onRawQuantityChange = onRawQuantityChanged,
                readOnly = isReadOnly,
                isError = isError
            )
        }
        if (isError) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Sección de Confirmación Recolección con datos obligatorios de quien entrega,
 * observaciones finales y firma digital (`SignaturePadWidget`) (HU-006 CA 7).
 */
@Composable
fun CollectionConfirmationSection(
    nombreResponsable: String,
    identificacionResponsable: String,
    observacionesCampo: String,
    firmaDigitalBase64: String?,
    numeroActaGenerada: String?,
    nombreError: String?,
    identificacionError: String?,
    firmaError: String?,
    isReadOnly: Boolean,
    onNombreChanged: (String) -> Unit,
    onIdentificacionChanged: (String) -> Unit,
    onObservacionesChanged: (String) -> Unit,
    onSignatureCaptured: (String) -> Unit,
    onSignatureCleared: () -> Unit,
    modifier: Modifier = Modifier,
    canViewActaPdf: Boolean = true,
    onViewActaClick: (() -> Unit)? = null
) {
    BupasSectionCard(
        title = "Confirmación Recolección",
        subtitle = numeroActaGenerada?.let { "Acta Respel generada: $it" }
            ?: "Datos obligatorios de entrega y firma digital",
        modifier = modifier
    ) {
        BupasTextField(
            value = nombreResponsable,
            onValueChange = onNombreChanged,
            label = "Responsable que entrega",
            placeholder = "Ej. Juan Carlos Gómez",
            isRequired = true,
            isError = !nombreError.isNullOrBlank(),
            errorMessage = nombreError,
            readOnly = isReadOnly
        )
        BupasTextField(
            value = identificacionResponsable,
            onValueChange = onIdentificacionChanged,
            label = "Documento Identificación",
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
            label = "Observaciones finales de recolección",
            placeholder = "Novedades o notas finales de la entrega en campo",
            readOnly = isReadOnly,
            singleLine = false,
            minLines = 2
        )
        SignaturePadWidget(
            firmaDigitalBase64 = firmaDigitalBase64,
            onSignatureCaptured = onSignatureCaptured,
            onSignatureCleared = onSignatureCleared,
            label = "Firma digital de quien entrega",
            isRequired = true,
            readOnly = isReadOnly,
            isError = !firmaError.isNullOrBlank(),
            errorMessage = firmaError
        )
        if (!numeroActaGenerada.isNullOrBlank() && onViewActaClick != null) {
            BupasSecondaryButton(
                text = "Ver Acta ($numeroActaGenerada.pdf)",
                onClick = onViewActaClick,
                enabled = canViewActaPdf
            )
        }
    }
}

/**
 * Acciones de guardado parcial y cierre definitivo de recolección (HU-006 CA 8).
 */
@Composable
fun CollectionActionsSection(
    isNoRecolectada: Boolean,
    isReadOnly: Boolean,
    isSaving: Boolean,
    onSaveDraftClick: () -> Unit,
    onFinalizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isReadOnly) return

    Column(
        modifier = modifier.fillMaxWidth(),
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
            text = if (isNoRecolectada) "Registrar No Recolectada" else "Finalizar recolección",
            onClick = onFinalizeClick,
            isLoading = isSaving,
            enabled = !isSaving
        )
    }
}
