package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.annotation.ArrayRes
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.ItemListener
import com.afollestad.materialdialogs.list.listItems
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.colorAttr
import dagger.android.support.AndroidSupportInjection

abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    //every page is listed in SettingsFragmentInjectionModule
    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferenceScreen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView?.background = ColorDrawable(context?.colorAttr(R.attr.colorSurface)!!)

        setDivider(ColorDrawable(Color.TRANSPARENT))
        setDividerHeight(0)
    }

    protected fun showListDialog(@ArrayRes items: Int, listener: ItemListener): Boolean {
        MaterialDialog(context!!).show {
            listItems(items, selection = listener)
        }
        return true
    }

    abstract val preferenceScreen : Int
}