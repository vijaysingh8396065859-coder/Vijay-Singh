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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.DairyFormatters
import com.example.data.PaymentMode
import com.example.data.PaymentRecord
import com.example.ui.theme.MeadowEmerald
import com.example.ui.theme.MeadowEmeraldLight
import com.example.viewmodel.DairyDataSnapshot

@Composable
fun PaymentsScreen(
    snapshot: DairyDataSnapshot,
    selectedMonthKey: String,
    onChangeMonth: (Int) -> Unit,
    onAddPaymentClick: () -> Unit,
    onEditPaymentClick: (PaymentRecord) -> Unit,
    onDeletePaymentClick: (PaymentRecord) -> Unit,
    onOpenCustomerDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var modeFilter by remember { mutableStateOf<PaymentMode?>(null) }
    val todayKey = remember { DairyFormatters.todayDateKey() }

    val monthPayments = remember(snapshot.payments, selectedMonthKey) {
        snapshot.payments.filter { it.dateKey.startsWith(selectedMonthKey) }
    }
    val filteredPayments = remember(monthPayments, modeFilter) {
        if (modeFilter == null) monthPayments
        else monthPayments.filter { it.parsedMode == modeFilter }
    }

    val todayTotal = remember(snapshot.payments, todayKey) {
        DairyFormatters.roundToRupee(snapshot.payments.filter { it.dateKey == todayKey }.sumOf { it.amount })
    }
    val monthTotal = remember(monthPayments) { DairyFormatters.roundToRupee(monthPayments.sumOf { it.amount }) }
    val cashTotal = remember(monthPayments) {
        DairyFormatters.roundToRupee(monthPayments.filter { it.parsedMode == PaymentMode.CASH }.sumOf { it.amount })
    }
    val upiTotal = remember(monthPayments) {
        DairyFormatters.roundToRupee(monthPayments.filter { it.parsedMode == PaymentMode.UPI }.sumOf { it.amount })
    }
    val bankTotal = remember(monthPayments) {
        DairyFormatters.roundToRupee(monthPayments.filter { it.parsedMode == PaymentMode.BANK }.sumOf { it.amount })
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "month_header") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
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
                }
            }

            item(key = "payment_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(MeadowEmerald, MeadowEmeraldLight)))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "इस महीने की कुल वसूली",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = DairyFormatters.formatRupees(monthTotal),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "आज प्राप्त",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = DairyFormatters.formatRupees(todayTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PaymentModePill("नकद", DairyFormatters.formatRupees(cashTotal), Modifier.weight(1f))
                            PaymentModePill("UPI", DairyFormatters.formatRupees(upiTotal), Modifier.weight(1f))
                            PaymentModePill("बैंक", DairyFormatters.formatRupees(bankTotal), Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "mode_filters") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = modeFilter == null,
                        onClick = { modeFilter = null },
                        label = { Text("सभी") }
                    )
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = modeFilter == mode,
                            onClick = { modeFilter = if (modeFilter == mode) null else mode },
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            if (filteredPayments.isEmpty()) {
                item(key = "empty_payments") {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "इस अवधि में कोई पेमेंट दर्ज नहीं है",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (snapshot.customers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onAddPaymentClick,
                                    modifier = Modifier.testTag("empty_add_payment_button")
                                ) {
                                    Text(stringResource(R.string.action_add_payment))
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredPayments,
                    key = { "pay_card_${it.id}" }
                ) { payment ->
                    PaymentHistoryCard(
                        payment = payment,
                        onOpenCustomer = { onOpenCustomerDetail(payment.customerId) },
                        onEdit = { onEditPaymentClick(payment) },
                        onDelete = { onDeletePaymentClick(payment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentModePill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PaymentHistoryCard(
    payment: PaymentRecord,
    onOpenCustomer: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val modeIcon = when (payment.parsedMode) {
        PaymentMode.CASH -> Icons.Rounded.Payments
        PaymentMode.UPI -> Icons.Rounded.QrCode2
        PaymentMode.BANK -> Icons.Rounded.AccountBalance
    }
    val modeBg = when (payment.parsedMode) {
        PaymentMode.CASH -> MaterialTheme.colorScheme.primaryContainer
        PaymentMode.UPI -> MaterialTheme.colorScheme.tertiaryContainer
        PaymentMode.BANK -> MaterialTheme.colorScheme.secondaryContainer
    }
    val modeTint = when (payment.parsedMode) {
        PaymentMode.CASH -> MaterialTheme.colorScheme.onPrimaryContainer
        PaymentMode.UPI -> MaterialTheme.colorScheme.onTertiaryContainer
        PaymentMode.BANK -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onOpenCustomer,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .testTag("payment_card_${payment.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(modeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = null,
                    tint = modeTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${DairyFormatters.formatDateDisplayHi(payment.dateKey)} • ${payment.parsedMode.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.note.isNotBlank()) {
                    Text(
                        text = payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${DairyFormatters.formatRupees(payment.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.cd_edit_item),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_item),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
