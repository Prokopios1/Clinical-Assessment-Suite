import streamlit as st
import pandas as pd
from scales import SCALES
from database import save_screening
import datetime

# --- CONFIGURATION ---
st.set_page_config(
    page_title="Clinical Assessment Suite",
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
if "current_view" not in st.session_state:
    st.session_state.current_view = "Home"  # Options: Home, Registration, Test, Summary
if "active_test" not in st.session_state:
    st.session_state.active_test = None

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
    st.session_state.current_view = "Home"
    st.rerun()

# --- UI COMPONENTS ---

def home_page():
    st.title("Καλωσορίσατε στην Πλατφόρμα Κλινικής Αξιολόγησης")
    st.markdown(f"""
    <div style="text-align: justify; line-height: 1.6; margin-bottom: 2rem;">
    Η παρούσα εφαρμογή αποτελεί ένα ολοκληρωμένο ψηφιακό εργαλείο κλινικής διαλογής, σχεδιασμένο για την υποστήριξη της ψυχικής υγείας και της αυτογνωσίας. Μέσω επιστημονικά τεκμηριωμένων ερωτηματολογίων (PHQ-9, GAD-7, PID-5-BF, MSI-BPD, PQ-B), ο χρήστης έχει τη δυνατότητα να αξιολογήσει το επίπεδο συμπτωμάτων που σχετίζονται με την κατάθλιψη, το άγχος, τις διαταραχές προσωπικότητας και τον κίνδυνο ψύχωσης. Η εφαρμογή έχει αναπτυχθεί με γνώμονα την εγκυρότητα και την εμπιστευτικότητα των δεδομένων, χρησιμοποιώντας το Firebase για την ασφαλή αποθήκευση των αποτελεσμάτων. 
    <br><br>
    Τα ερωτηματολόγια ακολουθούν τα διεθνή κλινικά πρότυπα και τις οδηγίες του DSM-5. Είναι σημαντικό να σημειωθεί ότι τα αποτελέσματα των τεστ παρέχουν μια ένδειξη της τρέχουσας κατάστασης και δεν υποκαθιστούν την επαγγελματική κλινική διάγνωση από ψυχίατρο ή ψυχολόγο.
    </div>
    """, unsafe_allow_html=True)

    st.subheader("Διαθέσιμα Τεστ")
    
    # Test Selection Cards
    for scale_id, scale in SCALES.items():
        with st.container():
            st.markdown(f"""
            <div class="test-card">
                <h3>{scale['name']}</h3>
                <p>{scale['description']}</p>
            </div>
            """, unsafe_allow_html=True)
            if st.button(f"Έναρξη {scale_id}", key=f"btn_{scale_id}", use_container_width=True):
                if st.session_state.user_data is None:
                    st.session_state.current_view = "Registration"
                else:
                    st.session_state.current_view = "Test"
                st.session_state.active_test = scale_id
                st.rerun()

def registration_page():
    st.title("Εγγραφή Χρήστη")
    with st.form("reg_form"):
        name = st.text_input("Ονοματεπώνυμο")
        email = st.text_input("Email")
        gdpr = st.checkbox("Συμφωνώ με την επεξεργασία των δεδομένων μου (GDPR)")
        
        submit = st.form_submit_button("Συνέχεια")
        if submit:
            if not name or not email:
                st.error("Παρακαλώ συμπληρώστε όλα τα πεδία.")
            elif not gdpr:
                st.warning("Πρέπει να συμφωνήσετε με τους όρους GDPR.")
            else:
                st.session_state.user_data = {"name": name, "email": email, "gdpr": gdpr}
                st.session_state.current_view = "Test"
                st.rerun()
    
    if st.button("⬅ Επιστροφή στην Αρχική"):
        st.session_state.current_view = "Home"
        st.rerun()

def test_page():
    scale_id = st.session_state.active_test
    scale = SCALES[scale_id]
    st.title(scale["name"])
    
    responses = []
    
    with st.form(f"form_{scale_id}"):
        for i, item in enumerate(scale["items"]):
            st.write(item)
            if scale["scoring"] in ["weighted_sum", "mean_per_domain_weighted"]:
                # 1-5 Radio Scale
                res = st.radio(
                    f"Ερώτηση {i+1}",
                    options=scale["options"],
                    format_func=lambda x: f"{x} - {scale['labels'][x]}",
                    horizontal=True,
                    key=f"{scale_id}_{i}",
                    label_visibility="collapsed"
                )
                responses.append(res)
            else:
                # Binary scale (PQ-B, MSI-BPD)
                res = st.radio(
                    f"Ερώτηση {i+1}",
                    options=scale["options"],
                    horizontal=True,
                    key=f"{scale_id}_{i}",
                    label_visibility="collapsed"
                )
                responses.append(res)
            st.divider()
        
        submit = st.form_submit_button("Υποβολή")
        
        if submit:
            score = 0
            if scale["scoring"] == "weighted_sum":
                score = sum(scale["weights"][r] for r in responses)
            elif scale["scoring"] == "count_yes":
                score = sum(1 for r in responses if r == "Ναι")
            elif scale["scoring"] == "sum":
                score = sum(1 for r in responses if r == "Ναι")
            elif scale["scoring"] == "mean_per_domain_weighted":
                # Average of weighted scores
                score = sum(scale["weights"][r] for r in responses) / len(responses)
            
            save_results(scale_id, score)
            st.session_state.current_view = "Summary"
            st.rerun()

def summary_page():
    st.title("Αποτελέσματα Αξιολόγησης")
    scale_id = st.session_state.active_test
    scale = SCALES[scale_id]
    data = st.session_state.test_results[scale_id]
    score = data["score"]

    st.subheader(scale["name"])
    
    if scale["scoring"] in ["weighted_sum", "mean_per_domain_weighted"]:
        max_possible = len(scale["items"]) * 3 if scale["scoring"] == "weighted_sum" else 3.0
        st.progress(min(score / max_possible, 1.0))
        st.write(f"Σκορ: **{score:.1f}**")
        
        # Interpretation
        if "thresholds" in scale:
            for low, high, label in scale["thresholds"]:
                if low <= score <= high:
                    st.info(f"Ερμηνεία: {label}")
        elif "threshold" in scale:
             if score >= scale["threshold"]:
                 st.warning(f"Προσοχή: Υπερβαίνει το κλινικό όριο ({scale['threshold']})")
    
    else:
         st.progress(score / len(scale["items"]))
         st.write(f"Σκορ (Ναι): **{score}** / {len(scale['items'])}")
         if score >= scale.get("threshold", 0):
             st.error(scale.get("high_risk_label", scale.get("label", "Υψηλός κίνδυνος")))

    col1, col2 = st.columns(2)
    with col1:
        if st.button("Αποθήκευση στο Firebase"):
            if save_screening(st.session_state.user_data, st.session_state.test_results):
                st.success("Αποθηκεύτηκε!")
            else:
                st.info("Firebase μη συνδεδεμένο.")
    with col2:
        if st.button("Νέο Τεστ"):
            st.session_state.current_view = "Home"
            st.rerun()

# --- MAIN LOGIC ---

if st.session_state.current_view == "Home":
    home_page()
    if st.button("Mock Data (Testing)", type="secondary"):
        mock_data()
elif st.session_state.current_view == "Registration":
    registration_page()
elif st.session_state.current_view == "Test":
    test_page()
elif st.session_state.current_view == "Summary":
    summary_page()

# Footer
st.markdown(f'''
<div class="custom-footer">
    Copyright © 2025 Prokopios Andrianos<br>
    Clinical Assessment Suite | Created with Support for Mental Health
</div>
''', unsafe_allow_html=True)
