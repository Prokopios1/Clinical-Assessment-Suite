package com.clinical.assessment.models

data class AssessmentMetadata(
    val assessmentId: String,
    val alpha: Double,
    val reliability: Double,
    val validity: String,
    val p50Benchmark: Double
)

data class CompletedAssessment(
    val id: String,
    val date: String,
    val score: Double,
    val type: String
)

data class Patient(
    val id: String,
    val firstName: String,
    val lastName: String,
    val history: List<CompletedAssessment>
)
