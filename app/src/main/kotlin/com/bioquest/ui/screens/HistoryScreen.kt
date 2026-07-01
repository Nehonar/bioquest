package com.bioquest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import com.bioquest.ui.BioQuestViewModel
import com.bioquest.ui.UiState
import com.bioquest.ui.theme.AlertRed
import com.bioquest.ui.theme.Amber
import com.bioquest.ui.theme.PhosphorGreen
import com.bioquest.ui.theme.TerminalMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val stamp: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

@Composable
fun HistoryScreen(state: UiState, viewModel: BioQuestViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text("SHIP LOG", style = MaterialTheme.typography.titleLarge, color = PhosphorGreen)
            Text("Timeline de eventos registrados.", style = MaterialTheme.typography.bodyMedium, color = TerminalMuted)
        }
        if (state.recentLogs.isEmpty()) {
            item { Text("> sin registros", color = TerminalMuted, modifier = Modifier.padding(top = 12.dp)) }
        }
        items(state.recentLogs, key = { it.id }) { log ->
            LogRow(log) { viewModel.deleteLog(log.id) }
        }
    }
}

@Composable
private fun LogRow(log: HabitLogEntry, onDelete: () -> Unit) {
    val time = Instant.ofEpochMilli(log.timestampMillis).atZone(ZoneId.systemDefault()).format(stamp)
    val (label, color) = describe(log)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text("$time  $label", style = MaterialTheme.typography.bodyMedium, color = color)
        }
        TextButton(onClick = onDelete) { Text("DEL", color = TerminalMuted, style = MaterialTheme.typography.labelSmall) }
    }
}

private fun describe(log: HabitLogEntry): Pair<String, androidx.compose.ui.graphics.Color> = when (log.type) {
    HabitType.WATER -> "WATER +${log.quantity.toInt()}ml" to PhosphorGreen
    HabitType.FRUIT -> "FRUIT +${log.quantity.toInt()}" to PhosphorGreen
    HabitType.HEALTHY_MEAL -> "HEALTHY MEAL" to PhosphorGreen
    HabitType.RISK_FOOD -> "RISK FOOD [${log.foodRuleId ?: "custom"}]" to AlertRed
    HabitType.EXERCISE -> "EXERCISE +${log.quantity.toInt()}min" to Amber
    HabitType.MOOD -> "MOOD ${log.moodValue ?: log.quantity.toInt()}/5" to Amber
    HabitType.WEIGHT -> "WEIGHT ${log.weightKg ?: log.quantity} kg" to Amber
    HabitType.SLEEP_MANUAL -> "SLEEP ${log.sleepHours ?: log.quantity} h" to Amber
    HabitType.REST_BREAK -> "REST BREAK" to PhosphorGreen
}
