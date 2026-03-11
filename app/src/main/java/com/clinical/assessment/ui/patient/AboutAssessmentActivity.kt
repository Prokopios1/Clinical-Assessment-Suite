package com.clinical.assessment.ui.patient

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R
import com.clinical.assessment.utils.MetadataManager

class AboutAssessmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_assessment)

        val scaleId = intent.getStringExtra("SCALE_ID") ?: "PHQ-9"
        MetadataManager.load(this)
        
        val fullName = MetadataManager.getFullName(scaleId)
        findViewById<TextView>(R.id.tvTitle).text = fullName ?: scaleId
        
        // Populate Psychometrics using AssessmentMetadata schema (Task 3)
        val metadata = MetadataManager.getMetadata(scaleId)
        if (metadata != null) {
            val psychText = "This assessment shows strong psychometric properties. " +
                "It has a Cronbach's alpha of ${metadata.alpha}, indicating its internal consistency. " +
                "The overall reliability score is ${metadata.reliability}. " +
                "Its clinical validity score is ${metadata.validity}."
            
            findViewById<TextView>(R.id.tvPsychometrics).text = psychText
            findViewById<TextView>(R.id.tvBenchmark).text = "Clinical Benchmark (50th Percentile): ${metadata.p50Benchmark}"
        } else {
            findViewById<TextView>(R.id.tvPsychometrics).text = "Psychometric properties are not available for this scale."
            findViewById<TextView>(R.id.tvBenchmark).text = "Clinical Benchmark (50th Percentile): N/A"
        }

        findViewById<TextView>(R.id.tvDescription).text = "This assessment is a scientifically validated tool used by clinicians to measure patient symptoms and track progress over time. It is part of a comprehensive clinical evaluation."
        
        findViewById<android.widget.Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
