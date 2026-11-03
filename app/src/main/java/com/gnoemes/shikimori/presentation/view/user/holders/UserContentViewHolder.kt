package com.gnoemes.shikimori.presentation.view.user.holders

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutUserProfileContentBinding
import com.gnoemes.shikimori.entity.user.presentation.UserContentMoreItem
import com.gnoemes.shikimori.entity.user.presentation.UserContentType
import com.gnoemes.shikimori.entity.user.presentation.UserContentViewModel
import com.gnoemes.shikimori.presentation.view.common.adapter.StartSnapHelper
import com.gnoemes.shikimori.presentation.view.user.adapter.BaseUserContentAdapter
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.visible
import com.gnoemes.shikimori.utils.visibleIf
import com.gnoemes.shikimori.utils.widgets.HorizontalSpaceItemDecorator

class UserContentViewHolder(
        private val view: View,
        private val adapter: BaseUserContentAdapter
) {

    private val binding = LayoutUserProfileContentBinding.bind(view)

    init {
        if (binding.contentRecyclerView.onFlingListener == null) {
            val snapOffset = view.resources.getDimension(R.dimen.margin_normal).toInt()
            val snapHelper = StartSnapHelper(snapOffset)
            snapHelper.attachToRecyclerView(binding.contentRecyclerView)
        }

        //a holder is made for every load of the row, so the list is set up only once: each new holder
        //added its gaps again, and they grew after every «Повторить» (T5-18)
        binding.contentRecyclerView.apply {
            if (adapter !== this@UserContentViewHolder.adapter) {
                adapter = this@UserContentViewHolder.adapter.apply { if (!hasObservers()) setHasStableIds(true) }
            }
            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false).apply { initialPrefetchItemCount = 3 }
            }
            setHasFixedSize(true)
            if (itemDecorationCount == 0) {
                val spacing = resources.getDimension(R.dimen.margin_normal).toInt()
                val firstItemSpacing = resources.getDimension(R.dimen.margin_big).toInt()
                addItemDecoration(HorizontalSpaceItemDecorator(spacing, firstItemSpacing))
            }
        }
    }

    fun bind(item: UserContentViewModel) {
        //shown again when a reload has items: a row that was empty once stayed hidden (BUGS #58)
        view.visibleIf { item.content.isNotEmpty() }
        if (item.content.isEmpty()) return

        val items = mutableListOf<Any>()
        items.addAll(item.content)

        if (item.needShowMore) items.add(UserContentMoreItem(item.type, item.moreSize, item.moreIsAtLeast))
        adapter.bindItems(items)

        val stringRes = when (item.type) {
            UserContentType.FAVORITES -> R.string.common_favorite
            UserContentType.CLUBS -> R.string.common_clubs
            UserContentType.FRIENDS -> R.string.common_friends
        }

        with(view) {
            binding.contentLabelView.setText(stringRes)
            binding.progressBar.gone()
            binding.contentRecyclerView.visible()
        }
    }

    companion object {
        //a row back to the start: its spinner in place of the items shown
        fun reset(view: View) = with(view) {
            val binding = LayoutUserProfileContentBinding.bind(this)
            visible()
            binding.contentLabelView.text = null
            binding.contentRecyclerView.gone()
            binding.progressBar.visible()
        }
    }
}