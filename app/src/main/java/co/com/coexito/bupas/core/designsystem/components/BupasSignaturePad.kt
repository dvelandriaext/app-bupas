package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import java.util.Base64
import java.util.Locale

private val SignaturePadHeight = 160.dp

/**
 * Componente stateless de captura de firma digital en campo para HU-006 (CA 7).
 * Serializa trazos en SVG compacto codificado mediante `java.util.Base64`.
 */
@Composable
fun SignaturePadWidget(
    firmaDigitalBase64: String?,
    onSignatureCaptured: (String) -> Unit,
    onSignatureCleared: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Firma digital de quien entrega",
    isRequired: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val showError = isError || !errorMessage.isNullOrBlank()
    val hasSignature = !firmaDigitalBase64.isNullOrBlank()
    val decodedStrokes = remember(firmaDigitalBase64) { decodeSignatureStrokesBase64(firmaDigitalBase64) }
    var activeStrokes by remember(firmaDigitalBase64) { mutableStateOf(decodedStrokes) }
    var currentStroke by remember(firmaDigitalBase64) { mutableStateOf<List<Offset>>(emptyList()) }
    var padWidthPx by remember { mutableIntStateOf(600) }
    var padHeightPx by remember { mutableIntStateOf(300) }

    Column(modifier = modifier.fillMaxWidth()) {
        SignaturePadHeader(
            label = if (isRequired) "$label *" else label,
            showClearAction = !readOnly && hasSignature,
            showError = showError,
            onSignatureCleared = {
                activeStrokes = emptyList()
                currentStroke = emptyList()
                onSignatureCleared()
            }
        )
        SignatureCanvasSurface(
            strokes = activeStrokes,
            currentStroke = currentStroke,
            hasSignature = hasSignature,
            readOnly = readOnly,
            showError = showError,
            onSizeUpdated = { w, h ->
                if (w > 0) padWidthPx = w
                if (h > 0) padHeightPx = h
            },
            onStrokeStart = { start -> currentStroke = listOf(start) },
            onStrokeMove = { next -> currentStroke = currentStroke + next },
            onStrokeEnd = {
                if (currentStroke.isNotEmpty()) {
                    val updated = activeStrokes + listOf(currentStroke)
                    activeStrokes = updated
                    currentStroke = emptyList()
                    onSignatureCaptured(encodeSignatureStrokesBase64(updated, padWidthPx, padHeightPx))
                }
            }
        )
        AnimatedVisibility(visible = showError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = BupasTheme.spacing.xs)
            )
        }
    }
}

@Composable
private fun SignaturePadHeader(
    label: String,
    showClearAction: Boolean,
    showError: Boolean,
    onSignatureCleared: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = BupasTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        if (showClearAction) {
            BupasTextButton(text = "Limpiar firma", onClick = onSignatureCleared)
        }
    }
}

@Composable
private fun SignatureCanvasSurface(
    strokes: List<List<Offset>>,
    currentStroke: List<Offset>,
    hasSignature: Boolean,
    readOnly: Boolean,
    showError: Boolean,
    onSizeUpdated: (Int, Int) -> Unit,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit
) {
    val containerColor = if (readOnly) BupasTheme.extendedColors.readOnlySurface else MaterialTheme.colorScheme.surface
    val borderColor = when {
        showError -> MaterialTheme.colorScheme.error
        readOnly -> BupasTheme.extendedColors.borderDisabled
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(SignaturePadHeight)
            .onSizeChanged { onSizeUpdated(it.width, it.height) },
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(width = if (showError) 2.dp else 1.dp, color = borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(readOnly) {
                    if (!readOnly) {
                        detectDragGestures(
                            onDragStart = onStrokeStart,
                            onDrag = { change, _ ->
                                change.consume()
                                onStrokeMove(change.position)
                            },
                            onDragEnd = onStrokeEnd,
                            onDragCancel = onStrokeEnd
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SignatureOverlayFeedback(
                hasStrokes = strokes.isNotEmpty() || currentStroke.isNotEmpty(),
                hasSignature = hasSignature,
                readOnly = readOnly
            )
            SignatureDrawingCanvas(strokes, currentStroke, MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SignatureOverlayFeedback(hasStrokes: Boolean, hasSignature: Boolean, readOnly: Boolean) {
    when {
        !hasStrokes && hasSignature -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BupasTheme.spacing.xs)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BupasTheme.extendedColors.successAccent)
            Text(
                text = "Firma digital registrada",
                style = MaterialTheme.typography.bodyMedium,
                color = BupasTheme.extendedColors.successAccent
            )
        }
        !hasStrokes && !hasSignature -> Text(
            text = if (readOnly) "Sin firma digital registrada" else "Firme aquí con el dedo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SignatureDrawingCanvas(strokes: List<List<Offset>>, currentStroke: List<Offset>, strokeColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val allStrokes = if (currentStroke.isEmpty()) strokes else strokes + listOf(currentStroke)
        allStrokes.forEach { points ->
            if (points.size == 1) {
                drawCircle(color = strokeColor, radius = 3.dp.toPx(), center = points.first())
            } else if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { pt -> lineTo(pt.x, pt.y) }
                }
                drawPath(path, strokeColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

internal fun encodeSignatureStrokesBase64(strokes: List<List<Offset>>, widthPx: Int, heightPx: Int): String {
    val polylines = strokes.filter { it.isNotEmpty() }.joinToString(separator = "") { points ->
        val coords = points.joinToString(" ") { String.format(Locale.US, "%.1f,%.1f", it.x, it.y) }
        "<polyline fill=\"none\" stroke=\"black\" stroke-width=\"3\" points=\"$coords\"/>"
    }
    val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ${widthPx.coerceAtLeast(1)} ${heightPx.coerceAtLeast(1)}\">$polylines</svg>"
    return Base64.getEncoder().encodeToString(svg.toByteArray(Charsets.UTF_8))
}

internal fun decodeSignatureStrokesBase64(base64: String?): List<List<Offset>> {
    if (base64.isNullOrBlank()) return emptyList()
    val svg = runCatching { String(Base64.getDecoder().decode(base64.trim()), Charsets.UTF_8) }.getOrNull()
        ?: return emptyList()
    if (!svg.contains("<polyline")) return emptyList()
    return Regex("points=\"([^\"]+)\"").findAll(svg).mapNotNull { match ->
        match.groupValues[1].trim().split(" ").mapNotNull { pair ->
            val parts = pair.split(",")
            val x = parts.getOrNull(0)?.toFloatOrNull()
            val y = parts.getOrNull(1)?.toFloatOrNull()
            if (x != null && y != null) Offset(x, y) else null
        }.takeIf { it.isNotEmpty() }
    }.toList()
}

@Preview(showBackground = true)
@Composable
private fun SignaturePadWidgetPreview() {
    BupasTheme {
        Column(Modifier.padding(BupasTheme.spacing.md), Arrangement.spacedBy(BupasTheme.spacing.md)) {
            SignaturePadWidget(null, {}, {}, isError = true, errorMessage = "Campo requerido")
            SignaturePadWidget("U0lHTkFUVVJFX1BPU18wMTEy", {}, {}, readOnly = true)
        }
    }
}
