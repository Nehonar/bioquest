package com.bioquest.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioquest.BioQuestApplication
import com.bioquest.di.AppContainer
import com.bioquest.domain.GameSnapshot
import com.bioquest.domain.model.FoodImpactRule
import com.bioquest.domain.model.HabitLogEntry
import com.bioquest.domain.model.HabitType
import com.bioquest.domain.model.StatType
import com.bioquest.domain.model.UserGoal
import com.bioquest.notifications.ReminderScheduler
import com.bioquest.settings.AppPreferences
import com.bioquest.widget.BioCoreWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val snapshot: GameSnapshot? = null,
    val recentLogs: List<HabitLogEntry> = emptyList(),
    val foodRules: List<FoodImpactRule> = FoodImpactRule.DEFAULTS,
    val reminderIntervalHours: Int = AppPreferences.DEFAULT_REMINDER_HOURS,
)

class BioQuestViewModel(app: Application) : AndroidViewModel(app) {

    private val container: AppContainer = BioQuestApplication.from(app).container

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val snapshot = container.engine.snapshot()
            val recent = container.repository.recentLogs(30)
            val rules = container.repository.foodRules()
            val reminderHours = container.preferences.reminderIntervalHours.first()
            _state.value = UiState(false, snapshot, recent, rules, reminderHours)
        }
    }

    fun log(
        type: HabitType,
        quantity: Double = com.bioquest.domain.usecase.LogHabitUseCase.defaultQuantity(type),
        foodRuleId: String? = null,
        moodValue: Int? = null,
        weightKg: Double? = null,
        sleepHours: Double? = null,
    ) {
        viewModelScope.launch {
            container.logHabit(type, quantity, foodRuleId, moodValue, weightKg, sleepHours)
            refresh()
            BioCoreWidget.requestUpdate(getApplication())
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            container.repository.deleteLog(id)
            refresh()
            BioCoreWidget.requestUpdate(getApplication())
        }
    }

    fun explain(type: StatType): List<String> {
        val snapshot = _state.value.snapshot ?: return emptyList()
        return container.explainStat(snapshot.stats, type)
    }

    fun updateGoals(goals: UserGoal) {
        viewModelScope.launch {
            container.repository.updateGoals(goals)
            refresh()
            BioCoreWidget.requestUpdate(getApplication())
        }
    }

    /** Persist the reminder cadence and reschedule the periodic worker. */
    fun setReminderInterval(hours: Int) {
        viewModelScope.launch {
            container.preferences.setReminderIntervalHours(hours)
            ReminderScheduler.reschedule(getApplication(), hours)
            refresh()
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                return BioQuestViewModel(app) as T
            }
        }
    }
}
