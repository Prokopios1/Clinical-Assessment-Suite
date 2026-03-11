package com.clinical.assessment.ui.clinician

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clinical.assessment.R
import com.clinical.assessment.data.ScalesData
import com.clinical.assessment.firebase.FirebaseManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var chart: LineChart
    private lateinit var historyAdapter: HistoryAdapter
    private var allScreenings = listOf<Map<String, Any>>()
    private var currentScaleId = "PHQ-9" // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_detail)

        val email = intent.getStringExtra("PATIENT_EMAIL") ?: return
        val name = intent.getStringExtra("PATIENT_NAME") ?: "Patient"

        findViewById<android.widget.TextView>(R.id.tvPatientNameDetail).text = name
        
        setupTabs()
        setupChart()
        setupRecyclerView()
        
        loadData(email)
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val scales = ScalesData.getAllScales(this)
        scales.forEach { scale ->
            tabLayout.addTab(tabLayout.newTab().setText(scale.id).setTag(scale.id))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentScaleId = tab?.tag as? String ?: "PHQ-9"
                updateUI()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupChart() {
        chart = findViewById(R.id.detailChart)
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return sdf.format(Date(value.toLong()))
            }
        }
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)
        historyAdapter = HistoryAdapter()
        rv.adapter = historyAdapter
    }

    private fun loadData(email: String) {
        FirebaseManager.getUserScreenings(email) { screenings ->
            allScreenings = screenings
            runOnUiThread {
                updateUI()
            }
        }
    }

    private fun updateUI() {
        val scale = ScalesData.getScale(this, currentScaleId) ?: return
        com.clinical.assessment.utils.MetadataManager.load(this)
        val benchmark = com.clinical.assessment.utils.MetadataManager.getBenchmark(currentScaleId)
        
        // Filter screenings that have this scale
        val relevantScreenings = allScreenings.filter { 
            val res = it["results"] as? Map<String, Any>
            res?.containsKey(currentScaleId) == true
        }.sortedBy { 
            (it["timestamp"] as? Timestamp)?.toDate()?.time ?: 0L
        }

        // Build History List
        val historyItems = relevantScreenings.map { s ->
            val timestamp = (s["timestamp"] as? Timestamp)?.toDate() ?: Date()
            val results = s["results"] as? Map<String, Map<String, Any>>
            val result = results?.get(currentScaleId)
            val score = (result?.get("score") as? Number)?.toDouble() ?: 0.0
            
            // Determine Label
            val label = scale.thresholds?.find { score >= it.low && score <= it.high }?.label 
                       ?: if (score > (scale.threshold ?: 100.0)) "High" else "Normal"
            
            HistoryItem(timestamp, score, label, benchmark)
        }.reversed() // Newest first for list

        historyAdapter.submitList(historyItems)

        // Update Chart
        val entries = relevantScreenings.map { s ->
            val timestamp = (s["timestamp"] as? Timestamp)?.toDate()?.time ?: 0L
            val results = s["results"] as? Map<String, Map<String, Any>>
            val result = results?.get(currentScaleId)
            val score = (result?.get("score") as? Number)?.toDouble() ?: 0.0
            Entry(timestamp.toFloat(), score.toFloat())
        }

        if (entries.isNotEmpty()) {
            val dataSet = LineDataSet(entries, scale.name)
            dataSet.color = androidx.core.content.ContextCompat.getColor(this, R.color.purple_500)
            dataSet.valueTextSize = 10f
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 4f
            dataSet.setDrawCircleHole(false)
            
            chart.data = LineData(dataSet)
            
            // Add Clinical Benchmark (Task 4)
            com.clinical.assessment.utils.MetadataManager.load(this)
            val benchmark = com.clinical.assessment.utils.MetadataManager.getBenchmark(currentScaleId)
            if (benchmark != null) {
                val ll = com.github.mikephil.charting.components.LimitLine(benchmark.toFloat(), "Population Average (50th Percentile)")
                ll.lineColor = android.graphics.Color.RED
                ll.lineWidth = 2f
                ll.textColor = android.graphics.Color.RED
                ll.textSize = 10f
                ll.enableDashedLine(10f, 10f, 0f)
                
                chart.axisLeft.removeAllLimitLines()
                chart.axisLeft.addLimitLine(ll)
                chart.axisLeft.setDrawLimitLinesBehindData(true)
            }
            
            chart.invalidate()
        } else {
            chart.clear()
        }
    }
}
