package co.com.coexito.bupas.domain.usecase.collection

import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateFieldCollectionUseCaseTest {

    private val useCase = ValidateFieldCollectionUseCase()

    private val sampleArticulos = listOf(
        ArticuloBupas(
            codigoArticulo = "BUP-AUTO-42",
            descripcion = "Batería Auto",
            cantidadInicial = 10,
            cantidadActa = null,
            pesoUnitarioKg = 14.0,
            valorCompraUnitario = 50000.0
        )
    )

    @Test
    fun `no recolectada sin detalle novedad retorna error de campo requerido`() {
        val result = useCase(
            estado = OrdenStatus.NO_RECOLECTADA,
            articulos = sampleArticulos,
            detalleNovedad = "   ",
            nombreResponsable = "",
            identificacionResponsable = "",
            isFinalizing = true
        )

        assertFalse(result.isValid)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, result.detalleNovedadError)
    }

    @Test
    fun `no recolectada con detalle novedad es valido sin exigir articulos ni responsable`() {
        val result = useCase(
            estado = OrdenStatus.NO_RECOLECTADA,
            articulos = sampleArticulos,
            detalleNovedad = "Cliente cerrado al momento de la visita",
            nombreResponsable = "",
            identificacionResponsable = "",
            isFinalizing = true
        )

        assertTrue(result.isValid)
    }

    @Test
    fun `finalizar en atencion sin cantidad acta ni responsable retorna errores`() {
        val result = useCase(
            estado = OrdenStatus.EN_ATENCION,
            articulos = sampleArticulos,
            detalleNovedad = "",
            nombreResponsable = "",
            identificacionResponsable = "",
            isFinalizing = true
        )

        assertFalse(result.isValid)
        assertTrue("BUP-AUTO-42" in result.missingCantidadActaCodigos)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, result.nombreResponsableError)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, result.identificacionResponsableError)
    }

    @Test
    fun `finalizar con cantidad acta cero y responsable completo es valido`() {
        val result = useCase(
            estado = OrdenStatus.RECOLECCION_PARCIAL,
            articulos = listOf(sampleArticulos.first().copy(cantidadActa = 0)),
            detalleNovedad = "",
            nombreResponsable = "Carlos Gómez",
            identificacionResponsable = "1020304050",
            isFinalizing = true
        )

        assertTrue(result.isValid)
    }
}
