package com.clinical.assessment.ui.clinician

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R

class PatientListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clinician_placeholder)
        findViewById<TextView>(R.id.tvPlaceholder).text = "Patient List - Coming Soon"
    }
}
