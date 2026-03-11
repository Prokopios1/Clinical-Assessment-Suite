package com.clinical.assessment.models

data class Scale(
    val id: String,
    val name: String,
    val description: String,
    val items: List<String>,
    val options: List<String>,
    val labels: Map<String, String>?,
    val scoring: ScoringType,
    val weights: Map<String, Double>?,
    val thresholds: List<Threshold>?,
    val threshold: Double?,
    val domains: Map<String, List<Int>>?
)

enum class ScoringType {
    WEIGHTED_SUM,
    COUNT_YES,
    SUM,
    MEAN_PER_DOMAIN_WEIGHTED
}

data class Threshold(
    val low: Double,
    val high: Double,
    val label: String,
    val interpretation: String
)

data class TestResult(
    val scaleId: String,
    val score: Double,
    val details: Map<String, Any>? = null,
    val timestamp: Long = System.currentTimeMillis()
)
