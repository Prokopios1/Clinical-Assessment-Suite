package com.clinical.assessment.ui.clinician

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clinical.assessment.R

class ClinicianLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clinician_login)
        
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            com.google.firebase.auth.FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            com.clinical.assessment.firebase.FirebaseManager.getUserData(user.uid) { userData ->
                                runOnUiThread {
                                    if (userData?.role == "clinician") {
                                        startActivity(Intent(this@ClinicianLoginActivity, DashboardActivity::class.java))
                                    } else {
                                        Toast.makeText(this@ClinicianLoginActivity, "Account is not a therapist account. Routing to patient portal...", Toast.LENGTH_LONG).show()
                                        startActivity(Intent(this@ClinicianLoginActivity, com.clinical.assessment.ui.patient.HomeActivity::class.java))
                                    }
                                    finish()
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
            val intent = Intent(this, com.clinical.assessment.ui.auth.SignupActivity::class.java)
            intent.putExtra("IS_THERAPIST", true)
            startActivity(intent)
        }
    }
}
