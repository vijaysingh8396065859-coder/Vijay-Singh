package com.example.ui.screens

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val canSave = supplierName.isNotBlank() && qty > 0.0 && fat > 0.0 && snf > 0.0 && rate > 0.0

    val todayCollections = remember(snapshot.collections, selectedDateKey) {
        snapshot.collections.filter { it.dateKey == selectedDateKey }
    }
    val todayLiters = DairyFormatters.roundTwoDecimals(todayCollections.sumOf { it.quantityLiters })
    val todayAmount = DairyFormatters.roundTwoDecimals(todayCollections.sumOf { it.totalAmount })

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
                                    text = "${col.parsedShift.labelHi} • " +
                                        "${DairyFormatters.formatLiters(col.quantityLiters)} • " +
                                        "FAT ${col.fat} / SNF ${col.snf} • " +
                                        "${DairyFormatters.formatRupees(col.ratePerLiter)}/लीटर",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
