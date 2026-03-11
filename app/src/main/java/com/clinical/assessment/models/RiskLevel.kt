package com.clinical.assessment.models

import com.clinical.assessment.R

enum class RiskLevel(val labelResId: Int, val colorResId: Int) {
    SEVERE(R.string.risk_severe, R.drawable.bg_chip_severe),
    HIGH(R.string.risk_high, R.drawable.bg_chip_high),
    MODERATE(R.string.risk_moderate, R.drawable.bg_chip_moderate),
    LOW(R.string.risk_low, R.drawable.bg_chip_low),
    COMPLETED(R.string.risk_completed, R.drawable.bg_chip_neutral),
    UNKNOWN(R.string.risk_unknown, R.drawable.bg_chip_low)
}
