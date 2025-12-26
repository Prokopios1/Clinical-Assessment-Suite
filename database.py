import firebase_admin
from firebase_admin import credentials, firestore
import streamlit as st
import json

import os

def init_db():
    if not firebase_admin._apps:
        try:
            # Try Streamlit Secrets first
            if "firebase" in st.secrets:
                key_dict = json.loads(st.secrets["firebase"]["service_account"])
                cred = credentials.Certificate(key_dict)
                firebase_admin.initialize_app(cred)
            # Fallback to local file if it exists (for local dev)
            elif os.path.exists("serviceAccountKey.json"):
                cred = credentials.Certificate("serviceAccountKey.json")
                firebase_admin.initialize_app(cred)
            else:
                # Use Application Default Credentials (ADC) for Cloud
                firebase_admin.initialize_app()
                
        except Exception as e:
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
