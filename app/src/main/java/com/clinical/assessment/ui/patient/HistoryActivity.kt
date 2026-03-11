package com.clinical.assessment.ui.patient

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clinical.assessment.R
import com.clinical.assessment.firebase.FirebaseManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.clinical.assessment.ui.clinician.HistoryAdapter
import com.clinical.assessment.ui.clinician.HistoryItem
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var chart: LineChart
    private lateinit var adapter: HistoryAdapter
    private var currentScaleId = "PHQ-9"
    private var allScreenings = listOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        chart = findViewById(R.id.chartHistory)
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        if (firebaseUser == null) {
            Toast.makeText(this, "Please login to see your progress.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupTabs()
        setupChart()
        setupRecyclerView()
        loadData(firebaseUser.email ?: "")
    }

    private fun setupTabs() {
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutHistory)
        val scales = com.clinical.assessment.data.ScalesData.getAllScales(this)
        
        scales.forEach { scale ->
            tabLayout.addTab(tabLayout.newTab().setText(scale.id).setTag(scale.id))
        }

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentScaleId = tab?.tag as? String ?: "PHQ-9"
                updateUI()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter()
        rv.adapter = adapter
    }

    private fun setupChart() {
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        // Limit lines will be dynamic based on scale
        
        chart.axisLeft.axisMinimum = 0f
        
        chart.setTouchEnabled(true)
        chart.setDragEnabled(true)
        chart.setScaleEnabled(true)
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
        val scale = com.clinical.assessment.data.ScalesData.getScale(this, currentScaleId) ?: return
        com.clinical.assessment.utils.MetadataManager.load(this)
        val benchmark = com.clinical.assessment.utils.MetadataManager.getBenchmark(currentScaleId)

        // Filter valid data for this scale
        val scaleData = allScreenings.mapNotNull { 
            val results = it["results"] as? Map<String, Map<String, Any>>
            val score = (results?.get(currentScaleId)?.get("score") as? Number)?.toDouble()
            val timestamp = (it["timestamp"] as? com.google.firebase.Timestamp)?.toDate()
            
            if (score != null && timestamp != null) {
                // Determine label dynamically
                val label = if (scale.thresholds != null) {
                     scale.thresholds.find { t -> score >= t.low && score <= t.high }?.label ?: "Unknown"
                } else {
                     if (score > (scale.threshold ?: 100.0)) "High" else "Normal"
                }
                Triple(timestamp, score, label)
            } else null
        }.sortedBy { it.first }

        if (scaleData.isEmpty()) {
            chart.clear()
            adapter.submitList(emptyList())
            return
        }

        // Prepare Chart
        val entries = scaleData.mapIndexed { index, triple ->
            Entry(index.toFloat(), triple.second.toFloat())
        }
        val dates = scaleData.map { SimpleDateFormat("MM/dd", Locale.getDefault()).format(it.first) }

        val set = LineDataSet(entries, "${scale.name} Result")
        set.color = android.graphics.Color.parseColor("#2D5AF0")
        set.setCircleColor(android.graphics.Color.parseColor("#2D5AF0"))
        set.lineWidth = 3f
        set.circleRadius = 5f
        set.setDrawCircleHole(true)
        set.circleHoleRadius = 2.5f
        set.setDrawValues(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.setDrawFilled(true)
        set.fillColor = android.graphics.Color.parseColor("#2D5AF0")
        set.fillAlpha = 30

        val data = LineData(set)
        
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = android.graphics.Color.parseColor("#EBEEF5")
        
        // Update Y Axis Maximum based on scale options usually
        chart.animateX(800)
        chart.invalidate()

        // Prepare List
        val historyItems = scaleData.map { 
            HistoryItem(it.first, it.second, it.third, benchmark) 
        }.reversed()
        
        adapter.submitList(historyItems)
    }

    // Generic updateUI handles everything now
}
