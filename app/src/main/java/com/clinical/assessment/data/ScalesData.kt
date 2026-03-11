package com.clinical.assessment.data

import android.content.Context
import com.clinical.assessment.R
import com.clinical.assessment.models.*

object ScalesData {
    
    fun getScale(context: Context, id: String): Scale? {
        return when(id) {
            "PHQ-9" -> createPHQ9(context)
            "GAD-7" -> createGAD7(context)
            "PID-5-BF" -> createPID5BF(context)
            "MSI-BPD" -> createMSIBPD(context)
            "PQ-B" -> createPQB(context)
            else -> null
        }
    }
    
    fun getAllScales(context: Context): List<Scale> {
        return listOf(
            createPHQ9(context),
            createGAD7(context),
            createPID5BF(context),
            createMSIBPD(context),
            createPQB(context)
        )
    }

    private fun createPHQ9(context: Context): Scale {
        return Scale(
            id = "PHQ-9",
            name = context.getString(R.string.phq9_title),
            description = context.getString(R.string.phq9_desc),
            items = context.resources.getStringArray(R.array.phq9_items).toList(),
            options = listOf("0", "1", "2", "3"),
            labels = mapOf(
                "0" to context.resources.getStringArray(R.array.phq9_labels)[0],
                "1" to context.resources.getStringArray(R.array.phq9_labels)[1],
                "2" to context.resources.getStringArray(R.array.phq9_labels)[2],
                "3" to context.resources.getStringArray(R.array.phq9_labels)[3]
            ),
            scoring = ScoringType.WEIGHTED_SUM,
            weights = mapOf("0" to 0.0, "1" to 1.0, "2" to 2.0, "3" to 3.0),
            thresholds = listOf(
                Threshold(0.0, 4.0, "Minimal", context.getString(R.string.phq9_minimal_interp)),
                Threshold(5.0, 9.0, "Mild", context.getString(R.string.phq9_mild_interp)),
                Threshold(10.0, 14.0, "Moderate", context.getString(R.string.phq9_moderate_interp)),
                Threshold(15.0, 19.0, "Moderately Severe", context.getString(R.string.phq9_mod_severe_interp)),
                Threshold(20.0, 27.0, "Severe", context.getString(R.string.phq9_severe_interp))
            ),
            threshold = null,
            domains = null
        )
    }

    private fun createGAD7(context: Context): Scale {
        val phq9Labels = context.resources.getStringArray(R.array.phq9_labels)
        return Scale(
            id = "GAD-7",
            name = context.getString(R.string.gad7_title),
            description = context.getString(R.string.gad7_desc),
            items = context.resources.getStringArray(R.array.gad7_items).toList(),
            options = listOf("0", "1", "2", "3"),
            labels = mapOf(
                "0" to phq9Labels[0],
                "1" to phq9Labels[1],
                "2" to phq9Labels[2],
                "3" to phq9Labels[3]
            ),
            scoring = ScoringType.WEIGHTED_SUM,
            weights = mapOf("0" to 0.0, "1" to 1.0, "2" to 2.0, "3" to 3.0),
            thresholds = listOf(
                Threshold(0.0, 4.0, "Minimal", context.getString(R.string.gad7_minimal_interp)),
                Threshold(5.0, 9.0, "Mild", context.getString(R.string.gad7_mild_interp)),
                Threshold(10.0, 14.0, "Moderate", context.getString(R.string.gad7_moderate_interp)),
                Threshold(15.0, 21.0, "Severe", context.getString(R.string.gad7_severe_interp))
            ),
            threshold = null,
            domains = null
        )
    }

    private fun createPID5BF(context: Context): Scale {
        val pidLabels = context.resources.getStringArray(R.array.pid5bf_labels)
        return Scale(
            id = "PID-5-BF",
            name = context.getString(R.string.pid5bf_title),
            description = context.getString(R.string.pid5bf_desc),
            items = context.resources.getStringArray(R.array.pid5bf_items).toList(),
            options = listOf("0", "1", "2", "3"),
            labels = mapOf(
                "0" to pidLabels[0],
                "1" to pidLabels[1],
                "2" to pidLabels[2],
                "3" to pidLabels[3]
            ),
            scoring = ScoringType.MEAN_PER_DOMAIN_WEIGHTED,
            weights = mapOf("0" to 0.0, "1" to 1.0, "2" to 2.0, "3" to 3.0),
            thresholds = listOf(
                Threshold(0.0, 100.0, "Completed", context.getString(R.string.pid5bf_interp))
            ),
            threshold = 2.0,
            domains = mapOf(
                "Negative Affectivity" to listOf(6, 7, 12, 13, 20),
                "Detachment" to listOf(8, 9, 11, 14, 24),
                "Antagonism" to listOf(2, 3, 4, 5, 10, 15, 22),
                "Disinhibition" to listOf(0, 1, 16, 19, 23),
                "Psychoticism" to listOf(17, 18, 21)
            )
            )

    }

    private fun createMSIBPD(context: Context): Scale {
        return Scale(
            id = "MSI-BPD",
            name = context.getString(R.string.msibpd_title),
            description = context.getString(R.string.msibpd_desc),
            items = context.resources.getStringArray(R.array.msibpd_items).toList(),
            options = listOf("0", "1"),
            labels = mapOf("0" to "No", "1" to "Yes"), 
            scoring = ScoringType.SUM,
            weights = mapOf("0" to 0.0, "1" to 1.0),
            thresholds = listOf(
                Threshold(0.0, 6.0, "Low", context.getString(R.string.msibpd_low_interp)),
                Threshold(7.0, 10.0, "High", context.getString(R.string.msibpd_high_interp))
            ),
            threshold = 7.0,
            domains = null
        )
    }

    private fun createPQB(context: Context): Scale {
        return Scale(
            id = "PQ-B",
            name = context.getString(R.string.pqb_title),
            description = context.getString(R.string.pqb_desc),
            items = context.resources.getStringArray(R.array.pqb_items).toList(),
            options = listOf("0", "1"),
            labels = mapOf("0" to "No", "1" to "Yes"),
            scoring = ScoringType.SUM,  // Changed from COUNT_YES for consistency with weights
            weights = mapOf("0" to 0.0, "1" to 1.0),
            thresholds = listOf(
                Threshold(0.0, 5.0, "Low", context.getString(R.string.pqb_low_interp)),
                Threshold(6.0, 21.0, "High/Connect with Technician", context.getString(R.string.pqb_high_interp))
            ),
            threshold = 6.0,
            domains = null
        )
    }
}
