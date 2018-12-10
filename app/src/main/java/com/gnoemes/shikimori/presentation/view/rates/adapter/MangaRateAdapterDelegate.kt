package com.gnoemes.shikimori.presentation.view.rates.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.entity.common.domain.SpinnerAction
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.presentation.presenter.common.provider.RatingResourceProvider
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.unknownIfZero
import com.gnoemes.shikimori.utils.visibleIf
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate
import kotlinx.android.synthetic.main.item_rate.view.*

class MangaRateAdapterDelegate(
        private val settings: SettingsSource,
        private val imageLoader: ImageLoader,
        private val resourceProvider: RatingResourceProvider,
        private val navigationCallback: (Type, Long) -> Unit,
        private val callback: (DetailsAction) -> Unit
) : AbsListItemAdapterDelegate<Rate, Any, MangaRateAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is Rate && (item.type == Type.MANGA || item.type == Type.RANOBE)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_rate))

    override fun onBindViewHolder(item: Rate, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var item: Rate

        init {
            with(itemView) {
                watchView.onClick { callback.invoke(DetailsAction.WatchOnline(item.manga?.id)) }
                container.onClick { navigationCallback.invoke(item.type, item.manga?.id!!) }
                rateSpinnerView.callback = { action, status ->
                    if (action == SpinnerAction.RATE_CHANGE) callback.invoke(DetailsAction.ChangeRateStatus(status, item.id))
                    else callback.invoke(DetailsAction.EditRate(item))
                }
            }
        }

        fun bind(item: Rate) {
            this.item = item
            with(itemView) {
                imageLoader.setImageWithPlaceHolder(imageView, item.manga?.image?.original)

                nameView.text = if (settings.isRomadziNaming) item.manga?.name else item.manga?.nameRu
                        ?: item.manga?.name

                rateSpinnerView.isAnime = false
                rateSpinnerView.setRateStatus(item.status)
                val ratingText = "${resourceProvider.getRatingDescription(item.score)} (${item.score})"
                ratingView.text = ratingText

                typeView.text = item.manga?.type?.type

                episodesLabelView.text = context.getString(R.string.common_chapters)
                val chaptersText = "${item.chapters}/${item.manga?.chapters?.unknownIfZero()}"
                episodesView.text = chaptersText

                commentView.text = item.text
                commentView.visibleIf { !item.text.isNullOrBlank() }
            }
        }
    }
}