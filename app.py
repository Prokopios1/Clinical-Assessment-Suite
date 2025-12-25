import streamlit as st
import pandas as pd
from scales import SCALES
from database import save_screening
import datetime

# --- CONFIGURATION ---
st.set_page_config(
    page_title="Clinical Screening App",
    page_icon="🧠",
    layout="centered",
    initial_sidebar_state="collapsed",
)

# Load CSS
with open("styles.css") as f:
    st.markdown(f"<style>{f.read()}</style>", unsafe_allow_html=True)

# --- SESSION STATE ---
if "user_data" not in st.session_state:
    st.session_state.user_data = None
if "test_results" not in st.session_state:
    st.session_state.test_results = {}
if "current_scale" not in st.session_state:
    st.session_state.current_scale = "Registration"

# --- HELPER FUNCTIONS ---
def save_results(scale_id, score, details=None):
    st.session_state.test_results[scale_id] = {
        "score": score,
        "details": details,
        "timestamp": datetime.datetime.now().isoformat()
    }

def mock_data():
    st.session_state.user_data = {
        "name": "Δοκιμαστικός Χρήστης",
        "email": "test@example.com",
        "gdpr": True
    }
    st.rerun()

# --- UI COMPONENTS ---

def registration_page():
    st.title("Εγγραφή Χρήστη")
    with st.form("reg_form"):
        name = st.text_input("Ονοματεπώνυμο")
        email = st.text_input("Email")
        gdpr = st.checkbox("Συμφωνώ με την επεξεργασία των δεδομένων μου για κλινικούς σκοπούς (GDPR)")
        
        submit = st.form_submit_button("Έναρξη Αξιολόγησης")
        if submit:
            if not name or not email:
                st.error("Παρακαλώ συμπληρώστε όλα τα πεδία.")
            elif not gdpr:
                st.warning("Πρέπει να συμφωνήσετε με τους όρους GDPR.")
            else:
                st.session_state.user_data = {"name": name, "email": email, "gdpr": gdpr}
                st.session_state.current_scale = "PHQ-9"
                st.rerun()

def scale_page(scale_id):
    scale = SCALES[scale_id]
    st.title(scale["name"])
    
    responses = []
    
    with st.form(f"form_{scale_id}"):
        for i, item in enumerate(scale["items"]):
            if scale["scoring"] == "count_yes" or scale_id == "MSI-BPD":
                res = st.radio(item, options=scale["options"], horizontal=True, key=f"{scale_id}_{i}")
                responses.append(1 if res == "Ναι" else 0)
            else:
                res = st.select_slider(item, options=scale["options"], key=f"{scale_id}_{i}")
                responses.append(scale["options"].index(res))
        
        submit = st.form_submit_button("Υποβολή")
        
        if submit:
            if scale["scoring"] == "sum":
                score = sum(responses)
            elif scale["scoring"] == "count_yes":
                score = sum(responses)
            elif scale["scoring"] == "mean_per_domain":
                # Basic sum for PID-5-BF if domain mapping is complex for this view
                score = sum(responses) / len(responses)
            
            save_results(scale_id, score)
            
            # Navigate to next scale
            scale_order = list(SCALES.keys())
            current_index = scale_order.index(scale_id)
            if current_index < len(scale_order) - 1:
                st.session_state.current_scale = scale_order[current_index + 1]
            else:
                st.session_state.current_scale = "Summary"
            st.rerun()

def summary_page():
    st.title("Σύνοψη Αποτελεσμάτων")
    st.write(f"Χρήστης: {st.session_state.user_data['name']}")
    
    for scale_id, data in st.session_state.test_results.items():
        scale = SCALES[scale_id]
        score = data["score"]
        st.subheader(scale["name"])
        
        if scale["scoring"] == "sum":
            max_val = (len(scale["items"]) * (len(scale["options"]) - 1))
            st.progress(score / max_val)
            st.write(f"Σκορ: {score} / {max_val}")
            
            # Interpretation
            if "thresholds" in scale:
                for low, high, label in scale["thresholds"]:
                    if low <= score <= high:
                        st.info(f"Ερμηνεία: {label}")
            elif "threshold" in scale:
                 if score >= scale["threshold"]:
                     st.warning(scale.get("label", "Υψηλή τιμή"))
        
        elif scale["scoring"] == "count_yes":
             st.progress(score / len(scale["items"]))
             st.write(f"Σκορ (Ναι): {score} / {len(scale['items'])}")
             if score >= scale.get("threshold", 0):
                 st.error(scale.get("high_risk_label", "Υψηλός κίνδυνος"))
        
        elif scale["scoring"] == "mean_per_domain":
             st.write(f"Μέσο Σκορ: {score:.2f}")
             if score >= scale.get("threshold", 2.0):
                 st.warning(f"Υπερβαίνει το όριο ({scale['threshold']})")

    if st.button("Αποθήκευση Αποτελεσμάτων"):
        if save_screening(st.session_state.user_data, st.session_state.test_results):
            st.success("Τα αποτελέσματα αποθηκεύτηκαν επιτυχώς!")
        else:
            st.info("Η αποθήκευση δεν είναι διαθέσιμη (προσθήκη κλειδιών Firebase).")

    if st.button("Νέα Αξιολόγηση"):
        st.session_state.user_data = None
        st.session_state.test_results = {}
        st.session_state.current_scale = "Registration"
        st.rerun()

# --- MAIN LOGIC ---

if st.session_state.user_data is None:
    registration_page()
    if st.button("Mock Data (Δοκιμαστικά Δεδομένα)"):
        mock_data()
else:
    if st.session_state.current_scale in SCALES:
        scale_page(st.session_state.current_scale)
    else:
        summary_page()

# Footer
st.markdown('<div class="custom-footer">Copyright © 2025 Prokopios Andrianos</div>', unsafe_allow_html=True)
