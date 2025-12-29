# Clinician authentication module
import hashlib
import streamlit as st

def hash_password(password):
    """Hash a password using SHA-256"""
    return hashlib.sha256(password.encode()).hexdigest()

def verify_clinician_login(email, password):
    """Verify clinician credentials against secrets"""
    try:
        clinician_email = st.secrets.get("clinician", {}).get("email", "")
        clinician_password_hash = st.secrets.get("clinician", {}).get("password_hash", "")
        
        if email == clinician_email and hash_password(password) == clinician_password_hash:
            return True
    except Exception as e:
        st.error(f"Authentication error: {e}")
    return False

def is_clinician():
    """Check if current user is logged in as clinician"""
    return st.session_state.get("is_clinician", False)

def clinician_login_form():
    """Display clinician login form"""
    st.subheader("🔐 Σύνδεση Κλινικού Ψυχολόγου")
    
    email = st.text_input("Email", key="clinician_email_input")
    password = st.text_input("Κωδικός", type="password", key="clinician_password_input")
    
    col1, col2 = st.columns(2)
    with col1:
        if st.button("Σύνδεση", key="clinician_login_btn"):
            if verify_clinician_login(email, password):
                st.session_state.is_clinician = True
                st.session_state.clinician_email = email
                st.success("Επιτυχής σύνδεση!")
                st.rerun()
            else:
                st.error("Λάθος στοιχεία σύνδεσης")
    
    with col2:
        if st.button("Επιστροφή", key="clinician_back_btn"):
            st.session_state.current_view = "Home"
            st.rerun()

def clinician_logout():
    """Log out clinician"""
    st.session_state.is_clinician = False
    st.session_state.clinician_email = None
    st.session_state.current_view = "Home"
    st.rerun()
