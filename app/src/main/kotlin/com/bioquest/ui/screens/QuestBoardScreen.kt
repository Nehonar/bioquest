package com.bioquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioquest.domain.model.Quest
import com.bioquest.ui.UiState
import com.bioquest.ui.components.SectionHeader
import com.bioquest.ui.components.TerminalPanel
import com.bioquest.ui.theme.Amber
import com.bioquest.ui.theme.PhosphorDim
import com.bioquest.ui.theme.PhosphorGreen
import com.bioquest.ui.theme.TerminalMuted

@Composable
fun QuestBoardScreen(state: UiState) {
    val snapshot = state.snapshot
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("QUEST BOARD", style = MaterialTheme.typography.titleLarge, color = PhosphorGreen) }

        if (snapshot == null) {
            item { Text("Cargando...", color = TerminalMuted) }
            return@LazyColumn
        }

        item { SectionHeader("Daily Quests") }
        items(snapshot.dailyQuests.size) { i -> QuestCard(snapshot.dailyQuests[i]) }

        item { SectionHeader("Weekly Quests") }
        items(snapshot.weeklyQuests.size) { i -> QuestCard(snapshot.weeklyQuests[i]) }
    }
}

@Composable
private fun QuestCard(quest: Quest) {
    TerminalPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (quest.completed) "[X] ${quest.title}" else "[ ] ${quest.title}",
                color = if (quest.completed) PhosphorGreen else MaterialTheme.colorScheme.onSurface,
            )
            Text("+${quest.xpReward} XP", color = Amber)
        }
        Text(quest.description, style = MaterialTheme.typography.bodyMedium, color = TerminalMuted)
        QuestProgressBar(quest)
        Text("${quest.progress}/${quest.target}", style = MaterialTheme.typography.labelSmall, color = PhosphorGreen)
    }
}

@Composable
private fun QuestProgressBar(quest: Quest) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .padding(vertical = 2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(quest.fraction)
                .height(10.dp)
                .background(if (quest.completed) PhosphorGreen else PhosphorDim, RoundedCornerShape(2.dp)),
        )
    }
}
