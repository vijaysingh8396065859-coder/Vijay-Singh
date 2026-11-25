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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.MilkType
import com.example.data.MonthlyCustomerStatement
import com.example.ui.theme.BrassAmber
import com.example.ui.theme.MeadowEmerald
import com.example.ui.theme.MeadowEmeraldLight
import com.example.ui.theme.UpiIndigo
import com.example.viewmodel.DairyDataSnapshot

@Composable
fun ReportsScreen(
    snapshot: DairyDataSnapshot,
    selectedMonthKey: String,
    userDisplayName: String?,
    userEmail: String?,
    onChangeMonth: (Int) -> Unit,
    onOpenCustomerDetail: (String) -> Unit,
    onShareBill: (MonthlyCustomerStatement) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthEntries = remember(snapshot.milkEntries, selectedMonthKey) {
        snapshot.milkEntries.filter { it.dateKey.startsWith(selectedMonthKey) && !it.absent }
    }
    val monthPayments = remember(snapshot.payments, selectedMonthKey) {
        snapshot.payments.filter { it.dateKey.startsWith(selectedMonthKey) }
    }
    val statements = remember(snapshot, selectedMonthKey) {
        snapshot.monthlyStatements(selectedMonthKey)
    }

    val totalMonthLiters = remember(monthEntries) { monthEntries.sumOf { it.quantityLiters } }
    val totalMonthBill = remember(monthEntries) { monthEntries.sumOf { it.totalAmount } }
    val totalMonthPaid = remember(monthPayments) { monthPayments.sumOf { it.amount } }
    val buffaloLiters = remember(monthEntries) {
        monthEntries.filter { it.parsedMilkType == MilkType.BUFFALO }.sumOf { it.quantityLiters }
    }
    val cowLiters = remember(monthEntries) {
        monthEntries.filter { it.parsedMilkType == MilkType.COW }.sumOf { it.quantityLiters }
    }
    val mixedLiters = remember(monthEntries) {
        monthEntries.filter { it.parsedMilkType == MilkType.MIXED }.sumOf { it.quantityLiters }
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
            item(key = "month_picker") {
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
                            text = "${DairyFormatters.formatMonthDisplayHi(selectedMonthKey)} का हिसाब",
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

            item(key = "monthly_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(MeadowEmerald, MeadowEmeraldLight)))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "मासिक दूध और वसूली रिपोर्ट",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ReportHeroCol("कुल दूध", DairyFormatters.formatLiters(totalMonthLiters), Modifier.weight(1f))
                            ReportHeroCol("मासिक बिल", DairyFormatters.formatRupees(totalMonthBill), Modifier.weight(1f))
                            ReportHeroCol("कुल वसूली", DairyFormatters.formatRupees(totalMonthPaid), Modifier.weight(1f))
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "सभी ग्राहकों का कुल शेष बकाया",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = DairyFormatters.formatRupees(snapshot.totalPendingDues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item(key = "milk_type_breakdown") {
                MilkTypeBreakdownCard(
                    buffaloLiters = buffaloLiters,
                    cowLiters = cowLiters,
                    mixedLiters = mixedLiters
                )
            }

            item(key = "statements_title") {
                Text(
                    text = "ग्राहकवार मासिक बिल (${statements.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (statements.isEmpty()) {
                item(key = "no_statements") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "मासिक बिल देखने के लिए पहले ग्राहक और दूध एंट्री जोड़ें।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(
                    items = statements,
                    key = { "stmt_${it.customer.id}" }
                ) { statement ->
                    MonthlyStatementCard(
                        statement = statement,
                        onOpenDetail = { onOpenCustomerDetail(statement.customer.id) },
                        onShareBill = { onShareBill(statement) }
                    )
                }
            }

            item(key = "account_card") {
                AccountSyncCard(
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    onSignOutClick = onSignOutClick
                )
            }
        }
    }
}

@Composable
private fun ReportHeroCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
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
private fun MilkTypeBreakdownCard(
    buffaloLiters: Double,
    cowLiters: Double,
    mixedLiters: Double
) {
    val total = (buffaloLiters + cowLiters + mixedLiters).coerceAtLeast(0.01)
    val buffaloFrac = (buffaloLiters / total).toFloat().coerceIn(0f, 1f)
    val cowFrac = (cowLiters / total).toFloat().coerceIn(0f, 1f)
    val mixedFrac = (mixedLiters / total).toFloat().coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "दूध के प्रकार के अनुसार वितरण",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (buffaloLiters > 0.0) {
                    Box(
                        modifier = Modifier
                            .weight(buffaloFrac.coerceAtLeast(0.02f))
                            .height(12.dp)
                            .background(MeadowEmerald)
                    )
                }
                if (cowLiters > 0.0) {
                    Box(
                        modifier = Modifier
                            .weight(cowFrac.coerceAtLeast(0.02f))
                            .height(12.dp)
                            .background(BrassAmber)
                    )
                }
                if (mixedLiters > 0.0) {
                    Box(
                        modifier = Modifier
                            .weight(mixedFrac.coerceAtLeast(0.02f))
                            .height(12.dp)
                            .background(UpiIndigo)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                MilkLegendItem("भैंस", DairyFormatters.formatLiters(buffaloLiters), MeadowEmerald)
                MilkLegendItem("गाय", DairyFormatters.formatLiters(cowLiters), BrassAmber)
                MilkLegendItem("मिक्स", DairyFormatters.formatLiters(mixedLiters), UpiIndigo)
            }
        }
    }
}

@Composable
private fun MilkLegendItem(label: String, value: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthlyStatementCard(
    statement: MonthlyCustomerStatement,
    onOpenDetail: () -> Unit,
    onShareBill: () -> Unit
) {
    Card(
        onClick = onOpenDetail,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .testTag("monthly_statement_${statement.customer.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statement.customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${statement.deliveredDaysCount} दिन दूध • ${statement.absentDaysCount} छुट्टी",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(
                    onClick = onShareBill,
                    modifier = Modifier.testTag("share_stmt_${statement.customer.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.cd_share_bill)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatementMiniCol("दूध", DairyFormatters.formatLiters(statement.monthLiters))
                StatementMiniCol("बिल", DairyFormatters.formatRupees(statement.monthBillAmount))
                StatementMiniCol("जमा", DairyFormatters.formatRupees(statement.monthPaidAmount))
                StatementMiniCol(
                    label = if (statement.totalBalanceDue >= 0) "कुल बकाया" else "एडवांस",
                    value = DairyFormatters.formatRupees(kotlin.math.abs(statement.totalBalanceDue)),
                    valueColor = if (statement.totalBalanceDue > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
private fun StatementMiniCol(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun AccountSyncCard(
    userDisplayName: String?,
    userEmail: String?,
    onSignOutClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userDisplayName?.takeIf { it.isNotBlank() } ?: "डेयरी खाता",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = userEmail?.takeIf { it.isNotBlank() } ?: "क्लाउड बैकअप सक्रिय",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_sign_out))
            }
        }
    }
}
