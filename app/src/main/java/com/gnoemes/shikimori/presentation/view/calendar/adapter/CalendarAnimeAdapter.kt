package com.gnoemes.shikimori.presentation.view.calendar.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemCalendarAnimeBinding
import com.gnoemes.shikimori.entity.calendar.presentation.CalendarAnimeItem
import com.gnoemes.shikimori.utils.badgeColorRes
import com.gnoemes.shikimori.utils.iconRes
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf

class CalendarAnimeAdapter(
        private val imageLoader: ImageLoader,
        private val callback: (id: Long) -> Unit,
        private val items: List<CalendarAnimeItem>
) : RecyclerView.Adapter<CalendarAnimeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_calendar_anime))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.apply {
            imageLoader.clearImage(holder.binding.imageView)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemCalendarAnimeBinding.bind(view)

        private lateinit var item: CalendarAnimeItem

        init {
            binding.materialCardView.onClick { callback.invoke(item.id) }
        }

        fun bind(item: CalendarAnimeItem) {
            this.item = item
            with(itemView) {
                imageLoader.setImageListItem(binding.imageView, item.image.original)
                binding.nameView.text = item.name
                binding.descriptionView.text = item.description

                if (item.status != null) {
                    binding.rateView.setIconResource(item.status.iconRes)
                    binding.rateView.setIconTintResource(item.status.badgeColorRes)
                }

                binding.rateView.visibleIf { item.status != null }
                binding.seasonLastView.visibleIf { item.isLast }
            }
        }
    }
}
