package com.gnoemes.shikimori.presentation.view.base.fragment

import android.app.Activity
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
 * id). Each binds on first use to its root - the one [of] was given, by default [root] - and [clear] drops
 * them all when the fragment's view goes: the base fragments call it after `super.onDestroyView()`, once Moxy
 * has detached.
 *
 * Read before the view exists or after it went, a binding throws: a late use fails loudly instead of
 * silently doing nothing. That includes code after `super.onDestroyView()`, where the fragment still holds
 * the dying view: a binding made there would keep that view and serve it to the next one. A delayed callback
 * that can outlive the view (a posted action, a listener, an Rx result) checks `view ?: return` first (P7-T14).
 */
class ViewBindings(private val root: () -> View?) {

    private val bindings = mutableListOf<Binding<*>>()

    /** A binding of the view [root] gives; by default the one this holder was made with. */
    fun <T : ViewBinding> of(bind: (View) -> T, root: () -> View? = this.root): ReadOnlyProperty<Any, T> =
            Binding(bind, root).also { bindings += it }

    fun clear() = bindings.forEach { it.clear() }

    private class Binding<T : ViewBinding>(
            private val bind: (View) -> T,
            private val root: () -> View?
    ) : ReadOnlyProperty<Any, T> {

        private var value: T? = null

        //the root of the view that went; weak, so it does not keep that view alive
        private var gone: WeakReference<View>? = null

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            value?.let { return it }
            val view = root() ?: throw IllegalStateException("${property.name} read without a view")
            if (view === gone?.get()) throw IllegalStateException("${property.name} read after its view went")
            return bind(view).also { value = it }
        }

        fun clear() {
            gone = root()?.let { WeakReference(it) }
            value = null
        }
    }
}

/**
 * An activity's binding of the layout it set with `setContentView(layout)`, made on first use, after that call.
 * An activity's views live as long as it does, so nothing clears it (P7-T14).
 */
fun <T : ViewBinding> Activity.viewBinding(@LayoutRes layout: Int, bind: (View) -> T): Lazy<T> =
        lazy(LazyThreadSafetyMode.NONE) { bindContent(layout, bind) }

/** A binding of the layout the activity set with `setContentView(layout)`, made now. */
fun <T : ViewBinding> Activity.bindContent(@LayoutRes layout: Int, bind: (View) -> T): T =
        bind(layoutRoot(findViewById(android.R.id.content), layout))

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
