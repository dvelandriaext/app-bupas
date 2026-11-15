package co.com.coexito.bupas.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import co.com.coexito.bupas.data.local.DataStoreSessionLocalDataSource
import co.com.coexito.bupas.data.local.SessionLocalDataSource
import co.com.coexito.bupas.data.remote.AuthApiService
import co.com.coexito.bupas.data.remote.FakeAuthApiService
import co.com.coexito.bupas.data.repository.AuthRepositoryImpl
import co.com.coexito.bupas.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindSessionLocalDataSource(
        impl: DataStoreSessionLocalDataSource
    ): SessionLocalDataSource

    /**
     * Para conectar al backend real de CDA, reemplazar [FakeAuthApiService] por
     * una instancia de Retrofit (`retrofit.create(AuthApiService::class.java)`).
     */
    @Binds
    @Singleton
    abstract fun bindAuthApiService(
        impl: FakeAuthApiService
    ): AuthApiService

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    companion object {
        private const val SESSION_PREFERENCES_NAME = "bupas_session_preferences"

        @Provides
        @Singleton
        fun provideSessionDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile(SESSION_PREFERENCES_NAME) }
            )
        }
    }
}
