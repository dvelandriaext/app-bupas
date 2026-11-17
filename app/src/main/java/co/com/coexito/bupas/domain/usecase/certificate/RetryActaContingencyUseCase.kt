package co.com.coexito.bupas.domain.usecase.certificate

import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.model.EstadoEnvioActa
import co.com.coexito.bupas.domain.model.EstadoGeneracionActa
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Caso de uso para ejecutar el reintento manual de contingencia de generación o reenvío
 * por correo del Acta Respel sin revertir el estado cerrado de la orden (HU-007 CA 2 y CA 3).
 */
class RetryActaContingencyUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(numeroSolicitud: String): Result<ActaRespel> {
        return orderRepository.retryActaContingency(numeroSolicitud)
    }

    suspend fun simulateContingencyState(
        numeroSolicitud: String,
        estadoGeneracion: EstadoGeneracionActa,
        estadoEnvio: EstadoEnvioActa,
        detalleError: String?
    ): Result<ActaRespel> {
        return orderRepository.simulateActaContingencyState(
            numeroSolicitud = numeroSolicitud,
            estadoGeneracion = estadoGeneracion,
            estadoEnvio = estadoEnvio,
            detalleError = detalleError
        )
    }
}
