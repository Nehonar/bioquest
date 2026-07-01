package com.bioquest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitType
import com.bioquest.ui.BioQuestViewModel
import com.bioquest.ui.UiState
import com.bioquest.ui.components.SectionHeader
import com.bioquest.ui.components.TerminalPanel
import com.bioquest.ui.theme.AlertRed
import com.bioquest.ui.theme.Amber
import com.bioquest.ui.theme.PhosphorGreen
import com.bioquest.ui.theme.TerminalMuted

private data class QuickButton(val label: String, val type: HabitType, val quantity: Double, val accent: Boolean = false)

@Composable
fun DailyLogScreen(state: UiState, viewModel: BioQuestViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("DAILY LOG", style = MaterialTheme.typography.titleLarge, color = PhosphorGreen)
            Text("1 toque = 1 accion registrada.", style = MaterialTheme.typography.bodyMedium, color = TerminalMuted)
        }

        item {
            SectionHeader("Acciones rapidas")
            val buttons = listOf(
                QuickButton("AGUA +250", HabitType.WATER, 250.0),
                QuickButton("FRUTA +1", HabitType.FRUIT, 1.0),
                QuickButton("COMIDA SANA", HabitType.HEALTHY_MEAL, 1.0),
                QuickButton("COMIDA NORMAL", HabitType.NORMAL_MEAL, 1.0),
                QuickButton("EJERCICIO +30", HabitType.EXERCISE, 30.0),
                QuickButton("PAUSA", HabitType.REST_BREAK, 1.0),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(buttons) { b ->
                    Button(
                        onClick = { viewModel.log(b.type, b.quantity) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(b.label, color = PhosphorGreen)
                    }
                }
            }
        }

        item { MoodSlider(onLog = { viewModel.log(HabitType.MOOD, moodValue = it) }) }

        item { WeightInput(onLog = { viewModel.log(HabitType.WEIGHT, weightKg = it) }) }

        item { SleepInput(onLog = { viewModel.log(HabitType.SLEEP_MANUAL, sleepHours = it) }) }

        item { SectionHeader("Comida de riesgo / capricho") }
        item {
            Text(
                "Sin alimentos prohibidos: cada opcion suma puntos de corrupcion a la ventana de 30 dias.",
                style = MaterialTheme.typography.labelSmall,
                color = TerminalMuted,
            )
        }
        items(state.foodRules.filter { it.corruptionPoints > 0 }) { rule ->
            RiskFoodRow(rule) { viewModel.log(HabitType.RISK_FOOD, 1.0, foodRuleId = rule.id) }
        }
    }
}

@Composable
private fun MoodSlider(onLog: (Int) -> Unit) {
    var mood by remember { mutableFloatStateOf(3f) }
    TerminalPanel(title = "MOOD 1-5") {
        Text("Estado: ${mood.toInt()}", color = Amber)
        Slider(value = mood, onValueChange = { mood = it }, valueRange = 1f..5f, steps = 3)
        Button(onClick = { onLog(mood.toInt()) }, modifier = Modifier.fillMaxWidth()) {
            Text("REGISTRAR MOOD")
        }
    }
}

@Composable
private fun WeightInput(onLog: (Double) -> Unit) {
    var weight by remember { mutableFloatStateOf(70f) }
    TerminalPanel(title = "PESO (opcional)") {
        Text("${"%.1f".format(weight)} kg", color = Amber)
        Slider(value = weight, onValueChange = { weight = it }, valueRange = 40f..140f)
        OutlinedButton(onClick = { onLog(weight.toDouble()) }, modifier = Modifier.fillMaxWidth()) {
            Text("REGISTRAR PESO")
        }
    }
}

@Composable
private fun SleepInput(onLog: (Double) -> Unit) {
    var sleep by remember { mutableFloatStateOf(7.5f) }
    TerminalPanel(title = "SUENO MANUAL") {
        Text("${"%.1f".format(sleep)} h", color = Amber)
        Slider(value = sleep, onValueChange = { sleep = it }, valueRange = 3f..12f, steps = 17)
        OutlinedButton(onClick = { onLog(sleep.toDouble()) }, modifier = Modifier.fillMaxWidth()) {
            Text("REGISTRAR SUENO")
        }
    }
}

@Composable
private fun RiskFoodRow(rule: FoodImpactRule, onLog: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(rule.label, color = MaterialTheme.colorScheme.onSurface)
            Text("+${rule.corruptionPoints} corrupcion", style = MaterialTheme.typography.labelSmall, color = AlertRed)
        }
        OutlinedButton(onClick = onLog) { Text("LOG", color = Amber, textAlign = TextAlign.Center) }
    }
}
