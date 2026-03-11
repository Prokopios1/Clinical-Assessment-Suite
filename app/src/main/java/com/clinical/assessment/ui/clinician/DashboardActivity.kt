package com.clinical.assessment.ui.clinician

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clinical.assessment.R
import com.clinical.assessment.firebase.FirebaseManager
import com.clinical.assessment.models.Patient
import com.clinical.assessment.models.CompletedAssessment
import com.clinical.assessment.utils.QRCodeGenerator
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.Timestamp
import java.util.UUID

class DashboardActivity : AppCompatActivity() {

    private lateinit var adapter: PatientAdapter
    private var allPatients = listOf<Patient>()
    private lateinit var prefs: SharedPreferences
    private lateinit var therapistId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        prefs = getSharedPreferences("clinician_prefs", Context.MODE_PRIVATE)
        therapistId = getOrGenerateTherapistId()

        setupRecyclerView()
        setupSearch()
        setupInviteButton()
        setupLogoutButton()
        loadData()
    }

    private fun setupLogoutButton() {
        findViewById<android.widget.ImageButton>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.continue_btn) { _, _ ->
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                // Clear prefs
                prefs.edit().clear().apply()
                startActivity(Intent(this, com.clinical.assessment.MainActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun getOrGenerateTherapistId(): String {
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val uid = firebaseUser?.uid ?: ""
        
        if (uid.isNotEmpty()) {
            FirebaseManager.getUserData(uid) { userData ->
                if (userData?.therapistId != uid) {
                    FirebaseManager.updateUserData(uid, mapOf("therapistId" to uid)) { }
                }
            }
        }
        return uid
    }

    private fun setupRecyclerView() {
        adapter = PatientAdapter { patient ->
            val intent = Intent(this, PatientDetailActivity::class.java)
            val fullName = "${patient.firstName} ${patient.lastName}".trim()
            intent.putExtra("PATIENT_EMAIL", patient.id)
            intent.putExtra("PATIENT_NAME", fullName)
            startActivity(intent)
        }
        val rv = findViewById<RecyclerView>(R.id.rvPatients)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun setupSearch() {
        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupInviteButton() {
        findViewById<ExtendedFloatingActionButton>(R.id.btnInvitePatient).setOnClickListener {
            showInviteDialog()
        }
    }

    private fun showInviteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_invite, null)
        val ivQr = dialogView.findViewById<ImageView>(R.id.ivQrCode)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        
        // Generate QR with the Therapist ID
        val deepLink = "clinical://connect?id=$therapistId"
        val qrBitmap = QRCodeGenerator.generateQRCode(deepLink)
        ivQr.setImageBitmap(qrBitmap)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadData() {
        FirebaseManager.getTherapistPatients(therapistId) { userList ->
            if (userList.isEmpty()) {
                runOnUiThread {
                    allPatients = emptyList()
                    adapter.submitList(allPatients)
                    Toast.makeText(this@DashboardActivity, R.string.no_linked_patients, Toast.LENGTH_LONG).show()
                }
                return@getTherapistPatients
            }

            // For each user, fetch their history.
            val patientObjects = mutableListOf<Patient>()
            var fetchedCount = 0

            for (userData in userList) {
                val email = userData["email"] as? String ?: "unknown"
                val name = userData["name"] as? String ?: ""
                val fullNameParts = name.split(" ")
                val firstName = fullNameParts.firstOrNull() ?: ""
                val lastName = if (fullNameParts.size > 1) fullNameParts.drop(1).joinToString(" ") else ""

                FirebaseManager.getUserScreenings(email) { screenings ->
                    val history = screenings.mapNotNull { screening ->
                        try {
                            val rawTimestamp = screening["timestamp"]
                            val timestampDate = when (rawTimestamp) {
                                is com.google.firebase.Timestamp -> rawTimestamp.toDate()
                                is Long -> java.util.Date(rawTimestamp)
                                is String -> try {
                                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).parse(rawTimestamp)
                                } catch (e: Exception) { java.util.Date() }
                                else -> java.util.Date()
                            }
                            
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(timestampDate)
                            val id = (screening["id"] as? String) ?: java.util.UUID.randomUUID().toString()

                            val resultsMap = screening["results"] as? Map<String, Map<String, Any>>
                            val assessments = mutableListOf<CompletedAssessment>()
                            resultsMap?.forEach { (type, res) ->
                                val score = (res["score"] as? Number)?.toDouble() ?: 0.0
                                assessments.add(CompletedAssessment(id = id, date = dateStr, score = score, type = type))
                            }
                            assessments
                        } catch (e: Exception) {
                            android.util.Log.e("DashboardActivity", "Error parsing screening", e)
                            null
                        }
                    }.flatten()

                    val patient = Patient(id = email, firstName = firstName, lastName = lastName, history = history)
                    patientObjects.add(patient)

                    fetchedCount++
                    if (fetchedCount == userList.size) {
                        runOnUiThread {
                            allPatients = patientObjects.sortedByDescending { p -> p.history.maxByOrNull { a -> a.date }?.date ?: "" }
                            adapter.submitList(allPatients)
                        }
                    }
                }
            }
        }
    }

    private fun filter(query: String) {
        val filtered = allPatients.filter { 
            "${it.firstName} ${it.lastName}".contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
    }


}
