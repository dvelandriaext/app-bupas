package co.com.coexito.bupas.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "cached_orders")
data class CachedOrderEntity(
    @PrimaryKey
    val numeroSolicitud: String,
    val orderJson: String,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "field_collection_drafts")
data class FieldCollectionDraftEntity(
    @PrimaryKey
    val numeroSolicitud: String,
    val draftJson: String,
    val pendingSync: Boolean,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "cached_client_price_lists")
data class CachedClientPriceListEntity(
    @PrimaryKey
    val nitCliente: String,
    val priceListJson: String,
    val updatedAtEpochMs: Long
)

@Dao
interface FieldCollectionDraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedOrder(entity: CachedOrderEntity)

    @Query("SELECT * FROM cached_orders WHERE UPPER(numeroSolicitud) = UPPER(:numeroSolicitud) LIMIT 1")
    suspend fun getCachedOrder(numeroSolicitud: String): CachedOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(entity: FieldCollectionDraftEntity)

    @Query("SELECT * FROM field_collection_drafts WHERE UPPER(numeroSolicitud) = UPPER(:numeroSolicitud) LIMIT 1")
    suspend fun getDraft(numeroSolicitud: String): FieldCollectionDraftEntity?

    @Query("DELETE FROM field_collection_drafts WHERE UPPER(numeroSolicitud) = UPPER(:numeroSolicitud)")
    suspend fun deleteDraft(numeroSolicitud: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClientPriceList(entity: CachedClientPriceListEntity)

    @Query("SELECT * FROM cached_client_price_lists WHERE nitCliente = :nitCliente LIMIT 1")
    suspend fun getClientPriceList(nitCliente: String): CachedClientPriceListEntity?
}

@Database(
    entities = [
        CachedOrderEntity::class,
        FieldCollectionDraftEntity::class,
        CachedClientPriceListEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BupasDatabase : RoomDatabase() {
    abstract fun fieldCollectionDraftDao(): FieldCollectionDraftDao

    companion object {
        const val DATABASE_NAME = "bupas_local.db"
    }
}
