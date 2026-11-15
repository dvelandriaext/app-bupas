package co.com.coexito.bupas.domain.usecase.orders

import java.time.LocalDate
import javax.inject.Inject

/**
 * Valida la coherencia del rango de fechas y el límite máximo de 1 mes (HU-005 CA 1).
 */
class ValidateDateRangeUseCase @Inject constructor() {

    operator fun invoke(startDate: String?, endDate: String?): String? {
        if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) return null

        val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
            ?: return INVALID_FORMAT_MESSAGE
        val end = runCatching { LocalDate.parse(endDate) }.getOrNull()
            ?: return INVALID_FORMAT_MESSAGE

        if (end.isBefore(start)) {
            return INVERTED_RANGE_MESSAGE
        }
        if (end.isAfter(start.plusMonths(1))) {
            return MAX_RANGE_EXCEEDED_MESSAGE
        }
        return null
    }

    companion object {
        const val INVALID_FORMAT_MESSAGE = "Formato de fecha inválido."
        const val INVERTED_RANGE_MESSAGE = "La fecha final no puede ser anterior a la fecha inicial."
        const val MAX_RANGE_EXCEEDED_MESSAGE = "El rango de fechas no puede superar 1 mes."
    }
}
