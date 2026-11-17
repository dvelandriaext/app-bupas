package co.com.coexito.bupas.core.di

import android.content.Context
import androidx.room.Room
import co.com.coexito.bupas.core.common.SystemTimeProvider
import co.com.coexito.bupas.core.common.TimeProvider
import co.com.coexito.bupas.core.config.EnvironmentConfig
import co.com.coexito.bupas.data.local.BupasDatabase
import co.com.coexito.bupas.data.local.FieldCollectionDraftDao
import co.com.coexito.bupas.data.local.FieldCollectionLocalDataSource
import co.com.coexito.bupas.data.local.RoomFieldCollectionLocalDataSource
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class OrderModule {

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        impl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        impl: SystemTimeProvider
    ): TimeProvider

    @Binds
    @Singleton
    abstract fun bindFieldCollectionLocalDataSource(
        impl: RoomFieldCollectionLocalDataSource
    ): FieldCollectionLocalDataSource

    companion object {
        @Provides
        @Singleton
        fun provideOrderApiService(
            environmentConfig: EnvironmentConfig,
            retrofit: Retrofit
        ): OrderApiService {
            return if (environmentConfig.useFakeData) {
                FakeOrderApiService()
            } else {
                retrofit.create(OrderApiService::class.java)
            }
        }

        @Provides
        @Singleton
        fun provideBupasDatabase(
            @ApplicationContext context: Context
        ): BupasDatabase {
            return Room.databaseBuilder(
                context,
                BupasDatabase::class.java,
                BupasDatabase.DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideFieldCollectionDraftDao(
            database: BupasDatabase
        ): FieldCollectionDraftDao = database.fieldCollectionDraftDao()
    }
}
