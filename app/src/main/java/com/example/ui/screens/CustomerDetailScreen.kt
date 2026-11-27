package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Customer
import com.example.data.DairyFormatters
import com.example.data.MilkEntry
import com.example.data.MonthlyCustomerStatement
import com.example.data.PaymentRecord
import com.example.ui.theme.MeadowEmerald
import com.example.ui.theme.MeadowEmeraldLight
import com.example.viewmodel.DairyDataSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    snapshot: DairyDataSnapshot,
    selectedMonthKey: String,
    onChangeMonth: (Int) -> Unit,
    onBack: () -> Unit,
    onEditCustomer: (Customer) -> Unit,
    onDeleteCustomer: (Customer) -> Unit,
    onAddMilk: (Customer) -> Unit,
    onAddPayment: (Customer) -> Unit,
    onEditEntry: (MilkEntry) -> Unit,
    onDeleteEntry: (MilkEntry) -> Unit,
    onEditPayment: (PaymentRecord) -> Unit,
    onDeletePayment: (PaymentRecord) -> Unit,
    onShareBill: (MonthlyCustomerStatement) -> Unit
) {
    val customer = snapshot.customers.firstOrNull { it.id == customerId }
    val summary = snapshot.summaryByCustomerId[customerId]
    var activeTab by remember { mutableIntStateOf(0) }

    val monthEntries = remember(snapshot.milkEntries, customerId, selectedMonthKey) {
        snapshot.milkEntries.filter {
            it.customerId == customerId && it.dateKey.startsWith(selectedMonthKey)
        }
    }
    val monthPayments = remember(snapshot.payments, customerId, selectedMonthKey) {
        snapshot.payments.filter {
            it.customerId == customerId && it.dateKey.startsWith(selectedMonthKey)
        }
    }

    val monthlyStatement = remember(customer, monthEntries, monthPayments, summary) {
        customer?.let { cust ->
            MonthlyCustomerStatement(
                customer = cust,
                deliveredDaysCount = monthEntries.filter { !it.absent && it.quantityLiters > 0.0 }
                    .map { it.dateKey }.distinct().size,
                absentDaysCount = monthEntries.filter { it.absent }
                    .map { it.dateKey }.distinct().size,
                monthLiters = DairyFormatters.roundTwoDecimals(
                    monthEntries.sumOf { if (it.absent) 0.0 else it.quantityLiters }
                ),
                monthBillAmount = DairyFormatters.roundTwoDecimals(
                    monthEntries.sumOf { if (it.absent) 0.0 else it.totalAmount }
                ),
                monthPaidAmount = DairyFormatters.roundTwoDecimals(
                    monthPayments.sumOf { it.amount }
                ),
                totalBalanceDue = summary?.balanceDue ?: cust.openingBalance
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = customer?.name ?: "ग्राहक खाता",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("customer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (monthlyStatement != null) {
                        IconButton(
                            onClick = { onShareBill(monthlyStatement) },
                            modifier = Modifier.testTag("customer_share_bill_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.cd_share_bill)
                            )
                        }
                    }
                    if (customer != null) {
                        IconButton(
                            onClick = { onEditCustomer(customer) },
                            modifier = Modifier.testTag("customer_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.cd_edit_item)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCustomer(customer) },
                            modifier = Modifier.testTag("customer_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_item),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (customer == null || summary == null || monthlyStatement == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("ग्राहक का रिकॉर्ड उपलब्ध नहीं है।")
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "balance_hero") {
                    CustomerLedgerHeroCard(
                        customer = customer,
                        summaryDue = summary.balanceDue,
                        totalMilkLiters = summary.totalMilkLiters,
                        totalBilled = summary.totalMilkAmount,
                        totalPaid = summary.totalPaidAmount
                    )
                }

                item(key = "quick_actions") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onAddMilk(customer) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_add_milk_button")
                        ) {
                            Icon(Icons.Rounded.LocalDrink, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_add_milk))
                        }
                        FilledTonalButton(
                            onClick = { onAddPayment(customer) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_add_payment_button")
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_add_payment))
                        }
                    }
                }

                item(key = "month_switcher") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { onChangeMonth(-1) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.cd_prev_month)
                                    )
                                }
                                Text(
                                    text = DairyFormatters.formatMonthDisplayHi(selectedMonthKey),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { onChangeMonth(1) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.cd_next_month)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            ) {
                                MiniMonthStat("मासिक दूध", DairyFormatters.formatKg(monthlyStatement.monthLiters))
                                MiniMonthStat("मासिक बिल", DairyFormatters.formatRupees(DairyFormatters.roundToRupee(monthlyStatement.monthBillAmount)))
                                MiniMonthStat("मासिक जमा", DairyFormatters.formatRupees(DairyFormatters.roundToRupee(monthlyStatement.monthPaidAmount)))
                            }
                        }
                    }
                }

                item(key = "ledger_tabs") {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("दूध का हिसाब (${monthEntries.size})") }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("पेमेंट इतिहास (${monthPayments.size})") }
                        )
                    }
                }

                if (activeTab == 0) {
                    if (monthEntries.isEmpty()) {
                        item(key = "empty_milk") {
                            EmptyMonthRecordNotice("इस महीने दूध की कोई एंट्री दर्ज नहीं है।")
                        }
                    } else {
                        items(items = monthEntries, key = { "detail_milk_${it.id}" }) { entry ->
                            CustomerMilkRow(
                                entry = entry,
                                onEdit = { onEditEntry(entry) },
                                onDelete = { onDeleteEntry(entry) }
                            )
                        }
                    }
                } else {
                    if (monthPayments.isEmpty()) {
                        item(key = "empty_pay") {
                            EmptyMonthRecordNotice("इस महीने कोई पेमेंट दर्ज नहीं है।")
                        }
                    } else {
                        items(items = monthPayments, key = { "detail_pay_${it.id}" }) { payment ->
                            CustomerPaymentRow(
                                payment = payment,
                                onEdit = { onEditPayment(payment) },
                                onDelete = { onDeletePayment(payment) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerHeroCard(
    customer: Customer,
    summaryDue: Double,
    totalMilkLiters: Double,
    totalBilled: Double,
    totalPaid: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(MeadowEmerald, MeadowEmeraldLight)))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            summaryDue > 0.0 -> "कुल डेरी बाकी"
                            summaryDue < 0.0 -> "कुल जमा"
                            else -> "खाता स्थिति"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = when {
                            summaryDue > 0.0 -> DairyFormatters.formatRupees(DairyFormatters.roundToRupee(summaryDue))
                            summaryDue < 0.0 -> DairyFormatters.formatRupees(DairyFormatters.roundToRupee(-summaryDue))
                            else -> "₹0 (साफ)"
                        },
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "${customer.parsedMilkType.label}\n${DairyFormatters.formatRupees(customer.ratePerLiter)}/किलो",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (customer.phone.isNotBlank() || customer.address.isNotBlank()) {
                Text(
                    text = listOf(customer.phone, customer.address)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("कुल दूध", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                    Text(DairyFormatters.formatKg(totalMilkLiters), style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Column {
                    Text("कुल बिल", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                    Text(DairyFormatters.formatRupees(DairyFormatters.roundToRupee(totalBilled)), style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Column {
                    Text("कुल जमा", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                    Text(DairyFormatters.formatRupees(DairyFormatters.roundToRupee(totalPaid)), style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MiniMonthStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CustomerMilkRow(
    entry: MilkEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${DairyFormatters.formatShortDateHi(entry.dateKey)} • ${entry.parsedShift.label}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (entry.absent) {
                        "छुट्टी / नागा"
                    } else {
                        "${entry.parsedMilkType.label} • ${DairyFormatters.formatKg(entry.quantityLiters)} × ${DairyFormatters.formatRupees(entry.ratePerLiter)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (entry.absent) "छुट्टी" else DairyFormatters.formatRupees(entry.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (entry.absent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.cd_edit_item), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomerPaymentRow(
    payment: PaymentRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${DairyFormatters.formatShortDateHi(payment.dateKey)} • ${payment.parsedMode.label}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (payment.note.isNotBlank()) {
                    Text(
                        text = payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "+${DairyFormatters.formatRupees(payment.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.cd_edit_item), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyMonthRecordNotice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}
