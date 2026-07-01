package com.bioquest.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.Composable
import com.bioquest.BioQuestApplication
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.model.WidgetState

private val Green = Color(0xFF39FF14)
private val Amber = Color(0xFFFFB000)
private val Red = Color(0xFFFF3B30)
private val Bg = Color(0xFF05090A)
private val Muted = Color(0xFF5C8A66)

// Button background tints (dark, so the coloured label reads as a "chip").
private val PositiveBg = Color(0xFF0E2A16)
private val NeutralBg = Color(0xFF2A2410)
private val RiskBg = Color(0xFF2A1010)

/**
 * Bio Core 4x2 widget: the product's centrepiece. Renders four stats +
 * corruption and two rows of one-tap food logging (Water / Fruit / Healthy and
 * Normal / Processed / Pastry) via Glance [actionRunCallback].
 *
 * A Glance Column does not scroll, so anything below the widget's height is
 * simply clipped. The layout is kept deliberately compact — no standalone quest
 * or warning line (a "!" folds into the header) — so the action buttons always
 * stay on screen and tappable.
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
        modifier = GlanceModifier.fillMaxWidth().background(Bg).padding(8.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("BIO CORE", style = TextStyle(color = ColorProvider(Green), fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "// ${state.statusLabel}",
                style = TextStyle(color = ColorProvider(if (state.corruption >= 60) Red else Muted)),
            )
            if (state.warning != null) {
                Spacer(GlanceModifier.width(6.dp))
                Text(" ! ", style = TextStyle(color = ColorProvider(Red), fontWeight = FontWeight.Bold))
            }
        }
        Spacer(GlanceModifier.height(3.dp))

        WidgetStatRow("VITALITY", state.vitality)
        WidgetStatRow("RECOVERY", state.recovery)
        WidgetStatRow("NUTRITION", state.nutrition)
        WidgetStatRow("FOCUS", state.focus)
        WidgetStatRow("CORRUPTION", state.corruption, invert = true)

        Spacer(GlanceModifier.height(4.dp))

        // Row 1 — positive actions (green).
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetActionButton("AGUA", HabitType.WATER, 250.0, PositiveBg, Green)
            WidgetActionButton("FRUTA", HabitType.FRUIT, 1.0, PositiveBg, Green)
            WidgetActionButton("SANO", HabitType.HEALTHY_MEAL, 1.0, PositiveBg, Green)
        }
        Spacer(GlanceModifier.height(3.dp))
        // Row 2 — neutral + risk foods (amber / red).
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetActionButton("NORMAL", HabitType.NORMAL_MEAL, 1.0, NeutralBg, Amber)
            WidgetActionButton("PROCES", HabitType.RISK_FOOD, 1.0, RiskBg, Red, ruleId = "fast_food")
            WidgetActionButton("BOLLE", HabitType.RISK_FOOD, 1.0, RiskBg, Red, ruleId = "pastry")
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
    val bar = "█".repeat(filled) + "░".repeat(segments - filled)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(label.padEnd(11).take(11), style = TextStyle(color = ColorProvider(Muted), fontSize = 12.sp))
        Text("[$bar]", style = TextStyle(color = ColorProvider(color), fontSize = 12.sp))
        Spacer(GlanceModifier.width(4.dp))
        Text(
            value.toString().padStart(3),
            style = TextStyle(color = ColorProvider(color), fontWeight = FontWeight.Bold, fontSize = 12.sp),
        )
    }
}

/**
 * A tappable "chip" button that fills its share of the row ([defaultWeight]) so
 * every button is a large, obvious tap target with a coloured background.
 *
 * Declared as a [RowScope] extension because Glance exposes `defaultWeight()`
 * only inside a Row/Column scope — there is no importable top-level function.
 */
@Composable
private fun RowScope.WidgetActionButton(
    label: String,
    type: HabitType,
    quantity: Double,
    bg: Color,
    fg: Color,
    ruleId: String? = null,
) {
    val params = actionParametersOf(
        LogHabitAction.typeKey to type.name,
        LogHabitAction.quantityKey to quantity,
    ).toMutableParameters().apply {
        if (ruleId != null) set(LogHabitAction.ruleKey, ruleId)
    }
    Box(
        modifier = GlanceModifier
            .defaultWeight()
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(bg)
                .padding(vertical = 7.dp)
                .clickable(actionRunCallback<LogHabitAction>(params)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(fg), fontWeight = FontWeight.Bold, fontSize = 12.sp),
            )
        }
    }
}
