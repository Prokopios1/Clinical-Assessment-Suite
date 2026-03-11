package com.clinical.assessment.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R
import com.clinical.assessment.ui.patient.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class PatientLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_login)
        
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            com.clinical.assessment.firebase.FirebaseManager.getUserData(user.uid) { userData ->
                                runOnUiThread {
                                    if (userData != null) {
                                        if (userData.role == "clinician") {
                                            startActivity(Intent(this, com.clinical.assessment.ui.clinician.DashboardActivity::class.java))
                                        } else {
                                            startActivity(Intent(this, HomeActivity::class.java))
                                        }
                                        finish()
                                    } else {
                                        startActivity(Intent(this, HomeActivity::class.java))
                                        finish()
                                    }
                                }
                            }
                        }
                    } else {
                        val exception = task.exception
                        val errorMsg = when (exception) {
                            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found with this email."
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect password or email."
                            else -> exception?.message ?: "Login failed"
                        }
                        Toast.makeText(this, "Login failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
        }

        findViewById<Button>(R.id.btnSignupRedirect).setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            intent.putExtra("IS_THERAPIST", false)
            startActivity(intent)
        }
    }
}
