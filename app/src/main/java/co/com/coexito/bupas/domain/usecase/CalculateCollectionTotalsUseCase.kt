package co.com.coexito.bupas.domain.usecase

import androidx.compose.runtime.Immutable
import co.com.coexito.bupas.domain.model.ArticuloBupas
import javax.inject.Inject

@Immutable
data class CollectionTotalsSummary(
    val totalUnidades: Int,
    val totalPesoKg: Double,
    val totalValorCompra: Double,
    val hasMissingCantidadActa: Boolean
)

/**
 * Caso de uso de dominio para el recálculo dinámico en tiempo real de unidades,
 * peso (kg) y valor de compra total ($) según la Cantidad Acta registrada en campo (HU-006).
 */
class CalculateCollectionTotalsUseCase @Inject constructor() {

    operator fun invoke(articulos: List<ArticuloBupas>): CollectionTotalsSummary {
        val totalUnidades = articulos.sumOf { it.cantidadActa ?: 0 }
        val totalPesoKg = articulos.sumOf { it.pesoTotalKg }
        val totalValorCompra = articulos.sumOf { it.valorCompraTotal }
        val hasMissingCantidadActa = articulos.any { it.cantidadActa == null }

        return CollectionTotalsSummary(
            totalUnidades = totalUnidades,
            totalPesoKg = totalPesoKg,
            totalValorCompra = totalValorCompra,
            hasMissingCantidadActa = hasMissingCantidadActa
        )
    }
}
