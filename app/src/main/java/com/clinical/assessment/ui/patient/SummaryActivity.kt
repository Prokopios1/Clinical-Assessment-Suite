package com.clinical.assessment.ui.patient

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R
import com.clinical.assessment.data.ScalesData
import com.clinical.assessment.firebase.FirebaseManager
import com.clinical.assessment.models.TestResult

class SummaryActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)
        
        val scaleId = intent.getStringExtra("SCALE_ID") ?: return
        val score = intent.getDoubleExtra("SCORE", 0.0)
        
        val scale = ScalesData.getScale(this, scaleId)
        
        findViewById<TextView>(R.id.tvScaleName).text = scale?.name
        findViewById<TextView>(R.id.tvScore).text = String.format("%.0f", score)
        
        // Deserialize details for domain scores
        val details = intent.getSerializableExtra("DETAILS") as? Map<String, Any>
        
        // Show interpretation based on thresholds
        val matchingThreshold = scale?.thresholds?.find { score >= it.low && score <= it.high }
        
        val sb = StringBuilder()
        
        if (details != null && details.isNotEmpty()) {
            sb.append("Analysis by Domain:\n\n")
            details.forEach { (domain, value) ->
                val v = (value as? Number)?.toDouble() ?: 0.0
                sb.append("$domain: ${String.format("%.1f", v)}\n")
            }
            sb.append("\n")
        }
        
        if (matchingThreshold != null) {
            sb.append(matchingThreshold.label).append("\n\n")
            sb.append(matchingThreshold.interpretation)
        }
        
        if (sb.isNotEmpty()) {
            findViewById<TextView>(R.id.tvInterpretation).text = sb.toString()
        }
        
        // Benchmarking (Task 4)
        com.clinical.assessment.utils.MetadataManager.load(this)
        val benchmark = com.clinical.assessment.utils.MetadataManager.getBenchmark(scaleId)
        if (benchmark != null) {
            val benchmarkText = "\n\nClinical Benchmark (50th Percentile): ${benchmark.toInt()}"
            findViewById<TextView>(R.id.tvInterpretation).append(benchmarkText)
            
            val comparison = if (score > benchmark) {
                "\nYour score is above the general population median."
            } else {
                "\nYour score is at or below the general population median."
            }
            findViewById<TextView>(R.id.tvInterpretation).append(comparison)
        }
        
        // Progress bar (normalized to 0-100)
        val maxScore = scale?.items?.size?.let { it * 3.0 } ?: 27.0
        val progress = ((score / maxScore) * 100).toInt()
        findViewById<ProgressBar>(R.id.progressBar).progress = progress
        
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                FirebaseManager.getUserData(firebaseUser.uid) { user ->
                    if (user != null) {
                        val testResult = TestResult(scaleId, score)
                        FirebaseManager.saveScreening(user, mapOf(scaleId to testResult)) { success ->
                            runOnUiThread {
                                if (success) {
                                    Toast.makeText(this, R.string.saved_msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, R.string.save_error_msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please login to save results.", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnNewAssessment).setOnClickListener {
            finish()
        }
    }
}
