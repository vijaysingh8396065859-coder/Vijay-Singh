package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

enum class MilkType(val labelHi: String) {
    BUFFALO("भैंस"),
    COW("गाय"),
    MIXED("मिक्स");

    companion object {
        fun fromName(raw: String): MilkType =
            entries.firstOrNull { it.name == raw } ?: BUFFALO
    }
}

enum class DeliveryShift(val labelHi: String) {
    MORNING("सुबह"),
    EVENING("शाम");

    companion object {
        fun fromName(raw: String): DeliveryShift =
            entries.firstOrNull { it.name == raw } ?: MORNING
    }
}

enum class CustomerShift(val labelHi: String) {
    MORNING("सुबह"),
    EVENING("शाम"),
    BOTH("सुबह व शाम");

    companion object {
        fun fromName(raw: String): CustomerShift =
            entries.firstOrNull { it.name == raw } ?: MORNING
    }
}

enum class PaymentMode(val labelHi: String) {
    CASH("नकद"),
    UPI("UPI"),
    BANK("बैंक");

    companion object {
        fun fromName(raw: String): PaymentMode =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val milkType: String = MilkType.BUFFALO.name,
    val defaultShift: String = CustomerShift.MORNING.name,
    val defaultQtyLiters: Double = 1.0,
    val ratePerLiter: Double = 65.0,
    val openingBalance: Double = 0.0,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedMilkType: MilkType get() = MilkType.fromName(milkType)
    val parsedDefaultShift: CustomerShift get() = CustomerShift.fromName(defaultShift)
}

@Keep
@IgnoreExtraProperties
data class MilkEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val dateKey: String = "",
    val shift: String = DeliveryShift.MORNING.name,
    val milkType: String = MilkType.BUFFALO.name,
    val quantityLiters: Double = 0.0,
    val ratePerLiter: Double = 0.0,
    val totalAmount: Double = 0.0,
    val absent: Boolean = false,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedShift: DeliveryShift get() = DeliveryShift.fromName(shift)
    val parsedMilkType: MilkType get() = MilkType.fromName(milkType)
}

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val dateKey: String = "",
    val amount: Double = 0.0,
    val paymentMode: String = PaymentMode.CASH.name,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedMode: PaymentMode get() = PaymentMode.fromName(paymentMode)
}

data class CustomerAccountSummary(
    val customer: Customer,
    val totalMilkLiters: Double,
    val totalMilkAmount: Double,
    val totalPaidAmount: Double,
    val balanceDue: Double
)

data class MonthlyCustomerStatement(
    val customer: Customer,
    val deliveredDaysCount: Int,
    val absentDaysCount: Int,
    val monthLiters: Double,
    val monthBillAmount: Double,
    val monthPaidAmount: Double,
    val totalBalanceDue: Double
)

object DairyFormatters {
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    private val hindiMonths = listOf(
        "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

    private val hindiWeekdays = mapOf(
        Calendar.SUNDAY to "रवि",
        Calendar.MONDAY to "सोम",
        Calendar.TUESDAY to "मंगल",
        Calendar.WEDNESDAY to "बुध",
        Calendar.THURSDAY to "गुरु",
        Calendar.FRIDAY to "शुक्र",
        Calendar.SATURDAY to "शनि"
    )

    fun todayDateKey(): String = synchronized(dateKeyFormat) {
        dateKeyFormat.format(Date())
    }

    fun currentMonthKey(): String = synchronized(monthKeyFormat) {
        monthKeyFormat.format(Date())
    }

    fun shiftDateKey(dateKey: String, daysOffset: Int): String = synchronized(dateKeyFormat) {
        val parsed = runCatching { dateKeyFormat.parse(dateKey) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, daysOffset)
        }
        dateKeyFormat.format(cal.time)
    }

