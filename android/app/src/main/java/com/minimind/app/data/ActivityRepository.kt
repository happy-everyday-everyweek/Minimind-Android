package com.minimind.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActivityRecord(
    val id: String,
    val type: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "completed"
)

class ActivityRepository {

    private val _activities = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val activities: StateFlow<List<ActivityRecord>> = _activities.asStateFlow()

    fun addActivity(record: ActivityRecord) {
        val current = _activities.value.toMutableList()
        current.add(0, record)
        if (current.size > 50) {
            current.removeAt(current.lastIndex)
        }
        _activities.value = current
    }

    fun clearActivities() {
        _activities.value = emptyList()
    }

    fun getRecentActivities(limit: Int = 5): List<ActivityRecord> {
        return _activities.value.take(limit)
    }
}
