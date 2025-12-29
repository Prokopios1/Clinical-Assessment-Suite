import streamlit as st
import pandas as pd
from scales import SCALES
from database import save_screening, get_user_screenings
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
    st.session_state.current_view = "Home"  # Options: Home, Registration, Test, Summary, History
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
    
    # Create cards in a 2-column layout
    scale_items = list(SCALES.items())
    
    for i in range(0, len(scale_items), 2):
        cols = st.columns(2)
        
        for col_idx, (scale_id, scale) in enumerate(scale_items[i:i+2]):
            with cols[col_idx]:
                # Card without icon
                st.markdown(f"""
                <div class="test-card">
                    <h3 style="text-align: center; margin-bottom: 0.5rem;">{scale['name']}</h3>
                    <p style="text-align: center;">{scale['description']}</p>
                </div>
                """, unsafe_allow_html=True)
                
                if st.button(f"Έναρξη", key=f"btn_{scale_id}", use_container_width=True):
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
            if scale["scoring"] in ["weighted_sum", "mean_per_domain_weighted", "pid5_full"]:
                # Scaled Radio (1-5 or 0-3)
                res = st.radio(
                    f"Ερώτηση {i+1}",
                    options=scale["options"],
                    format_func=lambda x: f"{x} - {scale['labels'][x]}" if scale["scoring"] != "pid5_full" else scale["labels"][x],
                    horizontal=True,
                    key=f"{scale_id}_{i}",
                    label_visibility="collapsed",
                    index=None
                )
                responses.append(res)
            else:
                # Binary scale (PQ-B, MSI-BPD)
                res = st.radio(
                    f"Ερώτηση {i+1}",
                    options=scale["options"],
                    horizontal=True,
                    key=f"{scale_id}_{i}",
                    label_visibility="collapsed",
                    index=None
                )
                responses.append(res)
            st.divider()
        
        submit = st.form_submit_button("Υποβολή")
        
        if submit:
            score = 0
            details = {}
            
            if scale["scoring"] == "weighted_sum":
                score = sum(scale["weights"][r] for r in responses)
            elif scale["scoring"] == "count_yes":
                score = sum(1 for r in responses if r == "Ναι")
            elif scale["scoring"] == "sum":
                score = sum(1 for r in responses if r == "Ναι")
            elif scale["scoring"] == "mean_per_domain_weighted":
                # Average of weighted scores
                score = sum(scale["weights"][r] for r in responses) / len(responses)
            elif scale["scoring"] == "pid5_full":
                # PID-5 Full scoring with reverse items and facet/domain calculation
                reverse_items = scale.get("reverse_items", [])
                
                # Convert responses to numeric and handle reverse scoring
                numeric_responses = []
                for i, r in enumerate(responses):
                    val = int(r)
                    # Reverse scoring: 3->0, 2->1, 1->2, 0->3
                    if (i + 1) in reverse_items:
                        val = 3 - val
                    numeric_responses.append(val)
                
                # Calculate facet scores
                facet_scores = {}
                for facet_id, facet_data in scale["facets"].items():
                    facet_items = facet_data["items"]
                    facet_sum = sum(numeric_responses[item_num - 1] for item_num in facet_items)
                    facet_avg = facet_sum / len(facet_items)
                    facet_scores[facet_id] = {
                        "name": facet_data["name"],
                        "raw": facet_sum,
                        "average": round(facet_avg, 2)
                    }
                
                # Calculate domain scores
                domain_scores = {}
                for domain_id, domain_data in scale["domains"].items():
                    domain_facets = domain_data["facets"]
                    facet_avgs = [facet_scores[f]["average"] for f in domain_facets]
                    domain_avg = sum(facet_avgs) / len(facet_avgs)
                    domain_scores[domain_id] = {
                        "name": domain_data["name"],
                        "average": round(domain_avg, 2)
                    }
                
                score = sum(numeric_responses)
                details = {
                    "facets": facet_scores,
                    "domains": domain_scores
                }
            
            save_results(scale_id, score, details)
            st.session_state.current_view = "Summary"
            st.rerun()

