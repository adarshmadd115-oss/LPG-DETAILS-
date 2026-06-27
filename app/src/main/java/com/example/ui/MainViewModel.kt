package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ConsumerRepository(database.consumerDao())

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val transactionsType = Types.newParameterizedType(List::class.java, LpgTransaction::class.java)
    private val transactionsAdapter = moshi.adapter<List<LpgTransaction>>(transactionsType)

    // Observable states
    val consumers: StateFlow<List<Consumer>> = repository.allConsumers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Basic CRUD operations ---

    fun addOrUpdateConsumer(
        id: Int = 0,
        consumerNumber: String,
        localName: String,
        officialPassbookName: String,
        phone: String,
        address: String,
        companyType: String,
        agencyName: String,
        hasPassbook: Boolean,
        cycleDays: Int = 25,
        lastAgencyBookingDate: String? = null,
        lastAgencyDeliveryDate: String? = null,
        lastCustomerDeliveryDate: String? = null,
        nextPredictedBookingDate: String? = null
    ) {
        viewModelScope.launch {
            val existing = if (id != 0) {
                repository.getConsumerById(id)
            } else {
                repository.getConsumerByNumber(consumerNumber)
            }

            // Construct or append transaction history
            val newTransactions = mutableListOf<LpgTransaction>()
            if (existing != null) {
                try {
                    val oldList = transactionsAdapter.fromJson(existing.transactionHistoryJson)
                    if (oldList != null) {
                        newTransactions.addAll(oldList)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing transaction history JSON", e)
                }
            }

            val currentDate = LpgCalculationUtils.getCurrentDateStr()

            if (existing == null) {
                newTransactions.add(
                    LpgTransaction(currentDate, "Consumer Created", "Success")
                )
            } else {
                if (!lastAgencyBookingDate.isNullOrBlank() && lastAgencyBookingDate != existing.lastAgencyBookingDate) {
                    newTransactions.add(LpgTransaction(lastAgencyBookingDate, "Booking Request", "Completed"))
                }
                if (!lastAgencyDeliveryDate.isNullOrBlank() && lastAgencyDeliveryDate != existing.lastAgencyDeliveryDate) {
                    newTransactions.add(LpgTransaction(lastAgencyDeliveryDate, "Agency Pickup/Delivery", "Completed"))
                }
                if (!lastCustomerDeliveryDate.isNullOrBlank() && lastCustomerDeliveryDate != existing.lastCustomerDeliveryDate) {
                    newTransactions.add(LpgTransaction(lastCustomerDeliveryDate, "Customer Delivery/Refill", "Completed"))
                }
            }

            // Save user edited prediction date if provided, otherwise compute programmatically if last refill changed
            val finalPredictedDate = if (!nextPredictedBookingDate.isNullOrBlank()) {
                nextPredictedBookingDate
            } else if (!lastCustomerDeliveryDate.isNullOrBlank() && lastCustomerDeliveryDate != existing?.lastCustomerDeliveryDate) {
                LpgCalculationUtils.calculateNextPredictedBookingDate(lastCustomerDeliveryDate, cycleDays)
            } else if (existing != null && existing.cycleDays != cycleDays && !existing.lastCustomerDeliveryDate.isNullOrBlank()) {
                // If cycle days configuration changed, recalculate based on the existing delivery date
                LpgCalculationUtils.calculateNextPredictedBookingDate(existing.lastCustomerDeliveryDate, cycleDays)
            } else {
                existing?.nextPredictedBookingDate
            }

            val historyJson = transactionsAdapter.toJson(newTransactions) ?: "[]"

            val updatedConsumer = Consumer(
                id = existing?.id ?: 0,
                consumerNumber = consumerNumber,
                localName = localName,
                officialPassbookName = officialPassbookName,
                phone = phone,
                address = address,
                companyType = companyType,
                agencyName = agencyName,
                hasPassbook = hasPassbook,
                cycleDays = cycleDays,
                lastAgencyBookingDate = lastAgencyBookingDate,
                lastAgencyDeliveryDate = lastAgencyDeliveryDate,
                lastCustomerDeliveryDate = lastCustomerDeliveryDate,
                nextPredictedBookingDate = finalPredictedDate,
                transactionHistoryJson = historyJson
            )

            repository.insert(updatedConsumer)
        }
    }

    fun deleteConsumer(consumer: Consumer) {
        viewModelScope.launch {
            repository.delete(consumer)
        }
    }

    // Log explicit event by specific database ID
    fun logDateEvent(consumerId: Int, eventType: String, eventDate: String, amountStatus: String = "Completed") {
        viewModelScope.launch {
            val c = repository.getConsumerById(consumerId) ?: return@launch
            val list = mutableListOf<LpgTransaction>()
            try {
                val oldList = transactionsAdapter.fromJson(c.transactionHistoryJson)
                if (oldList != null) list.addAll(oldList)
            } catch (e: Exception) {
                // ignore
            }

            // Append new historical transaction
            list.add(LpgTransaction(eventDate, eventType, amountStatus))

            var booking = c.lastAgencyBookingDate
            var agencyDel = c.lastAgencyDeliveryDate
            var custDel = c.lastCustomerDeliveryDate
            var predicted = c.nextPredictedBookingDate

            when (eventType) {
                "Booking", "Booking Request" -> booking = eventDate
                "Agency Delivery", "Agency Pickup/Delivery" -> agencyDel = eventDate
                "Customer Delivery", "Customer Delivery/Refill" -> {
                    custDel = eventDate
                    predicted = LpgCalculationUtils.calculateNextPredictedBookingDate(eventDate, c.cycleDays)
                }
            }

            val updated = c.copy(
                lastAgencyBookingDate = booking,
                lastAgencyDeliveryDate = agencyDel,
                lastCustomerDeliveryDate = custDel,
                nextPredictedBookingDate = predicted,
                transactionHistoryJson = transactionsAdapter.toJson(list) ?: "[]"
            )

            repository.insert(updated)
        }
    }

    // Helper to delete a specific transaction event by database index
    fun deleteTransactionEvent(consumerId: Int, index: Int) {
        viewModelScope.launch {
            val c = repository.getConsumerById(consumerId) ?: return@launch
            val list = mutableListOf<LpgTransaction>()
            try {
                val oldList = transactionsAdapter.fromJson(c.transactionHistoryJson)
                if (oldList != null) list.addAll(oldList)
            } catch (e: Exception) {
                // ignore
            }

            if (index in list.indices) {
                list.removeAt(index)
                val updated = c.copy(
                    transactionHistoryJson = transactionsAdapter.toJson(list) ?: "[]"
                )
                repository.insert(updated)
            }
        }
    }
}
