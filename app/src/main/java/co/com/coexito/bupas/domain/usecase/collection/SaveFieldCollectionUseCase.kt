package co.com.coexito.bupas.domain.usecase.collection

import co.com.coexito.bupas.core.common.SystemTimeProvider
import co.com.coexito.bupas.core.common.TimeProvider
import co.com.coexito.bupas.domain.model.FieldCollectionSubmission
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Caso de uso para persistir el avance o finalizar la recolección en campo (HU-006):
 * - Impide modificar o reabrir órdenes cerradas en modo solo lectura (R2.4).
 * - Registra y preserva automáticamente `fechaHoraInicioAtencion` y `fechaHoraFinalizacion` (CA 3).
 */
class SaveFieldCollectionUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val timeProvider: TimeProvider
) {

    constructor(orderRepository: OrderRepository) : this(
        orderRepository = orderRepository,
        timeProvider = SystemTimeProvider()
    )

    suspend operator fun invoke(submission: FieldCollectionSubmission): Result<OrdenRecoleccion> {
        val currentOrder = orderRepository.getOrderBySolicitud(submission.numeroSolicitud).getOrNull()
        if (currentOrder != null && currentOrder.estado.isReadOnlyLocked) {
            return Result.failure(IllegalStateException(CLOSED_ORDER_ERROR))
        }

        val enrichedSubmission = enrichTimestamps(submission, currentOrder)
        return orderRepository.saveFieldCollection(enrichedSubmission)
    }

    suspend fun hasPendingOfflineDraft(numeroSolicitud: String): Boolean =
        orderRepository.hasPendingOfflineDraft(numeroSolicitud)

    private fun enrichTimestamps(
        submission: FieldCollectionSubmission,
        currentOrder: OrdenRecoleccion?
    ): FieldCollectionSubmission {
        val resolvedInicio = currentOrder?.fechaHoraInicioAtencion?.takeIf { it.isNotBlank() }
            ?: submission.fechaHoraInicioAtencion?.takeIf { it.isNotBlank() }
            ?: if (submission.estado in ATTENTION_STATUSES) timeProvider.nowFormatted() else null

        val isClosing = submission.finalizeCollection ||
            submission.estado == OrdenStatus.NO_RECOLECTADA ||
            submission.estado == OrdenStatus.RECOLECCION_REALIZADA

        val resolvedFinalizacion = if (isClosing) {
            submission.fechaHoraFinalizacion?.takeIf { it.isNotBlank() } ?: timeProvider.nowFormatted()
        } else {
            currentOrder?.fechaHoraFinalizacion
        }

        return submission.copy(
            fechaHoraInicioAtencion = resolvedInicio,
            fechaHoraFinalizacion = resolvedFinalizacion
        )
    }

    companion object {
        const val CLOSED_ORDER_ERROR =
            "La orden ya fue cerrada y se encuentra bloqueada en modo solo lectura."

        private val ATTENTION_STATUSES = setOf(
            OrdenStatus.EN_ATENCION,
            OrdenStatus.RECOLECCION_PARCIAL,
            OrdenStatus.RECOLECCION_REALIZADA
        )
    }
}
