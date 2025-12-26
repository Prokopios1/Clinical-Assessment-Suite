import firebase_admin
from firebase_admin import credentials, firestore
import datetime
import time

# Mocking streamlit secrets for local run if needed, or relying on database.py logic
# But database.py relies on streamlit.secrets. 
# We should probably use the same logic as test_firebase.py to initialize.

import os
import toml
import json

def init_test_db():
    if not firebase_admin._apps:
        # Try to load from secrets.toml like before
        secrets_path = os.path.join(".streamlit", "secrets.toml")
        if os.path.exists(secrets_path):
            with open(secrets_path, "r") as f:
                secrets = toml.load(f)
                if "firebase" in secrets and "service_account" in secrets["firebase"]:
                    key_dict = json.loads(secrets["firebase"]["service_account"])
                    cred = credentials.Certificate(key_dict)
                    firebase_admin.initialize_app(cred)
                    return firestore.client()
    
    # Fallback if already initialized or other method
    try:
        return firestore.client()
    except ValueError:
        return None

def verify_backend():
    print("Initializing DB...")
    db = init_test_db()
    if not db:
        print("Failed to initialize DB. Check secrets.")
        return

    test_email = "verify_backend@example.com"
    test_timestamp = datetime.datetime.now()
    
    # 1. Write a new screening
    print(f"Writing test screening for {test_email}...")
    doc_ref = db.collection("screenings").document()
    doc_ref.set({
        "user": {
            "name": "Verification Bot",
            "email": test_email,
            "gdpr": True
        },
        "results": {
            "TEST-SCALE": {
                "score": 99,
                "details": "Automated verification",
                "timestamp": test_timestamp.isoformat()
            }
        },
        "timestamp": firestore.SERVER_TIMESTAMP
    })
    print("Write successful.")

    # 2. Read it back using the same logic as get_user_screenings
    print("Reading back history...")
    # Allow a moment for consistency if needed, though Firestore is usually strong consistency for simple reads, 
    # but queries might be eventually consistent? Indexes should be fine.
    time.sleep(2) 
    
    docs = db.collection("screenings")\
             .where("user.email", "==", test_email)\
             .stream()
    
    results = [doc.to_dict() for doc in docs]
    results.sort(key=lambda x: x.get("timestamp", ""), reverse=True)
    
    if not results:
        print("❌ No results found! Read failed.")
    else:
        print(f"Found {len(results)} results.")
        latest = results[0]
        if latest["user"]["email"] == test_email and "TEST-SCALE" in latest["results"]:
            print("✅ Verification Successful: Data matches.")
        else:
            print("❌ Data mismatch.")
            print(latest)

    # Cleanup (Optional)
    # print("Cleaning up test data...")
    # for doc in docs: 
    #    doc.reference.delete()

if __name__ == "__main__":
    verify_backend()
