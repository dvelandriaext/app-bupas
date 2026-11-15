package co.com.coexito.bupas.core.di

import co.com.coexito.bupas.data.local.DataStoreThemeLocalDataSource
import co.com.coexito.bupas.data.local.ThemeLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeLocalDataSource(
        impl: DataStoreThemeLocalDataSource
    ): ThemeLocalDataSource
}
