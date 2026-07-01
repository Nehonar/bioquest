package com.bioquest.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.toMutableParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bioquest.BioQuestApplication
import com.bioquest.domain.model.QuickAction

/**
 * Quick Action 1x1 widget. Configurable per-instance (Agua / Fruta / Mood /
 * Capricho); the choice is stored in [WidgetActionConfigEntity] keyed by the
 * app widget id and defaults to Agua when unset.
 */
class QuickActionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as android.app.Application
        val container = BioQuestApplication.from(app).container
        val widgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrNull()
        val configured = widgetId?.let {
            runCatching { container.database.widgetActionConfigDao().get(it)?.quickAction }.getOrNull()
        }
        val action = configured?.let { runCatching { QuickAction.valueOf(it) }.getOrNull() } ?: QuickAction.WATER

        provideContent { QuickActionContent(action) }
    }
}

class QuickActionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionWidget()
}

@Composable
private fun QuickActionContent(action: QuickAction) {
    val params = actionParametersOf(
        LogHabitAction.typeKey to action.habit.name,
        LogHabitAction.quantityKey to action.quantity,
    ).toMutableParameters().apply {
        if (action == QuickAction.TREAT) set(LogHabitAction.ruleKey, "ice_cream")
        if (action == QuickAction.MOOD) set(LogHabitAction.moodKey, 3)
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF05090A))
            .padding(6.dp)
            .clickable(actionRunCallback<LogHabitAction>(params)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            action.label,
            style = TextStyle(color = ColorProvider(Color(0xFF39FF14)), fontWeight = FontWeight.Bold),
        )
    }
}
