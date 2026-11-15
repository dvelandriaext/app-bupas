package co.com.coexito.bupas.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.com.coexito.bupas.core.designsystem.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ThemeLocalDataSource {
    val themeModeFlow: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

@Singleton
class DataStoreThemeLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemeLocalDataSource {

    override val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE]
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
    }
}
