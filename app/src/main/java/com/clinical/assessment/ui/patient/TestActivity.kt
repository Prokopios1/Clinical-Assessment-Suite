package com.clinical.assessment.ui.patient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R
import com.clinical.assessment.data.ScalesData
import com.clinical.assessment.models.Scale
import com.clinical.assessment.models.TestResult

class TestActivity : AppCompatActivity() {
    
    private lateinit var scale: Scale
    private val responses = mutableListOf<String?>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        
        val scaleId = intent.getStringExtra("SCALE_ID") ?: return
        scale = ScalesData.getScale(this, scaleId) ?: return
        
        title = scale.name
        
        findViewById<TextView>(R.id.assessmentTitle).text = scale.name
        
        val questionsContainer = findViewById<LinearLayout>(R.id.questionsContainer)
        
        // Initialize responses list
        repeat(scale.items.size) { responses.add(null) }
        
        // Create question views
        scale.items.forEachIndexed { index, question ->
            val questionView = layoutInflater.inflate(R.layout.item_question, questionsContainer, false)
            
            questionView.findViewById<TextView>(R.id.questionText).text = "${index + 1}. $question"
            
            val radioGroup = questionView.findViewById<RadioGroup>(R.id.radioGroup)
            radioGroup.orientation = RadioGroup.HORIZONTAL
            
            // Add radio buttons for options
            scale.options.forEach { option ->
                val radioButton = RadioButton(this)
                radioButton.text = scale.labels?.get(option) ?: option
                radioButton.tag = option
                radioGroup.addView(radioButton)
                
                radioButton.setOnClickListener {
                    responses[index] = option
                }
            }
            // Ensure Vertical orientation as requested
            radioGroup.orientation = RadioGroup.VERTICAL
            
            questionsContainer.addView(questionView)
        }
        
        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            // Check all questions answered
            if (responses.any { it == null }) {
                Toast.makeText(this, R.string.error_answer_all, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Calculate score
            val (score, details) = calculateResult()
            
            // Navigate to summary
            val intent = Intent(this, SummaryActivity::class.java)
            intent.putExtra("SCALE_ID", scale.id)
            intent.putExtra("SCORE", score)
            if (details != null && details is java.io.Serializable) {
                intent.putExtra("DETAILS", details as java.io.Serializable)
            }
            startActivity(intent)
            finish()
        }
    }
    
    private fun calculateResult(): Pair<Double, Map<String, Any>?> {
        return when (scale.scoring) {
            com.clinical.assessment.models.ScoringType.WEIGHTED_SUM -> {
                val score = responses.sumOf { response ->
                    scale.weights?.get(response) ?: 0.0
                }
                Pair(score, null)
            }
            com.clinical.assessment.models.ScoringType.SUM -> {
                // Assuming options are numbers or have weights. Since we defined weights for MSI-BPD/PQ-B in ScalesData, use them.
                val score = responses.sumOf { response ->
                    scale.weights?.get(response) ?: 0.0
                }
                Pair(score, null)
            }
            com.clinical.assessment.models.ScoringType.COUNT_YES -> {
                 // Counts logic if needed, but we migrated PQ-B to SUM/Weights. 
                 // Keeping fallback if needed, traditionally 'Yes' check.
                 val score = responses.count { it?.equals("Yes", ignoreCase = true) == true || it == "1" }.toDouble()
                 Pair(score, null)
            }
            com.clinical.assessment.models.ScoringType.MEAN_PER_DOMAIN_WEIGHTED -> {
                val domains = scale.domains ?: return Pair(0.0, null)
                val domainScores = mutableMapOf<String, Double>()
                
                domains.forEach { (domainName, indices) ->
                    val domainSum = indices.sumOf { index ->
                        if (index < responses.size) {
                            val response = responses[index]
                            scale.weights?.get(response) ?: 0.0
                        } else 0.0
                    }
                    val domainMean = if (indices.isNotEmpty()) domainSum / indices.size else 0.0
                    domainScores[domainName] = domainMean
                }
                
                // For the main score, maybe return the highest domain or average?
                // Or just 0.0 if not applicable. Let's return the KEY domain if possible, or MAX.
                // Usually PID-5 doesn't have a total score. We'll return 0.0 or max mean.
                val maxScore = domainScores.values.maxOrNull() ?: 0.0
                Pair(maxScore, domainScores)
            }
        }
    }
}
