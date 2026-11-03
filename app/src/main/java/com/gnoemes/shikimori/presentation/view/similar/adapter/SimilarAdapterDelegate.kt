package com.gnoemes.shikimori.presentation.view.similar.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemSimilarBinding
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.similar.presentation.SimilarViewModel
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class SimilarAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val navigationCallback: (Type, Long) -> Unit,
        private val callback: (Long) -> Unit
) : AbsListItemAdapterDelegate<SimilarViewModel, Any, SimilarAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is SimilarViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_similar))

    override fun onBindViewHolder(item: SimilarViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemSimilarBinding.bind(view)

        private lateinit var item: SimilarViewModel

        init {
            binding.container.onClick { navigationCallback.invoke(item.type, item.id) }
            binding.statusRateBtn.onClick { callback.invoke(item.id) }
        }

        fun bind(item: SimilarViewModel) {
            this.item = item
            with(itemView) {
                imageLoader.setImageWithPlaceHolder(binding.imageView, item.image.original)
                binding.nameView.text = item.title
                binding.descriptionView.text = item.description
                binding.statusRateBtn.visibleIf { !item.isGuest }
                binding.scoreView.text = item.score?.toString()

                binding.scoreView.visibleIf { item.score != null }
                binding.starImage.visibleIf { item.score != null }


                binding.statusRateBtn.bindRateStatus(item.status)

            }
        }

    }
}