package co.com.coexito.bupas.domain.usecase.collection

import co.com.coexito.bupas.domain.model.OfflineSyncSummary
import co.com.coexito.bupas.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Sincroniza hacia el backend (App -> Backend) todos los avances y cierres definitivos
 * registrados localmente en Room bajo estado `PENDING_SYNC` (HU-006 Caso Borde 4 y HU-007 CA 1).
 */
class SyncPendingCollectionsUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(): Result<OfflineSyncSummary> =
        orderRepository.syncPendingCollections()

    suspend fun getPendingSyncCount(): Int =
        orderRepository.getPendingSyncCount()

    fun isSimulatedOfflineMode(): Boolean =
        orderRepository.isSimulatedOfflineMode()

    fun setSimulatedOfflineMode(offline: Boolean) {
        orderRepository.setSimulatedOfflineMode(offline)
    }
}
