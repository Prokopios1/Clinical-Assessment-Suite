package com.clinical.assessment.ui.patient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R
import com.clinical.assessment.data.ScalesData
import com.clinical.assessment.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.clinical.assessment.firebase.FirebaseManager
import com.clinical.assessment.ui.auth.PatientLoginActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.journeyapps.barcodescanner.ScanIntentResult

class HomeActivity : AppCompatActivity() {
    
    private var currentUserData: UserData? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Task 1: Landing page title set to "Assessments"
        supportActionBar?.title = getString(R.string.available_tests_title)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.title = getString(R.string.available_tests_title)
        
        loadUserData()
        
        val scalesContainer = findViewById<LinearLayout>(R.id.scalesContainer)
        
        // Display all available scales
        ScalesData.getAllScales(this).forEach { scale ->
            val scaleCard = layoutInflater.inflate(R.layout.item_scale_card, scalesContainer, false)
            
            scaleCard.findViewById<TextView>(R.id.id_scale_name).text = scale.name
            scaleCard.findViewById<TextView>(R.id.id_scale_description).text = scale.description
            
            scaleCard.findViewById<android.view.View>(R.id.tv_about_assessment).setOnClickListener {
                val intent = Intent(this, AboutAssessmentActivity::class.java)
                intent.putExtra("SCALE_ID", scale.id)
                startActivity(intent)
            }
            
            scaleCard.findViewById<Button>(R.id.btn_start).setOnClickListener {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    val intent = Intent(this, PatientLoginActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, TestActivity::class.java)
                    intent.putExtra("SCALE_ID", scale.id)
                    startActivity(intent)
                }
            }
            
            scalesContainer.addView(scaleCard)
        }


        handleDeepLink(intent)
        setupLogoutButton()
    }

    private fun setupLogoutButton() {
        findViewById<android.widget.ImageButton>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.continue_btn) { _, _ ->
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, com.clinical.assessment.MainActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadUserData() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            FirebaseManager.getUserData(firebaseUser.uid) { userData ->
                currentUserData = userData
                runOnUiThread {
                    if (userData?.role == "clinician") {
                        startActivity(Intent(this, com.clinical.assessment.ui.clinician.DashboardActivity::class.java))
                        finish()
                    } else {
                        setupButtons(userData)
                    }
                }
            }
        } else {
            setupButtons(null)
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val data = intent.data
            if (data?.scheme == "clinical" && data.host == "connect") {
                val therapistId = data.getQueryParameter("id")
                if (!therapistId.isNullOrEmpty()) {
                    linkTherapist(therapistId)
                }
            }
        }
    }

    private fun setupButtons(user: UserData?) {
        val btnHistory = findViewById<Button>(R.id.btnHistory)
        val btnConnect = findViewById<Button>(R.id.btnConnect)

        btnHistory.setOnClickListener {
            if (user == null) {
                Toast.makeText(this, R.string.error_register_first, Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, HistoryActivity::class.java))
            }
        }

        btnConnect.setOnClickListener {
            startQRScanner()
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedData = result.contents
            // Check if it's a deep link or raw ID
            val therapistId = if (scannedData.startsWith("clinical://")) {
                android.net.Uri.parse(scannedData).getQueryParameter("id")
            } else {
                scannedData
            }
            
            if (therapistId != null) {
                linkTherapist(therapistId)
            } else {
                Toast.makeText(this, R.string.error_invalid_qr, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, R.string.error_cancelled, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startQRScanner() {
        val options = ScanOptions()
        options.setPrompt(getString(R.string.scan_instruction))
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setOrientationLocked(true)
        options.setBeepEnabled(true)
        barcodeLauncher.launch(options)
    }

    private fun linkTherapist(therapistId: String) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            FirebaseManager.updateUserData(firebaseUser.uid, mapOf("therapistId" to therapistId)) { success ->
                runOnUiThread {
                    if (success) {
                        currentUserData = currentUserData?.copy(therapistId = therapistId)
                        Toast.makeText(this, R.string.connect_success, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to connect to therapist.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Patient is not logged in, store the ID and redirect to signup
            val prefs = getSharedPreferences("pending_prefs", MODE_PRIVATE)
            prefs.edit().putString("pending_therapist_id", therapistId).apply()
            
            Toast.makeText(this, "Practitioner ID captured. Please create an account to connect.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, com.clinical.assessment.ui.auth.SignupActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}
