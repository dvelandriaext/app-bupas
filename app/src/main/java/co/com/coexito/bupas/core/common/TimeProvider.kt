package co.com.coexito.bupas.core.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Proveedor de fecha y hora actual desacoplado del reloj del sistema para trazabilidad
 * determinista de inicio y cierre de atención en campo (HU-006 CA 3).
 */
interface TimeProvider {
    fun nowFormatted(): String
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {

    override fun nowFormatted(): String = LocalDateTime.now().format(DATE_TIME_FORMATTER)

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
