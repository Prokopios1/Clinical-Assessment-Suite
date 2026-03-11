package com.clinical.assessment.firebase

import com.clinical.assessment.models.Screening
import com.clinical.assessment.models.TestResult
import com.clinical.assessment.models.UserData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    
    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    
    fun saveScreening(user: UserData, results: Map<String, TestResult>, therapistId: String? = null, customTimestamp: java.util.Date? = null, callback: (Boolean) -> Unit) {
        val screening = hashMapOf(
            "user" to hashMapOf(
                "name" to user.name,
                "email" to user.email,
                "gdpr" to user.gdprConsent,
                "therapistId" to (therapistId ?: user.therapistId)
            ),
            "results" to results.mapValues { (_, result) ->
                hashMapOf(
                    "score" to result.score,
                    "timestamp" to result.timestamp,
                    "details" to (result.details ?: emptyMap<String, Any>())
                )
            },
            "timestamp" to (customTimestamp ?: com.google.firebase.firestore.FieldValue.serverTimestamp())
        )
        
        db.collection("screenings")
            .add(screening)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
    
    fun getUserScreenings(email: String, callback: (List<Map<String, Any>>) -> Unit) {
        db.collection("screenings")
            .whereEqualTo("user.email", email)
            .get()
            .addOnSuccessListener { documents ->
                val screenings = documents.map { it.data }
                callback(screenings)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    fun getTherapistPatients(therapistId: String, callback: (List<Map<String, Any>>) -> Unit) {
        db.collection("users")
            .whereEqualTo("therapistId", therapistId)
            .get()
            .addOnSuccessListener { documents ->
                val patients = documents.map { doc ->
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    data
                }
                callback(patients)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirebaseManager", "Failed to get patients: ${e.message}", e)
                callback(emptyList())
            }
    }

    fun getAllScreenings(callback: (List<Map<String, Any>>) -> Unit) {
        db.collection("screenings")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val screenings = documents.map { it.data }
                callback(screenings)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    fun getUserData(uid: String, callback: (UserData?) -> Unit) {
        android.util.Log.d("FirebaseManager", "Fetching user data for $uid")
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    android.util.Log.d("FirebaseManager", "User data found for $uid")
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    val gdpr = document.getBoolean("gdprConsent") ?: false
                    val therapistId = document.getString("therapistId") ?: ""
                    val role = document.getString("role") ?: "patient"
                    callback(UserData(name, email, gdpr, therapistId, role))
                } else {
                    android.util.Log.w("FirebaseManager", "No user data document for $uid")
                    callback(null)
                }
            }
            .addOnFailureListener {
                android.util.Log.e("FirebaseManager", "Failed to get user data for $uid: ${it.message}", it)
                callback(null)
            }
    }
    
    fun updateUserData(uid: String, updates: Map<String, Any>, callback: (Boolean) -> Unit) {
        android.util.Log.d("FirebaseManager", "Updating user data for $uid: $updates")
        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { 
                android.util.Log.d("FirebaseManager", "Update success for $uid")
                callback(true) 
            }
            .addOnFailureListener { 
                android.util.Log.e("FirebaseManager", "Update failed for $uid: ${it.message}", it)
                callback(false) 
            }
    }
}
