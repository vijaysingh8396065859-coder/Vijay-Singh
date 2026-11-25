package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DairyFormatters
import com.example.ui.theme.MeadowEmerald
import com.example.ui.theme.MeadowEmeraldContainer
import com.example.ui.theme.MeadowEmeraldLight
import com.example.viewmodel.DairyDataSnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun weekdayShortHi(dateKey: String): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val parsed = runCatching { fmt.parse(dateKey) }.getOrNull() ?: return ""
    val cal = Calendar.getInstance().apply { time = parsed }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "रवि"
        Calendar.MONDAY -> "सोम"
        Calendar.TUESDAY -> "मंगल"
        Calendar.WEDNESDAY -> "बुध"
        Calendar.THURSDAY -> "गुरु"
        Calendar.FRIDAY -> "शुक्र"
        Calendar.SATURDAY -> "शनि"
        else -> ""
    }
}

/**
 * Last-7-days milk chart with animated bars, plus a pending-dues strip.
 * Shown on the Today screen under the hero summary card.
 */
@Composable
fun WeekMilkChartCard(
    snapshot: DairyDataSnapshot,
    selectedDateKey: String,
    modifier: Modifier = Modifier
) {
    val days = remember(snapshot.milkEntries, selectedDateKey) {
        (6 downTo 0).map { back ->
            val key = DairyFormatters.shiftDateKey(selectedDateKey, -back)
            val liters = snapshot.milkEntries
                .filter { it.dateKey == key }
                .sumOf { if (it.absent) 0.0 else it.quantityLiters }
            key to liters
        }
    }
    val maxLiters = (days.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    val weekTotal = days.sumOf { it.second }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "पिछले 7 दिन का दूध",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "कुल ${DairyFormatters.formatLiters(weekTotal)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, (key, liters) ->
                    val fraction by animateFloatAsState(
                        targetValue = (liters / maxLiters).toFloat(),
                        animationSpec = tween(durationMillis = 700, delayMillis = index * 60),
                        label = "weekBar$index"
                    )
                    val isSelected = key == selectedDateKey
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (liters > 0.0) DairyFormatters.formatLitersNumeric(liters) else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(
                                    if (liters > 0.0) {
                                        (84f * fraction).dp.coerceAtLeast(6.dp)
                                    } else {
                                        3.dp
                                    }
                                )
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        topEnd = 8.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    }
                                )
                        )
                        Text(
                            text = if (isSelected) "आज" else weekdayShortHi(key),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            if (snapshot.totalPendingDues > 0.0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "कुल बाकी वसूली: ${DairyFormatters.formatRupees(snapshot.totalPendingDues)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Branded animated splash shown briefly when the app opens:
 * the logo scales and fades in over a fresh mint-to-teal gradient.
 */
@Composable
fun SplashOverlay(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, animationSpec = tween(600)) }
        launch { alpha.animateTo(1f, animationSpec = tween(500)) }
        delay(1000)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MeadowEmeraldContainer, MeadowEmeraldLight, MeadowEmerald)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_milk_diary_icon_1795579553116),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
            Text(
                text = "Milk Diary",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "शुद्ध दूध • साफ हिसाब",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}
