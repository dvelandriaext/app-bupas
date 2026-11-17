package co.com.coexito.bupas.data.remote.api

import co.com.coexito.bupas.data.remote.dto.AssignedOrdersResponseDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.PrecioArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato REST para consulta de órdenes asignadas al conductor (HU-005).
 * Actualmente respaldado por [co.com.coexito.bupas.data.remote.FakeOrderApiService];
 * al conectar el backend real basta con proveer la instancia de Retrofit en OrderModule.
 */
interface OrderApiService {

    @GET("api/v1/mobile/orders/assigned")
    suspend fun getAssignedOrders(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("status") status: String? = null,
        @Query("clientQuery") clientQuery: String? = null,
        @Query("numeroSolicitud") numeroSolicitud: String? = null,
        @Query("identificadorOm") identificadorOm: String? = null
    ): AssignedOrdersResponseDto

    @GET("api/v1/mobile/orders/{solicitudId}")
    suspend fun getOrderBySolicitud(
        @Path("solicitudId") solicitudId: String
    ): OrderDto

    @GET("api/v1/mobile/clients/{nitCliente}/price-list")
    suspend fun getClientPriceList(
        @Path("nitCliente") nitCliente: String
    ): List<PrecioArticuloBupasDto> = emptyList()

    @PUT("api/v1/mobile/orders/{solicitudId}/collection")
    suspend fun saveFieldCollection(
        @Path("solicitudId") solicitudId: String,
        @Body request: SaveFieldCollectionRequestDto
    ): OrderDto

    @POST("api/v1/mobile/orders/{solicitudId}/certificate/retry")
    suspend fun retryActaContingency(
        @Path("solicitudId") solicitudId: String
    ): OrderDto = getOrderBySolicitud(solicitudId)
}
