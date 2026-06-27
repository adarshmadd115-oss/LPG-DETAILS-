package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "consumers")
data class Consumer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val consumerNumber: String,
    val localName: String,
    val officialPassbookName: String,
    val phone: String,
    val address: String,
    val companyType: String, // Indane, HP Gas, Bharat Gas, etc.
    val agencyName: String = "", // Specific agency name, e.g. "Sri Ram HP Agency"
    val hasPassbook: Boolean = false, // Passbook available status
    val cycleDays: Int = 25, // Prediction cycle days count (customizable per consumer)
    val lastAgencyBookingDate: String?, // YYYY-MM-DD
    val lastAgencyDeliveryDate: String?, // YYYY-MM-DD
    val lastCustomerDeliveryDate: String?, // YYYY-MM-DD
    val nextPredictedBookingDate: String?, // YYYY-MM-DD
    val transactionHistoryJson: String = "[]" // JSON representation of List<LpgTransaction>
) {
    // Helper to extract next predicted date
    fun getFormattedNextPredictedDate(): String {
        return nextPredictedBookingDate ?: "No date predicted"
    }
}

data class LpgTransaction(
    val date: String,
    val eventType: String, // "Booking", "Agency Delivery" (or Pick-up), "Customer Delivery"
    val status: String // "Pending", "Completed", etc.
) {
    override fun toString(): String {
        return "$date | $eventType | $status"
    }
}

object LpgCalculationUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun calculateNextPredictedBookingDate(lastCustomerDeliveryDateStr: String?, cycleDays: Int = 25): String? {
        if (lastCustomerDeliveryDateStr.isNullOrBlank()) return null
        return try {
            val date = dateFormat.parse(lastCustomerDeliveryDateStr) ?: return null
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_YEAR, cycleDays) // Customer Delivery + cycleDays days
            dateFormat.format(calendar.time)
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentDateStr(): String {
        return dateFormat.format(Date())
    }
}
