package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.viewmodel.DairyDataSnapshot

/**
 * Supplier Profile: ek supplier ki poori file — naam, mobile, ID, line number,
 * gaon, rozana doodh entries, kul doodh/rakam, samaan-advance record,
 * purane 10-day settlements aur pichla + vartamaan bakaya.
 */
@Composable
fun SupplierProfileScreen(
    snapshot: DairyDataSnapshot,
    supplierName: String,
    onBack: () -> Unit,
    onUpdateDetails: (supplierId: String, phone: String, village: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayKey = remember { DairyFormatters.todayDateKey() }
    val supplier = snapshot.suppliers.firstOrNull { it.name == supplierName }
    val lineNo = snapshot.suppliers.indexOfFirst { it.name == supplierName } + 1

    val myCols = remember(snapshot.collections, supplierName) {
        snapshot.collections.filter { it.supplierName == supplierName }
            .sortedWith(compareByDescending<com.example.data.MilkCollection> { it.dateKey }
                .thenByDescending { it.shift })
    }
    val myTxns = remember(snapshot.supplierTransactions, supplierName) {
        snapshot.supplierTransactions.filter { it.supplierName == supplierName && !it.isHome }
            .sortedByDescending { it.dateKey }
    }

    fun balanceBefore(startKey: String): Double {
        val milkB = myCols.filter { it.dateKey < startKey }.sumOf { it.totalAmount }
        val txnB = myTxns.filter { it.dateKey < startKey }.sumOf { it.amount }
        return DairyFormatters.roundToRupee(milkB - txnB)
    }

    val curStart = DairyFormatters.cycleStartKey(todayKey)
    val curEnd = DairyFormatters.cycleEndKey(todayKey)
    val overall = balanceBefore(DairyFormatters.shiftCycleKey(todayKey, 1))
    val prevClosing = balanceBefore(curStart)
    val cycleMilkList = myCols.filter { it.dateKey in curStart..curEnd }
    val cycleMilk = DairyFormatters.roundToRupee(cycleMilkList.sumOf { it.totalAmount })
    val cycleTxns = DairyFormatters.roundToRupee(
        myTxns.filter { it.dateKey in curStart..curEnd }.sumOf { it.amount }
    )
    val totalLitres = DairyFormatters.roundTwoDecimals(myCols.sumOf { it.quantityLiters })
    val totalAmount = DairyFormatters.roundToRupee(myCols.sumOf { it.totalAmount })

    val pastCycles = (1..3).map { back ->
        val start = DairyFormatters.shiftCycleKey(todayKey, -back)
        val end = DairyFormatters.cycleEndKey(start)
        val milk = DairyFormatters.roundToRupee(
            myCols.filter { it.dateKey in start..end }.sumOf { it.totalAmount }
        )
        val txn = DairyFormatters.roundToRupee(
            myTxns.filter { it.dateKey in start..end }.sumOf { it.amount }
        )
        val closing = balanceBefore(DairyFormatters.shiftCycleKey(start, 1))
        PastCycle(start, end, milk, txn, closing)
    }.filter { it.milk > 0.0 || it.txns > 0.0 || it.closing != 0.0 }

    var phoneText by remember(supplier?.id) { mutableStateOf(supplier?.phone ?: "") }
    var villageText by remember(supplier?.id) { mutableStateOf(supplier?.village ?: "") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "profile_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "वापस"
                    )
                }
                Text(
                    text = "👤 Supplier Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item(key = "profile_info") {
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
                        text = supplierName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📱 Mobile: ${supplier?.phone?.takeIf { it.isNotBlank() } ?: "—"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "🏠 गाँव/घर: ${supplier?.village?.takeIf { it.isNotBlank() } ?: "—"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (lineNo > 0) {
                        Text(
                            text = "🚶 Line / Route नंबर: $lineNo",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        text = "🆔 Supplier ID: ${supplier?.id?.takeIf { it.isNotBlank() } ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Mobile / WhatsApp नंबर") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = villageText,
                        onValueChange = { villageText = it },
                        label = { Text("गाँव / घर की जानकारी") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            supplier?.let {
                                onUpdateDetails(it.id, phoneText.trim(), villageText.trim())
                            }
                        },
                        enabled = supplier != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💾 जानकारी Save करो")
                    }
                }
            }
        }

        item(key = "profile_balance") {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💰 बकाया हिसाब",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (overall >= 0.0) {
                            "वर्तमान बकाया (देना है): ${DairyFormatters.formatRupees(overall)}"
                        } else {
                            "डेरी बाकी (supplier से लेना है): ${DairyFormatters.formatRupees(-overall)}"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (overall >= 0.0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "पिछला बकाया (इस cycle से पहले): ${DairyFormatters.formatRupees(prevClosing)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "इस cycle (${DairyFormatters.formatShortDateHi(curStart)} — " +
                            "${DairyFormatters.formatShortDateHi(curEnd)}): दूध " +
                            "${DairyFormatters.formatRupees(cycleMilk)} • कटौती " +
                            DairyFormatters.formatRupees(cycleTxns),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()
                    Text(
                        text = "🥛 कुल दूध (आज तक): ${DairyFormatters.formatLiters(totalLitres)} • " +
                            DairyFormatters.formatRupees(totalAmount) +
                            " • ${myCols.size} entries",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (pastCycles.isNotEmpty()) {
            item(key = "profile_past_title") {
                Text(
                    text = "📜 पुराने Settlements (10-Day)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            pastCycles.forEachIndexed { idx, pc ->
                item(key = "past_$idx") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${DairyFormatters.formatShortDateHi(pc.start)} — " +
                                    DairyFormatters.formatShortDateHi(pc.end),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "दूध: ${DairyFormatters.formatRupees(pc.milk)} • " +
                                    "कटौती (समान/advance/payment): ${DairyFormatters.formatRupees(pc.txns)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (pc.closing >= 0.0) {
                                    "Cycle के अंत में बकाया: ${DairyFormatters.formatRupees(pc.closing)}"
                                } else {
                                    "Cycle के अंत में डेरी बाकी: ${DairyFormatters.formatRupees(-pc.closing)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item(key = "profile_entries_title") {
            Text(
                text = "🥛 रोजाना दूध Entries (आखिरी ${minOf(myCols.size, 20)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (myCols.isEmpty()) {
            item(key = "profile_entries_empty") {
                Text(
                    text = "अभी तक कोई दूध entry नहीं है।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        myCols.take(20).forEachIndexed { idx, col ->
            item(key = "entry_$idx") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${DairyFormatters.formatShortDateHi(col.dateKey)} • " +
                                if (col.shift == "MORNING") "सुबह" else "शाम",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Sample: ${col.sampleNo} • ${DairyFormatters.formatLiters(col.quantityLiters)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (col.fat == 0.0) {
                            "FAT/SNF बाकी है"
                        } else {
                            "FAT ${col.fat} / SNF ${col.snf} • ${DairyFormatters.formatRupees(col.totalAmount)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (col.fat == 0.0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }

        item(key = "profile_txns_title") {
            Text(
                text = "📦 सामान / Advance / Payment Record (आखिरी ${minOf(myTxns.size, 20)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (myTxns.isEmpty()) {
            item(key = "profile_txns_empty") {
                Text(
                    text = "अभी तक कोई सामान/advance record नहीं है।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        myTxns.take(20).forEachIndexed { idx, txn ->
            item(key = "txn_$idx") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (txn.type) {
                                "GOODS" -> "📦 सामान — ${txn.itemName}"
                                "ADVANCE" -> "💵 Advance Jama"
                                else -> "💰 Payment"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${DairyFormatters.formatShortDateHi(txn.dateKey)}" +
                                if (txn.type == "GOODS") {
                                    " • ${txn.quantity} ${if (txn.unit == "KG") "किलो" else "बोरी"}"
                                } else {
                                    ""
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "− ${DairyFormatters.formatRupees(txn.amount)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private data class PastCycle(
    val start: String,
    val end: String,
    val milk: Double,
    val txns: Double,
    val closing: Double
)