    /** 10-din cycle (1-10, 11-20, 21-mahine ke aakhir) ki shuruaat wali dateKey. */
    fun cycleStartKey(dateKey: String): String = synchronized(dateKeyFormat) {
        val parsed = runCatching { dateKeyFormat.parse(dateKey) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply { time = parsed }
        val startDay = when (cal.get(Calendar.DAY_OF_MONTH)) {
            in 1..10 -> 1
            in 11..20 -> 11
            else -> 21
        }
        cal.set(Calendar.DAY_OF_MONTH, startDay)
        dateKeyFormat.format(cal.time)
    }

    /** Us cycle ki aakhri dateKey. */
    fun cycleEndKey(dateKey: String): String = synchronized(dateKeyFormat) {
        val parsed = runCatching { dateKeyFormat.parse(cycleStartKey(dateKey)) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply { time = parsed }
        when (cal.get(Calendar.DAY_OF_MONTH)) {
            1 -> cal.set(Calendar.DAY_OF_MONTH, 10)
            11 -> cal.set(Calendar.DAY_OF_MONTH, 20)
            else -> cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        dateKeyFormat.format(cal.time)
    }

    /** Poora ek cycle aage/peeche jao (offset +1 = agla cycle, -1 = pichla). */
    fun shiftCycleKey(dateKey: String, cyclesOffset: Int): String {
        var current = cycleStartKey(dateKey)
        if (cyclesOffset == 0) return current
        val step = if (cyclesOffset > 0) 1 else -1
        repeat(kotlin.math.abs(cyclesOffset)) {
            current = synchronized(dateKeyFormat) {
                val parsed = runCatching { dateKeyFormat.parse(current) }.getOrNull() ?: Date()
                val cal = Calendar.getInstance().apply { time = parsed }
                if (step > 0) {
                    when (cal.get(Calendar.DAY_OF_MONTH)) {
                        1 -> cal.set(Calendar.DAY_OF_MONTH, 11)
                        11 -> cal.set(Calendar.DAY_OF_MONTH, 21)
                        else -> {
                            cal.add(Calendar.MONTH, 1)
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                } else {
                    when (cal.get(Calendar.DAY_OF_MONTH)) {
                        11 -> cal.set(Calendar.DAY_OF_MONTH, 1)
                        21 -> cal.set(Calendar.DAY_OF_MONTH, 11)
                        else -> {
                            cal.add(Calendar.MONTH, -1)
                            cal.set(Calendar.DAY_OF_MONTH, 21)
                        }
                    }
                }
                dateKeyFormat.format(cal.time)
            }
        }
        return current
    }

    fun shiftMonthKey(monthKey: String, monthsOffset: Int): String = synchronized(monthKeyFormat) {
        val parsed = runCatching { monthKeyFormat.parse(monthKey) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.MONTH, monthsOffset)
        }
        monthKeyFormat.format(cal.time)
    }

    fun formatDateDisplayHi(dateKey: String): String = synchronized(dateKeyFormat) {
        val parsed = runCatching { dateKeyFormat.parse(dateKey) }.getOrNull() ?: return dateKey
        val cal = Calendar.getInstance().apply { time = parsed }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = hindiMonths[cal.get(Calendar.MONTH)]
        val weekday = hindiWeekdays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
        val today = todayDateKey()
        val yesterday = shiftDateKey(today, -1)
        return when (dateKey) {
            today -> "आज • $day $month"
            yesterday -> "कल • $day $month"
            else -> "$weekday, $day $month ${cal.get(Calendar.YEAR)}"
        }
    }

    fun formatShortDateHi(dateKey: String): String = synchronized(dateKeyFormat) {
        val parsed = runCatching { dateKeyFormat.parse(dateKey) }.getOrNull() ?: return dateKey
        val cal = Calendar.getInstance().apply { time = parsed }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = hindiMonths[cal.get(Calendar.MONTH)]
        return "$day $month"
    }

    fun formatMonthDisplayHi(monthKey: String): String = synchronized(monthKeyFormat) {
        val parsed = runCatching { monthKeyFormat.parse(monthKey) }.getOrNull() ?: return monthKey
        val cal = Calendar.getInstance().apply { time = parsed }
        val month = hindiMonths[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$month $year"
    }

    fun formatRupees(amount: Double): String {
        val rounded = roundTwoDecimals(amount)
        val df = if (abs(rounded - rounded.roundToLong().toDouble()) < 0.005) {
            DecimalFormat("#,##0")
        } else {
            DecimalFormat("#,##0.##")
        }
        return "₹${df.format(rounded)}"
    }

    fun formatLiters(liters: Double): String {
        val rounded = roundTwoDecimals(liters)
        val df = DecimalFormat("0.##")
        return "${df.format(rounded)} ली."
    }

    fun formatLitersNumeric(liters: Double): String {
        val rounded = roundTwoDecimals(liters)
        return DecimalFormat("0.##").format(rounded)
    }

    fun roundTwoDecimals(value: Double): Double =
        (value * 100.0).roundToLong() / 100.0
}

@Keep
@IgnoreExtraProperties
data class MilkCollection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val supplierName: String = "",
    val sampleNo: String = "",
    val dateKey: String = "",
    val shift: String = DeliveryShift.MORNING.name,
    val quantityLiters: Double = 0.0,
    val fat: Double = 0.0,
    val snf: Double = 0.0,
    val ratePerLiter: Double = 0.0,
    val totalAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedShift: DeliveryShift get() = DeliveryShift.fromName(shift)
}

data class MilkSupplier(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val village: String = "",
    val sortOrder: Long = 0,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)

data class SupplierTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val supplierName: String = "",
    val type: String = "GOODS",
    val itemName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "BORI",
    val ratePerUnit: Double = 0.0,
    val amount: Double = 0.0,
    val paymentMode: String = "CASH",
    val note: String = "",
    val dateKey: String = "",
    val isHome: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SupplierSummary(
    val supplierName: String,
    val totalLiters: Double,
    val totalAmount: Double,
    val entriesCount: Int
)
