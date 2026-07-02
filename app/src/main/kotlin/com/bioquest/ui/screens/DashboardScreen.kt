package com.bioquest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bioquest.domain.model.EventSeverity
import com.bioquest.domain.model.StatType
import com.bioquest.ui.BioQuestViewModel
import com.bioquest.ui.UiState
import com.bioquest.ui.components.KeyValueRow
import com.bioquest.ui.components.SectionHeader
import com.bioquest.ui.components.StatBar
import com.bioquest.ui.components.TerminalPanel
import com.bioquest.ui.theme.AlertRed
import com.bioquest.ui.theme.Amber
import com.bioquest.ui.theme.PhosphorGreen
import com.bioquest.ui.theme.TerminalMuted

@Composable
fun DashboardScreen(state: UiState, viewModel: BioQuestViewModel) {
    val snapshot = state.snapshot
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("BIO CORE", style = MaterialTheme.typography.displaySmall, color = PhosphorGreen)
        Text(
            "STATUS: ${snapshot?.corruption?.level?.label ?: "..."}",
            style = MaterialTheme.typography.labelLarge,
            color = if ((snapshot?.stats?.corruption ?: 0) >= 60) AlertRed else PhosphorGreen,
        )
        Spacer(Modifier.height(16.dp))

        if (snapshot == null) {
            Text("Inicializando nucleo...", color = TerminalMuted)
            return@Column
        }

        // How-it-works primer: expanded until the user has logged something.
        ProtocolPanel(startExpanded = state.recentLogs.isEmpty())
        Spacer(Modifier.height(12.dp))

        // Character card
        TerminalPanel(title = "CHARACTER") {
            Text(
                snapshot.characterClass.title,
                style = MaterialTheme.typography.titleLarge,
                color = Amber,
                fontWeight = FontWeight.Bold,
            )
            Text(snapshot.characterClass.blurb, style = MaterialTheme.typography.bodyMedium, color = TerminalMuted)
            Spacer(Modifier.height(8.dp))
            KeyValueRow("CLASS", snapshot.characterClass.title)
            KeyValueRow("CORRUPTION", "${snapshot.corruption.rawPoints} pts / 30d")
            if (snapshot.corruption.chainDamage) {
                KeyValueRow("CHAIN", "${snapshot.corruption.chainLength} dias")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Active event
        val topEvent = snapshot.events.firstOrNull()
        if (topEvent != null) {
            val color = when (topEvent.severity) {
                EventSeverity.CRITICAL -> AlertRed
                EventSeverity.WARNING -> Amber
                EventSeverity.INFO -> PhosphorGreen
            }
            TerminalPanel(title = "ACTIVE EVENT") {
                Text("[${topEvent.type.code}]", style = MaterialTheme.typography.labelLarge, color = color)
                Text(topEvent.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
        }

        // Stats with tap-to-explain
        SectionHeader("CORE STATS")
        var expanded by remember { mutableStateOf<StatType?>(null) }
        val statOrder = listOf(
            StatType.VITALITY, StatType.RECOVERY, StatType.NUTRITION,
            StatType.STRENGTH, StatType.FOCUS, StatType.CORRUPTION,
        )
        TerminalPanel {
            statOrder.forEach { type ->
                val value = snapshot.stats.of(type).value
                Column(Modifier.clickable { expanded = if (expanded == type) null else type }) {
                    StatBar(type.label, value, invert = type == StatType.CORRUPTION)
                    if (expanded == type) {
                        Column(Modifier.padding(start = 8.dp, bottom = 6.dp)) {
                            viewModel.explain(type).forEach {
                                Text("  $it", style = MaterialTheme.typography.labelSmall, color = TerminalMuted)
                            }
                        }
                    }
                }
            }
            Text(
                "Toca una stat para ver por que subio o bajo.",
                style = MaterialTheme.typography.labelSmall,
                color = TerminalMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Next quest
        val nextQuest = snapshot.dailyQuests.firstOrNull { !it.completed }
        TerminalPanel(title = "NEXT QUEST") {
            if (nextQuest != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(nextQuest.title, color = MaterialTheme.colorScheme.onSurface)
                    Text("+${nextQuest.xpReward} XP", color = Amber)
                }
                Text(nextQuest.description, style = MaterialTheme.typography.bodyMedium, color = TerminalMuted)
                Text("${nextQuest.progress}/${nextQuest.target}", style = MaterialTheme.typography.labelSmall, color = PhosphorGreen)
            } else {
                Text("Todas las quests diarias completas. CORE STABLE.", color = PhosphorGreen)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProtocolPanel(startExpanded: Boolean) {
    var expanded by remember { mutableStateOf(startExpanded) }
    TerminalPanel {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("// PROTOCOLO — como funciona", style = MaterialTheme.typography.labelLarge, color = PhosphorGreen)
            Text(if (expanded) "[-]" else "[+]", color = TerminalMuted)
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            listOf(
                "1. Registra lo que haces (agua, comida, ejercicio, mood) desde LOG o el widget. 1 toque = 1 registro.",
                "2. Tus stats parten de un estado natural (~40-60) y llevan el momentum de tus ultimos 7 dias: nada se resetea a cero a medianoche.",
                "3. Lo que no registres cuenta como neutro, nunca como malo. Registrar solo puede ayudarte a entender tus patrones.",
                "4. CORRUPTION mide comida de riesgo acumulada en 30 dias. Nada esta prohibido: un helado apenas importa, quince bollos si.",
                "5. Tu clase (Balanced Human, Iron Monk...) refleja tu tendencia semanal, no la hora del dia.",
            ).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TerminalMuted, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
