package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumerDao {
    @Query("SELECT * FROM consumers ORDER BY localName ASC")
    fun getAllConsumers(): Flow<List<Consumer>>

    @Query("SELECT * FROM consumers WHERE consumerNumber = :consumerNumber LIMIT 1")
    suspend fun getConsumerByNumber(consumerNumber: String): Consumer?

    @Query("SELECT * FROM consumers WHERE id = :id LIMIT 1")
    suspend fun getConsumerById(id: Int): Consumer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumer(consumer: Consumer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllConsumers(consumers: List<Consumer>)

    @Delete
    suspend fun deleteConsumer(consumer: Consumer)

    @Query("DELETE FROM consumers")
    suspend fun clearAll()
}
