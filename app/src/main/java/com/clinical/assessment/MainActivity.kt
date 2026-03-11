package com.clinical.assessment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.ui.patient.HomeActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto-redirect if already logged in
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            android.util.Log.d("MainActivity", "User already logged in: ${firebaseUser.uid}. Checking role...")
            com.clinical.assessment.firebase.FirebaseManager.getUserData(firebaseUser.uid) { userData ->
                runOnUiThread {
                    android.util.Log.d("MainActivity", "Role fetched: ${userData?.role}")
                    val prefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
                    val lastCrash = prefs.getString("last_crash", null)
                    if (lastCrash != null) {
                        prefs.edit().remove("last_crash").apply()
                        android.app.AlertDialog.Builder(this)
                            .setTitle("App Crashed Previously")
                            .setMessage("Please copy this text and share it:\n\n$lastCrash")
                            .setPositiveButton("OK") { _, _ ->
                                navigateNext(userData?.role)
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        navigateNext(userData?.role)
                    }
                }
            }
            return // Skip setting layout if redirecting
        } else {
            setupWithoutUser()
        }
    }

    private fun navigateNext(role: String?) {
        if (role == "clinician") {
            startActivity(Intent(this, com.clinical.assessment.ui.clinician.DashboardActivity::class.java))
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }

    private fun checkCrashAndShow() {
        val prefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)
        if (lastCrash != null) {
            prefs.edit().remove("last_crash").apply()
            android.app.AlertDialog.Builder(this)
                .setTitle("App Crashed Previously")
                .setMessage("Please copy this text and share it:\n\n$lastCrash")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setupWithoutUser() {

        android.util.Log.d("MainActivity", "No user logged in. Showing main selector.")
        setContentView(R.layout.activity_main)
        checkCrashAndShow()
        
        val patientButton = findViewById<Button>(R.id.btnPatient)
        
        patientButton.setOnClickListener {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                startActivity(Intent(this, com.clinical.assessment.ui.auth.PatientLoginActivity::class.java))
            }
        }

        val clinicianButton = findViewById<Button>(R.id.btnClinician)
        clinicianButton.setOnClickListener {
            startActivity(Intent(this, com.clinical.assessment.ui.clinician.ClinicianLoginActivity::class.java))
        }

        findViewById<Button>(R.id.btnLangEn).setOnClickListener { setLocale("en") }
        findViewById<Button>(R.id.btnLangEs).setOnClickListener { setLocale("es") }
        findViewById<Button>(R.id.btnLangEl).setOnClickListener { setLocale("el") }
    }

    private fun setLocale(languageCode: String) {
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }
}
