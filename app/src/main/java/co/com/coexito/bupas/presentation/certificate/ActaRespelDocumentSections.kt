package co.com.coexito.bupas.presentation.certificate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.com.coexito.bupas.core.designsystem.components.BupasPrimaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasSecondaryButton
import co.com.coexito.bupas.core.designsystem.components.BupasSectionCard
import co.com.coexito.bupas.core.designsystem.components.SignaturePadWidget
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.model.DatosGeneradorActa
import co.com.coexito.bupas.domain.model.DatosTransporteOtmActa
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
import co.com.coexito.bupas.domain.model.ItemResiduoActa
import co.com.coexito.bupas.domain.model.ProvenienteActa
import java.util.Locale

/**
 * Sección de trazabilidad, estado de generación/envío de correo y acciones (HU-007 CA 2 y CA 3).
 */
@Composable
fun ActaTraceabilitySection(
    acta: ActaRespel,
    requiresContingencyRetry: Boolean,
    isRetryingContingency: Boolean,
    onViewPdfClick: () -> Unit,
    onDownloadPdfClick: () -> Unit,
    onRetryContingencyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BupasSectionCard(
        title = "Trazabilidad y Envío del Acta",
        subtitle = "Solicitud ${acta.numeroSolicitud} • OM ${acta.identificadorOm} • ${acta.cedi}",
        modifier = modifier
    ) {
        ActaStatusBadgesRow(
            estadoGeneracion = acta.estadoGeneracion,
            estadoEnvio = acta.notificacionCorreo.estadoEnvio
        )
        ActaKeyValueLine(label = "Nro. de Acta (${acta.canal.label})", value = acta.numeroActa)
        ActaKeyValueLine(label = "Archivo Oficial PDF", value = acta.nombreArchivoPdf)
        ActaKeyValueLine(
            label = "Cliente",
            value = "${acta.datosGenerador.razonSocial} (${acta.datosGenerador.nit})"
        )
        ActaKeyValueLine(label = "Asunto de Correo", value = acta.notificacionCorreo.asunto)
        ActaKeyValueLine(
            label = "Correos Destino",
            value = acta.notificacionCorreo.correosDestino.joinToString(", ")
        )
        ActaKeyValueLine(label = "Fecha/Hora Generación", value = acta.fechaHoraGeneracion)
        ActaKeyValueLine(
            label = "Fecha/Hora Envío",
            value = acta.notificacionCorreo.fechaHoraEnvio ?: "Pendiente / Con novedad de envío"
        )
        ActaKeyValueLine(
            label = "Conductor y Vehículo OTM",
            value = "${acta.datosTransporteOtm.conductorResponsable} " +
                "(CC ${acta.datosTransporteOtm.cedulaCiudadania}) • Placa ${acta.datosTransporteOtm.placaVehiculo}"
        )
        ActaErrorAndRetryBanner(acta = acta)
        ActaTraceabilityActions(
            acta = acta,
            requiresContingencyRetry = requiresContingencyRetry,
            isRetryingContingency = isRetryingContingency,
            onViewPdfClick = onViewPdfClick,
            onDownloadPdfClick = onDownloadPdfClick,
            onRetryContingencyClick = onRetryContingencyClick
        )
    }
}

@Composable
private fun ActaStatusBadgesRow(
    estadoGeneracion: EstadoGeneracionActa,
    estadoEnvio: EstadoEnvioActa
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActaSemanticChip(
            label = "Acta: ${estadoGeneracion.displayName}",
            isSuccess = estadoGeneracion == EstadoGeneracionActa.GENERADA,
            modifier = Modifier.weight(1f)
        )
        ActaSemanticChip(
            label = "Envío: ${estadoEnvio.displayName}",
            isSuccess = estadoEnvio == EstadoEnvioActa.ENVIADA,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActaSemanticChip(
    label: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    val pair = if (isSuccess) {
        BupasTheme.extendedColors.recoleccionRealizada
    } else {
        BupasTheme.extendedColors.noRecolectada
    }
    Surface(
        modifier = modifier,
        color = pair.containerColor,
        contentColor = pair.contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                horizontal = BupasTheme.spacing.sm,
                vertical = BupasTheme.spacing.xs
            )
        )
    }
}

