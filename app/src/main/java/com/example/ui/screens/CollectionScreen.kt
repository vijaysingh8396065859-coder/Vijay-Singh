package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DairyFormatters
import com.example.data.MilkCollection
import com.example.data.DeliveryShift
import com.example.data.RateChart
import com.example.viewmodel.DairyDataSnapshot

/**
 * Doodh collection (supplier side): sample ka FAT/SNF bharte hi rate list se rate
 * aur kul raqam apne aap nikal aati hai. Record supplier ke naam se save hota hai.
 */
@Composable
fun CollectionScreen(
    snapshot: DairyDataSnapshot,
    selectedDateKey: String,
    onBack: () -> Unit,
    onSaveCollection: (
        supplierName: String,
        sampleNo: String,
        shift: DeliveryShift,
        quantityLiters: Double,
        fat: Double,
        snf: Double,
        ratePerLiter: Double,
        totalAmount: Double
    ) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onUpdateFatSnf: (MilkCollection, Double, Double) -> Unit,
    onAddGoods: (supplierName: String, itemName: String, quantity: Double, unit: String, ratePerUnit: Double, amount: Double) -> Unit,
    onAddSupplier: (String, String, String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onDeleteSupplier: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var supplierName by remember { mutableStateOf("") }
    var sampleNo by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf(DeliveryShift.MORNING) }
    var qtyText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var snfText by remember { mutableStateOf("") }

    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val fat = fatText.toDoubleOrNull() ?: 0.0
    val snf = snfText.toDoubleOrNull() ?: 0.0
    val rate = if (fat > 0.0 && snf > 0.0) {
        RateChart.rateFor(fat, snf, snapshot.importedRates, snapshot.rateOverrides)
    } else {
        0.0
    }
    val total = DairyFormatters.roundTwoDecimals(qty * rate)
    val fatSnfOk = (fatText.isBlank() && snfText.isBlank()) || (fat > 0.0 && snf > 0.0)
    val canSave = supplierName.isNotBlank() && qty > 0.0 && fatSnfOk

    val todayCollections = remember(snapshot.collections, selectedDateKey) {
        snapshot.collections.filter { it.dateKey == selectedDateKey }
    }
    val todayLiters = DairyFormatters.roundTwoDecimals(todayCollections.sumOf { it.quantityLiters })
    val todayAmount = DairyFormatters.roundTwoDecimals(todayCollections.sumOf { it.totalAmount })

    // --- Supplier search (naam likhte hi detail) ---
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(searchQuery, snapshot.suppliers, snapshot.supplierSummaries, snapshot.collections) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            emptyList()
        } else {
            val names = LinkedHashSet<String>()
            snapshot.suppliers.forEach { names.add(it.name) }
            snapshot.supplierSummaries.forEach { names.add(it.supplierName) }
            names.filter { it.lowercase().contains(q) }.map { name ->
                val summary = snapshot.supplierSummaries.firstOrNull { it.supplierName == name }
                val last = snapshot.collections
                    .filter { it.supplierName == name }
                    .maxByOrNull { it.createdAt?.seconds ?: 0L }
                Triple(name, summary, last)
            }
        }
    }

    // --- Line mode (ghar-ghar collection, automatic next) ---
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierPhone by remember { mutableStateOf("") }
    var newSupplierVillage by remember { mutableStateOf("") }
    var lineActive by remember { mutableStateOf(false) }
    var lineIndex by remember { mutableStateOf(0) }
    var lineFinished by remember { mutableStateOf(false) }
    var lineSavedCount by remember { mutableStateOf(0) }
    var lineGoodsAmount by remember { mutableStateOf(0.0) }
    var lineGoodsCount by remember { mutableStateOf(0) }
    var gItem by remember { mutableStateOf("") }
    var gQtyText by remember { mutableStateOf("") }
    var gUnit by remember { mutableStateOf("BORI") }
    var gRateText by remember { mutableStateOf("") }
    val gQty = gQtyText.toDoubleOrNull() ?: 0.0
    val gRate = gRateText.toDoubleOrNull() ?: 0.0
    val gTotal = DairyFormatters.roundTwoDecimals(gQty * gRate)
    var lineLiters by remember { mutableStateOf(0.0) }
    var lineAmount by remember { mutableStateOf(0.0) }
    val roster = snapshot.suppliers

    fun advanceLine() {
        if (lineIndex < roster.size - 1) {
            lineIndex += 1
        } else {
            lineActive = false
            lineFinished = true
        }
    }

    LaunchedEffect(lineActive, lineIndex) {
        val sup = if (lineActive) roster.getOrNull(lineIndex) else null
        if (sup != null) {
            supplierName = sup.name
            sampleNo = ""
            qtyText = ""
            fatText = ""
            snfText = ""
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "वापस"
                )
            }
            Column {
                Text(
                    text = "दूध Collection (FAT/SNF)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = DairyFormatters.formatShortDateHi(selectedDateKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "supplier_search") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Supplier खोजो",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("नाम लिखो — detail तुरंत आ जाएगी") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (searchQuery.isNotBlank()) {
                            if (searchResults.isEmpty()) {
                                Text(
                                    text = "इस नाम का कोई supplier नहीं मिला।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                searchResults.forEach { (name, summary, last) ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedButton(onClick = {
                                                supplierName = name
                                                searchQuery = ""
                                            }) {
                                                Text("Entry भरो")
                                            }
                                        }
                                        if (summary != null) {
                                            Text(
                                                text = "कुल: ${summary.entriesCount} बार • " +
                                                    "${DairyFormatters.formatLiters(summary.totalLiters)} • " +
                                                    DairyFormatters.formatRupees(summary.totalAmount),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (last != null) {
                                            Text(
                                                text = "आखिरी: ${DairyFormatters.formatShortDateHi(last.dateKey)} • " +
                                                    "FAT ${last.fat} / SNF ${last.snf} • " +
                                                    "${DairyFormatters.formatRupees(last.ratePerLiter)}/लीटर",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "supplier_line") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🚶 Line — घर-घर Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        when {
                            lineFinished -> {
                                Text(
                                    text = "✅ Line पूरी हो गई! $lineSavedCount घरों से दूध लिया • " +
                                        "कुल ${DairyFormatters.formatLiters(lineLiters)} • " +
                                        DairyFormatters.formatRupees(lineAmount) +
                                        if (lineGoodsCount > 0) {
                                            " • समान दिया: ${DairyFormatters.formatRupees(lineGoodsAmount)} ($lineGoodsCount बार)"
                                        } else {
                                            ""
                                        },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedButton(onClick = { lineFinished = false }) {
                                    Text("ठीक है")
                                }
                            }
                            lineActive -> {
                                Text(
                                    text = "घर ${lineIndex + 1} / ${roster.size}: " +
                                        (roster.getOrNull(lineIndex)?.name ?: ""),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "नीचे form में दूध भरो — 'दूध दर्ज करो' दबाते ही अगला घर अपने आप आ जाएगा। " +
                                        "सैंपल नंबर बैग पर लिखा होता है, वही भरना।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { advanceLine() }) {
                                        Text("⏭ Skip — अगला घर")
                                    }
                                    OutlinedButton(onClick = { lineActive = false }) {
                                        Text("✖ Line बंद करो")
                                    }
                                }
                                HorizontalDivider()
                                val currentName = roster.getOrNull(lineIndex)?.name ?: ""
                                Text(
                                    text = "📦 समान दिया — $currentName को (खल/चारा)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("सरसों खल", "खल", "चारा").forEach { preset ->
                                        FilterChip(
                                            selected = gItem == preset,
                                            onClick = { gItem = preset },
                                            label = { Text(preset) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = gItem,
                                    onValueChange = { gItem = it },
                                    label = { Text("समान का नाम") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = gQtyText,
                                        onValueChange = { gQtyText = it },
                                        label = { Text("मात्रा") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gRateText,
                                        onValueChange = { gRateText = it },
                                        label = { Text("Rate (₹)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = gUnit == "BORI",
                                        onClick = { gUnit = "BORI" },
                                        label = { Text("बोरी") }
                                    )
                                    FilterChip(
                                        selected = gUnit == "KG",
                                        onClick = { gUnit = "KG" },
                                        label = { Text("किलो") }
                                    )
                                    if (gTotal > 0.0) {
                                        Text(
                                            text = "कुल: ${DairyFormatters.formatRupees(gTotal)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        onAddGoods(
                                            currentName,
                                            gItem.trim(),
                                            gQty,
                                            gUnit,
                                            gRate,
                                            gTotal
                                        )
                                        lineGoodsAmount = DairyFormatters.roundTwoDecimals(lineGoodsAmount + gTotal)
                                        lineGoodsCount += 1
                                        gQtyText = ""
                                        gRateText = ""
                                    },
                                    enabled = currentName.isNotBlank() && gItem.isNotBlank() && gQty > 0.0 && gRate > 0.0,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📦 समान जोड़ो")
                                }
                                val todaysGoodsForCurrent = snapshot.supplierTransactions.filter {
                                    it.supplierName == currentName && it.dateKey == selectedDateKey &&
                                        it.type == "GOODS" && !it.isHome
                                }
                                if (todaysGoodsForCurrent.isNotEmpty()) {
                                    Text(
                                        text = "आज इस घर को दिया: " + todaysGoodsForCurrent.joinToString(" • ") {
                                            "${it.itemName} ${DairyFormatters.formatRupees(it.amount)}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                if (roster.isEmpty()) {
                                    Text(
                                        text = "पहले अपने supplier (घर) एक-एक करके जोड़ो — फिर line शुरू करके " +
                                            "एक के बाद एक घर का दूध भरते जाओ, अगला घर अपने आप आएगा।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    roster.forEachIndexed { index, sup ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${index + 1}. ${sup.name}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onOpenProfile(sup.name) }
                                            )
                                            IconButton(onClick = { onDeleteSupplier(sup.id) }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "हटाओ",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = newSupplierName,
                                            onValueChange = { newSupplierName = it },
                                            label = { Text("नया supplier / घर") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                if (newSupplierName.isNotBlank()) {
                                                    onAddSupplier(
                                                        newSupplierName.trim(),
                                                        newSupplierPhone.trim(),
                                                        newSupplierVillage.trim()
                                                    )
                                                    newSupplierName = ""
                                                    newSupplierPhone = ""
                                                    newSupplierVillage = ""
                                                }
                                            }
                                        ) {
                                            Text("जोड़ो")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = newSupplierPhone,
                                        onValueChange = { newSupplierPhone = it },
                                        label = { Text("Phone / WhatsApp नंबर (sheet भेजने के लिए)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = newSupplierVillage,
                                        onValueChange = { newSupplierVillage = it },
                                        label = { Text("गाँव / घर की जानकारी (route)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Button(
                                    onClick = {
                                        lineSavedCount = 0
                                        lineGoodsAmount = 0.0
                                        lineGoodsCount = 0
                                        lineLiters = 0.0
                                        lineAmount = 0.0
                                        lineIndex = 0
                                        lineFinished = false
                                        lineActive = true
                                    },
                                    enabled = roster.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🚶 Line शुरू करो")
                                }
                            }
                        }
                    }
                }
            }

            item(key = "collection_form") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = { supplierName = it },
                            label = { Text("सप्लायर का नाम") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = sampleNo,
                                onValueChange = { sampleNo = it },
                                label = { Text("सैंपल नंबर") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = qtyText,
                                onValueChange = { qtyText = it },
                                label = { Text("दूध (लीटर)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DeliveryShift.entries.forEach { option ->
                                FilterChip(
                                    selected = shift == option,
                                    onClick = { shift = option },
                                    label = { Text(option.labelHi) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = fatText,
                                onValueChange = { fatText = it },
                                label = { Text("FAT (जैसे 5.0)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = snfText,
                                onValueChange = { snfText = it },
                                label = { Text("SNF (जैसे 7.9)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (fat > 0.0 && snf > 0.0) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "रेट: ${DairyFormatters.formatRupees(rate)} / लीटर",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (qty > 0.0) {
                                    Text(
                                        text = "कुल रकम: ${DairyFormatters.formatRupees(total)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (snf < 8.0) {
                                    Text(
                                        text = "नोट: SNF 8.0 से कम है, इसलिए ₹0.75 प्रति पॉइंट कटौती लगी है।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        if (fatText.isBlank() && snfText.isBlank()) {
                            Text(
                                text = "FAT/SNF खाली छोड़ोगे तो entry pending दर्ज होगी — dairy पर machine से निकालकर नीचे वाली list से बाद में भर सकेंगे।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = {
                                onSaveCollection(
                                    supplierName.trim(),
                                    sampleNo.trim(),
                                    shift,
                                    qty,
                                    fat,
                                    snf,
                                    rate,
                                    total
                                )
                                if (lineActive) {
                                    lineSavedCount += 1
                                    lineLiters = DairyFormatters.roundTwoDecimals(lineLiters + qty)
                                    lineAmount = DairyFormatters.roundTwoDecimals(lineAmount + total)
                                    advanceLine()
                                }
                                sampleNo = ""
                                qtyText = ""
                                fatText = ""
                                snfText = ""
                            },
                            enabled = canSave,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("दूध दर्ज करो")
                        }
                    }
                }
            }

            item(key = "day_totals") {
                val dayGoods = snapshot.supplierTransactions.filter {
                    it.dateKey == selectedDateKey && it.type == "GOODS" && !it.isHome
                }
                val dayGoodsTotal = DairyFormatters.roundTwoDecimals(dayGoods.sumOf { it.amount })
                val goodsByItem = dayGoods
                    .groupBy { it.itemName to it.unit }
                    .map { (key, list) ->
                        Triple(
                            key.first,
                            key.second,
                            DairyFormatters.roundTwoDecimals(list.sumOf { it.amount }) to
                                list.sumOf { it.quantity }
                        )
                    }
                val pendingCount = todayCollections.count { it.fat == 0.0 }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🧮 दिन का कुल जोड़ — ${DairyFormatters.formatShortDateHi(selectedDateKey)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🥛 कुल दूध लिया: ${DairyFormatters.formatLiters(todayLiters)} • ${DairyFormatters.formatRupees(todayAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (pendingCount > 0) {
                            Text(
                                text = "⏳ $pendingCount sample का FAT/SNF बाकी है — उनकी रकम भरते ही इसमें जुड़ जाएगी।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "📦 कुल समान दिया: ${DairyFormatters.formatRupees(dayGoodsTotal)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        goodsByItem.forEach { (itemName, unit, amtQty) ->
                            Text(
                                text = "• $itemName: ${amtQty.second} ${if (unit == "KG") "किलो" else "बोरी"} • ${DairyFormatters.formatRupees(amtQty.first)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item(key = "collection_today_title") {
                Text(
                    text = "आज का Collection — ${DairyFormatters.formatLiters(todayLiters)} • " +
                        DairyFormatters.formatRupees(todayAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (todayCollections.isEmpty()) {
                item(key = "collection_empty") {
                    Text(
                        text = "आज अभी कोई दूध दर्ज नहीं हुआ। ऊपर पहला sample भरो।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = todayCollections, key = { "col_${it.id}" }) { col ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = col.supplierName +
                                        if (col.sampleNo.isNotBlank()) " • सैंपल ${col.sampleNo}" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (col.fat == 0.0) {
                                        "${col.parsedShift.labelHi} • " +
                                            "${DairyFormatters.formatLiters(col.quantityLiters)} • " +
                                            "FAT/SNF बाकी है"
                                    } else {
                                        "${col.parsedShift.labelHi} • " +
                                            "${DairyFormatters.formatLiters(col.quantityLiters)} • " +
                                            "FAT ${col.fat} / SNF ${col.snf} • " +
                                            "${DairyFormatters.formatRupees(col.ratePerLiter)}/लीटर"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (col.fat == 0.0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                text = DairyFormatters.formatRupees(col.totalAmount),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onDeleteCollection(col.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "हटाओ",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            val pendingToday = todayCollections.filter { it.fat == 0.0 }
            if (pendingToday.isNotEmpty()) {
                item(key = "pending_title") {
                    Text(
                        text = "⏳ FAT/SNF बाकी है (${pendingToday.size}) — sample number देखकर भरो",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(items = pendingToday, key = { "pend_${it.id}" }) { col ->
                    PendingFatSnfRow(col = col, onUpdateFatSnf = onUpdateFatSnf)
                }
            }

            if (snapshot.supplierSummaries.isNotEmpty()) {
                item(key = "supplier_totals_title") {
                    Text(
                        text = "सप्लायरों का कुल हिसाब",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(items = snapshot.supplierSummaries, key = { "sup_${it.supplierName}" }) { sup ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sup.supplierName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${sup.entriesCount} बार • ${DairyFormatters.formatLiters(sup.totalLiters)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = DairyFormatters.formatRupees(sup.totalAmount),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingFatSnfRow(
    col: MilkCollection,
    onUpdateFatSnf: (MilkCollection, Double, Double) -> Unit
) {
    var fatText by remember(col.id) { mutableStateOf("") }
    var snfText by remember(col.id) { mutableStateOf("") }
    val fat = fatText.toDoubleOrNull() ?: 0.0
    val snf = snfText.toDoubleOrNull() ?: 0.0
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = col.supplierName +
                    if (col.sampleNo.isNotBlank()) " • सैंपल ${col.sampleNo}" else "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${col.parsedShift.labelHi} • ${DairyFormatters.formatLiters(col.quantityLiters)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fatText,
                    onValueChange = { fatText = it },
                    label = { Text("FAT") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = snfText,
                    onValueChange = { snfText = it },
                    label = { Text("SNF") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onUpdateFatSnf(col, fat, snf) },
                    enabled = fat > 0.0 && snf > 0.0,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text("Rate लगाओ")
                }
            }
        }
    }
}
