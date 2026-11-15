package co.com.coexito.bupas.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.core.designsystem.theme.StatusColorPair
import co.com.coexito.bupas.domain.model.OrdenStatus

@Composable
fun BupasStatusBadge(
    status: OrdenStatus,
    modifier: Modifier = Modifier
) {
    val extendedColors = BupasTheme.extendedColors
    val colorPair: StatusColorPair = when (status) {
        OrdenStatus.ORDEN_ASIGNADA -> extendedColors.ordenAsignada
        OrdenStatus.EN_ATENCION -> extendedColors.enAtencion
        OrdenStatus.RECOLECCION_PARCIAL -> extendedColors.recoleccionParcial
        OrdenStatus.NO_RECOLECTADA -> extendedColors.noRecolectada
        OrdenStatus.RECOLECCION_REALIZADA -> extendedColors.recoleccionRealizada
    }

    Surface(
        modifier = modifier,
        color = colorPair.containerColor,
        contentColor = colorPair.contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            modifier = Modifier.padding(
                horizontal = BupasTheme.spacing.sm,
                vertical = BupasTheme.spacing.xs
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BupasStatusBadgePreview() {
    BupasTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrdenStatus.entries.forEach { status ->
                BupasStatusBadge(status = status)
            }
        }
    }
}
