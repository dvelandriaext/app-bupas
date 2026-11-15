package co.com.coexito.bupas.presentation.orders

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.domain.model.OrdenRecoleccion
import co.com.coexito.bupas.domain.model.OrdenStatus
import co.com.coexito.bupas.domain.usecase.orders.GetOrderBySolicitudUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class OrderDetailUiState(
    val order: OrdenRecoleccion? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showStartAttentionNotice: Boolean = false
) {
    val canStartAttention: Boolean
        get() = order?.estado == OrdenStatus.ORDEN_ASIGNADA
}

sealed interface OrderDetailUiEvent {
    data object OnStartAttentionClick : OrderDetailUiEvent
    data object OnDismissStartAttentionNotice : OrderDetailUiEvent
    data object OnRetry : OrderDetailUiEvent
}

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOrderBySolicitudUseCase: GetOrderBySolicitudUseCase
) : ViewModel() {

    private val numeroSolicitud: String = checkNotNull(savedStateHandle["numeroSolicitud"])

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrderDetail()
    }

    fun onEvent(event: OrderDetailUiEvent) {
        when (event) {
            OrderDetailUiEvent.OnStartAttentionClick -> {
                if (_uiState.value.canStartAttention) {
                    _uiState.update { it.copy(showStartAttentionNotice = true) }
                }
            }
            OrderDetailUiEvent.OnDismissStartAttentionNotice -> {
                _uiState.update { it.copy(showStartAttentionNotice = false) }
            }
            OrderDetailUiEvent.OnRetry -> loadOrderDetail()
        }
    }

    private fun loadOrderDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getOrderBySolicitudUseCase(numeroSolicitud)
                .onSuccess { order ->
                    _uiState.update {
                        it.copy(
                            order = order,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible consultar el detalle de la orden."
                        )
                    }
                }
        }
    }
}
