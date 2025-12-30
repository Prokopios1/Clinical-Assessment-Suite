import firebase_admin
from firebase_admin import credentials, firestore
import streamlit as st
import json

import os

def init_db():
    if not firebase_admin._apps:
        try:
            # 1. Try FIREBASE environment variable (for App Hosting deployment)
            if "FIREBASE" in os.environ:
                # print("Loading from FIREBASE env var...") # Debug
                key_dict = json.loads(os.environ["FIREBASE"])
                cred = credentials.Certificate(key_dict)
                firebase_admin.initialize_app(cred)
            # 2. Try Streamlit Secrets (for local dev with .streamlit/secrets.toml)
            elif "firebase" in st.secrets:
                # print("Loading from Streamlit secrets...") # Debug
                key_dict = json.loads(st.secrets["firebase"]["service_account"])
                cred = credentials.Certificate(key_dict)
                firebase_admin.initialize_app(cred)
            # 3. Fallback to local file if it exists (legacy local dev)
            elif os.path.exists("serviceAccountKey.json"):
                # print("Loading from local serviceAccountKey.json...") # Debug
                cred = credentials.Certificate("serviceAccountKey.json")
                firebase_admin.initialize_app(cred)
            else:
                # 4. Use Application Default Credentials (ADC) as last resort
                # print("Loading with ADC...") # Debug
                firebase_admin.initialize_app()
                
        except Exception as e:
            # Print to console for cloud logs
            print(f"Firebase initialization error: {e}")
            st.error(f"Σφάλμα σύνδεσης με τη βάση δεδομένων: {e}")
            return None
            
    return firestore.client()


def save_screening(user_data, results):
    db = init_db()
    if db:
        try:
            doc_ref = db.collection("screenings").document()
            doc_ref.set({
                "user": user_data,
                "results": results,
                "timestamp": firestore.SERVER_TIMESTAMP
            })
            return True
        except Exception as e:
            st.error(f"Σφάλμα κατά την αποθήκευση: {e}")
            return False
    return False

def get_user_screenings(email):
    """Retrieve all screening results for a specific user email."""
    db = init_db()
    if not db:
        return []
    
    try:
        docs = db.collection("screenings")\
                 .where("user.email", "==", email)\
                 .stream()
        
        results = [doc.to_dict() for doc in docs]
        # Sort in memory to avoid composite index requirement
        results.sort(key=lambda x: x.get("timestamp", ""), reverse=True)
        return results
    except Exception as e:
        st.error(f"Σφάλμα κατά την ανάκτηση ιστορικού: {e}")
        return []

# ===== CLINICIAN FUNCTIONS =====

def get_all_patients():
    """Get list of all unique patients who have taken assessments."""
    db = init_db()
    if not db:
        return []
    
    try:
        docs = db.collection("screenings").stream()
        patients = {}
        
        for doc in docs:
            data = doc.to_dict()
            user = data.get("user", {})
            email = user.get("email")
            
            if email and email not in patients:
                patients[email] = {
                    "email": email,
                    "name": user.get("name", "Άγνωστο"),
                    "first_assessment": data.get("timestamp"),
                    "assessment_count": 0
                }
            
            if email:
                patients[email]["assessment_count"] += 1
                # Keep track of earliest assessment
                doc_time = data.get("timestamp")
                if doc_time and (not patients[email]["first_assessment"] or doc_time < patients[email]["first_assessment"]):
                    patients[email]["first_assessment"] = doc_time
        
        return list(patients.values())
    except Exception as e:
        st.error(f"Σφάλμα: {e}")
        return []

def get_all_screenings():
    """Get all screening results for analytics."""
    db = init_db()
    if not db:
        return []
    
    try:
        docs = db.collection("screenings").stream()
        results = [doc.to_dict() for doc in docs]
        return results
    except Exception as e:
        st.error(f"Σφάλμα: {e}")
        return []

def calculate_test_statistics(scale_id):
    """Calculate statistics for a specific test across all users."""
    screenings = get_all_screenings()
    scores = []
    
    for screening in screenings:
        results = screening.get("results", {})
        if scale_id in results:
            score = results[scale_id].get("score")
            if score is not None:
                scores.append(score)
    
    if not scores:
        return None
    
    import statistics
    return {
        "count": len(scores),
        "mean": statistics.mean(scores),
        "median": statistics.median(scores),
        "stdev": statistics.stdev(scores) if len(scores) > 1 else 0,
        "min": min(scores),
        "max": max(scores)
    }

def get_patient_with_stats(email):
    """Get patient results with comparison to population."""
    screenings = get_user_screenings(email)
    
    # Calculate percentiles for each test
    for screening in screenings:
        results = screening.get("results", {})
        for scale_id, data in results.items():
            stats = calculate_test_statistics(scale_id)
            if stats:
                score = data.get("score")
                # Calculate percentile
                all_scores = []
                for s in get_all_screenings():
                    s_results = s.get("results", {})
                    if scale_id in s_results:
                        all_scores.append(s_results[scale_id].get("score"))
                
                if all_scores and score is not None:
                    percentile = sum(1 for s in all_scores if s <= score) / len(all_scores) * 100
                    data["percentile"] = round(percentile, 1)
                    data["population_mean"] = stats["mean"]
                    data["population_stdev"] = stats["stdev"]
    
    return screenings
