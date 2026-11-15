package co.com.coexito.bupas.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.com.coexito.bupas.domain.model.BupasUserRole
import co.com.coexito.bupas.domain.model.UserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Fuente de datos local para persistir y destruir de forma segura la sesión autenticada 2FA (HU-001).
 */
interface SessionLocalDataSource {
    val sessionFlow: Flow<UserSession?>
    suspend fun saveSession(session: UserSession)
    suspend fun clearSession()
}

@Singleton
class DataStoreSessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SessionLocalDataSource {

    override val sessionFlow: Flow<UserSession?> = dataStore.data.map { prefs ->
        mapPreferencesToSession(prefs)
    }

    override suspend fun saveSession(session: UserSession) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_USERNAME] = session.username
            prefs[KEY_FULL_NAME] = session.fullName
            prefs[KEY_EMAIL] = session.email
            prefs[KEY_ROLE] = session.role.name
            prefs[KEY_CEDI] = session.cedi
            prefs[KEY_ACCESS_TOKEN] = session.accessToken
            prefs[KEY_IS_2FA_VERIFIED] = session.isTwoFactorVerified
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_FULL_NAME)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_ROLE)
            prefs.remove(KEY_CEDI)
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_IS_2FA_VERIFIED)
        }
    }

    private fun mapPreferencesToSession(prefs: Preferences): UserSession? {
        val userId = prefs[KEY_USER_ID] ?: return null
        val username = prefs[KEY_USERNAME] ?: return null
        val token = prefs[KEY_ACCESS_TOKEN] ?: return null
        val isVerified = prefs[KEY_IS_2FA_VERIFIED] ?: false
        if (!isVerified || token.isBlank()) return null

        val roleName = prefs[KEY_ROLE] ?: BupasUserRole.CONDUCTOR.name
        val role = BupasUserRole.entries.firstOrNull { it.name == roleName }
            ?: BupasUserRole.CONDUCTOR

        return UserSession(
            userId = userId,
            username = username,
            fullName = prefs[KEY_FULL_NAME].orEmpty(),
            email = prefs[KEY_EMAIL].orEmpty(),
            role = role,
            cedi = prefs[KEY_CEDI].orEmpty(),
            accessToken = token,
            isTwoFactorVerified = true
        )
    }

    private companion object {
        val KEY_USER_ID = stringPreferencesKey("session_user_id")
        val KEY_USERNAME = stringPreferencesKey("session_username")
        val KEY_FULL_NAME = stringPreferencesKey("session_full_name")
        val KEY_EMAIL = stringPreferencesKey("session_email")
        val KEY_ROLE = stringPreferencesKey("session_role")
        val KEY_CEDI = stringPreferencesKey("session_cedi")
        val KEY_ACCESS_TOKEN = stringPreferencesKey("session_access_token")
        val KEY_IS_2FA_VERIFIED = booleanPreferencesKey("session_is_2fa_verified")
    }
}
