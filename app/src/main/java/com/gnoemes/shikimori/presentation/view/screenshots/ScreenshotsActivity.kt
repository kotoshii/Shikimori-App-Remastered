package com.gnoemes.shikimori.presentation.view.screenshots

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.services.impl.DownloadService
import com.gnoemes.shikimori.entity.anime.domain.ScreenshotsNavigationData
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.entity.download.DownloadFileData
import com.gnoemes.shikimori.presentation.view.base.activity.MvpActivity
import com.gnoemes.shikimori.presentation.view.screenshots.adapter.ScreenshotPagerAdapter
import com.gnoemes.shikimori.utils.*
import com.kotlinpermissions.KotlinPermissions
import kotlinx.android.synthetic.main.activity_screenshots.*


class ScreenshotsActivity : MvpActivity() {

    companion object {
        private const val CURRENT_PAGE = "CURRENT_PAGE"
        private const val UI_VISIBLE = "UI_VISIBLE"
        private const val SCREENSHOTS_DATA_KEY = "SCREENSHOTS_DATA_KEY"

        //next to the episodes' "anime" folder, with a folder per anime inside as well
        private const val SCREENSHOTS_FOLDER = "screenshots"
        fun newIntent(context: Context?, data: ScreenshotsNavigationData): Intent {
            val intent = Intent(context, ScreenshotsActivity::class.java)
            intent.putExtra(SCREENSHOTS_DATA_KEY, data)
            return intent
        }
    }

    private var adapter: ScreenshotPagerAdapter? = null
    private var itemCount = 0
    private val formatString by lazy { getString(R.string.common_count_format) }
    private var uiVisible = true
    private var animeName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ShikimoriAppTheme_Screenshots)
        theme.applyStyle(getCurrentAscentTheme, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screenshots)

        toolbar.run {
            addBackButton(R.drawable.ic_close) { finish() }
            inflateMenu(R.menu.menu_screenshots)
            onMenuClick {
                when (it?.itemId) {
                    R.id.item_share -> share(getCurrentScreenshot())
                    R.id.item_download -> download(getCurrentScreenshot())
                }
                true
            }
        }
        with(viewpager) {
            offscreenPageLimit = 5
            addOnPageChangeListener(pageChangeCallback)
        }

        if (intent != null) {
            val data: ScreenshotsNavigationData = intent.getParcelableExtra(SCREENSHOTS_DATA_KEY)
            adapter = ScreenshotPagerAdapter(data.items, this::toggleUI, this::onSwipe, this::onDismiss)
            itemCount = data.items.size
            animeName = data.animeName
            val pos = savedInstanceState?.getInt(CURRENT_PAGE, data.selected) ?: data.selected
            viewpager.adapter = this@ScreenshotsActivity.adapter
            viewpager.setCurrentItem(pos, false)
            toolbar.title = String.format(formatString, pos + 1, data.items.size)
        }

        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, insets ->
            v.setPadding(0, insets.systemWindowInsetTop, insets.systemWindowInsetRight, 0)
            insets
        }

        uiVisible = savedInstanceState?.getBoolean(UI_VISIBLE, true) ?: true
        if (uiVisible) showUi()
        else hideUi()
    }

    private fun getCurrentScreenshot(): String? {
        return (viewpager.adapter as? ScreenshotPagerAdapter)?.items?.getOrNull(viewpager.currentItem)?.original
    }

    private fun onSwipe() {
        hideUi(false)
    }

    private fun onDismiss() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun share(url: String?) {
        val intent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }, getString(R.string.common_share))
        startActivity(intent)
    }

    private fun download(url: String?) {
        if (url == null) return

        KotlinPermissions.with(this)
                .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .onAccepted { save(url) }
                .ask()
    }

    /**
     * Through the app's own downloader into the user's download folder, with the same prompts as an
     * episode. Android's `DownloadManager` used to put these in the app's private folder, which
     * gallery apps do not show and which is deleted with the app.
     */
    private fun save(url: String) {
        val folder = getDefaultSharedPreferences().getString(SettingsExtras.DOWNLOAD_FOLDER, "").orEmpty()
        if (folder.isEmpty()) {
            showFolderChooserDialog(url)
            return
        }

        val data = DownloadFileData(url,
                listOf(SCREENSHOTS_FOLDER, animeName),
                //the url ends in a "?<timestamp>" query, which is not part of the name
                Uri.parse(url).lastPathSegment ?: url.substringAfterLast('/'),
                getString(R.string.download_screenshot_title, animeName))
        DownloadService.enqueue(this, data, folder)
    }

    private fun showFolderChooserDialog(url: String) {
        MaterialDialog(this).show {
            folderChooser(
                    allowFolderCreation = true,
                    emptyTextRes = R.string.download_folder_empty,
                    folderCreationLabel = R.string.download_new_folder)
            { _, file ->
                this@ScreenshotsActivity.getDefaultSharedPreferences()
                        .putString(SettingsExtras.DOWNLOAD_FOLDER, file.absolutePath)
            }
            positiveButton { save(url) }
        }
    }

    private fun toggleUI() {
        if (uiVisible) hideUi()
        else showUi()
        uiVisible = !uiVisible
    }

    private fun showUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        TransitionManager.beginDelayedTransition(coordinator, Fade().apply { duration = 110 })
        appBarLayout.visible()
    }


    private fun hideUi(animate : Boolean = true) {
        window.decorView.systemUiVisibility = (
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
        if (animate) TransitionManager.beginDelayedTransition(coordinator, Fade().apply { duration = 110 })
        appBarLayout.gone()
    }

    override fun onDestroy() {
        super.onDestroy()

        viewpager?.removeOnPageChangeListener(pageChangeCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(CURRENT_PAGE, viewpager?.currentItem ?: 0)
        outState.putBoolean(UI_VISIBLE, uiVisible)
    }

    private val pageChangeCallback = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            toolbar?.title = String.format(formatString, position + 1, itemCount)
        }
    }
}