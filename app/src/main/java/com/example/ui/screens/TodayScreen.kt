package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.WeekMilkChartCard
import com.example.R
import com.example.data.Customer
import com.example.data.CustomerShift
import com.example.data.DairyFormatters
import com.example.data.DeliveryShift
import com.example.data.MilkEntry
import com.example.ui.theme.MeadowEmerald
import com.example.ui.theme.MeadowEmeraldLight
import com.example.ui.AppLang
import com.example.viewmodel.DairyDataSnapshot

@Composable
fun TodayScreen(
    snapshot: DairyDataSnapshot,
    selectedDateKey: String,
    selectedShift: DeliveryShift,
    onChangeDate: (Int) -> Unit,
    onResetToday: () -> Unit,
    onSelectShift: (DeliveryShift) -> Unit,
    onAddCustomerClick: () -> Unit,
    onQuickLogMilk: (Customer, Double) -> Unit,
    onQuickMarkAbsent: (Customer) -> Unit,
    onQuickPaymentClick: (Customer) -> Unit,
    onEditEntryClick: (MilkEntry) -> Unit,
    onDeleteEntryClick: (MilkEntry) -> Unit,
    onOpenCustomerDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayEntries = remember(snapshot.milkEntries, selectedDateKey) {
        snapshot.milkEntries.filter { it.dateKey == selectedDateKey }
    }
    val dayShiftEntries = remember(dayEntries, selectedShift) {
        dayEntries.filter { it.parsedShift == selectedShift }
    }
    val dayPayments = remember(snapshot.payments, selectedDateKey) {
        snapshot.payments.filter { it.dateKey == selectedDateKey }
    }

    val totalDayLiters = remember(dayEntries) {
        dayEntries.sumOf { if (it.absent) 0.0 else it.quantityLiters }
    }
    val totalDayBill = remember(dayEntries) {
        dayEntries.sumOf { if (it.absent) 0.0 else it.totalAmount }
    }
    val totalDayCollected = remember(dayPayments) {
        dayPayments.sumOf { it.amount }
    }

    val loggedCustomerIdsForShift = remember(dayShiftEntries) {
        dayShiftEntries.map { it.customerId }.toSet()
    }

    val pendingCustomersForShift = remember(snapshot.customers, selectedShift, loggedCustomerIdsForShift) {
        snapshot.customers.filter { customer ->
            customer.active &&
                customer.id !in loggedCustomerIdsForShift &&
                (customer.parsedDefaultShift == CustomerShift.BOTH ||
                    (selectedShift == DeliveryShift.MORNING && customer.parsedDefaultShift == CustomerShift.MORNING) ||
                    (selectedShift == DeliveryShift.EVENING && customer.parsedDefaultShift == CustomerShift.EVENING))
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
            item(key = "date_and_shift") {
                DateAndShiftHeader(
                    selectedDateKey = selectedDateKey,
                    selectedShift = selectedShift,
                    onChangeDate = onChangeDate,
                    onResetToday = onResetToday,
                    onSelectShift = onSelectShift
                )
            }

            item(key = "hero_summary") {
                TodayHeroSummaryCard(
                    totalLiters = totalDayLiters,
                    totalBill = totalDayBill,
                    totalCollected = totalDayCollected,
                    entryCount = dayEntries.size
                )
            }

            item(key = "week_chart") {
                WeekMilkChartCard(
                    snapshot = snapshot,
                    selectedDateKey = selectedDateKey
                )
            }

            if (snapshot.customers.isEmpty()) {
                item(key = "empty_customers") {
                    EmptyDairyWelcomeCard(onAddCustomerClick = onAddCustomerClick)
                }
            } else {
                if (pendingCustomersForShift.isNotEmpty()) {
                    item(key = "pending_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = "${selectedShift.label} की बाकी एंट्री (${pendingCustomersForShift.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "1-टैप में दर्ज करें",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(
                        items = pendingCustomersForShift,
                        key = { "pending_${it.id}_${selectedDateKey}_${selectedShift.name}" }
                    ) { customer ->
                        val due = snapshot.summaryByCustomerId[customer.id]?.balanceDue ?: 0.0
                        QuickDeliveryCustomerCard(
                            customer = customer,
                            balanceDue = due,
                            onLogMilk = { qty -> onQuickLogMilk(customer, qty) },
                            onMarkAbsent = { onQuickMarkAbsent(customer) },
                            onQuickPayment = { onQuickPaymentClick(customer) },
                            onOpenDetail = { onOpenCustomerDetail(customer.id) }
                        )
                    }
                }

                item(key = "logged_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = "दर्ज दूध एंट्रियाँ (${dayShiftEntries.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (dayEntries.size > dayShiftEntries.size) {
                            Text(
                                text = "कुल दिन में: ${dayEntries.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (dayShiftEntries.isEmpty()) {
                    item(key = "no_shift_entries") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalDrink,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${selectedShift.label} के लिए अभी कोई दूध एंट्री दर्ज नहीं है",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = dayShiftEntries,
                        key = { "entry_${it.id}" }
                    ) { entry ->
                        LoggedMilkEntryCard(
                            entry = entry,
                            onEdit = { onEditEntryClick(entry) },
                            onDelete = { onDeleteEntryClick(entry) },
                            onOpenCustomer = { onOpenCustomerDetail(entry.customerId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateAndShiftHeader(
    selectedDateKey: String,
    selectedShift: DeliveryShift,
    onChangeDate: (Int) -> Unit,
    onResetToday: () -> Unit,
    onSelectShift: (DeliveryShift) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { onChangeDate(-1) },
                    modifier = Modifier.testTag("prev_date_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_prev_date)
                    )
                }

                TextButton(
                    onClick = onResetToday,
                    modifier = Modifier.testTag("today_date_label")
                ) {
                    Text(
                        text = DairyFormatters.formatDateDisplayHi(selectedDateKey),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { onChangeDate(1) },
                    modifier = Modifier.testTag("next_date_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_date)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DeliveryShift.entries.forEach { shift ->
                val selected = selectedShift == shift
                FilterChip(
                    selected = selected,
                    onClick = { onSelectShift(shift) },
                    label = {
                        Text(
                            text = "${shift.label} की शिफ्ट",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (shift == DeliveryShift.MORNING) {
                                Icons.Rounded.WbSunny
                            } else {
                                Icons.Rounded.NightsStay
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("shift_chip_${shift.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun TodayHeroSummaryCard(
    totalLiters: Double,
    totalBill: Double,
    totalCollected: Double,
    entryCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MeadowEmerald, MeadowEmeraldLight)
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "आज का दूध व वसूली सारांश",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.88f)
                )
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "$entryCount एंट्री",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                HeroStatBlock(
                    label = com.example.ui.AppLang.s("कुल दूध", "Total Milk"),
                    value = DairyFormatters.formatKg(totalLiters),
                    modifier = Modifier.weight(1f)
                )
                HeroStatBlock(
                    label = com.example.ui.AppLang.s("दूध का बिल", "Milk Bill"),
                    value = DairyFormatters.formatRupees(totalBill),
                    modifier = Modifier.weight(1f)
                )
                HeroStatBlock(
                    label = com.example.ui.AppLang.s("प्राप्त पेमेंट", "Payments Received"),
                    value = DairyFormatters.formatRupees(totalCollected),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickDeliveryCustomerCard(
    customer: Customer,
    balanceDue: Double,
    onLogMilk: (Double) -> Unit,
    onMarkAbsent: () -> Unit,
    onQuickPayment: () -> Unit,
    onOpenDetail: () -> Unit
) {
    var qty by remember(customer.id, customer.defaultQtyLiters) {
        mutableDoubleStateOf(customer.defaultQtyLiters.coerceAtLeast(0.5))
    }
    val lineAmount = DairyFormatters.roundTwoDecimals(qty * customer.ratePerLiter)

    Card(
        onClick = onOpenDetail,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .testTag("quick_customer_card_${customer.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${customer.parsedMilkType.label} • ${DairyFormatters.formatRupees(customer.ratePerLiter)}/किलो" +
                            if (balanceDue > 0.0) " • डेरी बाकी ${DairyFormatters.formatRupees(DairyFormatters.roundToRupee(balanceDue))}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalIconButton(
                    onClick = onQuickPayment,
                    modifier = Modifier.testTag("quick_payment_btn_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CurrencyRupee,
                        contentDescription = stringResource(R.string.cd_quick_payment)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { qty = (qty - 0.5).coerceAtLeast(0.25) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = stringResource(R.string.cd_decrease_qty),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = DairyFormatters.formatKg(qty),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { qty = (qty + 0.5).coerceAtMost(100.0) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.cd_increase_qty),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onMarkAbsent,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("quick_absent_btn_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("छुट्टी", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { onLogMilk(qty) },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("quick_log_btn_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DairyFormatters.formatRupees(lineAmount),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LoggedMilkEntryCard(
    entry: MilkEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenCustomer: () -> Unit
) {
    Card(
        onClick = onOpenCustomer,
        colors = CardDefaults.cardColors(
            containerColor = if (entry.absent) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .testTag("logged_entry_card_${entry.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = if (entry.absent) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (entry.absent) "छुट्टी" else entry.parsedMilkType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.absent) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (entry.absent) {
                        "${entry.parsedShift.label} • दूध नहीं लिया"
                    } else {
                        "${entry.parsedShift.label} • ${DairyFormatters.formatKg(entry.quantityLiters)} × ${DairyFormatters.formatRupees(entry.ratePerLiter)}"
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (entry.absent) "₹0" else DairyFormatters.formatRupees(entry.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.absent) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.cd_edit_item),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
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

@Composable
private fun EmptyDairyWelcomeCard(onAddCustomerClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dairy_hero_1795579554345),
                contentDescription = stringResource(R.string.cd_hero_dairy),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "अपनी मिल्क डायरी शुरू करें",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "पहले अपने ग्राहक जोड़ें — इसके बाद रोज़ाना दूध की एंट्री और डेली पेमेंट सिर्फ एक टैप में दर्ज कर सकेंगे।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onAddCustomerClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_add_first_customer_button")
            ) {
                Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "पहला ग्राहक जोड़ें", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
