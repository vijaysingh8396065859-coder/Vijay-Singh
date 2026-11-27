package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Customer
import com.example.data.CustomerAccountSummary
import com.example.data.CustomerShift
import com.example.data.DairyFormatters
import com.example.data.DeliveryShift
import com.example.data.MilkEntry
import com.example.data.MilkType
import com.example.data.PaymentMode
import com.example.data.PaymentRecord

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerFormDialog(
    existingCustomer: Customer?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        address: String,
        milkType: MilkType,
        defaultShift: CustomerShift,
        defaultQtyLiters: Double,
        ratePerLiter: Double,
        openingBalance: Double,
        active: Boolean
    ) -> Unit
) {
    var name by remember(existingCustomer) { mutableStateOf(existingCustomer?.name ?: "") }
    var phone by remember(existingCustomer) { mutableStateOf(existingCustomer?.phone ?: "") }
    var address by remember(existingCustomer) { mutableStateOf(existingCustomer?.address ?: "") }
    var milkType by remember(existingCustomer) {
        mutableStateOf(existingCustomer?.parsedMilkType ?: MilkType.BUFFALO)
    }
    var defaultShift by remember(existingCustomer) {
        mutableStateOf(existingCustomer?.parsedDefaultShift ?: CustomerShift.MORNING)
    }
    var qtyText by remember(existingCustomer) {
        mutableStateOf(
            DairyFormatters.formatLitersNumeric(existingCustomer?.defaultQtyLiters ?: 1.0)
        )
    }
    var rateText by remember(existingCustomer) {
        mutableStateOf(
            DairyFormatters.formatLitersNumeric(existingCustomer?.ratePerLiter ?: 65.0)
        )
    }
    var openingBalanceText by remember(existingCustomer) {
        mutableStateOf(
            if (existingCustomer != null && existingCustomer.openingBalance != 0.0) {
                DairyFormatters.formatLitersNumeric(existingCustomer.openingBalance)
            } else {
                "0"
            }
        )
    }
    var active by remember(existingCustomer) { mutableStateOf(existingCustomer?.active ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingCustomer == null) "नया ग्राहक जोड़ें" else "ग्राहक विवरण बदलें",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it; validationError = null },
                    label = { Text("ग्राहक का नाम *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 20) phone = it },
                    label = { Text("मोबाइल नंबर (वैकल्पिक)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { if (it.length <= 120) address = it },
                    label = { Text("पता / मोहल्ला (वैकल्पिक)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "दूध का प्रकार",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MilkType.entries.forEach { type ->
                        FilterChip(
                            selected = milkType == type,
                            onClick = { milkType = type },
                            label = { Text(type.label) },
                            leadingIcon = if (milkType == type) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                Text(
                    text = "नियमित समय (शिफ्ट)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomerShift.entries.forEach { shift ->
                        FilterChip(
                            selected = defaultShift == shift,
                            onClick = { defaultShift = shift },
                            label = { Text(shift.label) }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it; validationError = null },
                        label = { Text("रोज़ मात्रा (किलो)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_qty_input")
                    )
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it; validationError = null },
                        label = { Text("दर (₹/किलो)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_rate_input")
                    )
                }

                OutlinedTextField(
                    value = openingBalanceText,
                    onValueChange = { openingBalanceText = it; validationError = null },
                    label = { Text("पुराना डेरी बाकी ₹ (जमा के लिए - लगाएं)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingCustomer != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "नियमित दूध चालू (सक्रिय)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = active,
                            onCheckedChange = { active = it }
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val qty = qtyText.toDoubleOrNull()
                    val rate = rateText.toDoubleOrNull()
                    val opening = openingBalanceText.toDoubleOrNull() ?: 0.0
                    when {
                        trimmedName.isEmpty() ->
                            validationError = "कृपया ग्राहक का नाम लिखें।"
                        qty == null || qty < 0.0 || qty > 1000.0 ->
                            validationError = "सही दूध की मात्रा (0 से 1000 किलो) दर्ज करें।"
                        rate == null || rate < 1.0 || rate > 1000.0 ->
                            validationError = "सही दर (₹1 से ₹1000 प्रति किलो) दर्ज करें।"
                        opening < -1000000.0 || opening > 1000000.0 ->
                            validationError = "पुराना डेरी बाकी मान्य सीमा में दर्ज करें।"
                        else -> onSave(
                            trimmedName,
                            phone.trim(),
                            address.trim(),
                            milkType,
                            defaultShift,
                            qty,
                            rate,
                            opening,
                            active
                        )
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MilkEntryFormDialog(
    customers: List<Customer>,
    preselectedCustomer: Customer?,
    existingEntry: MilkEntry?,
    defaultDateKey: String,
    defaultShift: DeliveryShift,
    onDismiss: () -> Unit,
    onSave: (
        customer: Customer,
        dateKey: String,
        shift: DeliveryShift,
        milkType: MilkType,
        quantityLiters: Double,
        ratePerLiter: Double,
        absent: Boolean,
        note: String
    ) -> Unit
) {
    val initialCustomer = existingEntry?.let { entry ->
        customers.firstOrNull { it.id == entry.customerId }
    } ?: preselectedCustomer ?: customers.firstOrNull()

    var selectedCustomer by remember(initialCustomer) { mutableStateOf(initialCustomer) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var dateKey by remember(existingEntry, defaultDateKey) {
        mutableStateOf(existingEntry?.dateKey ?: defaultDateKey)
    }
    var shift by remember(existingEntry, defaultShift) {
        mutableStateOf(existingEntry?.parsedShift ?: defaultShift)
    }
    var milkType by remember(existingEntry, initialCustomer) {
        mutableStateOf(
            existingEntry?.parsedMilkType
                ?: initialCustomer?.parsedMilkType
                ?: MilkType.BUFFALO
        )
    }
    var absent by remember(existingEntry) { mutableStateOf(existingEntry?.absent ?: false) }
    var qtyText by remember(existingEntry, initialCustomer) {
        mutableStateOf(
            DairyFormatters.formatLitersNumeric(
                existingEntry?.quantityLiters ?: initialCustomer?.defaultQtyLiters ?: 1.0
            )
        )
    }
    var rateText by remember(existingEntry, initialCustomer) {
        mutableStateOf(
            DairyFormatters.formatLitersNumeric(
                existingEntry?.ratePerLiter ?: initialCustomer?.ratePerLiter ?: 65.0
            )
        )
    }
    var note by remember(existingEntry) { mutableStateOf(existingEntry?.note ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val computedTotal = remember(qtyText, rateText, absent) {
        if (absent) 0.0 else {
            val q = qtyText.toDoubleOrNull() ?: 0.0
            val r = rateText.toDoubleOrNull() ?: 0.0
            DairyFormatters.roundTwoDecimals(q * r)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingEntry == null) "दूध एंट्री दर्ज करें" else "दूध एंट्री बदलें",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingEntry == null) {
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "ग्राहक चुनें",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ग्राहक") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = customerDropdownExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${cust.name} (${cust.parsedMilkType.label} • ${DairyFormatters.formatRupees(cust.ratePerLiter)})")
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        milkType = cust.parsedMilkType
                                        qtyText = DairyFormatters.formatLitersNumeric(cust.defaultQtyLiters)
                                        rateText = DairyFormatters.formatLitersNumeric(cust.ratePerLiter)
                                        customerDropdownExpanded = false
                                        validationError = null
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "ग्राहक: ${selectedCustomer?.name ?: existingEntry.customerName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "तारीख: ${DairyFormatters.formatDateDisplayHi(dateKey)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        TextButton(onClick = { dateKey = DairyFormatters.shiftDateKey(dateKey, -1) }) {
                            Text("-1 दिन")
                        }
                        TextButton(onClick = { dateKey = DairyFormatters.todayDateKey() }) {
                            Text("आज")
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeliveryShift.entries.forEach { s ->
                        FilterChip(
                            selected = shift == s,
                            onClick = { shift = s },
                            label = { Text(s.label) }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    MilkType.entries.forEach { t ->
                        FilterChip(
                            selected = milkType == t,
                            onClick = { milkType = t },
                            label = { Text(t.label) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "छुट्टी / आज दूध नहीं लिया (नागा)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = absent,
                        onCheckedChange = { absent = it }
                    )
                }

                if (!absent) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("0.5", "1", "1.5", "2", "3", "5").forEach { preset ->
                            FilterChip(
                                selected = qtyText == preset,
                                onClick = { qtyText = preset; validationError = null },
                                label = { Text("$preset किलो") }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it; validationError = null },
                            label = { Text("मात्रा (किलो)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("milk_entry_qty_input")
                        )
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it; validationError = null },
                            label = { Text("दर (₹/किलो)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("milk_entry_rate_input")
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (absent) "छुट्टी दर्ज होगी" else "कुल दूध राशि",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = DairyFormatters.formatRupees(computedTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("नोट (वैकल्पिक)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cust = selectedCustomer
                    val qty = if (absent) 0.0 else qtyText.toDoubleOrNull()
                    val rate = rateText.toDoubleOrNull()
                    when {
                        cust == null -> validationError = "कृपया ग्राहक चुनें।"
                        qty == null || (!absent && qty <= 0.0) || qty > 1000.0 ->
                            validationError = "कृपया सही मात्रा (0 से अधिक) दर्ज करें।"
                        rate == null || rate < 0.0 || rate > 1000.0 ->
                            validationError = "कृपया सही दर दर्ज करें।"
                        else -> onSave(
                            cust,
                            dateKey,
                            shift,
                            milkType,
                            qty,
                            rate,
                            absent,
                            note.trim()
                        )
                    }
                },
                modifier = Modifier.testTag("save_milk_entry_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentFormDialog(
    customers: List<Customer>,
    summaryByCustomerId: Map<String, CustomerAccountSummary>,
    preselectedCustomer: Customer?,
    existingPayment: PaymentRecord?,
    defaultDateKey: String,
    onDismiss: () -> Unit,
    onSave: (
        customer: Customer,
        dateKey: String,
        amount: Double,
        paymentMode: PaymentMode,
        note: String
    ) -> Unit
) {
    val initialCustomer = existingPayment?.let { pay ->
        customers.firstOrNull { it.id == pay.customerId }
    } ?: preselectedCustomer ?: customers.firstOrNull()

    var selectedCustomer by remember(initialCustomer) { mutableStateOf(initialCustomer) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var dateKey by remember(existingPayment, defaultDateKey) {
        mutableStateOf(existingPayment?.dateKey ?: defaultDateKey)
    }
    var amountText by remember(existingPayment) {
        mutableStateOf(
            existingPayment?.let { DairyFormatters.formatLitersNumeric(it.amount) } ?: ""
        )
    }
    var paymentMode by remember(existingPayment) {
        mutableStateOf(existingPayment?.parsedMode ?: PaymentMode.CASH)
    }
    var note by remember(existingPayment) { mutableStateOf(existingPayment?.note ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val currentDue = selectedCustomer?.let { summaryByCustomerId[it.id]?.balanceDue } ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingPayment == null) "पेमेंट प्राप्त करें" else "पेमेंट बदलें",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingPayment == null) {
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "ग्राहक चुनें",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ग्राहक") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = customerDropdownExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            customers.forEach { cust ->
                                val due = summaryByCustomerId[cust.id]?.balanceDue ?: 0.0
                                DropdownMenuItem(
                                    text = {
                                        Text("${cust.name} • डेरी बाकी ${DairyFormatters.formatRupees(DairyFormatters.roundToRupee(due))}")
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerDropdownExpanded = false
                                        validationError = null
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "ग्राहक: ${selectedCustomer?.name ?: existingPayment.customerName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (selectedCustomer != null && existingPayment == null) {
                    Surface(
                        color = if (currentDue > 0.0) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (currentDue >= 0) "कुल डेरी बाकी" else "जमा",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = DairyFormatters.formatRupees(DairyFormatters.roundToRupee(kotlin.math.abs(currentDue))),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentDue > 0.0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(
                                        onClick = {
                                            amountText = DairyFormatters.formatLitersNumeric(currentDue)
                                            validationError = null
                                        }
                                    ) {
                                        Text("पूरा भरें")
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "तारीख: ${DairyFormatters.formatDateDisplayHi(dateKey)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        TextButton(onClick = { dateKey = DairyFormatters.shiftDateKey(dateKey, -1) }) {
                            Text("-1 दिन")
                        }
                        TextButton(onClick = { dateKey = DairyFormatters.todayDateKey() }) {
                            Text("आज")
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; validationError = null },
                    label = { Text("प्राप्त राशि (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                Text(
                    text = "भुगतान का माध्यम",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode.label) },
                            leadingIcon = if (paymentMode == mode) {
                                { Icon(Icons.Rounded.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("नोट / रसीद विवरण (वैकल्पिक)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cust = selectedCustomer
                    val amt = amountText.toDoubleOrNull()
                    when {
                        cust == null -> validationError = "कृपया ग्राहक चुनें।"
                        amt == null || amt <= 0.0 || amt > 1000000.0 ->
                            validationError = "कृपया ₹1 से ₹10,00,000 के बीच सही राशि दर्ज करें।"
                        else -> onSave(cust, dateKey, amt, paymentMode, note.trim())
                    }
                },
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
