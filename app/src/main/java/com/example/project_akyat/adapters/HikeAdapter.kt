package com.example.project_akyat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.project_akyat.R
import com.example.project_akyat.model.local.HikeEntity
import com.example.project_akyat.utils.formatDate
import com.example.project_akyat.utils.formatDuration

class HikeAdapter(
    private val onUploadClick: (HikeEntity) -> Unit,
    private val onDeleteClick: (HikeEntity) -> Unit
) : RecyclerView.Adapter<HikeAdapter.HikeViewHolder>() {

    private var hikes = listOf<HikeEntity>()

    fun submitList(newList: List<HikeEntity>) {
        hikes = newList
        notifyDataSetChanged()
    }

    class HikeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvHikeDate)
        val tvDistance: TextView = view.findViewById(R.id.tvStatDistance)
        val tvElevation: TextView = view.findViewById(R.id.tvStatElevation)
        val tvDuration: TextView = view.findViewById(R.id.tvStatDuration)
        val tvBadge: TextView = view.findViewById(R.id.tvDifficultyBadge)
        val btnUpload: Button = view.findViewById(R.id.btnUpload)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hike, parent, false)
        return HikeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {
        val hike = hikes[position]
        val context = holder.itemView.context

        holder.tvDate.text = formatDate(hike.startedAt)
        holder.tvDistance.text = context.getString(R.string.distance_format, hike.distanceKm)
        holder.tvElevation.text = context.getString(R.string.elevation_format, hike.gainMeters)
        holder.tvDuration.text = formatDuration(hike.durationSeconds)
        holder.btnDelete.setOnClickListener { onDeleteClick(hike) }

        val (diffText, diffColor, diffBg) = when {
            hike.gainMeters < 300 -> {
                Triple("Easy", R.color.easy_text, R.drawable.bg_badge_easy)
            }
            hike.gainMeters <= 1000 -> {
                Triple("Moderate", R.color.mod_text, R.drawable.bg_badge_moderate)
            }
            else -> {
                Triple("Hard", R.color.hard_text, R.drawable.bg_badge_hard)
            }
        }

        holder.tvBadge.apply {
            text = diffText
            setTextColor(ContextCompat.getColor(context, diffColor))
            setBackgroundResource(diffBg)
        }

        holder.btnUpload.visibility = if (hike.synced) View.GONE else View.VISIBLE
        holder.btnUpload.setOnClickListener { onUploadClick(hike) }
    }

    override fun getItemCount() = hikes.size
}