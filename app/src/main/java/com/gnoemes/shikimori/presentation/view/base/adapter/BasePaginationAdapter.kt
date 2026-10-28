package com.gnoemes.shikimori.presentation.view.base.adapter

import androidx.recyclerview.widget.DiffUtil
import com.gnoemes.shikimori.entity.common.presentation.PageErrorItem
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.presentation.view.common.adapter.BaseAdapter
import com.gnoemes.shikimori.presentation.view.common.adapter.PageErrorAdapterDelegate
import com.gnoemes.shikimori.presentation.view.common.adapter.ProgressAdapterDelegate

/**
 * A paged list. It ends with at most one of two rows of its own: the next page's spinner, or
 * «Повторить» for a page that failed.
 */
abstract class BasePaginationAdapter : BaseAdapter<Any>() {

    /** What «Повторить» does: set by the screen. */
    var onRetryPage: () -> Unit = {}

    init {
        delegatesManager.addDelegate(ProgressAdapterDelegate())
        delegatesManager.addDelegate(PageErrorAdapterDelegate { onRetryPage() })
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    //the end row stays where it is, so only the rows before it are compared. A list with no rows
    //drops it: it ended the rows that were there (a type switch in «Поиск» showed it at the top until
    //its posted hide came)
    override fun bindItems(newItems: List<Any>) {
        val endRow = items.lastOrNull()?.takeIf { isEndRow(it) && newItems.isNotEmpty() }
        val oldData = if (endRow != null) items.filterNot { isEndRow(it) } else items.toList()

        items.clear()
        items.addAll(newItems)
        if (endRow != null) items.add(endRow)

        DiffUtil
                .calculateDiff(DiffCallback(newItems, oldData), false)
                .dispatchUpdatesTo(this)
    }

    open fun showProgress(isVisible: Boolean) {
        if (isVisible) showEndRow(ProgressItem())
        else if (isProgress()) removeEndRow()
    }

    fun showPageError(isVisible: Boolean) {
        if (isVisible) showEndRow(PageErrorItem)
        else if (isPageError()) removeEndRow()
    }

    open fun isProgress() = items.isNotEmpty() && items.last() is ProgressItem

    fun isPageError() = items.isNotEmpty() && items.last() is PageErrorItem

    fun isEndRowAt(position: Int) = items.getOrNull(position)?.let { isEndRow(it) } == true

    private fun isEndRow(item: Any) = item is ProgressItem || item is PageErrorItem

    //one replaces the other: the spinner of a page asked again replaces its «Повторить»
    private fun showEndRow(row: Any) {
        val last = items.lastOrNull()
        when {
            last != null && last.javaClass == row.javaClass -> Unit
            last != null && isEndRow(last) -> {
                items[items.lastIndex] = row
                notifyItemChanged(items.lastIndex)
            }
            else -> {
                items.add(row)
                notifyItemInserted(items.lastIndex)
            }
        }
    }

    private fun removeEndRow() {
        items.removeAt(items.lastIndex)
        notifyItemRemoved(items.size)
    }

}
