package com.gnoemes.shikimori.presentation.view.similar.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.similar.presentation.SimilarViewModel
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate
import kotlinx.android.synthetic.main.item_similar.view.*

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

        private lateinit var item: SimilarViewModel

        init {
            itemView.container.onClick { navigationCallback.invoke(item.type, item.id) }
            itemView.statusRateBtn.onClick { callback.invoke(item.id) }
        }

        fun bind(item: SimilarViewModel) {
            this.item = item
            with(itemView) {
                imageLoader.setImageWithPlaceHolder(imageView, item.image.original)
                nameView.text = item.title
                descriptionView.text = item.description
                statusRateBtn.visibleIf { !item.isGuest }
                scoreView.text = item.score?.toString()

                scoreView.visibleIf { item.score != null }
                starImage.visibleIf { item.score != null }


                statusRateBtn.bindRateStatus(item.status)

            }
        }

    }
}