@Composable
private fun ActaErrorAndRetryBanner(acta: ActaRespel) {
    val errorText = acta.detalleErrorGeneracion ?: acta.notificacionCorreo.detalleErrorEnvio
    if (errorText.isNullOrBlank()) return

    val horas = acta.notificacionCorreo.planReintento.horasProgramadas.joinToString("h, ") + "h"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, BupasTheme.extendedColors.errorBorderLighter)
    ) {
        Column(
            modifier = Modifier.padding(BupasTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xxs)
        ) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Plan de reintentos automáticos: hasta " +
                    "${acta.notificacionCorreo.planReintento.maxReintentos} intentos ($horas).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ActaTraceabilityActions(
    acta: ActaRespel,
    requiresContingencyRetry: Boolean,
    isRetryingContingency: Boolean,
    onViewPdfClick: () -> Unit,
    onDownloadPdfClick: () -> Unit,
    onRetryContingencyClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        BupasPrimaryButton(
            text = "Ver Acta (${acta.nombreArchivoPdf})",
            onClick = onViewPdfClick,
            enabled = acta.canViewPdf
        )
        if (acta.canViewPdf) {
            BupasSecondaryButton(
                text = "Descargar PDF (${acta.nombreArchivoPdf})",
                onClick = onDownloadPdfClick
            )
        }
        if (requiresContingencyRetry) {
            BupasSecondaryButton(
                text = "Reintentar contingencia de Acta / Correo",
                onClick = onRetryContingencyClick,
                enabled = !isRetryingContingency
            )
        }
    }
}

/**
 * Representación visual íntegra de las 9 secciones del Modelo Oficial del Acta Respel (`[NumeroActa].pdf`).
 */
@Composable
fun ActaOfficialPdfDocumentCard(
    acta: ActaRespel,
    modifier: Modifier = Modifier
) {
    BupasSectionCard(
        title = "Documento Oficial: ${acta.nombreArchivoPdf}",
        subtitle = "Fecha de expedición: ${acta.fechaGeneracionDdMmYyyy} • Consecutivo ${acta.numeroActa}",
        modifier = modifier
    ) {
        ActaHeaderBannerSection(acta = acta)
        ActaGeneratorDataSection(generador = acta.datosGenerador)
        ActaLegalResponsibilitySection(textoLegal = acta.textoResponsabilidadLegal)
        ActaWasteItemsGridSection(
            items = acta.itemsResiduos,
            totalCantidad = acta.totalCantidadRecolectada,
            totalPesoKg = acta.totalPesoKg
        )
        ActaProvenientesDocumentSection(
            aplicaProvenientes = acta.aplicaProvenientes,
            provenientes = acta.provenientes
        )
        ActaObservationsAndClassificationSection(
            observaciones = acta.observacionesRecoleccion,
            clasificacionRespel = acta.clasificacionRespel
        )
        ActaOtmTransportSection(transporte = acta.datosTransporteOtm)
        ActaPreliminaryDisclaimerSection(notaPreliminar = acta.notaAclaratoriaPreliminar)
    }
}

