package com.gnoemes.shikimori.presentation.view.search

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.DialogCatalogSettingsBinding
import com.gnoemes.shikimori.entity.search.domain.CatalogSettings
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.addBackButton
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.setUsable
import com.gnoemes.shikimori.utils.visibleIf
import com.gnoemes.shikimori.utils.withArgs

/**
 * «Поиск»'s «Отображение» sheet (P10-T3): the cards' size («Крупные · Средние · Мелкие»: 2, 3 or 4 in a row
 * on a phone held upright, more on a wider screen - Q11), which parts of a card show, and search as you type. Each change goes to the catalog at once ([Callback]), so the grid behind the
 * sheet changes as the user picks; there is no «Применить». It opens with the catalog's settings and keeps
 * its own through a rotation. A guest's «Статус в списке» is greyed out: a guest has no list.
 */
class CatalogSettingsDialog : BaseBottomSheetDialogFragment() {

    companion object {
        private const val SETTINGS_KEY = "SETTINGS_KEY"
        private const val SIGNED_IN_KEY = "SIGNED_IN_KEY"

        fun newInstance(settings: CatalogSettings, signedIn: Boolean) = CatalogSettingsDialog().withArgs {
            putParcelable(SETTINGS_KEY, settings)
            putBoolean(SIGNED_IN_KEY, signedIn)
        }
    }

    private val binding by viewBinding(DialogCatalogSettingsBinding::bind)

    private lateinit var settings: CatalogSettings

    //each size is stored as the count it gives on a phone held upright
    private val columnButtons
        get() = listOf(2 to binding.sizeLarge, 3 to binding.sizeMedium, 4 to binding.sizeSmall)

    //a short sheet: as tall as its content, and no taller than the screen
    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = context.dimenAttr(android.R.attr.actionBarSize)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = savedInstanceState?.getParcelable(SETTINGS_KEY)
                ?: arguments?.getParcelable(SETTINGS_KEY)
                ?: CatalogSettings()

        with(wrapper.toolbar) {
            setTitle(R.string.catalog_settings_title)
            addBackButton(R.drawable.ic_close) { dismiss() }
        }

        //the values first, then the listeners: setting a value would report it as a change
        showColumns()
        binding.typeSwitch.isChecked = settings.showType
        binding.statusSwitch.isChecked = settings.showStatus
        binding.countSwitch.isChecked = settings.showCount
        binding.scoreSwitch.isChecked = settings.showScore
        binding.nameSwitch.isChecked = settings.showName
        binding.yearSwitch.isChecked = settings.showYear
        binding.liveSwitch.isChecked = settings.searchAsYouType
        //a guest has no list: the row is greyed out and does nothing, and the line under it says why
        val signedIn = arguments?.getBoolean(SIGNED_IN_KEY) == true
        binding.statusTitle.setUsable(signedIn)
        binding.statusSwitch.setUsable(signedIn)
        binding.statusRow.isEnabled = signedIn
        binding.statusGuestHint.visibleIf { !signedIn }

        //a checkable button turns itself off when tapped again: one of the three is always on
        columnButtons.forEach { (columns, button) ->
            button.setOnClickListener {
                change(settings.copy(columns = columns))
                showColumns()
            }
        }
        binding.typeSwitch.onChecked { change(settings.copy(showType = it)) }
        binding.statusSwitch.onChecked { change(settings.copy(showStatus = it)) }
        binding.countSwitch.onChecked { change(settings.copy(showCount = it)) }
        binding.scoreSwitch.onChecked { change(settings.copy(showScore = it)) }
        binding.nameSwitch.onChecked { change(settings.copy(showName = it)) }
        binding.yearSwitch.onChecked { change(settings.copy(showYear = it)) }
        binding.liveSwitch.onChecked { change(settings.copy(searchAsYouType = it)) }
        binding.statusRow.setOnClickListener { binding.statusSwitch.toggle() }
        binding.liveRow.setOnClickListener { binding.liveSwitch.toggle() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::settings.isInitialized) outState.putParcelable(SETTINGS_KEY, settings)
    }

    private fun showColumns() = columnButtons.forEach { (columns, button) -> button.isChecked = columns == settings.columns }

    private inline fun CompoundButton.onChecked(crossinline change: (Boolean) -> Unit) =
            setOnCheckedChangeListener { _, isChecked -> change(isChecked) }

    //a switch Android sets back after a rotation reports the value the sheet already has: no change
    private fun change(newSettings: CatalogSettings) {
        if (newSettings == settings) return
        settings = newSettings
        (parentFragment as? Callback)?.onCatalogSettingsChanged(newSettings)
    }

    override fun getDialogLayout(): Int = R.layout.dialog_catalog_settings

    interface Callback {
        fun onCatalogSettingsChanged(settings: CatalogSettings)
    }
}
