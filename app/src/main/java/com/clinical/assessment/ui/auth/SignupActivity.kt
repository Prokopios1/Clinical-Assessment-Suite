package com.clinical.assessment.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R
import com.clinical.assessment.models.UserData
import com.clinical.assessment.ui.clinician.DashboardActivity
import com.clinical.assessment.ui.patient.HomeActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val swIsTherapist = findViewById<SwitchMaterial>(R.id.swIsTherapist)
        val cbGdpr = findViewById<CheckBox>(R.id.cbGdpr)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val btnLoginRedirect = findViewById<Button>(R.id.btnLoginRedirect)

        // Set therapist switch based on intent if provided
        val isTherapistIntent = intent.getBooleanExtra("IS_THERAPIST", false)
        swIsTherapist.isChecked = isTherapistIntent

        btnSignup.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val isTherapist = swIsTherapist.isChecked
            val gdprAccepted = cbGdpr.isChecked

            android.util.Log.d("SignupActivity", "Signup attempt: email=$email, isTherapist=$isTherapist")

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!gdprAccepted) {
                Toast.makeText(this, "Please accept GDPR consent", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignup.isEnabled = false
            android.util.Log.d("SignupActivity", "Calling createUserWithEmailAndPassword...")
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = task.result?.user?.uid
                        android.util.Log.d("SignupActivity", "Auth success. UID: $userId")
                        if (userId != null) {
                            saveUserToFirestore(userId, name, email, gdprAccepted, isTherapist)
                        } else {
                            btnSignup.isEnabled = true
                            android.util.Log.e("SignupActivity", "Auth success but UID is null!")
                            Toast.makeText(this, "Failed to get user ID. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        btnSignup.isEnabled = true
                        val exception = task.exception
                        val errorMsg = when (exception) {
                            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "This email address is already in use."
                            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "The password is too weak."
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "The email address is badly formatted."
                            else -> exception?.message ?: "Unknown error"
                        }
                        android.util.Log.e("SignupActivity", "Auth failed: $errorMsg", exception)
                        Toast.makeText(this, "Signup failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    btnSignup.isEnabled = true
                    android.util.Log.e("SignupActivity", "Auth exception: ${it.message}", it)
                    Toast.makeText(this, "Connection error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnLoginRedirect.setOnClickListener {
            finish()
        }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String, gdpr: Boolean, isTherapist: Boolean) {
        android.util.Log.d("SignupActivity", "Saving to Firestore: uid=$uid, role=${if (isTherapist) "clinician" else "patient"}")
        
        var therapistId = if (isTherapist) {
            java.util.UUID.randomUUID().toString().substring(0, 8).uppercase()
        } else ""

        // Check for deferred linking from QR scan or deep link
        if (!isTherapist) {
            val prefs = getSharedPreferences("pending_prefs", android.content.Context.MODE_PRIVATE)
            val pendingId = prefs.getString("pending_therapist_id", "")
            if (!pendingId.isNullOrEmpty()) {
                therapistId = pendingId
                prefs.edit().remove("pending_therapist_id").apply()
                android.util.Log.d("SignupActivity", "Applying deferred therapistId: $therapistId")
            }
        }

        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "gdprConsent" to gdpr,
            "role" to if (isTherapist) "clinician" else "patient",
            "therapistId" to therapistId
        )

        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                android.util.Log.d("SignupActivity", "Firestore save success for $uid")
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                if (isTherapist) {
                    val prefs = getSharedPreferences("clinician_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("therapist_id", therapistId).apply()
                    startActivity(Intent(this, DashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
                finishAffinity()
            }
            .addOnFailureListener { e ->
                findViewById<Button>(R.id.btnSignup).isEnabled = true
                android.util.Log.e("SignupActivity", "Firestore save failed: ${e.message}", e)
                
                val errorMsg = if (e.message?.contains("permission", ignoreCase = true) == true) {
                    "Database permission denied. Please check Firestore Security Rules in Firebase Console."
                } else {
                    "Failed to save user info: ${e.message}"
                }
                
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()

                // Rollback: Delete the Auth user so they can retry with the same email
                auth.currentUser?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d("SignupActivity", "Rollback success: Auth user deleted.")
                    } else {
                        android.util.Log.e("SignupActivity", "Rollback failed: ${task.exception?.message}")
                    }
                }
            }
    }
}
