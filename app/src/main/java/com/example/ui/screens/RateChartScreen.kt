package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.RateChart
import com.example.data.RateChartParser
import com.example.viewmodel.DairyDataSnapshot
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FAT x SNF rate list (per litre).
 * - Kisi rate par tap karke apna rate badal sakte ho (edit).
 * - "नयी Rate List डालो" se PDF file ya paste kiye text se poori list ek saath badal sakte ho.
 */
@Composable
fun RateChartScreen(
    snapshot: DairyDataSnapshot,
    onBack: () -> Unit,
    onSaveRate: (fat: Double, snf: Double, rate: Double) -> Unit,
    onResetRate: (fat: Double, snf: Double) -> Unit,
    onImportChart: (Map<String, Double>) -> Unit,
    onClearImportedChart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hScroll = rememberScrollState()

    var editing by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var editText by remember { mutableStateOf("") }
    var showChooser by remember { mutableStateOf(false) }
    var showPaste by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var parsed by remember { mutableStateOf<RateChartParser.Result?>(null) }

    fun handleParsedText(text: String) {
        val result = RateChartParser.parse(text)
        if (result == null) {
            parseError = "इसमें rate list समझ नहीं आई। PDF सही वाली है या नहीं, देख लो — " +
                "list में FAT की lines और SNF के columns होने चाहिए।"
        } else {
            parsed = result
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isParsing = true
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw IllegalStateException("File खुल नहीं पाई")
                    PDDocument.load(bytes).use { doc ->
                        PDFTextStripper().apply { sortByPosition = true }.getText(doc)
                    }
                }
                handleParsedText(text)
            } catch (e: Exception) {
                parseError = "PDF पढ़ने में दिक्कत आई: ${e.localizedMessage ?: "अज्ञात त्रुटि"}"
            } finally {
                isParsing = false
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rate List (FAT × SNF)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "रेट ₹ प्रति लीटर • बदलने के लिए किसी रेट पर tap करो",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showChooser = true },
                enabled = !isParsing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isParsing) "List पढ़ी जा रही है…" else "नयी Rate List डालो (PDF / Text)")
            }
            if (snapshot.importedRates.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "तुम्हारी डाली हुई list लागू है (${snapshot.importedRates.size} रेट)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearImportedChart) {
                            Text("असली list पर वापस")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "chart_header") {
                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    ChartCell(text = "FAT\\SNF", bold = true, header = true, width = 62)
                    RateChart.SNF_VALUES.forEach { snf ->
                        ChartCell(text = "%.1f".format(snf), bold = true, header = true)
                    }
                }
            }
            items(items = RateChart.FAT_VALUES, key = { "fat_$it" }) { fat ->
                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    ChartCell(text = "%.1f".format(fat), bold = true, header = true, width = 62)
                    RateChart.SNF_VALUES.forEach { snf ->
                        val key = RateChart.key(fat, snf)
                        val override = snapshot.rateOverrides[key]
                        val rate = RateChart.effectiveRate(
                            fat, snf, snapshot.importedRates, snapshot.rateOverrides
                        ) ?: 0.0
                        ChartCell(
                            text = "%.2f".format(rate),
                            edited = override != null,
                            imported = override == null && snapshot.importedRates.containsKey(key),
                            onClick = {
                                editing = fat to snf
                                editText = "%.2f".format(rate)
                            }
                        )
                    }
                }
            }
            item(key = "chart_note") {
                Text(
                    text = "नोट: SNF 8.0 से कम वाले दूध पर ₹0.75 प्रति पॉइंट (0.1) कटौती " +
                        "collection में अपने आप लगती है। हाथ से बदला रेट हल्के पीले रंग में, " +
                        "डाली हुई list का रेट हल्के नीले रंग में दिखता है।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }

    // --- Edit single rate ---
    val cell = editing
    if (cell != null) {
        val (fat, snf) = cell
        val hasOverride = snapshot.rateOverrides.containsKey(RateChart.key(fat, snf))
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("रेट बदलो — FAT %.1f / SNF %.1f".format(fat, snf)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("नया रेट (₹ / लीटर)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newRate = editText.toDoubleOrNull()
                        if (newRate != null && newRate > 0.0) {
                            onSaveRate(fat, snf, newRate)
                        }
                        editing = null
                    }
                ) { Text("सेव करो") }
            },
            dismissButton = {
                Row {
                    if (hasOverride) {
                        TextButton(
                            onClick = {
                                onResetRate(fat, snf)
                                editing = null
                            }
                        ) { Text("असली रेट वापस") }
                    }
                    TextButton(onClick = { editing = null }) { Text("रद्द") }
                }
            }
        )
    }

    // --- Import chooser ---
    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text("नयी Rate List डालो") },
            text = {
                Text("Dairy से मिली नयी list की PDF file चुनो, या list का text paste करो। " +
                    "App chart अपने आप पढ़ लेगा और लागू करने से पहले तुम्हें दिखाएगा।")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showChooser = false
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    }
                ) { Text("PDF File चुनो") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showChooser = false
                            showPaste = true
                        }
                    ) { Text("Text Paste करो") }
                    TextButton(onClick = { showChooser = false }) { Text("रद्द") }
                }
            }
        )
    }

    // --- Paste text ---
    if (showPaste) {
        AlertDialog(
            onDismissRequest = { showPaste = false },
            title = { Text("Rate List का Text Paste करो") },
            text = {
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    label = { Text("पूरी list यहाँ paste करो") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 260.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPaste = false
                        handleParsedText(pasteText)
                    }
                ) { Text("List पढ़ो") }
            },
            dismissButton = {
                TextButton(onClick = { showPaste = false }) { Text("रद्द") }
            }
        )
    }

    // --- Parse error ---
    if (parseError != null) {
        AlertDialog(
            onDismissRequest = { parseError = null },
            title = { Text("List नहीं पढ़ पाया") },
            text = { Text(parseError ?: "") },
            confirmButton = {
                TextButton(onClick = { parseError = null }) { Text("ठीक है") }
            }
        )
    }

    // --- Parse preview ---
    val preview = parsed
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { parsed = null },
            title = { Text("नयी List मिली — लागू करूँ?") },
            text = {
                Text(
                    "FAT ${preview.fatValues.firstOrNull() ?: "-"} से " +
                        "${preview.fatValues.lastOrNull() ?: "-"} तक ${preview.fatValues.size} rows, " +
                        "SNF ${preview.snfValues.firstOrNull() ?: "-"} से " +
                        "${preview.snfValues.lastOrNull() ?: "-"} तक — " +
                        "कुल ${preview.cellCount} rate पढ़े गये।\n\n" +
                        "लागू करते ही collection में इन्हीं rates से हिसाब लगेगा। " +
                        "कोई rate गलत लगे तो बाद में tap करके ठीक कर सकते हो।"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImportChart(preview.rates)
                        parsed = null
                    }
                ) { Text("लागू करो") }
            },
            dismissButton = {
                TextButton(onClick = { parsed = null }) { Text("रद्द") }
            }
        )
    }
}

@Composable
private fun ChartCell(
    text: String,
    bold: Boolean = false,
    header: Boolean = false,
    edited: Boolean = false,
    imported: Boolean = false,
    width: Int = 58,
    onClick: (() -> Unit)? = null
) {
    val bg = when {
        header -> MaterialTheme.colorScheme.primaryContainer
        edited -> MaterialTheme.colorScheme.tertiaryContainer
        imported -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(38.dp)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
