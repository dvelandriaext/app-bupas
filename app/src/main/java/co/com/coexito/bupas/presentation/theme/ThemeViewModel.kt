package co.com.coexito.bupas.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.com.coexito.bupas.core.designsystem.theme.ThemeMode
import co.com.coexito.bupas.data.local.ThemeLocalDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeLocalDataSource: ThemeLocalDataSource
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themeLocalDataSource.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ThemeMode.SYSTEM
        )

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            themeLocalDataSource.setThemeMode(mode)
        }
    }
}
