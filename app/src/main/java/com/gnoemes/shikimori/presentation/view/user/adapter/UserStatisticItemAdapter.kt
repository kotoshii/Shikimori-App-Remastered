package com.gnoemes.shikimori.presentation.view.user.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemUserProfileStatisticBinding
import com.gnoemes.shikimori.entity.user.presentation.UserStatisticItem
import com.gnoemes.shikimori.utils.clearAndAddAll
import com.gnoemes.shikimori.utils.inflate

class UserStatisticItemAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<UserStatisticItem>()

    fun bindItems(newItems: List<UserStatisticItem>) {
        items.clearAndAddAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_user_profile_statistic))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(items[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemUserProfileStatisticBinding.bind(view)

        fun bind(item: UserStatisticItem) {
            with(itemView) {
                binding.categoryView.text = item.category
                binding.countView.text = item.count.toString()

                binding.progressView.post {
                    binding.progressView.layoutParams = (binding.progressView.layoutParams as? FrameLayout.LayoutParams)?.apply {
                        val newWidth = binding.container.measuredWidth * item.progress
                        width = if (newWidth != 0f && newWidth < binding.container.height) binding.container.height else newWidth.toInt()
                    }
                    invalidate()
                }
            }
        }

    }
}