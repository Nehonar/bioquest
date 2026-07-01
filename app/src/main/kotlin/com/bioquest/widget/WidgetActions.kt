package com.bioquest.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.bioquest.BioQuestApplication
import com.bioquest.domain.model.HabitType

/**
 * Glance [ActionCallback] that logs a habit straight from the widget and
 * refreshes both widgets. The habit type/quantity travel as action parameters.
 */
class LogHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val typeName = parameters[typeKey] ?: return
        val quantity = parameters[quantityKey] ?: 1.0
        val ruleId = parameters[ruleKey]
        val mood = parameters[moodKey]

        val container = BioQuestApplication.from(context.applicationContext as android.app.Application).container
        container.logHabit(
            type = HabitType.valueOf(typeName),
            quantity = quantity,
            foodRuleId = ruleId,
            moodValue = mood,
        )
        BioCoreWidget().updateAll(context)
        QuickActionWidget().updateAll(context)
    }

    companion object {
        val typeKey = ActionParameters.Key<String>("habit_type")
        val quantityKey = ActionParameters.Key<Double>("habit_quantity")
        val ruleKey = ActionParameters.Key<String>("food_rule")
        val moodKey = ActionParameters.Key<Int>("mood_value")
    }
}
