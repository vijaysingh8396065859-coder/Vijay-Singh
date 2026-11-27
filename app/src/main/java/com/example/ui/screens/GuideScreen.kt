package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * App Kaise Chalaye: naye user ke liye poora flow, simple shabdon me.
 */
@Composable
fun GuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "guide_header") {
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
                        text = "❓ App Kaise Chalaye",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pehli baar chala rahe ho? Bas ye 4 step yaad rakho",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "guide_steps") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🚶 Roz ka kaam — 4 Step",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    GuideLine(
                        "Step 1 — Supplier jodo (sirf pehli baar)",
                        "Collection kholo → neeche apne sab suppliers (ghar) ek-ek karke " +
                            "jodo, jis order me doodh lene jaate ho usi order me।"
                    )
                    GuideLine(
                        "Step 2 — Ghar par doodh lo (Route)",
                        "Collection me 'Route शुरू करो' dabao। Har ghar par sirf Sample " +
                            "नंबर aur लीटर bharo, Save dabao — agla ghar khud khulega। " +
                            "Khal/chara diya ho to wahi se add kar do। Rate yahan NAHI lagta।"
                    )
                    GuideLine(
                        "Step 3 — Dairy par FAT/SNF bharo",
                        "Dairy aakar Collection ke 'FAT/SNF Bharo' hisse me har sample ke " +
                            "saamne machine ka FAT aur SNF bharo — rate aur rakam khud lag jayegi।"
                    )
                    GuideLine(
                        "Step 4 — 10 din me Hisaab bhejo",
                        "Har 10 din (1, 11, 21 tareekh ko) 'Supplier Hisaab' kholo — har " +
                            "supplier ka hisaab taiyaar milega। Sheet ki photo WhatsApp par " +
                            "bhej do। Bas, kaam khatm!"
                    )
                }
            }
        }

        item(key = "guide_features_title") {
            Text(
                text = "📋 Baaki cheezein kahan milengi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item(key = "guide_features") {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuideLine(
                        "📊 Dashboard — Aaj ka Report",
                        "Aaj kitna doodh aaya, kitne ki keemat, kitne suppliers, agla " +
                            "settlement kab hai — sab app kholte hi dikhta hai।"
                    )
                    GuideLine(
                        "👤 Supplier Profile",
                        "Kisi supplier ke naam par tap karo — uska poora record: mobile, " +
                            "गाँव, rozana doodh, purane hisaab, kul bakaya।"
                    )
                    GuideLine(
                        "📈 Rate Chart",
                        "FAT/SNF ki rate list। Nayi list aaye to PDF se app ke andar hi " +
                            "daal sakte ho — koi bhi rate tap karke badal sakte ho।"
                    )
                    GuideLine(
                        "⚠️ Galti Check",
                        "Duplicate sample, galat rate, dohri entry — hisaab bhejne se " +
                            "pehle yahan check kar lo, galti khud pakdi jayegi।"
                    )
                    GuideLine(
                        "💾 Data hamesha safe",
                        "Sab data tumhare Google account ke saath cloud me save hota hai। " +
                            "Phone badlo ya app dobara lagao — data wapas aa jayega। " +
                            "App update bhi app ke andar se hi ho jata hai।"
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideLine(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