def history_page():
    st.title("Ιστορικό Αξιολογήσεων")
    
    if not st.session_state.user_data or "email" not in st.session_state.user_data:
        st.warning("Παρακαλώ συνδεθείτε/εγγραφείτε για να δείτε το ιστορικό σας.")
        if st.button("Πίσω"):
            st.session_state.current_view = "Home"
            st.rerun()
        return

    email = st.session_state.user_data["email"]
    with st.spinner("Φόρτωση ιστορικού..."):
        results = get_user_screenings(email)
    
    if not results:
        st.info("Δεν βρέθηκαν προηγούμενες αξιολογήσεις.")
    else:
        for res in results:
            timestamp = res.get("timestamp")
            # Handle Firestore timestamp or string
            if hasattr(timestamp, 'date'):
                date_str = timestamp.strftime("%d/%m/%Y %H:%M")
            else:
                date_str = str(timestamp)
                
            with st.expander(f"{date_str}"):
                # Based on how we save data structure:
                # res['results'] is a dict of scale_id -> {score, details, timestamp...}
                saved_results = res.get("results", {})
                for scale_id, data in saved_results.items():
                    scale_name = SCALES.get(scale_id, {}).get("name", scale_id)
                    st.markdown(f"**{scale_name}**: {data['score']}")
                    
    if st.button("⬅ Επιστροφή"):
        st.session_state.current_view = "Home"
        st.rerun()

def summary_page():
    st.title("Αποτελέσματα Αξιολόγησης")
    scale_id = st.session_state.active_test
    scale = SCALES[scale_id]
    data = st.session_state.test_results[scale_id]
    score = data["score"]

    st.subheader(scale["name"])
    
    if scale["scoring"] == "pid5_full":
        # PID-5 Full results
        st.write(f"Συνολικό Σκορ: **{score:.0f}** / {len(scale['items']) * 3}")
        
        # Display domain scores
        if "details" in data and "domains" in data["details"]:
            st.subheader("Βαθμολογίες Τομέων (Domains)")
            domains = data["details"]["domains"]
            for domain_id, domain_data in domains.items():
                st.metric(domain_data["name"], f"{domain_data['average']:.2f}")
            
            # Display facet scores in expander
            with st.expander("Δείτε Αναλυτικές Βαθμολογίες Πλευρών (Facets)"):
                facets = data["details"]["facets"]
                cols = st.columns(3)
                for idx, (facet_id, facet_data) in enumerate(facets.items()):
                    with cols[idx % 3]:
                        st.write(f"**{facet_data['name']}**")
                        st.write(f"Μέσος: {facet_data['average']:.2f}")
                        st.write(f"Συνολικό: {facet_data['raw']}")
                        st.divider()
        
    elif scale["scoring"] in ["weighted_sum", "mean_per_domain_weighted"]:
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
    
    # Show interpretation
    from interpretations import get_interpretation
    interpretation = get_interpretation(scale_id, score, scale)
    if interpretation:
        with st.expander("📋 Ερμηνεία Αποτελεσμάτων", expanded=True):
            st.write(interpretation)


    col1, col2 = st.columns(2)
    with col1:
        if st.button("Αποθήκευση"):
            if save_screening(st.session_state.user_data, st.session_state.test_results):
                st.success("Αποθηκεύτηκε!")
            else:
                st.info("Firebase μη συνδεδεμένο.")
    with col2:
        if st.button("Προβολή Ιστορικού"):
            st.session_state.current_view = "History"
            st.rerun()
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
elif st.session_state.current_view == "History":
    history_page()

# Footer
st.markdown(f'''
<div class="custom-footer">
    Copyright © 2025 Prokopios Andrianos<br>
    Clinical Assessment Suite | Created with Support for Mental Health
</div>
''', unsafe_allow_html=True)
