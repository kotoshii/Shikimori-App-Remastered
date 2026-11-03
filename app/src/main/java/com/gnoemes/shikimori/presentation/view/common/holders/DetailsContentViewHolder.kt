package com.gnoemes.shikimori.presentation.view.common.holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutDetailsContentBinding
import com.gnoemes.shikimori.databinding.LayoutDetailsContentWithSearchBinding
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.presentation.view.common.adapter.StartSnapHelper
import com.gnoemes.shikimori.presentation.view.common.adapter.content.ContentAdapter
import com.gnoemes.shikimori.utils.dp
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.isVisible
import com.gnoemes.shikimori.utils.visible
import com.gnoemes.shikimori.utils.widgets.HorizontalSpaceItemDecorator

class DetailsContentViewHolder(
        private val view: View,
        private val adapter: ContentAdapter,
        private val withSearch : Boolean= false
) {

    //two layouts, so two binding classes (P7-T14): the views both have, and the search's own, which
    //withSearch says the view has (the characters on the anime and manga pages)
    private val contentLabelView: TextView
    private val contentRecyclerView: RecyclerView
    private val progressBar: View
    private val search: LayoutDetailsContentWithSearchBinding?

    init {
        if (withSearch) {
            val binding = LayoutDetailsContentWithSearchBinding.bind(view)
            contentLabelView = binding.contentLabelView
            contentRecyclerView = binding.contentRecyclerView
            progressBar = binding.progressBar
            search = binding
        } else {
            val binding = LayoutDetailsContentBinding.bind(view)
            contentLabelView = binding.contentLabelView
            contentRecyclerView = binding.contentRecyclerView
            progressBar = binding.progressBar
            search = null
        }

        if (contentRecyclerView.onFlingListener == null) {
            val snapOffset = view.resources.getDimension(R.dimen.margin_normal).toInt()
            val snapHelper = StartSnapHelper(snapOffset)
            snapHelper.attachToRecyclerView(contentRecyclerView)
        }

        contentRecyclerView.apply {
            adapter = this@DetailsContentViewHolder.adapter.apply { if (!hasObservers()) setHasStableIds(true) }
            layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false).apply { initialPrefetchItemCount = 3 }
            setHasFixedSize(true)
            addItemDecoration(HorizontalSpaceItemDecorator(context.dp(if (withSearch) 16 else 8), context.dp(16)))
        }
    }

    fun bind(type: DetailsContentType, item: DetailsContentItem) {
        if (item.items.isEmpty()) {
            view.gone()
            return
        }

        val stringRes = when (type) {
            DetailsContentType.CHARACTERS -> R.string.common_characters
            DetailsContentType.RELATED -> R.string.common_related
            DetailsContentType.VIDEO -> R.string.common_video
            DetailsContentType.MANGAS -> R.string.common_manga
            DetailsContentType.ANIMES -> R.string.common_anime
            DetailsContentType.SEYUS -> R.string.common_seyu
            DetailsContentType.WORKS -> R.string.person_best_works
            DetailsContentType.SCREENSHOTS -> R.string.details_screenshots
        }

        //back after an empty list hid it: one empty list hid the section for good (P7-T5 17.5, BUGS #89)
        view.visible()
        with(view) {
            contentLabelView.setText(stringRes)
            adapter.bindItems(item.items)
            //not while the search is open: each result put 🔍 back beside the field (P7-T5 17.6)
            if (search != null && !search.searchView.isVisible()) search.searchBtn.visible()
            progressBar.gone()
            contentRecyclerView.visible()
        }
    }
}