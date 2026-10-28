package com.gnoemes.shikimori.presentation.view.common.fragment

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.colorStateList
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.withArgs
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.dialog_menu.*

/**
 * A sort picker: «Сортировка», the sorts, the current one marked. «Списки» and the catalog's filter
 * sheet both open it; it takes plain items, so each keeps its own sort type (BUGS #16b: the catalog's
 * list had no title and did not mark the current sort).
 */
class SortDialog : BaseBottomSheetDialogFragment() {

    companion object {
        private const val SORTS_KEY = "SORTS_KEY"
        fun newInstance(sorts: List<Sort>) = SortDialog()
                .withArgs { putParcelableArray(SORTS_KEY, sorts.toTypedArray()) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = Point().let { activity?.windowManager?.defaultDisplay?.getSize(it);it }.x - context.dimenAttr(android.R.attr.actionBarSize)
    }

    override val useWrapperLayout = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sorts = arguments?.getParcelableArray(SORTS_KEY)?.map { it as Sort }!!

        with(toolbar) {
            setTitle(R.string.sort)
        }

        navView.apply {
            setItemBackgroundResource(R.drawable.selector_item_menu_background_accent)
            itemTextColor = context.colorStateList(R.color.selector_item_menu_text_color_accent)
            //an item's id is its place in the list
            setNavigationItemSelectedListener { menu ->
                (parentFragment as? SortCallback)?.onSortClicked(sorts[menu.itemId].key)
                dismiss()
                true
            }
            menu.apply {
                sorts.forEachIndexed { index, sort -> add(0, index, index, sort.text) }
                setGroupCheckable(0, true, true)
                setCheckedItem(sorts.indexOfFirst { it.isSelected }.coerceAtLeast(0))
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getDialogLayout(): Int = R.layout.dialog_menu

    /** A sort: the [key] the caller knows it by, the [text] shown, and whether it is the current one. */
    @Parcelize
    data class Sort(
            val key: String,
            val text: String,
            val isSelected: Boolean
    ) : Parcelable

    interface SortCallback {
        fun onSortClicked(key: String)
    }
}