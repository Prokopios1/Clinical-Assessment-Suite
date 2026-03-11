package com.clinical.assessment.utils

import android.content.Context
import org.json.JSONObject

object MetadataManager {
    private var metaJson: JSONObject? = null

    fun load(context: Context) {
        if (metaJson != null) return
        try {
            val jsonString = context.assets.open("assessment_meta.json").bufferedReader().use { it.readText() }
            metaJson = JSONObject(jsonString).getJSONObject("assessments")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBenchmark(scaleId: String): Double? {
        val benchmark = metaJson?.optJSONObject(scaleId)?.optJSONObject("benchmarks")?.optDouble("median_50th_percentile")
        return if (benchmark != null && !benchmark.isNaN()) benchmark else null
    }

    fun getPsychometrics(scaleId: String): Map<String, String> {
        val psych = metaJson?.optJSONObject(scaleId)?.optJSONObject("psychometrics") ?: return emptyMap()
        return mapOf(
            "Cronbach Alpha" to (psych.optString("cronbach_alpha").takeIf { it.isNotEmpty() } ?: "N/A"),
            "Reliability" to (psych.optString("reliability").takeIf { it.isNotEmpty() } ?: "N/A"),
            "Validity" to (psych.optString("validity").takeIf { it.isNotEmpty() } ?: "N/A")
        )
    }
    
    fun getFullName(scaleId: String, locale: String = "en"): String? {
        val fullNameObj = metaJson?.optJSONObject(scaleId)?.optJSONObject("full_name")
        return fullNameObj?.optString(locale) ?: fullNameObj?.optString("en")
    }

    fun getMetadata(scaleId: String): com.clinical.assessment.models.AssessmentMetadata? {
        val scaleNode = metaJson?.optJSONObject(scaleId) ?: return null
        val psych = scaleNode.optJSONObject("psychometrics")
        val bench = scaleNode.optJSONObject("benchmarks")
        
        return com.clinical.assessment.models.AssessmentMetadata(
            assessmentId = scaleId,
            alpha = psych?.optDouble("cronbach_alpha", 0.0) ?: 0.0,
            reliability = psych?.optDouble("reliability", 0.0) ?: 0.0,
            validity = psych?.optString("validity", "0.0") ?: "0.0",
            p50Benchmark = bench?.optDouble("median_50th_percentile", 0.0) ?: 0.0
        )
    }
}
