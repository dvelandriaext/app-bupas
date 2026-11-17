package co.com.coexito.bupas.domain.usecase.collection

import co.com.coexito.bupas.domain.model.ArticuloBupas
import co.com.coexito.bupas.domain.model.OrdenStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias de reglas de validación de dominio para HU-006
 * (CA 2, CA 5, CA 7 y Casos Borde R2.1, R2.2, R2.3).
 */
class ValidateFieldCollectionUseCaseTest {

    private val useCase = ValidateFieldCollectionUseCase()

    private val sampleArticulos = listOf(
        ArticuloBupas(
            codigoArticulo = "BUP-AUTO-42",
            descripcion = "Batería Usada Auto 42R",
            cantidadInicial = 10,
            cantidadActa = null,
            pesoUnitarioKg = 14.5,
            valorCompraUnitario = 55000.0
        ),
        ArticuloBupas(
            codigoArticulo = "BUP-AUTO-34",
            descripcion = "Batería Usada Auto 34",
            cantidadInicial = 0,
            cantidadActa = null,
            pesoUnitarioKg = 16.0,
            valorCompraUnitario = 62000.0
        )
    )

    // HU-006 - CA 2 y Caso Borde R2.1: No Recolectada exige obligatoriamente Detalle Novedad
    @Test
    fun `CA2 R2 no recolectada con detalle novedad vacio o en blanco retorna error obligatorio`() {
        val emptyResult = useCase(
            estado = OrdenStatus.NO_RECOLECTADA,
            detalleNovedad = "",
            articulos = sampleArticulos,
            isFinalizing = true
        )
        val blankResult = useCase(
            estado = OrdenStatus.NO_RECOLECTADA,
            detalleNovedad = "   ",
            articulos = sampleArticulos,
            isFinalizing = true
        )

        assertFalse(emptyResult.isValid)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, emptyResult.detalleNovedadError)
        assertFalse(blankResult.isValid)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, blankResult.detalleNovedadError)
    }

    // HU-006 - CA 2: No Recolectada con Detalle Novedad válido omite validación de artículos, responsable y firma
    @Test
    fun `CA2 no recolectada con detalle novedad diligenciado es valida sin exigir recoleccion fisica`() {
        val result = useCase(
            estado = OrdenStatus.NO_RECOLECTADA,
            detalleNovedad = "Cliente cerrado al momento de la visita",
            articulos = sampleArticulos,
            nombreResponsable = "",
            identificacionResponsable = "",
            firmaDigitalBase64 = null,
            isFinalizing = true
        )

        assertTrue(result.isValid)
        assertNull(result.detalleNovedadError)
        assertTrue(result.missingCantidadActaCodigos.isEmpty())
        assertNull(result.nombreResponsableError)
        assertNull(result.identificacionResponsableError)
        assertNull(result.firmaDigitalError)
    }

    // HU-006 - CA 5 y Caso Borde R2.2: Bloqueo de operación de Provenientes si existe Cantidad Acta sin diligenciar
    @Test
    fun `CA5 R2 validar avance a provenientes con cantidadActa vacia retorna codigos faltantes e impide continuar`() {
        val articulosConFaltantes = listOf(
            sampleArticulos[0].copy(cantidadActa = 8),
            sampleArticulos[1].copy(cantidadActa = null),
            ArticuloBupas(
                codigoArticulo = "BUP-MOTO-12",
                descripcion = "Batería Moto 12V",
                cantidadInicial = 5,
                cantidadActa = null,
                pesoUnitarioKg = 4.0,
                valorCompraUnitario = 18000.0
            )
        )

        val blockedResult = useCase.validateProvenientesAccess(articulosConFaltantes)

        assertFalse(blockedResult.isValid)
        assertEquals(setOf("BUP-AUTO-34", "BUP-MOTO-12"), blockedResult.missingCantidadActaCodigos)
        assertEquals(ValidateFieldCollectionUseCase.PROVENIENTES_GATE_ERROR, blockedResult.provenientesGateError)

        val articulosCompletos = articulosConFaltantes.map { it.copy(cantidadActa = it.cantidadActa ?: 0) }
        val allowedResult = useCase.validateProvenientesAccess(articulosCompletos)
        assertTrue(allowedResult.isValid)
        assertNull(allowedResult.provenientesGateError)
    }

    // HU-006 - CA 5 y CA 7: Finalización exige Cantidad Acta en todos los ítems, responsable, documento y firma digital
    @Test
    fun `CA5 CA7 finalizar recoleccion sin cantidades responsable documento o firma retorna errores por cada campo`() {
        val articulosParciales = listOf(
            sampleArticulos[0].copy(cantidadActa = null),
            sampleArticulos[1].copy(cantidadActa = 2)
        )

        val result = useCase(
            estado = OrdenStatus.EN_ATENCION,
            detalleNovedad = "",
            articulos = articulosParciales,
            nombreResponsable = "  ",
            identificacionResponsable = "",
            firmaDigitalBase64 = "   ",
            isFinalizing = true
        )

        assertFalse(result.isValid)
        assertEquals(setOf("BUP-AUTO-42"), result.missingCantidadActaCodigos)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, result.nombreResponsableError)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, result.identificacionResponsableError)
        assertEquals(ValidateFieldCollectionUseCase.REQUIRED_FIELD_ERROR, result.firmaDigitalError)
    }

    // HU-006 - CA 5, CA 7 y Caso Borde R2.3: Validación estricta de enteros >= 0 (rechaza negativos/decimales) y finalización válida
    @Test
    fun `CA5 CA7 finalizar recoleccion con cantidades explicitas responsable documento y firma digital es valida`() {
        assertNull(useCase.parseAndValidateCantidadActa("").getOrThrow())
        assertEquals(0, useCase.parseAndValidateCantidadActa("0").getOrThrow())
        assertEquals(14, useCase.parseAndValidateCantidadActa("14").getOrThrow())

        listOf("-5", "-1", "3.5", "3,5", "12abc").forEach { rawInvalid ->
            val parsed = useCase.parseAndValidateCantidadActa(rawInvalid)
            assertTrue("Se esperaba rechazo para '$rawInvalid'", parsed.isFailure)
            assertEquals(
                ValidateFieldCollectionUseCase.INVALID_CANTIDAD_ACTA_ERROR,
                parsed.exceptionOrNull()?.message
            )
        }

        val articulosCompletos = listOf(
            sampleArticulos[0].copy(cantidadActa = 0),
            sampleArticulos[1].copy(cantidadActa = 4)
        )

        val validResult = useCase(
            estado = OrdenStatus.RECOLECCION_PARCIAL,
            articulos = articulosCompletos,
            nombreResponsable = "Carlos Gómez",
            identificacionResponsable = "1020304050",
            firmaDigitalBase64 = "U0lHTkFUVVJFX0JBU0U2NA==",
            isFinalizing = true
        )
        assertTrue(validResult.isValid)

        val invalidInputPendingResult = useCase(
            estado = OrdenStatus.RECOLECCION_PARCIAL,
            articulos = articulosCompletos,
            nombreResponsable = "Carlos Gómez",
            identificacionResponsable = "1020304050",
            firmaDigitalBase64 = "U0lHTkFUVVJFX0JBU0U2NA==",
            isFinalizing = true,
            invalidCantidadActaCodigos = mapOf(
                "BUP-AUTO-42" to ValidateFieldCollectionUseCase.INVALID_CANTIDAD_ACTA_ERROR
            )
        )
        assertFalse(invalidInputPendingResult.isValid)
    }
}
