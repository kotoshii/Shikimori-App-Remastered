package com.gnoemes.shikimori.presentation.view.series.episodes.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.series.presentation.EpisodeViewModel
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate
import kotlinx.android.synthetic.main.item_episode.view.*


class EpisodeAdapterDelegate(
        private val callback: (EpisodeViewModel) -> Unit,
        private val longPressListener: (EpisodeViewModel) -> Unit
) : AbsListItemAdapterDelegate<EpisodeViewModel, Any, EpisodeAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is EpisodeViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_episode))

    override fun onBindViewHolder(item: EpisodeViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var item: EpisodeViewModel

        init {
            itemView.episodeContainer.onClick { callback.invoke(item) }
            //handled, so releasing a long press does not open the episode as well
            itemView.episodeContainer.setOnLongClickListener { longPressListener.invoke(item);true }
        }

        fun bind(item: EpisodeViewModel) {
            this.item = item
            with(itemView) {
                val episodeName = String.format(context.getString(R.string.episode_number), item.index)
                episodeNameView.text = episodeName
                //only shows the episode is watched, as shikimori counts it; it does nothing on a tap.
                //Its colours are fixed in the layout: a view that is not clickable takes its row's
                //pressed state, and the style's pressed colours turned it dark on every tap
                watchedView.visibleIf { !item.isGuest && item.isWatched }
                currentEpisodeView.visibleIf { item.isOpened }
            }
        }

    }
}