package com.clinical.assessment.models

data class Screening(
    val user: UserData,
    val results: Map<String, TestResult>,
    val timestamp: com.google.firebase.Timestamp? = null
)
