package com.example.data.model

data class CrashReport(
    val id: String,
    val gameId: String,
    val gameTitle: String,
    val timestamp: Long,
    val errorReason: String,
    val stackTraceSnippet: String,
    val suggestedFix: String,
    val isSafeModeRecommended: Boolean,
    val activeProfileAtCrash: PerformanceProfile
)
