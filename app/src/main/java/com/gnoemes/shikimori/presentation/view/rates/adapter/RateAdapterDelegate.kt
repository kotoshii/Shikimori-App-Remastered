package com.gnoemes.shikimori.presentation.view.rates.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemRateBinding
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.rates.domain.RateListAction
import com.gnoemes.shikimori.entity.rates.presentation.RateViewModel
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class RateAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val navigationCallback: (Type, Long) -> Unit,
        private val callback: (DetailsAction) -> Unit,
        private val listActionCallback: (RateListAction) -> Unit
) : AbsListItemAdapterDelegate<RateViewModel, Any, RateAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is RateViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_rate))

    override fun onBindViewHolder(item: RateViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemRateBinding.bind(view)

        private lateinit var item: RateViewModel

        init {
            with(itemView) {
                binding.playBtn.onClick { callback.invoke(DetailsAction.WatchOnline(item.id)) }
                binding.container.onClick { navigationCallback.invoke(item.type, item.contentId) }
                binding.imageView.onClick { navigationCallback.invoke(item.type, item.contentId) }
                //the pins are the user's own: on another user's list the long-press does nothing (P7-T5 Q146)
                binding.imageView.setOnLongClickListener { if (item.canEdit) listActionCallback.invoke(RateListAction.Pin(item)); true }
                binding.rateEditBtn.onClick { listActionCallback.invoke(RateListAction.EditRate(item)) }
            }
        }

        fun bind(item: RateViewModel) {
            this.item = item
            with(itemView) {
                imageLoader.setImageWithPlaceHolder(binding.imageView, item.image.original)

                binding.nameView.text = item.name
                binding.ratingView.text = item.rating
                binding.progressView.text = item.progress
                binding.descriptionView.text = item.description

                binding.pinView.visibleIf { item.isPinned }
                //invisible, not gone: ▶ is lined up with it
                binding.rateEditBtn.visibility = if (item.canEdit) View.VISIBLE else View.INVISIBLE
                binding.playBtn.visibleIf { item.type == Type.ANIME }
                binding.ratingView.visibleIf { item.rating != "-" }
            }
        }
    }
}