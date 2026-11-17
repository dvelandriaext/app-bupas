package co.com.coexito.bupas.presentation.orders

import androidx.compose.runtime.Immutable
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.model.OrderSearchFilter

@Immutable
data class OrdersInboxUiState(
    val orders: List<OrdenRecoleccion> = emptyList(),
    val totalCount: Int = 0,
    val appliedFilter: OrderSearchFilter = OrderSearchFilter(),
    val draftFilter: OrderSearchFilter = OrderSearchFilter(),
    val dateRangeError: String? = null,
    val isFilterSheetVisible: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedActaPreview: OrdenRecoleccion? = null
)

sealed interface OrdersInboxUiEvent {
    data object OnRefreshOrders : OrdersInboxUiEvent
    data object OnOpenFilters : OrdersInboxUiEvent
    data object OnDismissFilters : OrdersInboxUiEvent
    data class OnStartDateSelected(val dateIso: String?) : OrdersInboxUiEvent
    data class OnEndDateSelected(val dateIso: String?) : OrdersInboxUiEvent
    data class OnStatusSelected(val status: OrdenStatus?) : OrdersInboxUiEvent
    data class OnClientQueryChanged(val query: String) : OrdersInboxUiEvent
    data class OnSolicitudChanged(val solicitud: String) : OrdersInboxUiEvent
    data class OnOmChanged(val om: String) : OrdersInboxUiEvent
    data object OnApplyFilters : OrdersInboxUiEvent
    data object OnClearFilters : OrdersInboxUiEvent
    data class OnOrderDetailClick(val numeroSolicitud: String) : OrdersInboxUiEvent
    data class OnViewActaClick(val order: OrdenRecoleccion) : OrdersInboxUiEvent
    data object OnDismissActaPreview : OrdersInboxUiEvent
}

sealed interface OrdersInboxNavigationEffect {
    data class NavigateToOrderDetail(val numeroSolicitud: String) : OrdersInboxNavigationEffect
    data class NavigateToActaCertificate(val numeroSolicitud: String) : OrdersInboxNavigationEffect
}