@Composable
private fun ActaHeaderBannerSection(acta: ActaRespel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(BupasTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
        ) {
            Text(
                text = acta.tituloEncabezado,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fecha: ${acta.fechaGeneracionDdMmYyyy} • Canal: ${acta.canal.label} (${acta.numeroActa})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ActaGeneratorDataSection(generador: DatosGeneradorActa) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        ActaSubsectionHeading("1. Datos del Generador del Residuo")
        ActaKeyValueLine(label = "Razón Social / NIT", value = "${generador.razonSocial} • NIT ${generador.nit}")
        ActaKeyValueLine(
            label = "Departamento / Municipio",
            value = "${generador.departamento} / ${generador.municipio}"
        )
        ActaKeyValueLine(label = "Dirección", value = generador.direccion)
        ActaKeyValueLine(
            label = "Teléfono / Correo",
            value = "${generador.telefono} • ${generador.correoElectronico}"
        )
        ActaKeyValueLine(label = "Entregado Por / Responsable", value = generador.entregadoPorResumen)
    }
}

@Composable
private fun ActaLegalResponsibilitySection(textoLegal: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BupasTheme.extendedColors.readOnlySurface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, BupasTheme.extendedColors.borderDisabled)
    ) {
        Text(
            text = textoLegal,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(BupasTheme.spacing.sm)
        )
    }
}

@Composable
private fun ActaWasteItemsGridSection(
    items: List<ItemResiduoActa>,
    totalCantidad: Int,
    totalPesoKg: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        ActaSubsectionHeading("2. Detalle de Residuos Recolectados (Cantidad > 0)")
        items.forEach { item ->
            ActaWasteItemRow(item = item)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TOTALES ACTA",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$totalCantidad uds • ${String.format(Locale.US, "%.1f", totalPesoKg)} kg",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ActaWasteItemRow(item: ItemResiduoActa) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = BupasTheme.spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ref: ${item.referencia}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${item.cantidad} uds • ${String.format(Locale.US, "%.1f", item.pesoTotalKg)} kg",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActaProvenientesDocumentSection(
    aplicaProvenientes: Boolean,
    provenientes: List<ProvenienteActa>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        ActaSubsectionHeading("3. Información de Terceros Provenientes")
        if (!aplicaProvenientes || provenientes.isEmpty()) {
            Text(
                text = "No aplica desglose de terceros provenientes para esta recolección.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            provenientes.forEach { prov ->
                ActaKeyValueLine(
                    label = "${prov.razonSocial} (NIT: ${prov.nit})",
                    value = "Tipo: ${prov.tipoBateria} • Cant: ${prov.cantidad} uds • " +
                        "Tel: ${prov.telefono} • Correo: ${prov.correoElectronico}"
                )
            }
        }
    }
}

@Composable
private fun ActaObservationsAndClassificationSection(
    observaciones: String,
    clasificacionRespel: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        ActaSubsectionHeading("4. Observaciones y Clasificación Respel")
        ActaKeyValueLine(
            label = "Observaciones de la Recolección",
            value = observaciones.ifBlank { "Sin observaciones adicionales." }
        )
        ActaKeyValueLine(
            label = "Clasificación Normativa Respel",
            value = clasificacionRespel
        )
    }
}

@Composable
private fun ActaOtmTransportSection(transporte: DatosTransporteOtmActa) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
    ) {
        ActaSubsectionHeading("5. Datos de Transporte OTM y Firma Digital")
        ActaKeyValueLine(label = "Conductor Responsable", value = transporte.conductorResponsable)
        ActaKeyValueLine(label = "Cédula de Ciudadanía", value = transporte.cedulaCiudadania)
        ActaKeyValueLine(label = "Placa del Vehículo", value = transporte.placaVehiculo)
        Spacer(modifier = Modifier.height(BupasTheme.spacing.xxs))
        SignaturePadWidget(
            firmaDigitalBase64 = transporte.firmaDigitalBase64,
            onSignatureCaptured = {},
            onSignatureCleared = {},
            label = "Firma digital registrada en acta",
            isRequired = false,
            readOnly = true
        )
    }
}

@Composable
private fun ActaPreliminaryDisclaimerSection(notaPreliminar: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BupasTheme.extendedColors.readOnlySurface,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = notaPreliminar,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(BupasTheme.spacing.sm)
        )
    }
}

@Composable
private fun ActaSubsectionHeading(title: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ActaKeyValueLine(
    label: String,
    value: String
) {
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
