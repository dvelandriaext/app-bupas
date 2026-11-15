package co.com.coexito.bupas.domain.usecase.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas unitarias alineadas con HU-005 CA 1 y casos borde de rango de fechas (máximo 1 mes).
 */
class ValidateDateRangeUseCaseTest {

    private val useCase = ValidateDateRangeUseCase()

    @Test
    fun `CA1 rango menor o igual a 1 mes es valido`() {
        val result = useCase(startDate = "2026-10-15", endDate = "2026-11-15")
        assertNull(result)
    }

    @Test
    fun `CA1 fechas abiertas o nulas son validas`() {
        assertNull(useCase(startDate = null, endDate = null))
        assertNull(useCase(startDate = "2026-11-01", endDate = null))
        assertNull(useCase(startDate = null, endDate = "2026-11-15"))
    }

    @Test
    fun `CA1 caso borde rango superior a 1 mes retorna error`() {
        val result = useCase(startDate = "2026-10-01", endDate = "2026-11-15")
        assertEquals(ValidateDateRangeUseCase.MAX_RANGE_EXCEEDED_MESSAGE, result)
    }

    @Test
    fun `CA1 caso borde fecha final anterior a fecha inicial retorna error`() {
        val result = useCase(startDate = "2026-11-15", endDate = "2026-11-01")
        assertEquals(ValidateDateRangeUseCase.INVERTED_RANGE_MESSAGE, result)
    }
}
