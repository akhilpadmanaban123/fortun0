package app.paisapilot.data.local.dao

import androidx.room.Upsert
import app.paisapilot.data.local.entity.Syncable

/** What the sync engine needs from every table. Each DAO supplies the queries. */
interface SyncDao<T : Syncable> {
    @Upsert
    suspend fun upsert(row: T)

    @Upsert
    suspend fun upsertAll(rows: List<T>)

    /** Includes tombstones. */
    suspend fun byId(id: String): T?

    suspend fun dirtyRows(): List<T>

    /** Clears the dirty flag only if the row was not edited again while uploading. */
    suspend fun markClean(id: String, updatedAt: Long)

    suspend fun softDelete(id: String, now: Long)
}
