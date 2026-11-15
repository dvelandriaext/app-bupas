package co.com.coexito.bupas.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.domain.model.OrderSearchFilter
import co.com.coexito.bupas.domain.usecase.orders.GetAssignedOrdersUseCase
import co.com.coexito.bupas.domain.usecase.orders.ValidateDateRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OrdersInboxViewModel @Inject constructor(
    private val getAssignedOrdersUseCase: GetAssignedOrdersUseCase,
    private val validateDateRangeUseCase: ValidateDateRangeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersInboxUiState())
    val uiState: StateFlow<OrdersInboxUiState> = _uiState.asStateFlow()

    private val _navigationEffects = MutableSharedFlow<OrdersInboxNavigationEffect>()
    val navigationEffects: SharedFlow<OrdersInboxNavigationEffect> = _navigationEffects.asSharedFlow()

    init {
        fetchOrders(OrderSearchFilter())
    }

    fun onEvent(event: OrdersInboxUiEvent) {
        when (event) {
            OrdersInboxUiEvent.OnRefreshOrders -> fetchOrders(_uiState.value.appliedFilter)
            OrdersInboxUiEvent.OnOpenFilters -> openFilters()
            OrdersInboxUiEvent.OnDismissFilters -> _uiState.update { it.copy(isFilterSheetVisible = false) }
            is OrdersInboxUiEvent.OnStartDateSelected -> updateDraftDates(startDate = event.dateIso)
            is OrdersInboxUiEvent.OnEndDateSelected -> updateDraftDates(endDate = event.dateIso)
            is OrdersInboxUiEvent.OnStatusSelected -> updateDraft { copy(status = event.status) }
            is OrdersInboxUiEvent.OnClientQueryChanged -> updateDraft { copy(clientQuery = event.query) }
            is OrdersInboxUiEvent.OnSolicitudChanged -> updateDraft { copy(numeroSolicitud = event.solicitud) }
            is OrdersInboxUiEvent.OnOmChanged -> updateDraft { copy(identificadorOm = event.om) }
            OrdersInboxUiEvent.OnApplyFilters -> applyFilters()
            OrdersInboxUiEvent.OnClearFilters -> clearFilters()
            is OrdersInboxUiEvent.OnOrderDetailClick -> navigateToDetail(event.numeroSolicitud)
            is OrdersInboxUiEvent.OnViewActaClick -> _uiState.update { it.copy(selectedActaPreview = event.order) }
            OrdersInboxUiEvent.OnDismissActaPreview -> _uiState.update { it.copy(selectedActaPreview = null) }
        }
    }

    private fun openFilters() {
        val currentApplied = _uiState.value.appliedFilter
        _uiState.update {
            it.copy(
                draftFilter = currentApplied,
                dateRangeError = validateDateRangeUseCase(currentApplied.startDate, currentApplied.endDate),
                isFilterSheetVisible = true
            )
        }
    }

    private fun updateDraftDates(
        startDate: String? = _uiState.value.draftFilter.startDate,
        endDate: String? = _uiState.value.draftFilter.endDate
    ) {
        val updatedDraft = _uiState.value.draftFilter.copy(
            startDate = startDate,
            endDate = endDate
        )
        val rangeError = validateDateRangeUseCase(updatedDraft.startDate, updatedDraft.endDate)
        _uiState.update {
            it.copy(
                draftFilter = updatedDraft,
                dateRangeError = rangeError
            )
        }
    }

    private fun updateDraft(transform: OrderSearchFilter.() -> OrderSearchFilter) {
        _uiState.update { state ->
            state.copy(draftFilter = state.draftFilter.transform())
        }
    }

    private fun applyFilters() {
        val draft = _uiState.value.draftFilter
        val rangeError = validateDateRangeUseCase(draft.startDate, draft.endDate)
        if (rangeError != null) {
            _uiState.update { it.copy(dateRangeError = rangeError) }
            return
        }
        _uiState.update {
            it.copy(
                appliedFilter = draft,
                isFilterSheetVisible = false,
                dateRangeError = null
            )
        }
        fetchOrders(draft)
    }

    private fun clearFilters() {
        val emptyFilter = OrderSearchFilter()
        _uiState.update {
            it.copy(
                appliedFilter = emptyFilter,
                draftFilter = emptyFilter,
                dateRangeError = null,
                isFilterSheetVisible = false
            )
        }
        fetchOrders(emptyFilter)
    }

    private fun navigateToDetail(numeroSolicitud: String) {
        viewModelScope.launch {
            _navigationEffects.emit(OrdersInboxNavigationEffect.NavigateToOrderDetail(numeroSolicitud))
        }
    }

    private fun fetchOrders(filter: OrderSearchFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getAssignedOrdersUseCase(filter)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            orders = result.orders,
                            totalCount = result.totalCount,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error al consultar las órdenes asignadas."
                        )
                    }
                }
        }
    }
}
