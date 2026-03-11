package com.clinical.assessment.models

data class UserData(
    val name: String = "",
    val email: String = "",
    val gdprConsent: Boolean = false,
    val therapistId: String = "",
    val role: String = "patient"
)
