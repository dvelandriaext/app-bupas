package co.com.coexito.bupas.domain.usecase.collection

import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Caso de uso para persistir el avance o finalizar la recolección en campo (HU-006).
 */
class SaveFieldCollectionUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(submission: FieldCollectionSubmission): Result<OrdenRecoleccion> {
        return orderRepository.saveFieldCollection(submission)
    }
}
