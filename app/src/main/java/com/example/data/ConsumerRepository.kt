package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ConsumerRepository(private val consumerDao: ConsumerDao) {

    val allConsumers: Flow<List<Consumer>> = consumerDao.getAllConsumers()

    suspend fun insert(consumer: Consumer) = withContext(Dispatchers.IO) {
        consumerDao.insertConsumer(consumer)
    }

    suspend fun insertAll(consumers: List<Consumer>) = withContext(Dispatchers.IO) {
        consumerDao.insertAllConsumers(consumers)
    }

    suspend fun delete(consumer: Consumer) = withContext(Dispatchers.IO) {
        consumerDao.deleteConsumer(consumer)
    }

    suspend fun getConsumerByNumber(number: String): Consumer? = withContext(Dispatchers.IO) {
        consumerDao.getConsumerByNumber(number)
    }

    suspend fun getConsumerById(id: Int): Consumer? = withContext(Dispatchers.IO) {
        consumerDao.getConsumerById(id)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        consumerDao.clearAll()
    }
}

