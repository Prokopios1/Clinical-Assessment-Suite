package com.clinical.assessment.models

import java.util.Date

data class PatientSummary(
    val user: UserData,
    val latestDate: Date?,
    val latestRisk: RiskLevel,
    val scores: List<Double>,
    val scaleResults: Map<String, ScaleResult> = emptyMap()
)

data class ScaleResult(
    val score: Double,
    val risk: RiskLevel,
    val history: List<Double>,
    val date: Date?
)
