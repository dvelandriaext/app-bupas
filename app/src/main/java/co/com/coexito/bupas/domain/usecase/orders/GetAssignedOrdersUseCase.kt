package co.com.coexito.bupas.domain.usecase.orders

import co.com.coexito.bupas.domain.model.AssignedOrdersResult
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

class GetAssignedOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        filter: OrderSearchFilter = OrderSearchFilter()
    ): Result<AssignedOrdersResult> {
        return orderRepository.getAssignedOrders(filter)
    }
}
