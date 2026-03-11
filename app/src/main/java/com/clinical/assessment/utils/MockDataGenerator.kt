package com.clinical.assessment.utils

import android.content.Context
import com.clinical.assessment.data.ScalesData
import com.clinical.assessment.firebase.FirebaseManager
import com.clinical.assessment.models.TestResult
import com.clinical.assessment.models.UserData
import java.util.Date
import kotlin.random.Random

object MockDataGenerator {

    private val firstNames = listOf("John", "Jane", "Alice", "Bob", "Charlie", "Diana", "Evan", "Fiona", "George", "Helen")
    private val lastNames = listOf("Doe", "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Garcia")
    private val risks = listOf("Low", "Moderate", "High", "Severe")

    fun generateRandomPatient(context: Context, callback: (Boolean) -> Unit) {
        val name = "${firstNames.random()} ${lastNames.random()}"
        val email = "${name.replace(" ", "").lowercase()}${Random.nextInt(1000, 9999)}@example.com"
        val user = UserData(name, email, true)

        // Generate 5 historical entries for this user
        generateHistory(user, context, 5, callback)
    }

    private fun generateHistory(user: UserData, context: Context, count: Int, callback: (Boolean) -> Unit) {
        if (count <= 0) {
            callback(true)
            return
        }

        val scales = ScalesData.getAllScales(context)
        val results = mutableMapOf<String, TestResult>()
        
        // Random date in the past (staggered)
        // e.g., count 5 -> 5 months ago
        // count 1 -> 1 week ago
        val daysAgo = count * 30L // Roughly 1 month apart
        val timestamp = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000)
        
        scales.forEach { scale ->
            val max = when(scale.id) {
                "PHQ-9" -> 27.0
                "GAD-7" -> 21.0
                "PID-5-BF" -> 3.0 // Mean
                "MSI-BPD" -> 10.0
                "PQ-B" -> 21.0
                else -> 20.0
            }
            
            // Random score with some variance but somewhat consistent per user to show trends?
            // Let's just do random for now.
            val score = if(scale.id == "PID-5-BF") Random.nextDouble(0.0, 3.0) else Random.nextInt(0, max.toInt()).toDouble()
            
            results[scale.id] = TestResult(scale.id, score, null, timestamp)
        }

        FirebaseManager.saveScreening(user, results, null, Date(timestamp)) { success ->
            // Recursive call for next history item
            generateHistory(user, context, count - 1, callback)
        }
    }
}
