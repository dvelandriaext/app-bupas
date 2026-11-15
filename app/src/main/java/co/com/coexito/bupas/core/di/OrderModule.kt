package co.com.coexito.bupas.core.di

import co.com.coexito.bupas.core.config.EnvironmentConfig
import co.com.coexito.bupas.data.remote.FakeOrderApiService
import co.com.coexito.bupas.data.remote.api.OrderApiService
import co.com.coexito.bupas.data.repository.OrderRepositoryImpl
import co.com.coexito.bupas.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    }
}
