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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Customer
import com.example.data.CustomerAccountSummary
import com.example.data.DairyFormatters
import com.example.viewmodel.DairyDataSnapshot

private enum class CustomerListFilter(val labelHi: String, val labelEn: String) {
    ALL("सभी", "All"),
    DUE("डेरी बाकी", "Dairy Due"),
    ACTIVE("सक्रिय", "Active");

    val label: String
        get() = if (com.example.ui.AppLang.isEnglish()) labelEn else labelHi
}

@Composable
fun CustomersScreen(
    snapshot: DairyDataSnapshot,
    onAddCustomerClick: () -> Unit,
    onOpenCustomerDetail: (String) -> Unit,
    onAddMilkForCustomer: (Customer) -> Unit,
    onAddPaymentForCustomer: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(CustomerListFilter.ALL) }

    val filteredSummaries = remember(snapshot.customerSummaries, searchQuery, filter) {
        val q = searchQuery.trim().lowercase()
        snapshot.customerSummaries.filter { summary ->
            val matchesQuery = q.isEmpty() ||
                summary.customer.name.lowercase().contains(q) ||
                summary.customer.phone.contains(q) ||
                summary.customer.address.lowercase().contains(q)
            val matchesFilter = when (filter) {
                CustomerListFilter.ALL -> true
                CustomerListFilter.DUE -> summary.balanceDue > 0.0
                CustomerListFilter.ACTIVE -> summary.customer.active
            }
            matchesQuery && matchesFilter
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "customers_overview") {
                CustomersOverviewStrip(
                    customerCount = snapshot.customers.size,
                    totalPendingDues = snapshot.totalPendingDues,
                    totalAdvance = snapshot.totalAdvanceAmount
                )
            }

            item(key = "search_and_filter") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(com.example.ui.AppLang.s("ग्राहक का नाम, नंबर या पता खोजें...", "Search customer name, number or address...")) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_clear_search)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_search_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CustomerListFilter.entries.forEach { itemFilter ->
                            FilterChip(
                                selected = filter == itemFilter,
                                onClick = { filter = itemFilter },
                                label = { Text(itemFilter.label) },
                                modifier = Modifier.testTag("customer_filter_${itemFilter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (filteredSummaries.isEmpty()) {
                item(key = "empty_list") {
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
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (snapshot.customers.isEmpty()) {
                                    com.example.ui.AppLang.s("अभी कोई ग्राहक नहीं जोड़ा गया है", "No customers added yet")
                                } else {
                                    com.example.ui.AppLang.s("इस फ़िल्टर में कोई ग्राहक नहीं मिला", "No customer in this filter")
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (snapshot.customers.isEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onAddCustomerClick,
                                    modifier = Modifier.testTag("customers_empty_add_btn")
                                ) {
                                    Text(stringResource(R.string.action_add_customer))
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredSummaries,
                    key = { "cust_card_${it.customer.id}" }
                ) { summary ->
                    CustomerLedgerCard(
                        summary = summary,
                        onOpenDetail = { onOpenCustomerDetail(summary.customer.id) },
                        onAddMilk = { onAddMilkForCustomer(summary.customer) },
                        onAddPayment = { onAddPaymentForCustomer(summary.customer) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomersOverviewStrip(
    customerCount: Int,
    totalPendingDues: Double,
    totalAdvance: Double
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OverviewMetricCard(
            label = com.example.ui.AppLang.s("कुल ग्राहक", "Total Customers"),
            value = "$customerCount",
            containerColor = MaterialTheme.colorScheme.surface,
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.85f)
        )
        OverviewMetricCard(
            label = com.example.ui.AppLang.s("कुल डेरी बाकी", "Total Dairy Due"),
            value = DairyFormatters.formatRupees(DairyFormatters.roundToRupee(totalPendingDues)),
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            valueColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1.1f)
        )
        OverviewMetricCard(
            label = com.example.ui.AppLang.s("कुल जमा", "Total Jama"),
            value = DairyFormatters.formatRupees(DairyFormatters.roundToRupee(totalAdvance)),
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
            valueColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1.05f)
        )
    }
}

@Composable
private fun OverviewMetricCard(
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
            RoundedCornerShape(18.dp)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerLedgerCard(
    summary: CustomerAccountSummary,
    onOpenDetail: () -> Unit,
    onAddMilk: () -> Unit,
    onAddPayment: () -> Unit
) {
    val customer = summary.customer
    val isDue = summary.balanceDue > 0.0
    val isAdvance = summary.balanceDue < 0.0

    Card(
        onClick = onOpenDetail,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .testTag("customer_ledger_card_${customer.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (customer.active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!customer.active) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = com.example.ui.AppLang.s("बंद", "Closed"),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${customer.parsedMilkType.label} • ${DairyFormatters.formatKg(customer.defaultQtyLiters)} • ${DairyFormatters.formatRupees(customer.ratePerLiter)}/किलो • ${customer.parsedDefaultShift.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.address.isNotBlank() || customer.phone.isNotBlank()) {
                        Text(
                            text = listOf(customer.phone, customer.address)
                                .filter { it.isNotBlank() }
                                .joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = when {
                        isDue -> MaterialTheme.colorScheme.errorContainer
                        isAdvance -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when {
                                isDue -> DairyFormatters.formatRupees(DairyFormatters.roundToRupee(summary.balanceDue))
                                isAdvance -> DairyFormatters.formatRupees(DairyFormatters.roundToRupee(-summary.balanceDue))
                                else -> "₹0"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isDue -> MaterialTheme.colorScheme.error
                                isAdvance -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = when {
                                isDue -> "डेरी बाकी"
                                isAdvance -> "जमा"
                                else -> com.example.ui.AppLang.s("हिसाब साफ", "Settled")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onAddMilk,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("customer_add_milk_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalDrink,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(com.example.ui.AppLang.s("दूध जोड़ें", "Add Milk"), style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onAddPayment,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("customer_add_pay_${customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(com.example.ui.AppLang.s("पेमेंट लें", "Take Payment"), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
