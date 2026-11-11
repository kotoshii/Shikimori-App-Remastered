package com.gnoemes.shikimori.presentation.view.common.fragment

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.DialogMenuBinding
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.colorStateList
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.withArgs
import kotlinx.parcelize.Parcelize

/**
 * A "pick one" sheet: a title, the choices, the current one marked. A tap answers the fragment that
 * opened it ([ChoiceCallback]) and closes the sheet. It takes plain items, so each caller keeps its own
 * types: the sorts of «Списки» and of the catalog's filter sheet (BUGS #16b), what the catalog searches
 * (P10-T2). The [requestKey] tells a caller with more than one picker which one answered.
 */
class ChoiceDialog : BaseBottomSheetDialogFragment() {

    companion object {
        private const val REQUEST_KEY = "REQUEST_KEY"
        private const val TITLE_KEY = "TITLE_KEY"
        private const val CHOICES_KEY = "CHOICES_KEY"

        fun newInstance(requestKey: String, title: String, choices: List<Choice>) = ChoiceDialog()
                .withArgs {
                    putString(REQUEST_KEY, requestKey)
                    putString(TITLE_KEY, title)
                    putParcelableArray(CHOICES_KEY, choices.toTypedArray())
                }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = Point().let { activity?.windowManager?.defaultDisplay?.getSize(it);it }.x - context.dimenAttr(android.R.attr.actionBarSize)
    }

    override val useWrapperLayout = false

    private val binding by viewBinding(DialogMenuBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val requestKey = arguments?.getString(REQUEST_KEY)!!
        val choices = arguments?.getParcelableArray(CHOICES_KEY)?.map { it as Choice }!!

        binding.toolbar.title = arguments?.getString(TITLE_KEY)

        binding.navView.apply {
            setItemBackgroundResource(R.drawable.selector_item_menu_background_accent)
            itemTextColor = context.colorStateList(R.color.selector_item_menu_text_color_accent)
            //an item's id is its place in the list
            setNavigationItemSelectedListener { menu ->
                (parentFragment as? ChoiceCallback)?.onChoiceClicked(requestKey, choices[menu.itemId].key)
                dismiss()
                true
            }
            menu.apply {
                choices.forEachIndexed { index, choice -> add(0, index, index, choice.text) }
                setGroupCheckable(0, true, true)
                setCheckedItem(choices.indexOfFirst { it.isSelected }.coerceAtLeast(0))
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getDialogLayout(): Int = R.layout.dialog_menu

    /** A choice: the [key] the caller knows it by, the [text] shown, and whether it is the current one. */
    @Parcelize
    data class Choice(
            val key: String,
            val text: String,
            val isSelected: Boolean
    ) : Parcelable

    interface ChoiceCallback {
        fun onChoiceClicked(requestKey: String, key: String)
    }
}
