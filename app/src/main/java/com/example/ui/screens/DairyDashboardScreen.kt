package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CustomerAccountSummary
import com.example.data.DairyFormatters
import com.example.data.DeliveryShift
import com.example.data.MilkEntry
import com.example.data.MilkType
import com.example.data.PaymentRecord
import com.example.ui.theme.MeadowEmerald
import com.example.ui.theme.MeadowEmeraldLight
import com.example.viewmodel.DairyDataSnapshot

private data class CustomerHisaabRow(
    val summary: CustomerAccountSummary,
    val lastEntry: MilkEntry?,
    val lastPayment: PaymentRecord?
)

/**
 * "Meri Dairy" dashboard: today's total sale, buffalo/cow/mixed breakdown,
 * today's collection, and every customer's latest hisaab at a glance.
 */
@Composable
fun DairyDashboardScreen(
    snapshot: DairyDataSnapshot,
    modifier: Modifier = Modifier,
    onOpenCollection: () -> Unit = {},
    onOpenRateChart: () -> Unit = {},
    onOpenSupplierHisaab: () -> Unit = {},
    onCheckUpdate: () -> Unit = {},
    onOpenGaltiCheck: () -> Unit = {},
    onOpenGuide: () -> Unit = {}
) {
    val todayKey = remember { DairyFormatters.todayDateKey() }

    val todayEntries = remember(snapshot.milkEntries, todayKey) {
        snapshot.milkEntries.filter { it.dateKey == todayKey && !it.absent }
    }
    val todayPayments = remember(snapshot.payments, todayKey) {
        snapshot.payments.filter { it.dateKey == todayKey }
    }
    val totalSale = todayEntries.sumOf { it.totalAmount }
    val totalLiters = todayEntries.sumOf { it.quantityLiters }
    val totalCollected = todayPayments.sumOf { it.amount }

    val byType = remember(todayEntries) {
        MilkType.entries.map { type ->
            val typeEntries = todayEntries.filter { it.parsedMilkType == type }
            Triple(
                type,
                typeEntries.sumOf { it.quantityLiters },
                typeEntries.sumOf { it.totalAmount }
            )
        }
    }

    val rows = remember(snapshot) {
        snapshot.customerSummaries
            .filter { it.customer.active }
            .sortedByDescending { it.balanceDue }
            .map { summary ->
                CustomerHisaabRow(
                    summary = summary,
                    lastEntry = snapshot.milkEntries
                        .filter { it.customerId == summary.customer.id }
                        .maxByOrNull { it.dateKey },
                    lastPayment = snapshot.payments
                        .filter { it.customerId == summary.customer.id }
                        .maxByOrNull { it.dateKey }
                )
            }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "dash_title") {
                val heroCols = snapshot.collections.filter {
                    it.dateKey == DairyFormatters.todayDateKey()
                }
                val heroLitres = DairyFormatters.roundTwoDecimals(
                    heroCols.sumOf { it.quantityLiters }
                )
                val heroAmount = DairyFormatters.roundToRupee(
                    heroCols.sumOf { it.totalAmount }
                )
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF3D9ACA), Color(0xFF1F6FB2))
                                )
                            )
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🥛 Milk Diary",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "आपका विश्वास ही हमारी पहचान है",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Aaj ka doodh: ${DairyFormatters.formatLiters(heroLitres)} • " +
                                DairyFormatters.formatRupees(heroAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            item(key = "dash_sale") {
                TodaySaleCard(
                    totalSale = totalSale,
                    totalLiters = totalLiters,
                    totalCollected = totalCollected
                )
            }

            item(key = "dash_type_title") {
                Text(
                    text = "दूध की किस्म के हिसाब से (आज)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "dash_type_card") {
                MilkTypeBreakdownCard(byType = byType, totalLiters = totalLiters)
            }

            item(key = "dash_hisaab_title") {
                Text(
                    text = "ग्राहकों का आखिरी हिसाब",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (rows.isEmpty()) {
                item(key = "dash_empty") {
                    Text(
                        text = "अभी कोई ग्राहक नहीं है। पहले ग्राहक जोड़ते ही यहाँ सबका हिसाब दिखेगा।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(items = rows, key = { "hisaab_${it.summary.customer.id}" }) { row ->
                    CustomerHisaabCard(row = row)
                }
            }

            item(key = "dash_collection") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🚀 Kaam shuru karo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashActionCard(
                            emoji = "🥛",
                            title = "Doodh Collection",
                            desc = "Route par ghar-ghar doodh, bina rate save",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenCollection
                        )
                        DashActionCard(
                            emoji = "📋",
                            title = "Supplier Hisaab",
                            desc = "10 din ka hisaab aur WhatsApp sheet",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenSupplierHisaab
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashActionCard(
                            emoji = "📈",
                            title = "Rate Chart",
                            desc = "FAT/SNF rate list dekho ya badlo",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenRateChart
                        )
                        DashActionCard(
                            emoji = "⚠️",
                            title = "Galti Check",
                            desc = "Duplicate aur galtiyaan pehle pakdo",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenGaltiCheck
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenGuide,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "❓ App Kaise Chalaye — naya user? Yahan se seekho")
                    }
                    OutlinedButton(
                        onClick = onCheckUpdate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "App Update देखो")
                    }
                }
            }

            item(key = "dash_hisaab_day") {
                val dayOfMonth = DairyFormatters.todayDateKey()
                    .split("-").getOrNull(2)?.toIntOrNull() ?: 0
                if (dayOfMonth == 1 || dayOfMonth == 11 || dayOfMonth == 21) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📋 Hisaab ka din!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kal pichla 10-din ka cycle khatm hua hai। Supplier Hisaab mein jaakar sabki sheets bhej do।",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = onOpenSupplierHisaab,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Supplier Hisaab kholo")
                            }
                        }
                    }
                }
            }

            item(key = "dash_today_report") {
                val tKey = DairyFormatters.todayDateKey()
                val tCols = snapshot.collections.filter { it.dateKey == tKey }
                val tLitres = DairyFormatters.roundTwoDecimals(tCols.sumOf { it.quantityLiters })
                val tAmount = DairyFormatters.roundToRupee(tCols.sumOf { it.totalAmount })
                val tSuppliers = tCols.map { it.supplierName }.distinct().size
                val tPending = tCols.count { it.fat == 0.0 }
                val tGoods = DairyFormatters.roundToRupee(
                    snapshot.supplierTransactions.filter {
                        it.dateKey == tKey && it.type == "GOODS" && !it.isHome
                    }.sumOf { it.amount }
                )
                val tAdvPay = DairyFormatters.roundToRupee(
                    snapshot.supplierTransactions.filter {
                        it.dateKey == tKey && (it.type == "ADVANCE" || it.type == "PAYMENT") && !it.isHome
                    }.sumOf { it.amount }
                )
                val tDay = tKey.split("-").getOrNull(2)?.toIntOrNull() ?: 0
                val nextSettle = if (tDay == 1 || tDay == 11 || tDay == 21) {
                    tKey
                } else {
                    DairyFormatters.shiftDateKey(DairyFormatters.cycleEndKey(tKey), 1)
                }
                val allNames = (
                    snapshot.collections.map { it.supplierName } +
                        snapshot.supplierTransactions.filter { !it.isHome }.map { it.supplierName }
                    ).distinct()
                var denaTotal = 0.0
                var dairyBaakiTotal = 0.0
                allNames.forEach { n ->
                    val bal = snapshot.collections.filter { it.supplierName == n }
                        .sumOf { it.totalAmount } -
                        snapshot.supplierTransactions.filter { it.supplierName == n && !it.isHome }
                            .sumOf { it.amount }
                    val rBal = DairyFormatters.roundToRupee(bal)
                    if (rBal > 0) denaTotal += rBal else dairyBaakiTotal += -rBal
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📊 Aaj ka Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🥛 Aaj ka doodh: ${DairyFormatters.formatLiters(tLitres)} • " +
                                DairyFormatters.formatRupees(tAmount),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "👥 Suppliers jinse doodh aaya: $tSuppliers • ⏳ FAT/SNF pending: $tPending",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "📦 Aaj samaan diya: ${DairyFormatters.formatRupees(tGoods)} • " +
                                "💵 Advance/Payment: ${DairyFormatters.formatRupees(tAdvPay)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (nextSettle == tKey) {
                                "📋 Settlement: AAJ hi hisaab ka din hai!"
                            } else {
                                "📋 Agla Settlement: ${DairyFormatters.formatShortDateHi(nextSettle)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "💰 Kul pending hisaab (dena hai): " +
                                DairyFormatters.formatRupees(DairyFormatters.roundTwoDecimals(denaTotal)) +
                                if (dairyBaakiTotal > 0.0) {
                                    " • डेरी बाकी: " +
                                        DairyFormatters.formatRupees(DairyFormatters.roundTwoDecimals(dairyBaakiTotal))
                                } else {
                                    ""
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item(key = "dash_daily_summary") {
                DailySummaryCard(snapshot = snapshot)
            }

            item(key = "dash_export") {
                ExportBackupCard(snapshot = snapshot)
            }
        }
    }
}

