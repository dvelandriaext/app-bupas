package co.com.coexito.bupas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.com.coexito.bupas.core.designsystem.theme.BupasTheme
import co.com.coexito.bupas.core.navigation.BupasNavHost
import co.com.coexito.bupas.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

            BupasTheme(themeMode = themeMode) {
                BupasNavHost(
                    themeMode = themeMode,
                    onThemeModeSelected = themeViewModel::onThemeModeSelected
                )
            }
        }
    }
}
