package com.bioquest.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioquest.domain.model.UserGoal
import com.bioquest.ui.BioQuestViewModel
import com.bioquest.ui.UiState
import com.bioquest.ui.components.KeyValueRow
import com.bioquest.ui.components.SectionHeader
import com.bioquest.ui.components.TerminalPanel
import com.bioquest.ui.theme.AlertRed
import com.bioquest.ui.theme.Amber
import com.bioquest.ui.theme.PhosphorGreen
import com.bioquest.ui.theme.TerminalMuted

@Composable
fun SettingsScreen(state: UiState, viewModel: BioQuestViewModel) {
    val goals = state.snapshot?.goals ?: UserGoal.DEFAULT

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("CONFIG", style = MaterialTheme.typography.titleLarge, color = PhosphorGreen) }

        item { GoalEditor(goals, onSave = viewModel::updateGoals) }

        item {
            SectionHeader("Reglas de corrupcion (30d)")
            TerminalPanel {
                KeyValueRow("Normal", "0 - ${goals.corruptionWatchThreshold}")
                KeyValueRow("Vigilancia", "${goals.corruptionWatchThreshold + 1} - ${goals.corruptionHighThreshold}")
                KeyValueRow("Riesgo alto", "${goals.corruptionHighThreshold + 1} - ${goals.corruptionCriticalThreshold}")
                KeyValueRow("Evento critico", "${goals.corruptionCriticalThreshold + 1}+")
            }
        }

        item {
            SectionHeader("Reglas de comida")
            TerminalPanel {
                state.foodRules.forEach { rule ->
                    KeyValueRow(
                        rule.label,
                        buildString {
                            if (rule.nutritionXp > 0) append("+${rule.nutritionXp} nut ")
                            if (rule.vitalityXp > 0) append("+${rule.vitalityXp} vit ")
                            if (rule.corruptionPoints > 0) append("+${rule.corruptionPoints} corr")
                        }.trim().ifEmpty { "-" },
                    )
                }
            }
        }

        item { ReminderEditor(state.reminderIntervalHours, onSave = viewModel::setReminderInterval) }

        item {
            SectionHeader("Permisos")
            TerminalPanel {
                Text("Health Connect: opcional, para pasos/sueno automaticos.", color = TerminalMuted, style = MaterialTheme.typography.bodyMedium)
                Text("Notificaciones: recordatorios contextuales, no spam.", color = TerminalMuted, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "La app funciona completa de forma manual sin ningun permiso.",
                    color = PhosphorGreen,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            Text(
                "BioQuest no es una app medica ni de dieta. Local-first, sin login ni nube.",
                style = MaterialTheme.typography.labelSmall,
                color = AlertRed,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun GoalEditor(goals: UserGoal, onSave: (UserGoal) -> Unit) {
    var water by remember(goals) { mutableFloatStateOf(goals.waterMlGoal.toFloat()) }
    var steps by remember(goals) { mutableFloatStateOf(goals.stepsGoal.toFloat()) }
    var fruit by remember(goals) { mutableFloatStateOf(goals.fruitGoal.toFloat()) }
    var sleep by remember(goals) { mutableFloatStateOf(goals.sleepHoursGoal.toFloat()) }
    var exercise by remember(goals) { mutableFloatStateOf(goals.exerciseMinutesGoal.toFloat()) }

    TerminalPanel(title = "OBJETIVOS DIARIOS") {
        GoalSlider("Agua", "${water.toInt()} ml", water, 500f..4000f) { water = it }
        GoalSlider("Pasos", "${steps.toInt()}", steps, 2000f..20000f) { steps = it }
        GoalSlider("Fruta", "${fruit.toInt()} u", fruit, 1f..6f) { fruit = it }
        GoalSlider("Sueno", "${"%.1f".format(sleep)} h", sleep, 4f..10f) { sleep = it }
        GoalSlider("Ejercicio", "${exercise.toInt()} min", exercise, 10f..120f) { exercise = it }
        Button(
            onClick = {
                onSave(
                    goals.copy(
                        waterMlGoal = water.toInt(),
                        stepsGoal = steps.toInt(),
                        fruitGoal = fruit.toInt(),
                        sleepHoursGoal = sleep.toDouble(),
                        exerciseMinutesGoal = exercise.toInt(),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("GUARDAR OBJETIVOS") }
    }
}

@Composable
private fun ReminderEditor(currentHours: Int, onSave: (Int) -> Unit) {
    var hours by remember(currentHours) { mutableFloatStateOf(currentHours.toFloat()) }
    TerminalPanel(title = "RECORDATORIOS") {
        Text(
            "Si pasan las horas sin registrar nada (en horario diurno), BioQuest " +
                "pregunta \"¿has bebido o comido?\" con botones de 1 toque.",
            style = MaterialTheme.typography.labelSmall,
            color = TerminalMuted,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Cada", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${hours.toInt()} h", color = Amber)
        }
        Slider(value = hours, onValueChange = { hours = it }, valueRange = 1f..8f, steps = 6)
        Button(onClick = { onSave(hours.toInt()) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("GUARDAR CADENCIA")
        }
    }
}

@Composable
private fun GoalSlider(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = Amber)
    }
    Slider(value = current, onValueChange = onChange, valueRange = range)
}
