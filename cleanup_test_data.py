
import firebase_admin
from firebase_admin import credentials, firestore
import toml
import json
import os

def cleanup():
    # Initialize (copied from verify_backend.py/test_firebase.py logic)
    if not firebase_admin._apps:
        secrets_path = os.path.join(".streamlit", "secrets.toml")
        if os.path.exists(secrets_path):
            with open(secrets_path, "r") as f:
                secrets = toml.load(f)
                if "firebase" in secrets and "service_account" in secrets["firebase"]:
                    key_dict = json.loads(secrets["firebase"]["service_account"])
                    cred = credentials.Certificate(key_dict)
                    firebase_admin.initialize_app(cred)
    
    db = firestore.client()
    
    # 1. Cleanup verify_backend data
    test_email = "verify_backend@example.com"
    print(f"Cleaning up screenings for {test_email}...")
    docs = db.collection("screenings").where("user.email", "==", test_email).stream()
    count = 0
    for doc in docs:
        doc.reference.delete()
        count += 1
    print(f"Deleted {count} screening documents.")

    # 2. Cleanup test_firebase data
    print("Cleaning up test_connection/verification...")
    doc_ref = db.collection("test_connection").document("verification")
    if doc_ref.get().exists:
        doc_ref.delete()
        print("Deleted test_connection/verification.")
    else:
        print("test_connection/verification not found.")

if __name__ == "__main__":
    cleanup()
