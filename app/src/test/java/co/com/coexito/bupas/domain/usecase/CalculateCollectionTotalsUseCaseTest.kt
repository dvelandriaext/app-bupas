package co.com.coexito.bupas.domain.usecase

import co.com.coexito.bupas.domain.model.ArticuloBupas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateCollectionTotalsUseCaseTest {

    private val useCase = CalculateCollectionTotalsUseCase()

    @Test
    fun `invoke recalculates total units weight and purchase value from cantidadActa`() {
        val items = listOf(
            ArticuloBupas(
                codigoArticulo = "BUP-01",
                descripcion = "Batería Usada Auto 42R",
                cantidadInicial = 10,
                cantidadActa = 12,
                pesoUnitarioKg = 15.0,
                valorCompraUnitario = 50000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-02",
                descripcion = "Batería Usada Moto 7Ah",
                cantidadInicial = 5,
                cantidadActa = 0,
                pesoUnitarioKg = 2.5,
                valorCompraUnitario = 10000.0
            )
        )

        val summary = useCase(items)

        assertEquals(12, summary.totalUnidades)
        assertEquals(180.0, summary.totalPesoKg, 0.001)
        assertEquals(600000.0, summary.totalValorCompra, 0.001)
        assertFalse(summary.hasMissingCantidadActa)
    }

    @Test
    fun `invoke flags missing cantidadActa when any item is null`() {
        val items = listOf(
            ArticuloBupas(
                codigoArticulo = "BUP-01",
                descripcion = "Batería Usada Auto 42R",
                cantidadInicial = 10,
                cantidadActa = null,
                pesoUnitarioKg = 15.0,
                valorCompraUnitario = 50000.0
            )
        )

        val summary = useCase(items)

        assertTrue(summary.hasMissingCantidadActa)
    }
}
