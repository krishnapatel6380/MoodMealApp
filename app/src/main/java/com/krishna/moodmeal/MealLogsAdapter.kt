package com.krishna.moodmeal


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class MealLog(val mealName: String, val mood: String, val timestamp: Long)

class MealLogsAdapter : ListAdapter<MealLog, MealLogsAdapter.MealLogViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_log, parent, false)
        return MealLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMealName: TextView = itemView.findViewById(R.id.textMealName)
        private val textMood: TextView = itemView.findViewById(R.id.textMood)
        private val textTimestamp: TextView = itemView.findViewById(R.id.textTimestamp)

        fun bind(mealLog: MealLog) {
            textMealName.text = mealLog.mealName
            textMood.text = "Mood : ${mealLog.mood}"
            textTimestamp.text = formatTimestamp(mealLog.timestamp)
        }

        private fun formatTimestamp(ts: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            return "Logged on: ${sdf.format(Date(ts))}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MealLog>() {
        override fun areItemsTheSame(oldItem: MealLog, newItem: MealLog): Boolean {
            // No unique ID, assume equality by timestamp
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: MealLog, newItem: MealLog): Boolean {
            return oldItem == newItem
        }
    }
}
