package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Customer
import com.example.data.CustomerAccountSummary
import com.example.data.CustomerShift
import com.example.data.DairyFormatters
import com.example.data.DairyRepository
import com.example.data.DeliveryShift
import com.example.data.MilkCollection
import com.example.data.MilkSupplier
import com.example.data.SupplierTransaction
import com.example.data.MilkEntry
import com.example.data.MilkType
import com.example.data.MonthlyCustomerStatement
import com.example.data.PaymentMode
import com.example.data.PaymentRecord
import com.example.data.RateChart
import com.example.data.SupplierSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DairyDataSnapshot(
    val customers: List<Customer> = emptyList(),
    val milkEntries: List<MilkEntry> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val collections: List<MilkCollection> = emptyList(),
    val suppliers: List<MilkSupplier> = emptyList(),
    val supplierTransactions: List<SupplierTransaction> = emptyList(),
    val rateOverrides: Map<String, Double> = emptyMap(),
    val importedRates: Map<String, Double> = emptyMap()
) {
    val supplierSummaries: List<SupplierSummary> by lazy {
        collections.groupBy { it.supplierName.trim().lowercase() }
            .map { (_, list) ->
                SupplierSummary(
                    supplierName = list.first().supplierName,
                    totalLiters = DairyFormatters.roundTwoDecimals(list.sumOf { it.quantityLiters }),
                    totalAmount = DairyFormatters.roundTwoDecimals(list.sumOf { it.totalAmount }),
                    entriesCount = list.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    val customerSummaries: List<CustomerAccountSummary> by lazy {
        val entriesByCustomer = milkEntries.groupBy { it.customerId }
        val paymentsByCustomer = payments.groupBy { it.customerId }
        customers.map { customer ->
            val custEntries = entriesByCustomer[customer.id].orEmpty()
            val custPayments = paymentsByCustomer[customer.id].orEmpty()
            val liters = custEntries.sumOf { if (it.absent) 0.0 else it.quantityLiters }
            val billed = custEntries.sumOf { if (it.absent) 0.0 else it.totalAmount }
            val paid = custPayments.sumOf { it.amount }
            val due = DairyFormatters.roundTwoDecimals(customer.openingBalance + billed - paid)
            CustomerAccountSummary(
                customer = customer,
                totalMilkLiters = DairyFormatters.roundTwoDecimals(liters),
                totalMilkAmount = DairyFormatters.roundTwoDecimals(billed),
                totalPaidAmount = DairyFormatters.roundTwoDecimals(paid),
                balanceDue = due
            )
        }
    }

    val summaryByCustomerId: Map<String, CustomerAccountSummary> by lazy {
        customerSummaries.associateBy { it.customer.id }
    }

    val totalPendingDues: Double by lazy {
        DairyFormatters.roundTwoDecimals(
            customerSummaries.filter { it.balanceDue > 0.0 }.sumOf { it.balanceDue }
        )
    }

    val totalAdvanceAmount: Double by lazy {
        DairyFormatters.roundTwoDecimals(
            customerSummaries.filter { it.balanceDue < 0.0 }.sumOf { -it.balanceDue }
        )
    }

    fun monthlyStatements(monthKey: String): List<MonthlyCustomerStatement> {
        val monthEntries = milkEntries.filter { it.dateKey.startsWith(monthKey) }
            .groupBy { it.customerId }
        val monthPayments = payments.filter { it.dateKey.startsWith(monthKey) }
            .groupBy { it.customerId }

        return customers.map { customer ->
            val entries = monthEntries[customer.id].orEmpty()
            val pays = monthPayments[customer.id].orEmpty()
            val deliveredDays = entries.filter { !it.absent && it.quantityLiters > 0.0 }
                .map { it.dateKey }
                .distinct()
                .size
            val absentDays = entries.filter { it.absent }
                .map { it.dateKey }
                .distinct()
                .size
            val liters = DairyFormatters.roundTwoDecimals(
                entries.sumOf { if (it.absent) 0.0 else it.quantityLiters }
            )
            val billed = DairyFormatters.roundTwoDecimals(
                entries.sumOf { if (it.absent) 0.0 else it.totalAmount }
            )
            val paid = DairyFormatters.roundTwoDecimals(pays.sumOf { it.amount })
            val overallDue = summaryByCustomerId[customer.id]?.balanceDue ?: customer.openingBalance

            MonthlyCustomerStatement(
                customer = customer,
                deliveredDaysCount = deliveredDays,
                absentDaysCount = absentDays,
                monthLiters = liters,
                monthBillAmount = billed,
                monthPaidAmount = paid,
                totalBalanceDue = overallDue
            )
        }
    }
}

class DairyViewModel(
    private val repository: DairyRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedDateKey = MutableStateFlow(DairyFormatters.todayDateKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _selectedShift = MutableStateFlow(DeliveryShift.MORNING)
    val selectedShift: StateFlow<DeliveryShift> = _selectedShift.asStateFlow()

    private val _selectedMonthKey = MutableStateFlow(DairyFormatters.currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val rateDataFlow = combine(
        repository.observeRateOverrides(currentUserId)
            .catch { emit(emptyMap<String, Double>()) },
        repository.observeImportedChart(currentUserId)
            .catch { emit(emptyMap<String, Double>()) }
    ) { overrides, imported -> overrides to imported }

    private val coreDataFlow = combine(
        repository.observeCustomers(currentUserId),
        repository.observeMilkEntries(currentUserId),
        repository.observePayments(currentUserId),
        repository.observeCollections(currentUserId)
            .catch { emit(emptyList<MilkCollection>()) },
        rateDataFlow
    ) { customers, entries, payments, collections, rateData ->
        val (rateOverrides, importedRates) = rateData
        DairyDataSnapshot(
            customers = customers,
            milkEntries = entries,
            payments = payments,
            collections = collections,
            rateOverrides = rateOverrides,
            importedRates = importedRates
        )
    }

    val uiState: StateFlow<UiState<DairyDataSnapshot>> = combine(
        coreDataFlow,
        repository.observeSuppliers(currentUserId)
            .catch { emit(emptyList<MilkSupplier>()) },
        repository.observeSupplierTransactions(currentUserId)
            .catch { emit(emptyList<SupplierTransaction>()) }
    ) { core, suppliers, txns ->
        UiState.Success(
            core.copy(suppliers = suppliers, supplierTransactions = txns)
        ) as UiState<DairyDataSnapshot>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing dairy data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "डेटा लोड करने में समस्या आई।"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun changeSelectedDate(daysOffset: Int) {
        _selectedDateKey.value = DairyFormatters.shiftDateKey(_selectedDateKey.value, daysOffset)
    }

    fun resetToToday() {
        _selectedDateKey.value = DairyFormatters.todayDateKey()
    }

    fun selectShift(shift: DeliveryShift) {
        _selectedShift.value = shift
    }

    fun changeSelectedMonth(monthsOffset: Int) {
        _selectedMonthKey.value = DairyFormatters.shiftMonthKey(_selectedMonthKey.value, monthsOffset)
    }

    fun notifyUser(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun saveCustomer(
        existingId: String?,
        name: String,
        phone: String,
        address: String,
        milkType: MilkType,
        defaultShift: CustomerShift,
        defaultQtyLiters: Double,
        ratePerLiter: Double,
        openingBalance: Double,
        active: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = if (existingId.isNullOrBlank()) {
                repository.addCustomer(
                    userId = currentUserId,
                    name = name,
                    phone = phone,
                    address = address,
                    milkType = milkType,
                    defaultShift = defaultShift,
                    defaultQtyLiters = defaultQtyLiters,
                    ratePerLiter = ratePerLiter,
                    openingBalance = openingBalance
                ).map { Unit }
            } else {
                repository.updateCustomer(
                    userId = currentUserId,
                    customerId = existingId,
                    name = name,
                    phone = phone,
                    address = address,
                    milkType = milkType,
                    defaultShift = defaultShift,
                    defaultQtyLiters = defaultQtyLiters,
                    ratePerLiter = ratePerLiter,
                    openingBalance = openingBalance,
                    active = active
                )
            }
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = if (existingId.isNullOrBlank()) {
                    "नया ग्राहक सहेजा गया"
                } else {
                    "ग्राहक विवरण अपडेट हुआ"
                }
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "ग्राहक सेव करने में त्रुटि हुई।"
            }
        }
    }

    fun deleteCustomer(customerId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteCustomer(currentUserId, customerId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "ग्राहक हटाया गया"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "ग्राहक हटाने में त्रुटि हुई।"
            }
        }
    }

    fun saveMilkEntry(
        existingEntryId: String?,
        customer: Customer,
        dateKey: String,
        shift: DeliveryShift,
        milkType: MilkType,
        quantityLiters: Double,
        ratePerLiter: Double,
        absent: Boolean,
        note: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = if (existingEntryId.isNullOrBlank()) {
                repository.addMilkEntry(
                    userId = currentUserId,
                    customerId = customer.id,
                    customerName = customer.name,
                    dateKey = dateKey,
                    shift = shift,
                    milkType = milkType,
                    quantityLiters = quantityLiters,
                    ratePerLiter = ratePerLiter,
                    absent = absent,
                    note = note
                )
            } else {
                repository.updateMilkEntry(
                    userId = currentUserId,
                    entryId = existingEntryId,
                    customerName = customer.name,
                    dateKey = dateKey,
                    shift = shift,
                    milkType = milkType,
                    quantityLiters = quantityLiters,
                    ratePerLiter = ratePerLiter,
                    absent = absent,
                    note = note
                )
            }
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = when {
                    absent -> "${customer.name} - छुट्टी दर्ज हुई"
                    existingEntryId.isNullOrBlank() ->
                        "${customer.name} - ${DairyFormatters.formatLiters(quantityLiters)} दूध दर्ज हुआ"
                    else -> "दूध एंट्री अपडेट हुई"
                }
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "दूध एंट्री सेव नहीं हो सकी।"
            }
        }
    }

    fun deleteMilkEntry(entryId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteMilkEntry(currentUserId, entryId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "दूध एंट्री हटाई गई"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "एंट्री हटाने में त्रुटि हुई।"
            }
        }
    }

    fun savePayment(
        existingPaymentId: String?,
        customer: Customer,
        dateKey: String,
        amount: Double,
        paymentMode: PaymentMode,
        note: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = if (existingPaymentId.isNullOrBlank()) {
                repository.addPayment(
                    userId = currentUserId,
                    customerId = customer.id,
                    customerName = customer.name,
                    dateKey = dateKey,
                    amount = amount,
                    paymentMode = paymentMode,
                    note = note
                )
            } else {
                repository.updatePayment(
                    userId = currentUserId,
                    paymentId = existingPaymentId,
                    customerName = customer.name,
                    dateKey = dateKey,
                    amount = amount,
                    paymentMode = paymentMode,
                    note = note
                )
            }
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = if (existingPaymentId.isNullOrBlank()) {
                    "${customer.name} से ${DairyFormatters.formatRupees(amount)} पेमेंट दर्ज हुआ"
                } else {
                    "पेमेंट विवरण अपडेट हुआ"
                }
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "पेमेंट सेव करने में त्रुटि हुई।"
            }
        }
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deletePayment(currentUserId, paymentId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "पेमेंट रिकॉर्ड हटाया गया"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "पेमेंट हटाने में त्रुटि हुई।"
            }
        }
    }

    fun saveCollectionEntry(
        supplierName: String,
        sampleNo: String,
        dateKey: String,
        shift: DeliveryShift,
        quantityLiters: Double,
        fat: Double,
        snf: Double,
        ratePerLiter: Double,
        totalAmount: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addCollection(
                userId = currentUserId,
                supplierName = supplierName,
                sampleNo = sampleNo,
                dateKey = dateKey,
                shift = shift,
                quantityLiters = quantityLiters,
                fat = fat,
                snf = snf,
                ratePerLiter = ratePerLiter,
                totalAmount = totalAmount
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value =
                    "$supplierName का दूध दर्ज हुआ — ${DairyFormatters.formatRupees(totalAmount)}"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "दूध दर्ज करने में त्रुटि हुई।"
            }
        }
    }

    fun addSupplier(name: String, phone: String = "") {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _snackbarMessage.value = "Supplier का नाम लिखो।"
            return
        }
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addSupplier(currentUserId, cleanName, phone.trim())
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Supplier जुड़ गया: $cleanName"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Supplier जोड़ने में त्रुटि हुई।"
            }
        }
    }

    fun deleteSupplier(supplierId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteSupplier(currentUserId, supplierId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Supplier हटा दिया गया"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Supplier हटाने में त्रुटि हुई।"
            }
        }
    }

    fun addSupplierTransaction(
        supplierName: String,
        type: String,
        itemName: String,
        quantity: Double,
        unit: String,
        ratePerUnit: Double,
        amount: Double,
        paymentMode: String,
        note: String,
        dateKey: String,
        isHome: Boolean
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addSupplierTransaction(
                userId = currentUserId,
                supplierName = supplierName,
                type = type,
                itemName = itemName,
                quantity = quantity,
                unit = unit,
                ratePerUnit = ratePerUnit,
                amount = amount,
                paymentMode = paymentMode,
                note = note,
                dateKey = dateKey,
                isHome = isHome
            )
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = when (type) {
                    "GOODS" -> "समान दर्ज हुआ — ${DairyFormatters.formatRupees(amount)}"
                    "ADVANCE" -> "Advance दर्ज हुआ — ${DairyFormatters.formatRupees(amount)}"
                    else -> "Payment दर्ज हुआ — ${DairyFormatters.formatRupees(amount)}"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Entry दर्ज करने में त्रुटि हुई।"
            }
        }
    }

    fun deleteSupplierTransaction(transactionId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteSupplierTransaction(currentUserId, transactionId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Entry हटाई गई"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Entry हटाने में त्रुटि हुई।"
            }
        }
    }

    fun deleteCollectionEntry(collectionId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteCollection(currentUserId, collectionId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "कलेक्शन रिकॉर्ड हटाया गया"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "रिकॉर्ड हटाने में त्रुटि हुई।"
            }
        }
    }

    fun saveRateOverride(fat: Double, snf: Double, rate: Double) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveRateOverride(currentUserId, fat, snf, rate)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "रेट अपडेट हुआ — FAT $fat / SNF $snf = ${DairyFormatters.formatRupees(rate)}"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "रेट सेव करने में त्रुटि हुई।"
            }
        }
    }

    fun resetRateOverride(fat: Double, snf: Double) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteRateOverride(currentUserId, fat, snf)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "रेट असली लिस्ट पर वापस हुआ"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "रेट रीसेट करने में त्रुटि हुई।"
            }
        }
    }

    fun importRateChart(rates: Map<String, Double>, sourceName: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveImportedChart(currentUserId, rates, sourceName)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "नयी Rate List लागू हो गयी — ${rates.size} रेट"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "लिस्ट लागू करने में त्रुटि हुई।"
            }
        }
    }

    fun clearImportedChart() {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteImportedChart(currentUserId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "असली Rate List वापस लागू हुई"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "लिस्ट हटाने में त्रुटि हुई।"
            }
        }
    }

    fun buildMonthlyBillText(
        statement: MonthlyCustomerStatement,
        monthKey: String
    ): String {
        val monthLabel = DairyFormatters.formatMonthDisplayHi(monthKey)
        val balanceLine = when {
            statement.totalBalanceDue > 0.0 ->
                "कुल शेष बकाया: ${DairyFormatters.formatRupees(statement.totalBalanceDue)}"
            statement.totalBalanceDue < 0.0 ->
                "कुल एडवांस जमा: ${DairyFormatters.formatRupees(-statement.totalBalanceDue)}"
            else -> "कुल शेष बकाया: ₹0 (हिसाब बराबर)"
        }
        return buildString {
            appendLine("मिल्क डायरी - मासिक दूध बिल")
            appendLine("ग्राहक: ${statement.customer.name}")
            appendLine("महीना: $monthLabel")
            appendLine("------------------------")
            appendLine("दूध के दिन: ${statement.deliveredDaysCount} दिन (छुट्टी: ${statement.absentDaysCount} दिन)")
            appendLine("कुल दूध: ${DairyFormatters.formatLiters(statement.monthLiters)}")
            appendLine("इस महीने का बिल: ${DairyFormatters.formatRupees(statement.monthBillAmount)}")
            appendLine("इस महीने जमा पेमेंट: ${DairyFormatters.formatRupees(statement.monthPaidAmount)}")
            appendLine("------------------------")
            appendLine(balanceLine)
            append("धन्यवाद!")
        }
    }

    private companion object {
        const val TAG = "DairyViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
