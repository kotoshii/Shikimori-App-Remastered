package com.gnoemes.shikimori.presentation.view.base.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.DialogBaseBottomSheetBinding
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.dimen
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.drawable
import com.gnoemes.shikimori.utils.getCurrentAscentTheme
import com.gnoemes.shikimori.utils.widgets.TopEdgeBorderDrawable
import com.gnoemes.shikimori.utils.wrapTheme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.properties.ReadOnlyProperty

abstract class BaseBottomSheetDialogFragment : MvpDialogFragment() {

    protected lateinit var bottomSheet: FrameLayout
    open var autoExpand = true

    //the sheet's layout inside the shared one (dialog_base_bottom_sheet: a toolbar over the content),
    //or alone, for a sheet that brings its own
    protected open val useWrapperLayout = true

    var peekHeight = -1

    private val viewHandler = Handler()

    private val bindings = ViewBindings { contentRoot }

    /** The root of the sheet's own layout: the whole view, or inside the wrapper's fragment_content ([layoutRoot]). */
    private val contentRoot: android.view.View?
        get() {
            val view = view ?: return null
            if (!useWrapperLayout) return view
            if (getDialogLayout() == android.view.View.NO_ID) return null
            return context?.layoutRoot(view.findViewById(R.id.fragment_content), getDialogLayout())
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
        if (!useWrapperLayout) return inflater.inflate(getDialogLayout(), container, false)

        val view = inflater.inflate(R.layout.dialog_base_bottom_sheet, container, false)
        if (getDialogLayout() != android.view.View.NO_ID) {
            inflater.inflate(getDialogLayout(), view.findViewById(R.id.fragment_content), true)
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(context!!.wrapTheme(R.style.Theme_MaterialComponents_BottomSheetDialog), context!!.getCurrentAscentTheme)
                .apply {
                    //Material's slide up and down: wrapTheme lays the app theme over the sheet theme, and
                    //its window animation, a fade in place, won - a short sheet seemed to snap open
                    //(P7-T5 Test 79b)
                    window?.setWindowAnimations(R.style.Animation_MaterialComponents_BottomSheetDialog)
                    //the sheet sits above the keyboard and its content scrolls. The window panned, as
                    //the main screen does, and showed only the line with the cursor: the rest of a field
                    //low in the sheet stayed under the keyboard (P7-T5 Test 87h)
                    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    isCancelable = true
                    setCanceledOnTouchOutside(true)
                }
    }

    /**
     * The sheet is set up here: it is in the dialog now, and onStart shows it next. Set up when shown,
     * after its first frame was drawn, a full-height sheet slid in half open, apart from the status bar
     * strip its window draws, and jumped up a moment later (P7-T5 Q128).
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dialog = dialog as? BottomSheetDialog ?: return
        //the dialog's own context: the theme laid over the sheet theme, with the accent
        val context = dialog.context

        bottomSheet = dialog.findViewById(R.id.design_bottom_sheet)!!
        //every sheet's status bar shows the dimmed screen behind it. Only the sheets with a peek height did:
        //the full-screen ones («Фильтры», «Студии», «Эпизоды») kept the theme's colour, so two panels
        //opened from the same sheet coloured the status bar differently (P10-T4 Q18)
        dialog.window?.statusBarColor = Color.TRANSPARENT
        //that strip is dark in every theme, so its icons are light: the light theme's dark icons, which the
        //sheet's window takes from the theme (windowLightStatusBar), were barely readable on it (P10-T4 Test 49)
        //Android 11 and up; the older flag before it (core 1.1 has no compat call for this)
        val sheetWindow = dialog.window
        if (sheetWindow != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sheetWindow.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        } else if (sheetWindow != null) {
            val decor = sheetWindow.decorView
            decor.systemUiVisibility = decor.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        bottomSheet.background = context.drawable(windowBackground)
        //Amoled's sheets are black like the screen: a line along the top sets them
        //apart. Over the content, which the full-height sheets paint black (BUGS #17)
        val border = context.dimenAttr(R.attr.themeStrokeWidth)
        if (border > 0) bottomSheet.foreground = TopEdgeBorderDrawable(context.colorAttr(R.attr.colorDivider),
                border.toFloat(), context.dimen(R.dimen.sheet_corner_radius))
        clipToRoundedTop(bottomSheet, context.dimen(R.dimen.sheet_corner_radius))

        if (peekHeight == -1) bottomSheet.layoutParams = bottomSheet.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
        else BottomSheetBehavior.from(bottomSheet).peekHeight = peekHeight

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        //before its first layout the sheet only keeps the state, and is laid out in it
        if (autoExpand) {
            expandDialog()
        }

        BottomSheetBehavior.from(bottomSheet).skipCollapsed = true

        //BottomSheetDialog's own listener on this view calls cancel() outright, so
        //the sheet vanished instead of closing. Hiding it slides it down, and the
        //dialog's callback cancels once the slide finishes - which keeps onCancel
        //firing exactly as before.
        dialog.findViewById<android.view.View>(R.id.touch_outside)?.setOnClickListener {
            if (isCancelable) {
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }


    /**
     * The sheet's content is clipped to its rounded top corners, so a layout that paints its own background
     * («Фильтры», «Эпизоды», «Ссылки», «Статистика») cannot square them: only the sheets that painted none
     * showed the background's corners (P10-T4 Q19). Before Android 13 a view clips only to a round rect with
     * four equal corners, so the shape reaches one radius below the sheet, and its bottom corners fall
     * outside it.
     */
    private fun clipToRoundedTop(sheet: android.view.View, radius: Float) {
        sheet.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height + radius.toInt(), radius)
            }
        }
        sheet.clipToOutline = true
    }

    protected open fun expandDialog() {
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onDestroyView() {
        viewHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        bindings.clear()
    }

    @LayoutRes
    abstract fun getDialogLayout(): Int

    /**
     * A binding of the sheet's own layout: `private val binding by viewBinding(DialogXBinding::bind)`.
     * Bound on first use, dropped when the view goes ([ViewBindings]).
     */
    protected fun <T : ViewBinding> viewBinding(bind: (android.view.View) -> T): ReadOnlyProperty<Any, T> =
            bindings.of(bind)

    /**
     * The shared wrapper's views (dialog_base_bottom_sheet: the toolbar over the content), for a sheet that
     * uses it. A sheet without it has no view for this binding, so a read throws.
     */
    protected val wrapper by bindings.of(DialogBaseBottomSheetBinding::bind) { view?.takeIf { useWrapperLayout } }

    protected fun postViewAction(action: () -> Unit) {
        viewHandler.post { action.invoke() }
    }

    protected open val windowBackground = R.drawable.bg_rate_dialog_window
}