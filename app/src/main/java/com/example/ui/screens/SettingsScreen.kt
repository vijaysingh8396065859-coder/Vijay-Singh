package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notifications.CollectionReminders
import com.example.ui.AppLang
import com.example.ui.AppLanguage
import com.example.update.AppUpdate
import com.example.viewmodel.DairyDataSnapshot
import kotlinx.coroutines.launch

/**
 * Settings: bhasha, dairy ka naam, reminder time, backup, update, about.
 */
@Composable
fun SettingsScreen(
    snapshot: DairyDataSnapshot,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dairyNameText by remember { mutableStateOf(AppLang.dairyName) }
    var nameSaved by remember { mutableStateOf(false) }

    val (mHour, mMinute) = remember { CollectionReminders.morningTime(context) }
    val (eHour, eMinute) = remember { CollectionReminders.eveningTime(context) }
    var morningText by remember {
        mutableStateOf("%d:%02d".format(mHour, mMinute))
    }
    var eveningText by remember {
        mutableStateOf("%d:%02d".format(eHour, eMinute))
    }
    var timeSaved by remember { mutableStateOf(false) }

    var updateStatus by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "set_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = AppLang.s("वापस", "Back")
                    )
                }
                Text(
                    text = "⚙️ " + AppLang.s("सेटिंग", "Settings"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item(key = "set_language") {
            SettingsCard {
                Text(
                    text = "🌐 " + AppLang.s("भाषा", "Language"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = AppLang.current == AppLanguage.HINDI,
                        onClick = { AppLang.setLanguage(context, AppLanguage.HINDI) },
                        label = { Text("हिंदी") }
                    )
                    FilterChip(
                        selected = AppLang.current == AppLanguage.ENGLISH,
                        onClick = { AppLang.setLanguage(context, AppLanguage.ENGLISH) },
                        label = { Text("English") }
                    )
                }
            }
        }

        item(key = "set_dairy_name") {
            SettingsCard {
                Text(
                    text = "🏷 " + AppLang.s("Dairy का नाम", "Dairy Name"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = dairyNameText,
                    onValueChange = {
                        dairyNameText = it
                        nameSaved = false
                    },
                    label = {
                        Text(AppLang.s("Sheet par chhapta hai", "Printed on the hisaab sheet"))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        AppLang.setDairyName(context, dairyNameText.trim())
                        nameSaved = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(AppLang.s("Save करो", "Save"))
                }
                if (nameSaved) {
                    Text(
                        text = "✅ " + AppLang.s("Save हो गया", "Saved"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item(key = "set_reminders") {
            SettingsCard {
                Text(
                    text = "⏰ " + AppLang.s("Collection Reminder", "Collection Reminders"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = morningText,
                    onValueChange = {
                        morningText = it
                        timeSaved = false
                    },
                    label = { Text(AppLang.s("Subah ka time (5:30)", "Morning time (5:30)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eveningText,
                    onValueChange = {
                        eveningText = it
                        timeSaved = false
                    },
                    label = { Text(AppLang.s("Shaam ka time (17:30)", "Evening time (17:30)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val m = parseTime(morningText)
                        val e = parseTime(eveningText)
                        if (m != null && e != null) {
                            CollectionReminders.saveTimes(
                                context, m.first, m.second, e.first, e.second
                            )
                            timeSaved = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(AppLang.s("Save करो", "Save"))
                }
                if (timeSaved) {
                    Text(
                        text = "✅ " + AppLang.s("Save हो गया", "Saved"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item(key = "set_backup") {
            SettingsCard {
                Text(
                    text = "💾 " + AppLang.s("Data Backup", "Data Backup"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = {
                        val csv = buildBackupCsv(snapshot)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Milk Diary Backup")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                intent,
                                AppLang.s("Backup भेजो", "Share backup")
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(AppLang.s("पूरा हिसाब Backup भेजो (CSV)", "Share full backup (CSV)"))
                }
            }
        }

        item(key = "set_update") {
            SettingsCard {
                Text(
                    text = "🔄 " + AppLang.s("App Update", "App Update"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = AppLang.s("Abhi ka version: ", "Current version: ") +
                        AppUpdate.currentVersionName(context),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        checking = true
                        updateStatus = ""
                        updateUrl = ""
                        scope.launch {
                            val info = AppUpdate.fetchLatest()
                            checking = false
                            if (info == null) {
                                updateStatus = AppLang.s(
                                    "Check nahi ho paya — net dekh lo",
                                    "Could not check — check connection"
                                )
                            } else if (AppUpdate.isNewer(
                                    info.versionName,
                                    AppUpdate.currentVersionName(context)
                                )
                            ) {
                                updateStatus = AppLang.s(
                                    "Naya version hai: ",
                                    "New version available: "
                                ) + info.versionName
                                updateUrl = info.downloadUrl
                            } else {
                                updateStatus = AppLang.s(
                                    "Aap sabse naye version par ho ✅",
                                    "You are on the latest version ✅"
                                )
                            }
                        }
                    },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(AppLang.s("Update देखो", "Check for update"))
                }
                if (updateStatus.isNotBlank()) {
                    Text(
                        text = updateStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (updateUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(AppLang.s("Download करो", "Download"))
                    }
                }
            }
        }

        item(key = "set_about") {
            SettingsCard {
                Text(
                    text = "🥛 Milk Diary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = AppLang.s(
                        "आपका विश्वास ही हमारी पहचान है",
                        "Your trust is our identity"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Version " + AppUpdate.currentVersionName(context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

private fun parseTime(text: String): Pair<Int, Int>? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].trim().toIntOrNull() ?: return null
    val m = parts[1].trim().toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h to m
}
