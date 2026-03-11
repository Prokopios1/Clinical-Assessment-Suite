package com.clinical.assessment.ui.clinician

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clinical.assessment.R
import com.clinical.assessment.models.Patient
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.Locale

class PatientAdapter(
    private val onItemClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    private var patients = listOf<Patient>()

    fun submitList(list: List<Patient>) {
        patients = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }

    override fun getItemCount() = patients.size

    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvPatientEmail)
        private val tvRisk: TextView = itemView.findViewById(R.id.tvRiskLabel)
        private val tvDate: TextView = itemView.findViewById(R.id.tvLastVisit)
        private val chart: LineChart = itemView.findViewById(R.id.chartSparkline)

        fun bind(patient: Patient) {
            val fullName = "${patient.firstName} ${patient.lastName}".trim()
            tvName.text = fullName
            tvEmail.text = patient.id
            
            // Format Date
            val latestAssessment = patient.history.maxByOrNull { it.date }
            val dateStr = latestAssessment?.date?.let { 
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(it)
                    SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault()).format(date!!)
                } catch(e: Exception) { it }
            } ?: "N/A"
            tvDate.text = "Last Session: $dateStr"

            // Risk Label Styling
            if (patient.history.isNotEmpty()) {
                val latestByTypes = patient.history.groupBy { it.type }.mapValues { (_, valList) -> valList.maxByOrNull { it.date } }
                
                val summaryText = latestByTypes.entries.sortedBy { it.key }.joinToString(" | ") { (id, res) ->
                    val shortId = when(id) {
                        "PID-5-BF" -> "PID5"
                        "MSI-BPD" -> "MSI"
                        else -> id
                    }
                    val riskStr = if (res != null) {
                       if (id == "PHQ-9" || id == "GAD-7") {
                           when {
                               res.score >= 20 -> itemView.context.getString(R.string.risk_severe)
                               res.score >= 15 -> itemView.context.getString(R.string.risk_high)
                               res.score >= 10 -> itemView.context.getString(R.string.risk_moderate)
                               else -> itemView.context.getString(R.string.risk_low)
                           }
                       } else itemView.context.getString(R.string.risk_completed)
                    } else itemView.context.getString(R.string.risk_unknown)
                    "$shortId: $riskStr"
                }
                tvRisk.text = summaryText
                
                // Fallback coloring to default for simplified adapter
                tvRisk.setBackgroundResource(R.color.primary_variant)
            } else {
                tvRisk.setText(R.string.risk_unknown)
                tvRisk.setBackgroundResource(R.color.surface_variant)
            }

            // Sparkline Setup (using PHQ-9 or primary as line chart proxy if available)
            val primaryScaleHistory = patient.history.filter { it.type == "PHQ-9" }.sortedBy { it.date }
            val scores = if (primaryScaleHistory.isNotEmpty()) {
                primaryScaleHistory.map { it.score }
            } else {
                patient.history.map { it.score }
            }
            setupChart(scores)
            
            itemView.setOnClickListener { onItemClick(patient) }
        }

        private fun setupChart(scores: List<Double>) {
            chart.description.isEnabled = false
            chart.legend.isEnabled = false
            chart.axisLeft.isEnabled = false
            chart.axisRight.isEnabled = false
            chart.xAxis.isEnabled = false
            chart.setTouchEnabled(false)
            
            if (scores.isEmpty()) {
                chart.clear()
                return
            }

            val entries = scores.mapIndexed { index, score -> Entry(index.toFloat(), score.toFloat()) }
            val dataSet = LineDataSet(entries, "Label")
            dataSet.color = android.graphics.Color.parseColor("#2D5AF0") // Primary Blue
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)
            dataSet.lineWidth = 2.5f
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    }
}
