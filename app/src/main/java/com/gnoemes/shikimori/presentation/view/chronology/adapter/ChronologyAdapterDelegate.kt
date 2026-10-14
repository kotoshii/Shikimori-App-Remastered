package com.gnoemes.shikimori.presentation.view.chronology.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.chronology.ChronologyViewModel
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate
import kotlinx.android.synthetic.main.item_chronology.view.*


class ChronologyAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val navigationCallback: (Type, Long) -> Unit,
        private val callback: (Long) -> Unit
) : AbsListItemAdapterDelegate<ChronologyViewModel, Any, ChronologyAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is ChronologyViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_chronology))

    override fun onBindViewHolder(item: ChronologyViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var item: ChronologyViewModel

        init {
            itemView.container.onClick { navigationCallback.invoke(item.type, item.id) }
            itemView.statusRateBtn.onClick { callback.invoke(item.id) }
        }

        fun bind(item: ChronologyViewModel) {
            this.item = item
            with(itemView) {
                imageLoader.setImageWithPlaceHolder(imageView, item.image.original)
                nameView.text = item.title
                descriptionView.text = item.description
                relationView.text = item.relation
                statusRateBtn.visibleIf { !item.isGuest }

                statusRateBtn.bindRateStatus(item.status)

            }
        }

    }
}