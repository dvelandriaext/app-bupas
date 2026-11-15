package co.com.coexito.bupas.domain.usecase.orders

import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

class GetOrderBySolicitudUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(numeroSolicitud: String): Result<OrdenRecoleccion> {
        return orderRepository.getOrderBySolicitud(numeroSolicitud)
    }
}
