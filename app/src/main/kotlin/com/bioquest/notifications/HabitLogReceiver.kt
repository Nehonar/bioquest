package com.bioquest.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.bioquest.BioQuestApplication
import com.bioquest.domain.model.HabitType
import com.bioquest.widget.BioCoreWidget
import com.bioquest.widget.QuickActionWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Logs a habit straight from a reminder notification action, then dismisses the
 * nudge and refreshes the widgets. Mirrors the widget's one-tap logging so a
 * reminder can be answered ("bebí agua", "comí sano"...) without opening the app.
 */
class HabitLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val typeName = intent.getStringExtra(EXTRA_TYPE) ?: return
        val type = runCatching { HabitType.valueOf(typeName) }.getOrNull() ?: return
        val quantity = intent.getDoubleExtra(EXTRA_QUANTITY, 1.0)
        val ruleId = intent.getStringExtra(EXTRA_RULE)

        val app = context.applicationContext as android.app.Application
        val container = BioQuestApplication.from(app).container

        // BroadcastReceiver.onReceive is sync; keep the process alive for the
        // suspend log + widget refresh with goAsync().
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.logHabit(type = type, quantity = quantity, foodRuleId = ruleId)
                runCatching { BioCoreWidget().updateAll(context) }
                runCatching { QuickActionWidget().updateAll(context) }
                runCatching { NotificationManagerCompat.from(context).cancel(NUDGE_NOTIFICATION_ID) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_QUANTITY = "extra_quantity"
        const val EXTRA_RULE = "extra_rule"
    }
}