private fun buildBackupCsv(snapshot: DairyDataSnapshot): String {
    val sb = StringBuilder()
    sb.appendLine("Milk Diary - Poora Hisaab Backup")
    sb.appendLine()
    sb.appendLine("=== GRAAHAK ===")
    sb.appendLine("Naam,Phone,Kisam,Rate(per liter),Kul Doodh(L),Kul Bill,Bhugtan,Baaki")
    snapshot.customerSummaries.forEach { sum ->
        val c = sum.customer
        sb.appendLine(
            listOf(
                c.name, c.phone, c.parsedMilkType.labelHi,
                c.ratePerLiter.toString(), sum.totalMilkLiters.toString(),
                sum.totalMilkAmount.toString(), sum.totalPaidAmount.toString(),
                sum.balanceDue.toString()
            ).joinToString(",") { it.replace(",", " ") }
        )
    }
    sb.appendLine()
    sb.appendLine("=== DOODH ENTRY ===")
    sb.appendLine("Taarikh,Shift,Graahak,Kisam,Liter,Rate,Amount,Absent")
    snapshot.milkEntries.sortedBy { it.dateKey }.forEach { e ->
        sb.appendLine(
            listOf(
                e.dateKey, e.parsedShift.labelHi, e.customerName, e.parsedMilkType.labelHi,
                e.quantityLiters.toString(), e.ratePerLiter.toString(),
                e.totalAmount.toString(), if (e.absent) "Haan" else "Nahi"
            ).joinToString(",") { it.replace(",", " ") }
        )
    }
    sb.appendLine()
    sb.appendLine("=== PAYMENT ===")
    sb.appendLine("Taarikh,Graahak,Amount,Mode")
    snapshot.payments.sortedBy { it.dateKey }.forEach { pay ->
        sb.appendLine(
            listOf(
                pay.dateKey, pay.customerName, pay.amount.toString(), pay.parsedMode.labelHi
            ).joinToString(",") { it.replace(",", " ") }
        )
    }
    return sb.toString()
}

