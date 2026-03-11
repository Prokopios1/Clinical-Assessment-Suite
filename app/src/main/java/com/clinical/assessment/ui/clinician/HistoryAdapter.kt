package com.clinical.assessment.ui.clinician

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.clinical.assessment.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryItem(val date: Date, val score: Double, val label: String, val benchmark: Double? = null)

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var items = listOf<HistoryItem>()

    fun submitList(list: List<HistoryItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_attempt, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvHistoryDate)
        private val tvScore: TextView = itemView.findViewById(R.id.tvHistoryScore)
        private val llTrend: View = itemView.findViewById(R.id.llTrend)
        private val ivTrendIcon: android.widget.ImageView = itemView.findViewById(R.id.ivTrendIcon)
        private val tvTrendText: TextView = itemView.findViewById(R.id.tvTrendText)

        fun bind(item: HistoryItem) {
            tvDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(item.date)
            tvScore.text = "Score: ${item.score.toInt()} (${item.label})"
            
            if (item.benchmark != null) {
                llTrend.visibility = View.VISIBLE
                val diff = item.score - item.benchmark
                
                when {
                    diff > 0 -> {
                        ivTrendIcon.setImageResource(R.drawable.ic_trend_up)
                        tvTrendText.text = "+${diff.toInt()} from 50th %"
                    }
                    diff < 0 -> {
                        ivTrendIcon.setImageResource(R.drawable.ic_trend_down)
                        tvTrendText.text = "${diff.toInt()} from 50th %"
                    }
                    else -> {
                        ivTrendIcon.setImageResource(R.drawable.ic_trend_flat)
                        tvTrendText.text = "At 50th %"
                    }
                }
            } else {
                llTrend.visibility = View.GONE
            }
        }
    }
}
