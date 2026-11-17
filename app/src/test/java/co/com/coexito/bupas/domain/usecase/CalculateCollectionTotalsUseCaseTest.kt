package co.com.coexito.bupas.domain.usecase

import co.com.coexito.bupas.domain.model.ArticuloBupas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias de dominio para el cálculo de subtotales por línea y totales
 * generales de recolección en campo (HU-006: CA 4, CA 5, CA 8 y Caso Borde R2.2).
 */
class CalculateCollectionTotalsUseCaseTest {

    private val useCase = CalculateCollectionTotalsUseCase()

    // HU-006 - CA 4 y CA 5: Recálculo dinámico por línea y totales generales conservando cantidad inicial en solo lectura
    @Test
    fun `CA4 CA5 recalcula subtotales y totales para cantidades menor igual mayor a inicial y cero`() {
        val items = listOf(
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-34",
                descripcion = "Batería Usada Auto 34 (Lista Precios Inicial 0)",
                cantidadInicial = 0,
                cantidadActa = 3,
                pesoUnitarioKg = 16.0,
                valorCompraUnitario = 62000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-42",
                descripcion = "Batería Usada Auto 42R (Menor a inicial)",
                cantidadInicial = 10,
                cantidadActa = 5,
                pesoUnitarioKg = 14.5,
                valorCompraUnitario = 55000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-MOTO-12",
                descripcion = "Batería Usada Moto 12V (Igual a inicial)",
                cantidadInicial = 8,
                cantidadActa = 8,
                pesoUnitarioKg = 4.0,
                valorCompraUnitario = 18000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-PESADA-31",
                descripcion = "Batería Usada Grupo 31H (Mayor a inicial)",
                cantidadInicial = 12,
                cantidadActa = 15,
                pesoUnitarioKg = 26.0,
                valorCompraUnitario = 95000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-SCRAP-01",
                descripcion = "Chatarra Plomo-Ácido Scrap (Cantidad cero)",
                cantidadInicial = 6,
                cantidadActa = 0,
                pesoUnitarioKg = 18.0,
                valorCompraUnitario = 48000.0
            )
        )

        val summary = useCase(items)

        assertEquals(48.0, items[0].pesoTotalKg, 0.001)
        assertEquals(186000.0, items[0].valorCompraTotal, 0.001)
        assertEquals(72.5, items[1].pesoTotalKg, 0.001)
        assertEquals(275000.0, items[1].valorCompraTotal, 0.001)
        assertEquals(32.0, items[2].pesoTotalKg, 0.001)
        assertEquals(144000.0, items[2].valorCompraTotal, 0.001)
        assertEquals(390.0, items[3].pesoTotalKg, 0.001)
        assertEquals(1425000.0, items[3].valorCompraTotal, 0.001)
        assertEquals(0.0, items[4].pesoTotalKg, 0.001)
        assertEquals(0.0, items[4].valorCompraTotal, 0.001)

        assertEquals(31, summary.totalUnidades)
        assertEquals(542.5, summary.totalPesoKg, 0.001)
        assertEquals(2030000.0, summary.totalValorCompra, 0.001)
        assertFalse(summary.hasMissingCantidadActa)
        assertEquals(listOf(0, 10, 8, 12, 6), items.map { it.cantidadInicial })
    }

    // HU-006 - CA 5 y Caso Borde R2.2: Detección de Cantidad Acta vacía (null) en cualquier referencia BUPAS
    @Test
    fun `CA5 R2 marca hasMissingCantidadActa cuando al menos un articulo tiene cantidadActa nula`() {
        val items = listOf(
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-42",
                descripcion = "Batería Usada Auto 42R",
                cantidadInicial = 15,
                cantidadActa = 6,
                pesoUnitarioKg = 14.5,
                valorCompraUnitario = 55000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-34",
                descripcion = "Batería Usada Auto 34 (Lista de precios sin diligenciar)",
                cantidadInicial = 0,
                cantidadActa = null,
                pesoUnitarioKg = 16.0,
                valorCompraUnitario = 62000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-MOTO-12",
                descripcion = "Batería Usada Moto 12V",
                cantidadInicial = 8,
                cantidadActa = 0,
                pesoUnitarioKg = 4.0,
                valorCompraUnitario = 18000.0
            )
        )

        val summary = useCase(items)

        assertEquals(6, summary.totalUnidades)
        assertEquals(87.0, summary.totalPesoKg, 0.001)
        assertEquals(330000.0, summary.totalValorCompra, 0.001)
        assertTrue(summary.hasMissingCantidadActa)
    }

    // HU-006 - CA 5 y CA 8: Todas las referencias diligenciadas explícitamente en cero son válidas sin faltantes
    @Test
    fun `CA5 todas las referencias con cantidadActa explicita en cero retornan totales en cero sin faltantes`() {
        val items = listOf(
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-42",
                descripcion = "Batería Usada Auto 42R",
                cantidadInicial = 10,
                cantidadActa = 0,
                pesoUnitarioKg = 14.5,
                valorCompraUnitario = 55000.0
            ),
            ArticuloBupas(
                codigoArticulo = "BUP-AUTO-34",
                descripcion = "Batería Usada Auto 34",
                cantidadInicial = 0,
                cantidadActa = 0,
                pesoUnitarioKg = 16.0,
                valorCompraUnitario = 62000.0
            )
        )

        val summary = useCase(items)

        assertEquals(0, summary.totalUnidades)
        assertEquals(0.0, summary.totalPesoKg, 0.001)
        assertEquals(0.0, summary.totalValorCompra, 0.001)
        assertFalse(summary.hasMissingCantidadActa)
    }
}