@Composable
private fun ExportBackupCard(snapshot: DairyDataSnapshot) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "डेटा की सुरक्षा (Backup)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "पूरा हिसाब एक file में निकालकर WhatsApp या Drive पर रख लो — " +
                    "phone badle ya kho jaye, हिसाब हमेशा safe रहेगा।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    val csv = buildBackupCsv(snapshot)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Milk Diary Backup")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, "Backup bhejein"))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(text = "  पूरा हिसाब Export करें")
            }
        }
    }
}

@Composable
private fun TodaySaleCard(
    totalSale: Double,
    totalLiters: Double,
    totalCollected: Double
) {
    val remaining = totalSale - totalCollected
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(colors = listOf(MeadowEmerald, MeadowEmeraldLight))
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "आज की कुल बिक्री",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.88f)
            )
            Text(
                text = DairyFormatters.formatRupees(totalSale),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SaleStatChip(
                    label = "दूध",
                    value = DairyFormatters.formatLiters(totalLiters),
                    modifier = Modifier.weight(1f)
                )
                SaleStatChip(
                    label = "वसूली हुई",
                    value = DairyFormatters.formatRupees(totalCollected),
                    modifier = Modifier.weight(1f)
                )
                SaleStatChip(
                    label = "आज का बाकी",
                    value = DairyFormatters.formatRupees(if (remaining > 0.0) remaining else 0.0),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SaleStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MilkTypeBreakdownCard(
    byType: List<Triple<MilkType, Double, Double>>,
    totalLiters: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            byType.forEachIndexed { index, (type, liters, amount) ->
                val share = if (totalLiters > 0.0) (liters / totalLiters).toFloat() else 0f
                val animatedShare by animateFloatAsState(
                    targetValue = share,
                    animationSpec = tween(durationMillis = 700, delayMillis = index * 80),
                    label = "typeShare$index"
                )
                val dotColor = when (type) {
                    MilkType.BUFFALO -> MaterialTheme.colorScheme.primary
                    MilkType.COW -> MaterialTheme.colorScheme.secondary
                    MilkType.MIXED -> MaterialTheme.colorScheme.tertiary
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = "  ${type.labelHi} का दूध",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${DairyFormatters.formatLiters(liters)} • ${DairyFormatters.formatRupees(amount)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedShare.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(dotColor)
                        )
                    }
                }
            }
            Text(
                text = "जिस किस्म का दूध ज़्यादा बिकता है, उसकी पट्टी उतनी ही लंबी दिखेगी।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomerHisaabCard(row: CustomerHisaabRow) {
    val customer = row.summary.customer
    val due = row.summary.balanceDue
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (due > 0.0) {
                    Text(
                        text = "बाकी ${DairyFormatters.formatRupees(due)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "हिसाब साफ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "${customer.parsedMilkType.labelHi} • रेट ${DairyFormatters.formatRupees(customer.ratePerLiter)}/ली.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val lastEntry = row.lastEntry
            if (lastEntry != null) {
                Text(
                    text = if (lastEntry.absent) {
                        "आखिरी एंट्री: ${DairyFormatters.formatShortDateHi(lastEntry.dateKey)} • अनुपस्थित"
                    } else {
                        "आखिरी दूध: ${DairyFormatters.formatShortDateHi(lastEntry.dateKey)} • " +
                            "${DairyFormatters.formatLiters(lastEntry.quantityLiters)} • " +
                            DairyFormatters.formatRupees(lastEntry.totalAmount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val lastPayment = row.lastPayment
            if (lastPayment != null) {
                Text(
                    text = "आखिरी पेमेंट: ${DairyFormatters.formatShortDateHi(lastPayment.dateKey)} • " +
                        "${DairyFormatters.formatRupees(lastPayment.amount)} (${lastPayment.parsedMode.labelHi})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (due > 0.0 && customer.phone.isNotBlank()) {
                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val digits = customer.phone.filter { it.isDigit() }
                        val waPhone = when {
                            digits.length == 10 -> "91$digits"
                            digits.startsWith("91") && digits.length == 12 -> digits
                            else -> digits
                        }
                        val msg = "नमस्ते ${customer.name} जी, आपका दूध का कुल बाकी " +
                            "${DairyFormatters.formatRupees(due)} है। कृपया भुगतान कर दें। धन्यवाद! — Milk Diary"
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/$waPhone?text=${Uri.encode(msg)}")
                        )
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(text = "  WhatsApp पर बाकी याद दिलाएँ")
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(snapshot: DairyDataSnapshot) {
    var dateKey by remember { mutableStateOf(DairyFormatters.todayDateKey()) }
    val dayCollections = remember(snapshot.collections, dateKey) {
        snapshot.collections.filter { it.dateKey == dateKey }
    }
    val collLiters = DairyFormatters.roundTwoDecimals(dayCollections.sumOf { it.quantityLiters })
    val collAmount = DairyFormatters.roundTwoDecimals(dayCollections.sumOf { it.totalAmount })
    val morningLiters = dayCollections
        .filter { it.parsedShift == DeliveryShift.MORNING }.sumOf { it.quantityLiters }
    val eveningLiters = dayCollections
        .filter { it.parsedShift == DeliveryShift.EVENING }.sumOf { it.quantityLiters }
    val pendingCount = dayCollections.count { it.fat == 0.0 }
    val daySales = remember(snapshot.milkEntries, dateKey) {
        snapshot.milkEntries.filter { it.dateKey == dateKey && !it.absent }
    }
    val saleLiters = DairyFormatters.roundTwoDecimals(daySales.sumOf { it.quantityLiters })
    val saleAmount = DairyFormatters.roundTwoDecimals(daySales.sumOf { it.totalAmount })
    val baakiLiters = DairyFormatters.roundTwoDecimals(collLiters - saleLiters)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Roz ka Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { dateKey = DairyFormatters.shiftDateKey(dateKey, -1) }) {
                    Text("◀")
                }
                Text(
                    text = DairyFormatters.formatShortDateHi(dateKey),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                OutlinedButton(onClick = { dateKey = DairyFormatters.shiftDateKey(dateKey, 1) }) {
                    Text("▶")
                }
            }
            SummaryLine(
                label = "आया दूध (सुबह ${DairyFormatters.formatLiters(morningLiters)} + शाम ${DairyFormatters.formatLiters(eveningLiters)})",
                value = "${DairyFormatters.formatLiters(collLiters)} • ${DairyFormatters.formatRupees(collAmount)}"
            )
            SummaryLine(
                label = "Local sale (ग्राहक बिक्री)",
                value = "${DairyFormatters.formatLiters(saleLiters)} • ${DairyFormatters.formatRupees(saleAmount)}"
            )
            HorizontalDivider()
            SummaryLine(
                label = "बाकी दूध (plant को गया)",
                value = DairyFormatters.formatLiters(baakiLiters)
            )
            if (pendingCount > 0) {
                Text(
                    text = "⏳ $pendingCount sample का FAT/SNF बाकी है — rate भरते ही रकम जुड़ जाएगी।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun DashActionCard(
    emoji: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
