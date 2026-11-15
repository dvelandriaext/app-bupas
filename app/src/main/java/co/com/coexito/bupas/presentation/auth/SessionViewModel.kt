package co.com.coexito.bupas.presentation.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.domain.model.UserSession
import co.com.coexito.bupas.domain.usecase.auth.LogoutUseCase
import co.com.coexito.bupas.domain.usecase.auth.ObserveUserSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
sealed interface SessionGuardState {
    data object Loading : SessionGuardState
    data object Unauthenticated : SessionGuardState
    data class Authenticated(val session: UserSession) : SessionGuardState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    observeUserSessionUseCase: ObserveUserSessionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    val sessionState: StateFlow<SessionGuardState> = observeUserSessionUseCase()
        .map { session ->
            if (session != null && session.isTwoFactorVerified) {
                SessionGuardState.Authenticated(session)
            } else {
                SessionGuardState.Unauthenticated
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionGuardState.Loading
        )

    fun performSafeLogout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }
}
