package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DairyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("डेटा सहेजने के लिए कृपया Google से साइन इन करें।")
        }
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("खाता बदल गया है; कृपया पुनः प्रयास करें।")
        }
        return uid
    }

    private fun isValidDateKey(dateKey: String): Boolean =
        dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))

    // --- REALTIME SNAPSHOT OBSERVERS ---

    fun observeCustomers(userId: String): Flow<List<Customer>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/customers"
        emitAll(
            db.collection("users").document(uid).collection("customers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Customer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<Customer> { it.active }
                            .thenBy { it.name.lowercase() }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMilkEntries(userId: String): Flow<List<MilkEntry>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/milk_entries"
        emitAll(
            db.collection("users").document(uid).collection("milk_entries")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MilkEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<MilkEntry> { it.dateKey }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePayments(userId: String): Flow<List<PaymentRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/payments"
        emitAll(
            db.collection("users").document(uid).collection("payments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<PaymentRecord> { it.dateKey }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- CUSTOMER MUTATIONS ---

    suspend fun addCustomer(
        userId: String,
        name: String,
        phone: String,
        address: String,
        milkType: MilkType,
        defaultShift: CustomerShift,
        defaultQtyLiters: Double,
        ratePerLiter: Double,
        openingBalance: Double
    ): Result<String> {
        var docPath = "users/$userId/customers"
        return try {
            val uid = requireUserId(userId)
            val cleanName = name.trim()
            val cleanPhone = phone.trim()
            val cleanAddress = address.trim()
            require(cleanName.length in 1..80) { "ग्राहक का नाम 1 से 80 अक्षरों का होना चाहिए।" }
            require(cleanPhone.length <= 20) { "मोबाइल नंबर अधिकतम 20 अंकों का हो सकता है।" }
            require(cleanAddress.length <= 120) { "पता अधिकतम 120 अक्षरों का हो सकता है।" }
            require(defaultQtyLiters in 0.0..1000.0) { "दैनिक दूध की मात्रा 0 से 1000 लीटर के बीच होनी चाहिए।" }
            require(ratePerLiter in 1.0..1000.0) { "दूध का भाव ₹1 से ₹1000 प्रति लीटर के बीच होना चाहिए।" }
            require(openingBalance in -1000000.0..1000000.0) { "पुराना बकाया मान्य सीमा में होना चाहिए।" }

            val docRef = db.collection("users").document(uid).collection("customers").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "phone" to cleanPhone,
                "address" to cleanAddress,
                "milkType" to milkType.name,
                "defaultShift" to defaultShift.name,
                "defaultQtyLiters" to DairyFormatters.roundTwoDecimals(defaultQtyLiters),
                "ratePerLiter" to DairyFormatters.roundTwoDecimals(ratePerLiter),
                "openingBalance" to DairyFormatters.roundTwoDecimals(openingBalance),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCustomer(
        userId: String,
        customerId: String,
        name: String,
        phone: String,
        address: String,
        milkType: MilkType,
        defaultShift: CustomerShift,
        defaultQtyLiters: Double,
        ratePerLiter: Double,
        openingBalance: Double,
        active: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/customers/$customerId"
        return try {
            val uid = requireUserId(userId)
            require(customerId.isNotBlank()) { "ग्राहक ID खाली नहीं हो सकती।" }
            val cleanName = name.trim()
            val cleanPhone = phone.trim()
            val cleanAddress = address.trim()
            require(cleanName.length in 1..80) { "ग्राहक का नाम 1 से 80 अक्षरों का होना चाहिए।" }
            require(cleanPhone.length <= 20) { "मोबाइल नंबर अधिकतम 20 अंकों का हो सकता है।" }
            require(cleanAddress.length <= 120) { "पता अधिकतम 120 अक्षरों का हो सकता है।" }
            require(defaultQtyLiters in 0.0..1000.0) { "दैनिक दूध की मात्रा 0 से 1000 लीटर के बीच होनी चाहिए।" }
            require(ratePerLiter in 1.0..1000.0) { "दूध का भाव ₹1 से ₹1000 प्रति लीटर के बीच होना चाहिए।" }
            require(openingBalance in -1000000.0..1000000.0) { "पुराना बकाया मान्य सीमा में होना चाहिए।" }

            val docRef = db.collection("users").document(uid)
                .collection("customers").document(customerId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "phone" to cleanPhone,
                    "address" to cleanAddress,
                    "milkType" to milkType.name,
                    "defaultShift" to defaultShift.name,
                    "defaultQtyLiters" to DairyFormatters.roundTwoDecimals(defaultQtyLiters),
                    "ratePerLiter" to DairyFormatters.roundTwoDecimals(ratePerLiter),
                    "openingBalance" to DairyFormatters.roundTwoDecimals(openingBalance),
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Result<Unit> {
        var docPath = "users/$userId/customers/$customerId"
        return try {
            val uid = requireUserId(userId)
            require(customerId.isNotBlank()) { "ग्राहक ID खाली नहीं हो सकती।" }
            val docRef = db.collection("users").document(uid)
                .collection("customers").document(customerId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- MILK ENTRY MUTATIONS ---

    suspend fun addMilkEntry(
        userId: String,
        customerId: String,
        customerName: String,
        dateKey: String,
        shift: DeliveryShift,
        milkType: MilkType,
        quantityLiters: Double,
        ratePerLiter: Double,
        absent: Boolean,
        note: String
    ): Result<Unit> {
        var docPath = "users/$userId/milk_entries"
        return try {
            val uid = requireUserId(userId)
            val cleanName = customerName.trim()
            val cleanNote = note.trim()
            val effectiveQty = if (absent) 0.0 else DairyFormatters.roundTwoDecimals(quantityLiters)
            val effectiveRate = DairyFormatters.roundTwoDecimals(ratePerLiter)
            val totalAmount = DairyFormatters.roundTwoDecimals(effectiveQty * effectiveRate)

            require(customerId.isNotBlank()) { "कृपया ग्राहक चुनें।" }
            require(cleanName.length in 1..80) { "ग्राहक का नाम मान्य नहीं है।" }
            require(isValidDateKey(dateKey)) { "तारीख का प्रारूप सही नहीं है।" }
            require(effectiveQty in 0.0..1000.0) { "दूध की मात्रा 0 से 1000 लीटर के बीच होनी चाहिए।" }
            require(effectiveRate in 0.0..1000.0) { "दूध का भाव ₹0 से ₹1000 के बीच होना चाहिए।" }
            require(totalAmount in 0.0..1000000.0) { "कुल राशि मान्य सीमा में होनी चाहिए।" }
            require(cleanNote.length <= 200) { "नोट अधिकतम 200 अक्षरों का हो सकता है।" }

            val docRef = db.collection("users").document(uid).collection("milk_entries").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "customerId" to customerId,
                "customerName" to cleanName,
                "dateKey" to dateKey,
                "shift" to shift.name,
                "milkType" to milkType.name,
                "quantityLiters" to effectiveQty,
                "ratePerLiter" to effectiveRate,
                "totalAmount" to totalAmount,
                "absent" to absent,
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMilkEntry(
        userId: String,
        entryId: String,
        customerName: String,
        dateKey: String,
        shift: DeliveryShift,
        milkType: MilkType,
        quantityLiters: Double,
        ratePerLiter: Double,
        absent: Boolean,
        note: String
    ): Result<Unit> {
        var docPath = "users/$userId/milk_entries/$entryId"
        return try {
            val uid = requireUserId(userId)
            require(entryId.isNotBlank()) { "एंट्री ID खाली नहीं हो सकती।" }
            val cleanName = customerName.trim()
            val cleanNote = note.trim()
            val effectiveQty = if (absent) 0.0 else DairyFormatters.roundTwoDecimals(quantityLiters)
            val effectiveRate = DairyFormatters.roundTwoDecimals(ratePerLiter)
            val totalAmount = DairyFormatters.roundTwoDecimals(effectiveQty * effectiveRate)

            require(cleanName.length in 1..80) { "ग्राहक का नाम मान्य नहीं है।" }
            require(isValidDateKey(dateKey)) { "तारीख का प्रारूप सही नहीं है।" }
            require(effectiveQty in 0.0..1000.0) { "दूध की मात्रा 0 से 1000 लीटर के बीच होनी चाहिए।" }
            require(effectiveRate in 0.0..1000.0) { "दूध का भाव ₹0 से ₹1000 के बीच होना चाहिए।" }
            require(totalAmount in 0.0..1000000.0) { "कुल राशि मान्य सीमा में होनी चाहिए।" }
            require(cleanNote.length <= 200) { "नोट अधिकतम 200 अक्षरों का हो सकता है।" }

            val docRef = db.collection("users").document(uid)
                .collection("milk_entries").document(entryId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "customerName" to cleanName,
                    "dateKey" to dateKey,
                    "shift" to shift.name,
                    "milkType" to milkType.name,
                    "quantityLiters" to effectiveQty,
                    "ratePerLiter" to effectiveRate,
                    "totalAmount" to totalAmount,
                    "absent" to absent,
                    "note" to cleanNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMilkEntry(userId: String, entryId: String): Result<Unit> {
        var docPath = "users/$userId/milk_entries/$entryId"
        return try {
            val uid = requireUserId(userId)
            require(entryId.isNotBlank()) { "एंट्री ID खाली नहीं हो सकती।" }
            val docRef = db.collection("users").document(uid)
                .collection("milk_entries").document(entryId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- PAYMENT MUTATIONS ---

    suspend fun addPayment(
        userId: String,
        customerId: String,
        customerName: String,
        dateKey: String,
        amount: Double,
        paymentMode: PaymentMode,
        note: String
    ): Result<Unit> {
        var docPath = "users/$userId/payments"
        return try {
            val uid = requireUserId(userId)
            val cleanName = customerName.trim()
            val cleanNote = note.trim()
            val cleanAmount = DairyFormatters.roundTwoDecimals(amount)

            require(customerId.isNotBlank()) { "कृपया ग्राहक चुनें।" }
            require(cleanName.length in 1..80) { "ग्राहक का नाम मान्य नहीं है।" }
            require(isValidDateKey(dateKey)) { "तारीख का प्रारूप सही नहीं है।" }
            require(cleanAmount > 0.0 && cleanAmount <= 1000000.0) {
                "पेमेंट राशि ₹0 से अधिक और ₹10,00,000 के भीतर होनी चाहिए।"
            }
            require(cleanNote.length <= 200) { "नोट अधिकतम 200 अक्षरों का हो सकता है।" }

            val docRef = db.collection("users").document(uid).collection("payments").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "customerId" to customerId,
                "customerName" to cleanName,
                "dateKey" to dateKey,
                "amount" to cleanAmount,
                "paymentMode" to paymentMode.name,
                "note" to cleanNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePayment(
        userId: String,
        paymentId: String,
        customerName: String,
        dateKey: String,
        amount: Double,
        paymentMode: PaymentMode,
        note: String
    ): Result<Unit> {
        var docPath = "users/$userId/payments/$paymentId"
        return try {
            val uid = requireUserId(userId)
            require(paymentId.isNotBlank()) { "पेमेंट ID खाली नहीं हो सकती।" }
            val cleanName = customerName.trim()
            val cleanNote = note.trim()
            val cleanAmount = DairyFormatters.roundTwoDecimals(amount)

            require(cleanName.length in 1..80) { "ग्राहक का नाम मान्य नहीं है।" }
            require(isValidDateKey(dateKey)) { "तारीख का प्रारूप सही नहीं है।" }
            require(cleanAmount > 0.0 && cleanAmount <= 1000000.0) {
                "पेमेंट राशि ₹0 से अधिक और ₹10,00,000 के भीतर होनी चाहिए।"
            }
            require(cleanNote.length <= 200) { "नोट अधिकतम 200 अक्षरों का हो सकता है।" }

            val docRef = db.collection("users").document(uid)
                .collection("payments").document(paymentId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "customerName" to cleanName,
                    "dateKey" to dateKey,
                    "amount" to cleanAmount,
                    "paymentMode" to paymentMode.name,
                    "note" to cleanNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePayment(userId: String, paymentId: String): Result<Unit> {
        var docPath = "users/$userId/payments/$paymentId"
        return try {
            val uid = requireUserId(userId)
            require(paymentId.isNotBlank()) { "पेमेंट ID खाली नहीं हो सकती।" }
            val docRef = db.collection("users").document(uid)
                .collection("payments").document(paymentId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- COLLECTIONS (supplier side, FAT/SNF) ---

    fun observeCollections(userId: String): Flow<List<MilkCollection>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/collections"
        emitAll(
            db.collection("users").document(uid).collection("collections")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MilkCollection::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<MilkCollection> { it.dateKey }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addCollection(
        userId: String,
        supplierName: String,
        sampleNo: String,
        dateKey: String,
        shift: DeliveryShift,
        quantityLiters: Double,
        fat: Double,
        snf: Double,
        ratePerLiter: Double,
        totalAmount: Double
    ): Result<Unit> {
        var docPath = "users/$userId/collections"
        return try {
            val uid = requireUserId(userId)
            val cleanName = supplierName.trim()
            val cleanSample = sampleNo.trim()
            require(cleanName.length in 1..80) { "सप्लायर का नाम मान्य नहीं है।" }
            require(cleanSample.length <= 20) { "सैंपल नंबर अधिकतम 20 अक्षरों का हो सकता है।" }
            require(isValidDateKey(dateKey)) { "तारीख का प्रारूप सही नहीं है।" }
            require(quantityLiters > 0.0 && quantityLiters <= 100000.0) { "दूध की मात्रा सही नहीं है।" }
            require(
                (fat == 0.0 && snf == 0.0) || (fat in 1.0..15.0 && snf in 1.0..15.0)
            ) { "FAT/SNF सही नहीं है।" }

            val docRef = db.collection("users").document(uid).collection("collections").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "supplierName" to cleanName,
                "sampleNo" to cleanSample,
                "dateKey" to dateKey,
                "shift" to shift.name,
                "quantityLiters" to DairyFormatters.roundTwoDecimals(quantityLiters),
                "fat" to fat,
                "snf" to snf,
                "ratePerLiter" to DairyFormatters.roundTwoDecimals(ratePerLiter),
                "totalAmount" to DairyFormatters.roundTwoDecimals(totalAmount),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCollection(userId: String, collectionId: String): Result<Unit> {
        var docPath = "users/$userId/collections/$collectionId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("collections").document(collectionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /** Dairy par machine se FAT/SNF nikalne ke baad pending entry ko pura karo. */
    suspend fun updateCollectionFatSnf(
        userId: String,
        collectionId: String,
        fat: Double,
        snf: Double,
        ratePerLiter: Double,
        totalAmount: Double
    ): Result<Unit> {
        var docPath = "users/$userId/collections/$collectionId"
        return try {
            val uid = requireUserId(userId)
            require(fat in 1.0..15.0 && snf in 1.0..15.0) { "FAT/SNF सही नहीं है।" }
            val docRef = db.collection("users").document(uid)
                .collection("collections").document(collectionId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "fat" to fat,
                    "snf" to snf,
                    "ratePerLiter" to DairyFormatters.roundTwoDecimals(ratePerLiter),
                    "totalAmount" to DairyFormatters.roundTwoDecimals(totalAmount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- SUPPLIERS (line-by-line collection ke liye gharon ki list) ---

    fun observeSuppliers(userId: String): Flow<List<MilkSupplier>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/suppliers"
        emitAll(
            db.collection("users").document(uid).collection("suppliers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MilkSupplier::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<MilkSupplier> { it.sortOrder }
                            .thenBy { it.name.lowercase() }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addSupplier(userId: String, name: String, phone: String, village: String = ""): Result<Unit> {
        var docPath = "users/$userId/suppliers"
        return try {
            val uid = requireUserId(userId)
            val cleanName = name.trim()
            require(cleanName.length in 1..80) { "सप्लायर का नाम मान्य नहीं है।" }
            val docRef = db.collection("users").document(uid).collection("suppliers").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "phone" to phone.trim(),
                "village" to village.trim(),
                "sortOrder" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSupplierDetails(userId: String, supplierId: String, phone: String, village: String): Result<Unit> {
        var docPath = "users/$userId/suppliers/$supplierId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("suppliers").document(supplierId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "phone" to phone.trim(),
                    "village" to village.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplier(userId: String, supplierId: String): Result<Unit> {
        var docPath = "users/$userId/suppliers/$supplierId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("suppliers").document(supplierId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- SUPPLIER TRANSACTIONS (hisaab: samaan / advance / payment) ---

    fun observeSupplierTransactions(userId: String): Flow<List<SupplierTransaction>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/supplier_transactions"
        emitAll(
            db.collection("users").document(uid).collection("supplier_transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SupplierTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<SupplierTransaction> { it.dateKey }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addSupplierTransaction(
        userId: String,
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
    ): Result<Unit> {
        var docPath = "users/$userId/supplier_transactions"
        return try {
            val uid = requireUserId(userId)
            val cleanName = supplierName.trim()
            require(cleanName.length in 1..80) { "Supplier का नाम मान्य नहीं है।" }
            require(type in listOf("GOODS", "ADVANCE", "PAYMENT")) { "Entry की किस्म सही नहीं है।" }
            require(amount > 0.0 && amount <= 10000000.0) { "रकम सही नहीं है।" }
            require(isValidDateKey(dateKey)) { "तारीख का प्रारूप सही नहीं है।" }
            val docRef = db.collection("users").document(uid)
                .collection("supplier_transactions").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "supplierName" to cleanName,
                "type" to type,
                "itemName" to itemName.trim(),
                "quantity" to quantity,
                "unit" to unit,
                "ratePerUnit" to DairyFormatters.roundTwoDecimals(ratePerUnit),
                "amount" to DairyFormatters.roundTwoDecimals(amount),
                "paymentMode" to paymentMode,
                "note" to note.trim(),
                "dateKey" to dateKey,
                "isHome" to isHome,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplierTransaction(userId: String, transactionId: String): Result<Unit> {
        var docPath = "users/$userId/supplier_transactions/$transactionId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("supplier_transactions").document(transactionId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- RATE CHART OVERRIDES (user-edited rates) ---

    fun observeRateOverrides(userId: String): Flow<Map<String, Double>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/rate_overrides"
        emitAll(
            db.collection("users").document(uid).collection("rate_overrides")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val rate = doc.getDouble("rate") ?: return@mapNotNull null
                        doc.id to rate
                    }.toMap()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveRateOverride(
        userId: String,
        fat: Double,
        snf: Double,
        rate: Double
    ): Result<Unit> {
        val key = RateChart.key(fat, snf)
        var docPath = "users/$userId/rate_overrides/$key"
        return try {
            val uid = requireUserId(userId)
            require(rate > 0.0 && rate <= 1000.0) { "रेट सही नहीं है।" }
            val docRef = db.collection("users").document(uid)
                .collection("rate_overrides").document(key)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "fat" to fat,
                    "snf" to snf,
                    "rate" to DairyFormatters.roundTwoDecimals(rate),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRateOverride(userId: String, fat: Double, snf: Double): Result<Unit> {
        val key = RateChart.key(fat, snf)
        var docPath = "users/$userId/rate_overrides/$key"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("rate_overrides").document(key)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- IMPORTED RATE CHART (user ki daali hui poori list) ---

    fun observeImportedChart(userId: String): Flow<Map<String, Double>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/rate_chart/current"
        emitAll(
            db.collection("users").document(uid).collection("rate_chart").document("current")
                .snapshots()
                .map { doc ->
                    val raw = doc.get("rates") as? Map<*, *> ?: emptyMap<Any, Any>()
                    raw.mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val value = (v as? Number)?.toDouble() ?: return@mapNotNull null
                        key to value
                    }.toMap()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveImportedChart(
        userId: String,
        rates: Map<String, Double>,
        sourceName: String
    ): Result<Unit> {
        val docPath = "users/$userId/rate_chart/current"
        return try {
            val uid = requireUserId(userId)
            require(rates.size >= 20) { "लिस्ट में बहुत कम रेट मिले।" }
            val docRef = db.collection("users").document(uid)
                .collection("rate_chart").document("current")
            docRef.set(
                mapOf(
                    "rates" to rates,
                    "source" to sourceName.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteImportedChart(userId: String): Result<Unit> {
        val docPath = "users/$userId/rate_chart/current"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("rate_chart").document("current")
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
