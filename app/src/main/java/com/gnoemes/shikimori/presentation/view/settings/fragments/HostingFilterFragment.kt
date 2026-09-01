package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.presentation.view.settings.ToolbarCallback
import com.gnoemes.shikimori.utils.HostingFilter
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.getDefaultSharedPreferences
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.putStringSet
import com.gnoemes.shikimori.utils.visibleIf
import kotlinx.android.synthetic.main.fragment_hosting_filter.*

/**
 * Lets the user hide hostings from the translations list.
 *
 * A plain `Fragment` rather than a `PreferenceFragmentCompat`, because the Add button has to stay
 * pinned below a scrolling list and a preference screen owns its whole layout. It is reached the
 * same way the preference screens are - `SettingsActivity.onPreferenceStartFragment` instantiates
 * whatever class `app:fragment` names, so the toolbar title and back stack come for free.
 *
 * Preferences are read directly here, as `SettingsAnimeFragment` does for the Anime365 token, rather
 * than through injected `SettingsSource`. Same store, same keys.
 */
class HostingFilterFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    private val adapter by lazy { HostingFilterAdapter(::onHostingToggled, ::confirmDelete) }

    private val prefs by lazy { context!!.getDefaultSharedPreferences() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_hosting_filter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? ToolbarCallback)?.showToolbarHint()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        addButton.onClick { showAddDialog() }

        showHostings()
    }

    override fun onDestroy() {
        super.onDestroy()
        //the menu belongs to the activity and is shared with every settings screen
        (activity as? ToolbarCallback)?.hideToolbarHint()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.item_hint) showHint()
        return true
    }

    private fun showHint() {
        MaterialDialog(context!!).show {
            title(R.string.settings_hosting_filter_hint_title)
            message(R.string.settings_hosting_filter_hint_message)
            positiveButton(res = R.string.common_understand)
        }
    }

    /**
     * Three sources: hostings the app has met, ones added by hand, and ones currently hidden. The
     * hidden set is folded in as a safety net - a hidden hosting must never be filtered out while
     * absent from the screen that unhides it.
     *
     * Kept apart because only a hand added domain can be deleted: the app would put a real one
     * straight back on the next translations load.
     */
    private fun showHostings() {
        val hidden = hidden()
        val seen = seen()
        val all = (seen + manual() + hidden).distinct().sorted()

        adapter.bindItems(all.map {
            //only a hand added domain can be deleted - one the app keeps meeting would come back on
            //the next translations load, so offering to remove it would be a lie
            HostingFilterAdapter.Item(it, hidden.contains(it), canDelete = !seen.contains(it))
        })

        emptyView.visibleIf { all.isEmpty() }
        recyclerView.visibleIf { all.isNotEmpty() }
    }

    /**
     * Saved as it is ticked - there is no OK button, so leaving the screen can never lose anything.
     *
     * The list is deliberately **not** rebuilt here - the rows do not change, only the ticks, and
     * rebuilding would restart the whole list under the user's finger. Unticking keeps the row:
     * removing one is the delete button's job, and it asks first.
     */
    private fun onHostingToggled(domain: String, isHidden: Boolean) {
        val hidden = hidden().toMutableSet()

        if (isHidden) hidden.add(domain) else hidden.remove(domain)
        prefs.putStringSet(SettingsExtras.HIDDEN_HOSTINGS, hidden)
    }

    private fun showAddDialog() {
        MaterialDialog(context!!).show {
            title(R.string.settings_hosting_filter_dialog_title)
            customView(R.layout.dialog_add_hosting)
            //the app's own strings: android.R.string.* follows the device locale, so it showed
            //"Cancel" on an english phone in an otherwise russian ui
            negativeButton(res = R.string.common_cancel) { dismiss() }
            //the dialog has to survive a bad value so the error can be shown in place
            noAutoDismiss()

            val input = getCustomView().findViewById<EditText>(R.id.hostingInput)
            val hint = getCustomView().findViewById<TextView>(R.id.hostingHint)

            //typing again puts the hint back, so the error never lingers after it is fixed
            input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = showHint(hint)
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            })

            positiveButton(res = R.string.settings_hosting_filter_add) {
                val domain = HostingFilter.normalize(input.text.toString())

                if (domain == null) showError(hint)
                else {
                    addHidden(domain)
                    dismiss()
                }
            }
        }
    }

    /**
     * Added already ticked - typing a domain in means wanting it hidden - and recorded as seen as
     * well, so that unticking it later leaves the row in place instead of making it disappear.
     */
    private fun addHidden(domain: String) {
        prefs.putStringSet(SettingsExtras.MANUAL_HOSTINGS, manual() + domain)
        prefs.putStringSet(SettingsExtras.HIDDEN_HOSTINGS, hidden() + domain)
        showHostings()
    }

    private fun confirmDelete(domain: String) {
        MaterialDialog(context!!).show {
            title(R.string.settings_hosting_filter_delete)
            //the domain is deliberately not named - some are long enough to wreck the dialog, and
            //the row that was just tapped is the answer to "which one"
            message(R.string.settings_hosting_filter_delete_message)
            negativeButton(res = R.string.common_cancel)
            positiveButton(res = R.string.settings_hosting_filter_delete) { delete(domain) }
        }
    }

    /**
     * Forgotten entirely, not just unhidden. Only offered for hand added domains, so this really
     * does remove the row for good.
     */
    private fun delete(domain: String) {
        prefs.putStringSet(SettingsExtras.MANUAL_HOSTINGS, manual() - domain)
        prefs.putStringSet(SettingsExtras.HIDDEN_HOSTINGS, hidden() - domain)

        adapter.remove(domain)
        //the list may have just become empty
        showEmptyIfNeeded()
    }

    private fun showEmptyIfNeeded() {
        val empty = adapter.itemCount == 0

        emptyView.visibleIf { empty }
        recyclerView.visibleIf { !empty }
    }

    private fun showHint(hint: TextView) {
        hint.setText(R.string.settings_hosting_filter_input_hint)
        hint.setTextColor(context!!.colorAttr(android.R.attr.textColorSecondary))
    }

    private fun showError(hint: TextView) {
        hint.setText(R.string.settings_hosting_filter_input_error)
        hint.setTextColor(androidx.core.content.ContextCompat.getColor(context!!, R.color.colorSecondary_red))
    }

    private fun seen(): Set<String> = prefs.getStringSet(SettingsExtras.SEEN_HOSTINGS, emptySet()).orEmpty()

    private fun manual(): Set<String> = prefs.getStringSet(SettingsExtras.MANUAL_HOSTINGS, emptySet()).orEmpty()

    private fun hidden(): Set<String> = prefs.getStringSet(SettingsExtras.HIDDEN_HOSTINGS, emptySet()).orEmpty()
}
