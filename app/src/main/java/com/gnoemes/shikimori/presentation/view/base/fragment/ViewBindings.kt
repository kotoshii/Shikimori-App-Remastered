package com.gnoemes.shikimori.presentation.view.base.fragment

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import org.xmlpull.v1.XmlPullParser
import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A fragment's view bindings (ViewBinding: the build writes a class per layout, with a typed field per view
 * id). Each binds on first use to the view [root] gives, and [clear] drops them all when the fragment's view
 * goes: the base fragments call it after `super.onDestroyView()`, once Moxy has detached.
 *
 * Read before the view exists or after it went, a binding throws: a late use fails loudly instead of
 * silently doing nothing. That includes code after `super.onDestroyView()`, where the fragment still holds
 * the dying view: a binding made there would keep that view and serve it to the next one. A delayed callback
 * that can outlive the view (a posted action, a listener, an Rx result) checks `view ?: return` first (P7-T14).
 */
class ViewBindings(private val root: () -> View?) {

    private val bindings = mutableListOf<Binding<*>>()

    //the root of the view that went; weak, so it does not keep that view alive
    private var gone: WeakReference<View>? = null

    fun <T : ViewBinding> of(bind: (View) -> T): ReadOnlyProperty<Any, T> =
            Binding(bind).also { bindings += it }

    fun clear() {
        gone = root()?.let { WeakReference(it) }
        bindings.forEach { it.clear() }
    }

    private inner class Binding<T : ViewBinding>(private val bind: (View) -> T) : ReadOnlyProperty<Any, T> {

        private var value: T? = null

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            value?.let { return it }
            val view = root() ?: throw IllegalStateException("${property.name} read without a view")
            if (view === gone?.get()) throw IllegalStateException("${property.name} read after its view went")
            return bind(view).also { value = it }
        }

        fun clear() {
            value = null
        }
    }
}

/**
 * The view a layout inflated into [parent] binds to: the layout's own root, the one child [parent] holds,
 * or [parent] itself for a `<merge>` layout, whose views went straight into it.
 */
fun Context.layoutRoot(parent: ViewGroup, @LayoutRes layout: Int): View =
        if (isMergeLayout(layout)) parent else parent.getChildAt(0)

private fun Context.isMergeLayout(@LayoutRes layout: Int): Boolean {
    val parser = resources.getLayout(layout)
    try {
        var event = parser.next()
        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) event = parser.next()
        return event == XmlPullParser.START_TAG && parser.name == "merge"
    } finally {
        parser.close()
    }
}
