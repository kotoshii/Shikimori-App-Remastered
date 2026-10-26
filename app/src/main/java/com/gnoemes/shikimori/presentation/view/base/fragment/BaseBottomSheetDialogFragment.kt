package com.gnoemes.shikimori.presentation.view.base.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.dimen
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.drawable
import com.gnoemes.shikimori.utils.getCurrentAscentTheme
import com.gnoemes.shikimori.utils.widgets.TopEdgeBorderDrawable
import com.gnoemes.shikimori.utils.wrapTheme
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

abstract class BaseBottomSheetDialogFragment : MvpDialogFragment() {

    protected lateinit var bottomSheet: FrameLayout
    open var autoExpand = true

    //the sheet's layout inside the shared one (dialog_base_bottom_sheet: a toolbar over the content),
    //or alone, for a sheet that brings its own
    protected open val useWrapperLayout = true

    var peekHeight = -1

    private val viewHandler = Handler()

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
        if (peekHeight != -1) {
            dialog.window?.statusBarColor = Color.TRANSPARENT
        }

        bottomSheet.background = context.drawable(windowBackground)
        //Amoled's sheets are black like the screen: a line along the top sets them
        //apart. Over the content, which the full-height sheets paint black (BUGS #17)
        val border = context.dimenAttr(R.attr.themeStrokeWidth)
        if (border > 0) bottomSheet.foreground = TopEdgeBorderDrawable(context.colorAttr(R.attr.colorDivider),
                border.toFloat(), context.dimen(R.dimen.sheet_corner_radius))

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


    protected open fun expandDialog() {
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onDestroyView() {
        viewHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    @LayoutRes
    abstract fun getDialogLayout(): Int

    protected fun postViewAction(action: () -> Unit) {
        viewHandler.post { action.invoke() }
    }

    protected open val windowBackground = R.drawable.bg_rate_dialog_window
}