package com.example.agricore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

class PlantingTipsAdapter(
    private val onTipClick: (PlantingTip) -> Unit
) : ListAdapter<PlantingTip, PlantingTipsAdapter.TipViewHolder>(TipDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_planting_tip, parent, false)  // ✅ Fixed: Use correct layout name
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.tv_tip_title)
        private val descriptionText: TextView = itemView.findViewById(R.id.tv_tip_description)
        private val categoryChip: Chip = itemView.findViewById(R.id.chip_category)
        private val seasonChip: Chip = itemView.findViewById(R.id.chip_season)
        private val difficultyChip: Chip = itemView.findViewById(R.id.chip_difficulty)
        private val seasonalBadge: TextView = itemView.findViewById(R.id.tv_seasonal_badge)
        private val expandIcon: ImageView = itemView.findViewById(R.id.iv_expand_icon)
        private val expandableSection: LinearLayout = itemView.findViewById(R.id.ll_expandable_section)
        private val bulletText: TextView = itemView.findViewById(R.id.tv_tip_bullet) // ✅ Fixed: Use the actual TextView for bullets

        private var isExpanded = false

        fun bind(tip: PlantingTip) {
            titleText.text = tip.title
            descriptionText.text = tip.description
            categoryChip.text = tip.category
            seasonChip.text = tip.season
            difficultyChip.text = tip.difficulty

            // Show seasonal badge if it's current season
            if (tip.isCurrentSeason) {
                seasonalBadge.visibility = View.VISIBLE
                seasonalBadge.text = "CURRENT SEASON"
                seasonalBadge.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.holo_green_light)
                )
            } else {
                seasonalBadge.visibility = View.GONE
            }

            // Set difficulty chip colors
            difficultyChip.setChipBackgroundColorResource(
                when (tip.difficulty.lowercase()) {
                    "beginner" -> android.R.color.holo_green_light
                    "intermediate" -> android.R.color.holo_orange_light
                    "advanced" -> android.R.color.holo_red_light
                    else -> android.R.color.darker_gray
                }
            )

            // Setup expandable content
            setupExpandableContent(tip)

            // Click listener for expansion
            itemView.setOnClickListener {
                toggleExpansion()
                onTipClick(tip)
            }

            // Click listener for expand icon
            expandIcon.setOnClickListener {
                toggleExpansion()
            }
        }

        private fun setupExpandableContent(tip: PlantingTip) {
            // Join all tips with bullet points
            val tipsText = tip.tips.joinToString("\n") { "• $it" }
            bulletText.text = tipsText

            // Initially collapsed
            expandableSection.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandIcon.rotation = if (isExpanded) 180f else 0f

            // Update description to show truncated version when collapsed
            updateDescriptionText(tip)
        }

        private fun updateDescriptionText(tip: PlantingTip) {
            if (isExpanded) {
                descriptionText.maxLines = Int.MAX_VALUE
                descriptionText.text = tip.description
            } else {
                descriptionText.maxLines = 2
                descriptionText.text = tip.description
            }
        }

        private fun toggleExpansion() {
            isExpanded = !isExpanded

            expandableSection.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandIcon.animate()
                .rotation(if (isExpanded) 180f else 0f)
                .setDuration(200)
                .start()

            // Update description text limits
            val tip = currentList[adapterPosition]
            updateDescriptionText(tip)
        }
    }
}

class TipDiffCallback : DiffUtil.ItemCallback<PlantingTip>() {
    override fun areItemsTheSame(oldItem: PlantingTip, newItem: PlantingTip): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PlantingTip, newItem: PlantingTip): Boolean {
        return oldItem == newItem
    }
}