package co.com.coexito.bupas.domain.usecase.certificate

import co.com.coexito.bupas.domain.model.ActaRespel
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Caso de uso para consultar la trazabilidad y el documento oficial del Acta Respel
 * asociada a una orden de recolección (HU-007 CA 3).
 */
class GetActaRespelUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(numeroSolicitud: String): Result<ActaRespel> {
        return orderRepository.getActaBySolicitud(numeroSolicitud)
    }
}
