package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DairyFormatters
import com.example.ui.sheets.HisaabSheetImage
import com.example.ui.sheets.SheetRow
import com.example.viewmodel.DairyDataSnapshot
import java.net.URLEncoder

private fun plainAmount(value: Double): String {
    val rounded = DairyFormatters.roundTwoDecimals(value)
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

private fun ddmmyyyy(dateKey: String): String {
    val parts = dateKey.split("-")
    return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else dateKey
}

/**
 * Supplier Hisaab: 10-din cycle (1-10, 11-20, 21-mahine ke aakhir) ka
 * automatic hisaab — doodh (+) minus samaan/advance/payment, purana baaki
 * apne aap aage badhta hai. Sheet WhatsApp par Hindi/Hinglish me jaati hai.
 */
@Composable
fun SupplierHisaabScreen(
    snapshot: DairyDataSnapshot,
    onBack: () -> Unit,
    onAddTransaction: (
        supplierName: String,
        type: String,
        itemName: String,
        quantity: Double,
        unit: String,
        ratePerUnit: Double,
        amount: Double,
        paymentMode: String,
        note: String,
        dateKey: String,
        isHome: Boolean
    ) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onOpenProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayKey = remember { DairyFormatters.todayDateKey() }

    val supplierNames = remember(snapshot.suppliers, snapshot.collections, snapshot.supplierTransactions) {
        val names = LinkedHashSet<String>()
        snapshot.suppliers.forEach { names.add(it.name) }
        snapshot.collections.forEach { names.add(it.supplierName) }
        snapshot.supplierTransactions.filter { !it.isHome }.forEach { names.add(it.supplierName) }
        names.toList()
    }
    var selectedName by remember { mutableStateOf<String?>(null) }
    val supplier = selectedName?.takeIf { it in supplierNames } ?: supplierNames.firstOrNull()

    var cycleAnchor by remember { mutableStateOf(todayKey) }
    val cycleStart = remember(cycleAnchor) { DairyFormatters.cycleStartKey(cycleAnchor) }
    val cycleEnd = remember(cycleAnchor) { DairyFormatters.cycleEndKey(cycleAnchor) }
    val isCurrentCycle = todayKey in cycleStart..cycleEnd

    // --- Hisaab ki ganit (sirf chune hue supplier ki, ghar wali entries alag) ---
    val calc = remember(snapshot.collections, snapshot.supplierTransactions, supplier, cycleStart, cycleEnd) {
        if (supplier == null) {
            null
        } else {
            val milk = snapshot.collections.filter { it.supplierName == supplier }
            val txns = snapshot.supplierTransactions.filter { it.supplierName == supplier && !it.isHome }
            val milkBefore = milk.filter { it.dateKey < cycleStart }.sumOf { it.totalAmount }
            val milkCycleList = milk.filter { it.dateKey in cycleStart..cycleEnd }
            val milkCycle = milkCycleList.sumOf { it.totalAmount }
            val milkCycleLiters = milkCycleList.sumOf { it.quantityLiters }
            fun sumBefore(type: String) =
                txns.filter { it.type == type && it.dateKey < cycleStart }.sumOf { it.amount }
            fun sumCycle(type: String) =
                txns.filter { it.type == type && it.dateKey in cycleStart..cycleEnd }.sumOf { it.amount }
            val balanceBefore = DairyFormatters.roundToRupee(
                milkBefore - (sumBefore("GOODS") + sumBefore("ADVANCE") + sumBefore("PAYMENT"))
            )
            val milkCycleRounded = DairyFormatters.roundToRupee(milkCycle)
            val goodsCycle = DairyFormatters.roundToRupee(sumCycle("GOODS"))
            val advanceCycle = DairyFormatters.roundToRupee(sumCycle("ADVANCE"))
            val paymentCycle = DairyFormatters.roundToRupee(sumCycle("PAYMENT"))
            val cycleDue = balanceBefore + milkCycleRounded - goodsCycle - advanceCycle - paymentCycle
            val overall = milk.sumOf { it.totalAmount } - txns.sumOf { it.amount }
            val cycleTxns = txns.filter { it.dateKey in cycleStart..cycleEnd }
            HisaabCalc(
                milkCycleList = milkCycleList,
                milkCycle = milkCycleRounded,
                milkCycleLiters = milkCycleLiters,
                balanceBefore = balanceBefore,
                goodsCycle = goodsCycle,
                advanceCycle = advanceCycle,
                paymentCycle = paymentCycle,
                cycleDue = DairyFormatters.roundTwoDecimals(cycleDue),
                overall = DairyFormatters.roundToRupee(overall),
                cycleTxns = cycleTxns
            )
        }
    }

    val homeTxns = remember(snapshot.supplierTransactions) {
        snapshot.supplierTransactions.filter { it.isHome }
    }
    val homeTotal = homeTxns.sumOf { it.amount }

    // --- Entry form state ---
    var entryType by remember { mutableStateOf("GOODS") }
    var targetHome by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("BORI") }
    var rateText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var payMode by remember { mutableStateOf("CASH") }
    var noteText by remember { mutableStateOf("") }
    var sheetLang by remember { mutableStateOf("HINDI") }

    val qty = qtyText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val goodsTotal = DairyFormatters.roundTwoDecimals(qty * rate)
    val manualAmount = amountText.toDoubleOrNull() ?: 0.0

    val lastRateForItem = remember(snapshot.supplierTransactions, itemName) {
        snapshot.supplierTransactions
            .filter { it.type == "GOODS" && it.itemName.equals(itemName.trim(), ignoreCase = true) }
            .maxByOrNull { it.dateKey }
            ?.ratePerUnit
    }

    LaunchedEffect(entryType, calc?.cycleDue) {
        if (entryType == "PAYMENT" && (calc?.cycleDue ?: 0.0) > 0.0) {
            amountText = plainAmount(calc!!.cycleDue)
        }
    }

    val canSave = when (entryType) {
        "GOODS" -> (targetHome || supplier != null) && itemName.isNotBlank() && qty > 0.0 && rate > 0.0
        else -> supplier != null && manualAmount > 0.0
    }

    fun buildSheet(): String {
        val c = calc ?: return ""
        val name = supplier ?: ""
        return if (sheetLang == "HINDI") {
            buildString {
                appendLine("नमस्ते $name जी 🙏")
                appendLine("आपका हिसाब (${DairyFormatters.formatShortDateHi(cycleStart)} से ${DairyFormatters.formatShortDateHi(cycleEnd)}):")
                appendLine("दूध: ${DairyFormatters.formatLiters(c.milkCycleLiters)} — ${DairyFormatters.formatRupees(c.milkCycle)}")
                if (c.balanceBefore != 0.0) {
                    appendLine("पुराना बाकी: ${DairyFormatters.formatRupees(c.balanceBefore)}")
                }
                if (c.goodsCycle > 0.0) appendLine("डेरी बाकी (समान): −${DairyFormatters.formatRupees(c.goodsCycle)}")
                if (c.paymentCycle > 0.0) appendLine("डेरी बाकी (Payment): −${DairyFormatters.formatRupees(c.paymentCycle)}")
                if (c.advanceCycle > 0.0) appendLine("Advance Jama: −${DairyFormatters.formatRupees(c.advanceCycle)}")
                appendLine("———————————")
                if (c.cycleDue >= 0.0) {
                    appendLine("कुल देना है: ${DairyFormatters.formatRupees(c.cycleDue)}")
                } else {
                    appendLine("डेरी बाकी: ${DairyFormatters.formatRupees(-c.cycleDue)} (अगले हिसाब में कटेगा)")
                }
                appendLine("— Milk Diary")
                append("आपका विश्वास ही हमारी पहचान है")
            }
        } else {
            buildString {
                appendLine("Namaste $name ji 🙏")
                appendLine("Aapka hisaab (${ddmmyyyy(cycleStart)} se ${ddmmyyyy(cycleEnd)}):")
                appendLine("Doodh: ${DairyFormatters.formatLiters(c.milkCycleLiters)} — ${DairyFormatters.formatRupees(c.milkCycle)}")
                if (c.balanceBefore != 0.0) {
                    appendLine("Purana baaki: ${DairyFormatters.formatRupees(c.balanceBefore)}")
                }
                if (c.goodsCycle > 0.0) appendLine("Dairy Baaki (samaan): −${DairyFormatters.formatRupees(c.goodsCycle)}")
                if (c.paymentCycle > 0.0) appendLine("Dairy Baaki (Payment): −${DairyFormatters.formatRupees(c.paymentCycle)}")
                if (c.advanceCycle > 0.0) appendLine("Advance Jama: −${DairyFormatters.formatRupees(c.advanceCycle)}")
                appendLine("———————————")
                if (c.cycleDue >= 0.0) {
                    appendLine("Kul dena hai: ${DairyFormatters.formatRupees(c.cycleDue)}")
                } else {
                    appendLine("Dairy Baaki: ${DairyFormatters.formatRupees(-c.cycleDue)} (agle hisaab me katega)")
                }
                appendLine("— Milk Diary")
                append("Aapka vishwas hi hamari pehchan hai")
            }
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
                    text = "Supplier Hisaab",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "10 दिन का automatic हिसाब — कुछ miss नहीं होगा",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (supplierNames.isEmpty() && homeTxns.isEmpty()) {
            Text(
                text = "अभी कोई supplier नहीं है। पहले Collection screen में supplier जोड़ो या दूध collection करो — फिर यहाँ उसका पूरा हिसाब अपने आप बनेगा।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "cycle_totals_all") {
                val cycMilk = DairyFormatters.roundToRupee(
                    snapshot.collections.filter { it.dateKey in cycleStart..cycleEnd }
                        .sumOf { it.totalAmount }
                )
                val cycGoods = DairyFormatters.roundToRupee(
                    snapshot.supplierTransactions.filter {
                        !it.isHome && it.type == "GOODS" && it.dateKey in cycleStart..cycleEnd
                    }.sumOf { it.amount }
                )
                val cycAdv = DairyFormatters.roundToRupee(
                    snapshot.supplierTransactions.filter {
                        !it.isHome && it.type == "ADVANCE" && it.dateKey in cycleStart..cycleEnd
                    }.sumOf { it.amount }
                )
                val cycPay = DairyFormatters.roundToRupee(
                    snapshot.supplierTransactions.filter {
                        !it.isHome && it.type == "PAYMENT" && it.dateKey in cycleStart..cycleEnd
                    }.sumOf { it.amount }
                )
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
                            text = "🧮 Sab Suppliers — Cycle ka Kul Jod",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${DairyFormatters.formatShortDateHi(cycleStart)} — ${DairyFormatters.formatShortDateHi(cycleEnd)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🥛 Kul doodh ki keemat: ${DairyFormatters.formatRupees(cycMilk)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "📦 Samaan: ${DairyFormatters.formatRupees(cycGoods)} • 💵 Advance: ${DairyFormatters.formatRupees(cycAdv)} • 💰 Payment: ${DairyFormatters.formatRupees(cycPay)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Katauti ke baad cycle ka shudh: ${DairyFormatters.formatRupees(cycMilk - cycGoods - cycAdv - cycPay)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (supplierNames.isNotEmpty()) {
                item(key = "supplier_picker") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Supplier चुनो",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(items = supplierNames, key = { "name_$it" }) { name ->
                                FilterChip(
                                    selected = supplier == name,
                                    onClick = { selectedName = name },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                }
            }

            if (supplier != null) {
                item(key = "profile_link") {
                    OutlinedButton(
                        onClick = { onOpenProfile(supplier) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("👤 $supplier की Profile देखो (पूरा record)")
                    }
                }
            }

            if (supplier != null && calc != null) {
                item(key = "cycle_picker") {
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
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { cycleAnchor = DairyFormatters.shiftCycleKey(cycleAnchor, -1) }
                            ) { Text("◀") }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${DairyFormatters.formatShortDateHi(cycleStart)} — ${DairyFormatters.formatShortDateHi(cycleEnd)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isCurrentCycle) {
                                    Text(
                                        text = "चल रहा cycle",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { cycleAnchor = DairyFormatters.shiftCycleKey(cycleAnchor, 1) }
                            ) { Text("▶") }
                        }
                    }
                }

                item(key = "hisaab_summary") {
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
                                text = "$supplier — इस cycle का हिसाब",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            HisaabRow(
                                label = "पुराना बाकी",
                                value = calc.balanceBefore
                            )
                            HisaabRow(
                                label = "दूध (${calc.milkCycleList.size} बार, ${DairyFormatters.formatLiters(calc.milkCycleLiters)})",
                                value = calc.milkCycle
                            )
                            if (calc.goodsCycle > 0.0) {
                                HisaabRow(label = "डेरी बाकी — समान (खल/चारा)", value = -calc.goodsCycle)
                            }
                            if (calc.paymentCycle > 0.0) {
                                HisaabRow(label = "डेरी बाकी — Payment", value = -calc.paymentCycle)
                            }
                            if (calc.advanceCycle > 0.0) {
                                HisaabRow(label = "Advance Jama", value = -calc.advanceCycle)
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (calc.cycleDue >= 0.0) "कुल देना है" else "डेरी बाकी",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = DairyFormatters.formatRupees(kotlin.math.abs(calc.cycleDue)),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (calc.overall >= 0.0) {
                                    "आज तक कुल देना है: ${DairyFormatters.formatRupees(calc.overall)}"
                                } else {
                                    "आज तक डेरी बाकी: ${DairyFormatters.formatRupees(-calc.overall)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item(key = "whatsapp_sheet") {
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
                                text = "Sheet WhatsApp पर भेजो",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = sheetLang == "HINDI",
                                    onClick = { sheetLang = "HINDI" },
                                    label = { Text("हिंदी") }
                                )
                                FilterChip(
                                    selected = sheetLang == "HINGLISH",
                                    onClick = { sheetLang = "HINGLISH" },
                                    label = { Text("Hinglish") }
                                )
                            }
                            Button(
                                onClick = {
                                    val phone = snapshot.suppliers
                                        .firstOrNull { it.name == supplier }?.phone
                                        ?.filter { ch -> ch.isDigit() }
                                        .orEmpty()
                                    val text = URLEncoder.encode(buildSheet(), "UTF-8")
                                    val url = if (phone.length >= 10) {
                                        "https://wa.me/91${phone.takeLast(10)}?text=$text"
                                    } else {
                                        "https://wa.me/?text=$text"
                                    }
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📤 WhatsApp पर Sheet भेजो")
                            }
                            OutlinedButton(
                                onClick = {
                                    val c = calc
                                    val rows = buildList {
                                        if (c.balanceBefore != 0.0) {
                                            add(SheetRow("पुराना बाकी", DairyFormatters.formatRupees(c.balanceBefore)))
                                        }
                                        add(
                                            SheetRow(
                                                "दूध (${DairyFormatters.formatLiters(c.milkCycleLiters)})",
                                                DairyFormatters.formatRupees(c.milkCycle)
                                            )
                                        )
                                        if (c.goodsCycle > 0.0) {
                                            add(SheetRow("डेरी बाकी (समान)", "−${DairyFormatters.formatRupees(c.goodsCycle)}", true))
                                        }
                                        if (c.paymentCycle > 0.0) {
                                            add(SheetRow("डेरी बाकी (Payment)", "−${DairyFormatters.formatRupees(c.paymentCycle)}", true))
                                        }
                                        if (c.advanceCycle > 0.0) {
                                            add(SheetRow("Advance Jama", "−${DairyFormatters.formatRupees(c.advanceCycle)}", true))
                                        }
                                    }
                                    val bmp = HisaabSheetImage.createSheetBitmap(
                                        supplierName = supplier,
                                        cycleText = "${DairyFormatters.formatShortDateHi(cycleStart)} से ${DairyFormatters.formatShortDateHi(cycleEnd)} तक",
                                        rows = rows,
                                        totalLabel = if (c.cycleDue >= 0.0) "कुल देना है" else "डेरी बाकी",
                                        totalText = DairyFormatters.formatRupees(kotlin.math.abs(c.cycleDue)),
                                        noteText = if (sheetLang == "HINDI") {
                                            "नमस्ते जी 🙏 ऊपर आपका 10 दिन का पूरा हिसाब है। कोई गलती लगे या कोई बात हो तो बेझिझक बताइएगा। धन्यवाद!"
                                        } else {
                                            "Namaste ji 🙏 Upar aapka 10 din ka pura hisaab hai. Koi galti lage ya koi baat ho to bejhijhak bataiyega. Dhanyavaad!"
                                        }
                                    )
                                    val phone = snapshot.suppliers
                                        .firstOrNull { it.name == supplier }?.phone
                                        ?.filter { ch -> ch.isDigit() }
                                        .orEmpty()
                                    val caption = if (sheetLang == "HINDI") {
                                        "नमस्ते $supplier जी 🙏 आपका 10 दिन का हिसाब भेजा है। — Milk Diary"
                                    } else {
                                        "Namaste $supplier ji 🙏 Aapka 10 din ka hisaab bheja hai. — Milk Diary"
                                    }
                                    HisaabSheetImage.shareSheet(context, bmp, supplier, phone, caption)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📄 Sheet ki Photo bhejo (proper sheet)")
                            }
                            Text(
                                text = "Sheet तैयार होकर WhatsApp खुल जाएगा — बस Send दबाना है।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "entry_form") {
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
                        Text(
                            text = "नयी Entry (तारीख: आज)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = entryType == "GOODS",
                                onClick = { entryType = "GOODS" },
                                label = { Text("📦 समान दो") }
                            )
                            FilterChip(
                                selected = entryType == "ADVANCE",
                                onClick = { entryType = "ADVANCE" },
                                label = { Text("💵 Advance Jama") }
                            )
                            FilterChip(
                                selected = entryType == "PAYMENT",
                                onClick = { entryType = "PAYMENT" },
                                label = { Text("✔ Payment") }
                            )
                        }
                        if (entryType == "GOODS") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !targetHome,
                                    onClick = { targetHome = false },
                                    label = { Text("Supplier को") }
                                )
                                FilterChip(
                                    selected = targetHome,
                                    onClick = { targetHome = true },
                                    label = { Text("🏠 अपने घर") }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("सरसों खल", "खल", "चारा").forEach { preset ->
                                    FilterChip(
                                        selected = itemName == preset,
                                        onClick = { itemName = preset },
                                        label = { Text(preset) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("समान का नाम") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = qtyText,
                                    onValueChange = { qtyText = it },
                                    label = { Text("मात्रा") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rateText,
                                    onValueChange = { rateText = it },
                                    label = { Text("Rate (₹)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = unit == "BORI",
                                    onClick = { unit = "BORI" },
                                    label = { Text("बोरी") }
                                )
                                FilterChip(
                                    selected = unit == "KG",
                                    onClick = { unit = "KG" },
                                    label = { Text("किलो") }
                                )
                                if (lastRateForItem != null && itemName.isNotBlank()) {
                                    Text(
                                        text = "पिछला rate: ${DairyFormatters.formatRupees(lastRateForItem)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            if (goodsTotal > 0.0) {
                                Text(
                                    text = "कुल: ${DairyFormatters.formatRupees(goodsTotal)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = { Text("रकम (₹)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (entryType == "PAYMENT") {
                                Text(
                                    text = "ऊपर पूरी बाकी रकम अपने आप भरी है — cash के हिसाब से कम-ज़्यादा कर सकते हो, फ़र्क अगले हिसाब में अपने आप जाएगा।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("CASH" to "Cash", "UPI" to "UPI", "BANK" to "Bank").forEach { (value, label) ->
                                    FilterChip(
                                        selected = payMode == value,
                                        onClick = { payMode = value },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("नोट (चाहो तो)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (entryType == "GOODS") {
                                    onAddTransaction(
                                        if (targetHome) "अपना घर" else (supplier ?: ""),
                                        "GOODS",
                                        itemName.trim(),
                                        qty,
                                        unit,
                                        rate,
                                        goodsTotal,
                                        "CASH",
                                        noteText.trim(),
                                        todayKey,
                                        targetHome
                                    )
                                    itemName = ""
                                    qtyText = ""
                                    rateText = ""
                                } else {
                                    onAddTransaction(
                                        supplier ?: "",
                                        entryType,
                                        "",
                                        0.0,
                                        "BORI",
                                        0.0,
                                        manualAmount,
                                        payMode,
                                        noteText.trim(),
                                        todayKey,
                                        false
                                    )
                                    amountText = ""
                                }
                                noteText = ""
                            },
                            enabled = canSave,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("दर्ज करो")
                        }
                    }
                }
            }

            if (calc != null && calc.cycleTxns.isNotEmpty()) {
                item(key = "cycle_txns_title") {
                    Text(
                        text = "इस cycle की entries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(items = calc.cycleTxns, key = { "txn_${it.id}" }) { txn ->
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
                                    text = when (txn.type) {
                                        "GOODS" -> "📦 ${txn.itemName} — ${plainAmount(txn.quantity)} ${if (txn.unit == "KG") "किलो" else "बोरी"} × ${DairyFormatters.formatRupees(txn.ratePerUnit)}"
                                        "ADVANCE" -> "💵 Advance Jama (${txn.paymentMode})"
                                        else -> "✔ डेरी बाकी — Payment (${txn.paymentMode})"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = DairyFormatters.formatShortDateHi(txn.dateKey) +
                                        if (txn.note.isNotBlank()) " • ${txn.note}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "−${DairyFormatters.formatRupees(txn.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onDeleteTransaction(txn.id) }) {
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

            if (homeTxns.isNotEmpty()) {
                item(key = "home_title") {
                    Text(
                        text = "🏠 घर का समान — कुल ${DairyFormatters.formatRupees(homeTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(items = homeTxns.take(10), key = { "home_${it.id}" }) { txn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${txn.itemName} — ${plainAmount(txn.quantity)} ${if (txn.unit == "KG") "किलो" else "बोरी"}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = DairyFormatters.formatShortDateHi(txn.dateKey),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = DairyFormatters.formatRupees(txn.amount),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { onDeleteTransaction(txn.id) }) {
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
    }
}

private data class HisaabCalc(
    val milkCycleList: List<com.example.data.MilkCollection>,
    val milkCycle: Double,
    val milkCycleLiters: Double,
    val balanceBefore: Double,
    val goodsCycle: Double,
    val advanceCycle: Double,
    val paymentCycle: Double,
    val cycleDue: Double,
    val overall: Double,
    val cycleTxns: List<com.example.data.SupplierTransaction>
)

@Composable
private fun HisaabRow(label: String, value: Double) {
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
            text = (if (value < 0) "−" else "") + DairyFormatters.formatRupees(kotlin.math.abs(value)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
