package com.example.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.RateChart
import com.example.viewmodel.DairyDataSnapshot

/**
 * FAT x SNF rate list (per litre). Kisi bhi rate par tap karke apna rate badal sakte ho —
 * badla hua rate alag rang me dikhta hai aur collection me wahi lagta hai.
 */
@Composable
fun RateChartScreen(
    snapshot: DairyDataSnapshot,
    onBack: () -> Unit,
    onSaveRate: (fat: Double, snf: Double, rate: Double) -> Unit,
    onResetRate: (fat: Double, snf: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()
    var editing by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var editText by remember { mutableStateOf("") }

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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "chart_header") {
                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    ChartCell(
                        text = "FAT\\SNF",
                        bold = true,
                        header = true,
                        width = 62
                    )
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
                        val rate = override ?: RateChart.defaultRate(fat, snf) ?: 0.0
                        ChartCell(
                            text = "%.2f".format(rate),
                            edited = override != null,
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
                        "collection में अपने आप लगती है। बदला हुआ रेट हल्के पीले रंग में दिखता है।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }

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
}

@Composable
private fun ChartCell(
    text: String,
    bold: Boolean = false,
    header: Boolean = false,
    edited: Boolean = false,
    width: Int = 58,
    onClick: (() -> Unit)? = null
) {
    val bg = when {
        header -> MaterialTheme.colorScheme.primaryContainer
        edited -> MaterialTheme.colorScheme.tertiaryContainer
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
