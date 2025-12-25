import firebase_admin
from firebase_admin import credentials, firestore
import toml
import json
import os

def test_connection():
    try:
        # Load secrets from .streamlit/secrets.toml
        secrets_path = os.path.join(".streamlit", "secrets.toml")
        if not os.path.exists(secrets_path):
            print(f"Error: {secrets_path} not found.")
            return

        with open(secrets_path, "r") as f:
            secrets = toml.load(f)
        
        if "firebase" not in secrets or "service_account" not in secrets["firebase"]:
            print("Error: 'firebase.service_account' not found in secrets.toml")
            return

        sa_content = secrets["firebase"]["service_account"]
        service_account_info = json.loads(sa_content)
        cred = credentials.Certificate(service_account_info)
        
        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred)
        
        db = firestore.client()
        
        # Test write
        print("Attempting to write test document...")
        doc_ref = db.collection("test_connection").document("verification")
        doc_ref.set({
            "status": "success",
            "timestamp": firestore.SERVER_TIMESTAMP,
            "message": "Local connection test successful with new valid key"
        })
        print("✓ Successfully wrote to Firestore!")
        
        # Test read
        print("Attempting to read test document...")
        doc = doc_ref.get()
        if doc.exists:
            print(f"✓ Successfully read from Firestore: {doc.to_dict()}")
        else:
            print("✗ Failed to read back the document.")

    except Exception as e:
        print(f"✗ An error occurred: {e}")

if __name__ == "__main__":
    test_connection()
