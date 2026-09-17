package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DrowsinessEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: DrowsinessEventEntity): Long

    @Update
    suspend fun update(event: DrowsinessEventEntity)

    @Delete
    suspend fun delete(event: DrowsinessEventEntity)

    @Query("DELETE FROM drowsiness_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM drowsiness_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<DrowsinessEventEntity>>

    @Query("SELECT * FROM drowsiness_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<DrowsinessEventEntity>>

    @Query("SELECT * FROM drowsiness_events WHERE uploadStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingUploadEvents(): List<DrowsinessEventEntity>

    @Query("UPDATE drowsiness_events SET uploadStatus = :status, uploadError = :error WHERE id = :id")
    suspend fun updateUploadStatus(id: Long, status: String, error: String? = null)

    @Query("UPDATE drowsiness_events SET uploadStatus = :status, uploadError = :error WHERE id IN (:ids)")
    suspend fun updateUploadStatusBatch(ids: List<Long>, status: String, error: String? = null)

    @Query("SELECT COUNT(*) FROM drowsiness_events WHERE uploadStatus = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM drowsiness_events WHERE uploadStatus = 'PENDING'")
    suspend fun getPendingCountDirect(): Int

    @Query("DELETE FROM drowsiness_events")
    suspend fun clearAll()

    // Backwards compatibility methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DrowsinessEventEntity): Long = insert(event)

    @Query("SELECT * FROM drowsiness_events WHERE uploadStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getUnsyncedEvents(): List<DrowsinessEventEntity> = getPendingUploadEvents()

    @Query("UPDATE drowsiness_events SET uploadStatus = 'UPLOADED', uploadError = NULL WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>) = updateUploadStatusBatch(ids, "UPLOADED", null)
}
