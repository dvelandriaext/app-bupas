package co.com.coexito.bupas.data.local

import co.com.coexito.bupas.data.remote.dto.ArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.OrderDto
import co.com.coexito.bupas.data.remote.dto.PrecioArticuloBupasDto
import co.com.coexito.bupas.data.remote.dto.SaveFieldCollectionRequestDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StoredFieldCollectionDraftDto(
    val numeroSolicitud: String,
    val request: SaveFieldCollectionRequestDto,
    val fullArticulos: List<ArticuloBupasDto> = emptyList(),
    val pendingSync: Boolean = true
)

/**
 * Fuente de datos local para caché de órdenes y borradores offline durante el registro
 * de recolección en sitio ante pérdida de conectividad (HU-006 R2.5).
 */
interface FieldCollectionLocalDataSource {
    suspend fun cacheOrder(order: OrderDto)
    suspend fun getCachedOrder(numeroSolicitud: String): OrderDto?
    suspend fun getAllCachedOrders(): List<OrderDto>
    suspend fun saveDraft(draft: StoredFieldCollectionDraftDto)
    suspend fun getDraft(numeroSolicitud: String): StoredFieldCollectionDraftDto?
    suspend fun getAllPendingDrafts(): List<StoredFieldCollectionDraftDto>
    suspend fun clearDraft(numeroSolicitud: String)
    suspend fun cacheClientPriceList(nitCliente: String, items: List<PrecioArticuloBupasDto>)
    suspend fun getCachedClientPriceList(nitCliente: String): List<PrecioArticuloBupasDto>?
}

@Singleton
class RoomFieldCollectionLocalDataSource @Inject constructor(
    private val dao: FieldCollectionDraftDao,
    private val json: Json
) : FieldCollectionLocalDataSource {

    override suspend fun cacheOrder(order: OrderDto) {
        val normalizedKey = order.numeroSolicitud.uppercase()
        dao.upsertCachedOrder(
            CachedOrderEntity(
                numeroSolicitud = normalizedKey,
                orderJson = json.encodeToString(order),
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getCachedOrder(numeroSolicitud: String): OrderDto? {
        val entity = dao.getCachedOrder(numeroSolicitud.uppercase()) ?: return null
        return runCatching { json.decodeFromString<OrderDto>(entity.orderJson) }.getOrNull()
    }

    override suspend fun getAllCachedOrders(): List<OrderDto> =
        dao.getAllCachedOrders().mapNotNull { entity ->
            runCatching { json.decodeFromString<OrderDto>(entity.orderJson) }.getOrNull()
        }

    override suspend fun saveDraft(draft: StoredFieldCollectionDraftDto) {
        val normalizedKey = draft.numeroSolicitud.uppercase()
        dao.upsertDraft(
            FieldCollectionDraftEntity(
                numeroSolicitud = normalizedKey,
                draftJson = json.encodeToString(draft),
                pendingSync = draft.pendingSync,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getDraft(numeroSolicitud: String): StoredFieldCollectionDraftDto? {
        val entity = dao.getDraft(numeroSolicitud.uppercase()) ?: return null
        return runCatching {
            json.decodeFromString<StoredFieldCollectionDraftDto>(entity.draftJson)
        }.getOrNull()
    }

    override suspend fun getAllPendingDrafts(): List<StoredFieldCollectionDraftDto> =
        dao.getAllPendingDrafts().mapNotNull { entity ->
            runCatching {
                json.decodeFromString<StoredFieldCollectionDraftDto>(entity.draftJson)
            }.getOrNull()
        }

    override suspend fun clearDraft(numeroSolicitud: String) {
        dao.deleteDraft(numeroSolicitud.uppercase())
    }

    override suspend fun cacheClientPriceList(
        nitCliente: String,
        items: List<PrecioArticuloBupasDto>
    ) {
        dao.upsertClientPriceList(
            CachedClientPriceListEntity(
                nitCliente = nitCliente.trim(),
                priceListJson = json.encodeToString(items),
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getCachedClientPriceList(
        nitCliente: String
    ): List<PrecioArticuloBupasDto>? {
        val entity = dao.getClientPriceList(nitCliente.trim()) ?: return null
        return runCatching {
            json.decodeFromString<List<PrecioArticuloBupasDto>>(entity.priceListJson)
        }.getOrNull()
    }
}

class InMemoryFieldCollectionLocalDataSource : FieldCollectionLocalDataSource {

    private val mutex = Mutex()
    private val cachedOrders = LinkedHashMap<String, OrderDto>()
    private val drafts = LinkedHashMap<String, StoredFieldCollectionDraftDto>()
    private val priceLists = mutableMapOf<String, List<PrecioArticuloBupasDto>>()

    override suspend fun cacheOrder(order: OrderDto) = mutex.withLock {
        cachedOrders[order.numeroSolicitud.uppercase()] = order
    }

    override suspend fun getCachedOrder(numeroSolicitud: String): OrderDto? = mutex.withLock {
        cachedOrders[numeroSolicitud.uppercase()]
    }

    override suspend fun getAllCachedOrders(): List<OrderDto> = mutex.withLock {
        cachedOrders.values.toList()
    }

    override suspend fun saveDraft(draft: StoredFieldCollectionDraftDto) = mutex.withLock {
        drafts[draft.numeroSolicitud.uppercase()] = draft
    }

    override suspend fun getDraft(numeroSolicitud: String): StoredFieldCollectionDraftDto? = mutex.withLock {
        drafts[numeroSolicitud.uppercase()]
    }

    override suspend fun getAllPendingDrafts(): List<StoredFieldCollectionDraftDto> = mutex.withLock {
        drafts.values.filter { it.pendingSync }
    }

    override suspend fun clearDraft(numeroSolicitud: String) {
        mutex.withLock {
            drafts.remove(numeroSolicitud.uppercase())
        }
    }

    override suspend fun cacheClientPriceList(
        nitCliente: String,
        items: List<PrecioArticuloBupasDto>
    ) = mutex.withLock {
        priceLists[nitCliente.trim()] = items
    }

    override suspend fun getCachedClientPriceList(
        nitCliente: String
    ): List<PrecioArticuloBupasDto>? = mutex.withLock {
        priceLists[nitCliente.trim()]
    }
}
