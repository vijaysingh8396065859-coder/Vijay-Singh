package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DairyFormatters
import com.example.data.RateChart
import com.example.viewmodel.DairyDataSnapshot
import kotlin.math.abs

/**
 * Galti Check: hisaab ki aam galtiyaan khud pakdo —
 * pending FAT/SNF, duplicate sample number, chart se alag rate,
 * aur ek jaisi samaan/advance/payment ki dohri entry.
 */
@Composable
fun GaltiCheckScreen(
    snapshot: DairyDataSnapshot,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pending = remember(snapshot.collections) {
        snapshot.collections.filter { it.fat == 0.0 }
            .sortedByDescending { it.dateKey }
    }
    val dupSamples = remember(snapshot.collections) {
        snapshot.collections.filter { it.sampleNo.isNotBlank() }
            .groupBy {
                Triple(it.dateKey, it.shift, it.sampleNo.trim().lowercase())
            }
            .filter { it.value.size > 1 }
            .values.toList()
    }
    val rateMismatch = remember(snapshot.collections, snapshot.importedRates, snapshot.rateOverrides) {
        snapshot.collections.filter { it.fat > 0.0 && it.snf > 0.0 }
            .map { col ->
                val chart = RateChart.rateFor(
                    col.fat, col.snf, snapshot.importedRates, snapshot.rateOverrides
                )
                col to chart
            }
            .filter { (col, chart) -> abs(col.ratePerLiter - chart) > 0.011 }
    }
    val dupTxns = remember(snapshot.supplierTransactions) {
        snapshot.supplierTransactions.filter { !it.isHome }
            .groupBy {
                listOf(
                    it.supplierName, it.type, it.dateKey,
                    it.amount.toString(), it.itemName.trim().lowercase()
                )
            }
            .filter { it.value.size > 1 }
            .values.toList()
    }
    val totalIssues = pending.size + dupSamples.size + rateMismatch.size + dupTxns.size

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "gc_header") {
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
                Column {
                    Text(
                        text = "⚠️ Galti Check",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hisaab ki galtiyaan — pehle pakdo, phir theek karo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "gc_summary") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalIssues == 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = if (totalIssues == 0) {
                        "✅ Koi galti nahi mili — sab saaf hai!"
                    } else {
                        "⚠️ Kul $totalIssues jagah dhyaan dena hai — neeche dekho"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (pending.isNotEmpty()) {
            item(key = "gc_pending_title") {
                Text(
                    text = "⏳ FAT/SNF bharna bhool gaye (${pending.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            pending.take(15).forEachIndexed { idx, col ->
                item(key = "gc_pend_$idx") {
                    IssueRow(
                        title = "${col.supplierName} • ${DairyFormatters.formatShortDateHi(col.dateKey)}",
                        detail = "सैंपल ${col.sampleNo} • ${DairyFormatters.formatLiters(col.quantityLiters)} — " +
                            "Collection me jaakar FAT/SNF bharo"
                    )
                }
            }
        }

        if (dupSamples.isNotEmpty()) {
            item(key = "gc_dup_title") {
                Text(
                    text = "🔁 Ek hi Sample Number do baar (${dupSamples.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            dupSamples.forEachIndexed { idx, group ->
                val first = group.first()
                item(key = "gc_dup_$idx") {
                    IssueRow(
                        title = "सैंपल ${first.sampleNo} • ${DairyFormatters.formatShortDateHi(first.dateKey)}",
                        detail = group.joinToString(" • ") {
                            "${it.supplierName} ${DairyFormatters.formatLiters(it.quantityLiters)}"
                        } + " — ek entry galat ho sakti hai, check karo"
                    )
                }
            }
        }

        if (rateMismatch.isNotEmpty()) {
            item(key = "gc_rate_title") {
                Text(
                    text = "💲 Rate chart se alag laga (${rateMismatch.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            rateMismatch.take(15).forEachIndexed { idx, pair ->
                val (col, chart) = pair
                item(key = "gc_rate_$idx") {
                    IssueRow(
                        title = "${col.supplierName} • ${DairyFormatters.formatShortDateHi(col.dateKey)}",
                        detail = "Laga rate ${DairyFormatters.formatRupees(col.ratePerLiter)}/लीटर, " +
                            "chart rate ${DairyFormatters.formatRupees(chart)}/लीटर " +
                            "(FAT ${col.fat} / SNF ${col.snf}) — khaas rate tha to theek, warna sudharo"
                    )
                }
            }
        }

        if (dupTxns.isNotEmpty()) {
            item(key = "gc_duptxn_title") {
                Text(
                    text = "🔁 Samaan/Advance/Payment dohri entry jaise (${dupTxns.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            dupTxns.forEachIndexed { idx, group ->
                val first = group.first()
                item(key = "gc_dt_$idx") {
                    IssueRow(
                        title = "${first.supplierName} • ${DairyFormatters.formatShortDateHi(first.dateKey)}",
                        detail = when (first.type) {
                            "GOODS" -> "समान — ${first.itemName}"
                            "ADVANCE" -> "Advance Jama"
                            else -> "Payment"
                        } + " • ${DairyFormatters.formatRupees(first.amount)} • " +
                            "${group.size} baar ek jaisi entry — ek galat ho sakti hai"
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueRow(title: String, detail: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
