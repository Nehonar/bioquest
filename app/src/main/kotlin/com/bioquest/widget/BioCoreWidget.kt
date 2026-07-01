package com.bioquest.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.toMutableParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bioquest.BioQuestApplication
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.model.WidgetState

private val Green = Color(0xFF39FF14)
private val Amber = Color(0xFFFFB000)
private val Red = Color(0xFFFF3B30)
private val Bg = Color(0xFF05090A)
private val Muted = Color(0xFF5C8A66)

/**
 * Bio Core 4x2 widget: the product's centrepiece. Renders four stats +
 * corruption and the next quest, and exposes one-tap logging for Water, Fruit,
 * Treat and Mood via Glance [actionRunCallback].
 */
class BioCoreWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as android.app.Application
        val state = runCatching {
            BioQuestApplication.from(app).container.engine.snapshot().widgetState
        }.getOrDefault(WidgetState.PLACEHOLDER)

        provideContent {
            GlanceTheme { BioCoreContent(state) }
        }
    }

    companion object {
        suspend fun requestUpdate(context: Context) {
            BioCoreWidget().updateAll(context)
        }
    }
}

class BioCoreWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BioCoreWidget()
}

@Composable
private fun BioCoreContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier.fillMaxWidth().background(Bg).padding(10.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("BIO CORE", style = TextStyle(color = ColorProvider(Green), fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "// ${state.statusLabel}",
                style = TextStyle(color = ColorProvider(if (state.corruption >= 60) Red else Muted)),
            )
        }
        Spacer(GlanceModifier.height(4.dp))

        WidgetStatRow("VITALITY", state.vitality)
        WidgetStatRow("RECOVERY", state.recovery)
        WidgetStatRow("NUTRITION", state.nutrition)
        WidgetStatRow("FOCUS", state.focus)
        WidgetStatRow("CORRUPTION", state.corruption, invert = true)

        Spacer(GlanceModifier.height(4.dp))
        Text(
            "QUEST: ${state.nextQuestTitle} ${state.nextQuestDetail}",
            style = TextStyle(color = ColorProvider(Amber)),
        )
        if (state.warning != null) {
            Text("WARNING: ${state.warning}", style = TextStyle(color = ColorProvider(Red)))
        }

        Spacer(GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetActionButton("AGUA", HabitType.WATER, 250.0)
            Spacer(GlanceModifier.width(4.dp))
            WidgetActionButton("FRUTA", HabitType.FRUIT, 1.0)
            Spacer(GlanceModifier.width(4.dp))
            WidgetActionButton("CAPRICHO", HabitType.RISK_FOOD, 1.0, ruleId = "ice_cream")
            Spacer(GlanceModifier.width(4.dp))
            WidgetActionButton("MOOD", HabitType.MOOD, 3.0, mood = 3)
        }
    }
}

@Composable
private fun WidgetStatRow(label: String, value: Int, invert: Boolean = false) {
    val v = if (invert) 100 - value else value
    val color = when {
        v >= 60 -> Green
        v >= 35 -> Amber
        else -> Red
    }
    val segments = 10
    val filled = (value.coerceIn(0, 100) * segments) / 100
    val bar = "#".repeat(filled) + "-".repeat(segments - filled)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(label.padEnd(11).take(11), style = TextStyle(color = ColorProvider(Muted)))
        Text("[$bar]", style = TextStyle(color = ColorProvider(color)))
        Spacer(GlanceModifier.width(4.dp))
        Text(value.toString().padStart(3), style = TextStyle(color = ColorProvider(color), fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun WidgetActionButton(
    label: String,
    type: HabitType,
    quantity: Double,
    ruleId: String? = null,
    mood: Int? = null,
) {
    val params = actionParametersOf(
        LogHabitAction.typeKey to type.name,
        LogHabitAction.quantityKey to quantity,
    ).toMutableParameters().apply {
        if (ruleId != null) set(LogHabitAction.ruleKey, ruleId)
        if (mood != null) set(LogHabitAction.moodKey, mood)
    }
    Text(
        text = "[$label]",
        modifier = GlanceModifier
            .padding(3.dp)
            .clickable(actionRunCallback<LogHabitAction>(params)),
        style = TextStyle(color = ColorProvider(Green), fontWeight = FontWeight.Bold),
    )
}
