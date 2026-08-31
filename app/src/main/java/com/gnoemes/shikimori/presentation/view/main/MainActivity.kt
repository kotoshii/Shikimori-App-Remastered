package com.gnoemes.shikimori.presentation.view.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.crashlytics.android.Crashlytics
import com.gnoemes.shikimori.BuildConfig
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.AnalyticEvent
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.data.local.services.impl.AppUpdateService
import com.gnoemes.shikimori.data.network.GithubApi
import com.gnoemes.shikimori.entity.app.data.GithubReleaseResponse
import com.gnoemes.shikimori.entity.main.BottomScreens
import com.gnoemes.shikimori.presentation.view.update.ChangelogDialog
import com.gnoemes.shikimori.presentation.presenter.main.MainPresenter
import com.gnoemes.shikimori.presentation.view.base.activity.BaseActivity
import com.gnoemes.shikimori.presentation.view.base.fragment.BottomNavigationProvider
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.base.fragment.TabContainer
import com.gnoemes.shikimori.presentation.view.bottom.BottomTabContainer
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.navigation.SupportAppNavigator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.layout_bottom_bar.*
import ru.terrakok.cicerone.Navigator
import io.reactivex.schedulers.Schedulers
import ru.terrakok.cicerone.NavigatorHolder
import ru.terrakok.cicerone.Router
import ru.terrakok.cicerone.commands.Command
import ru.terrakok.cicerone.commands.Replace
import javax.inject.Inject

class MainActivity : BaseActivity<MainPresenter, MainView>(), MainView, RouterProvider, BottomNavigationProvider {

    companion object {
        /** Set by the "Изменения" action of the update notification. */
        const val EXTRA_SHOW_CHANGELOG = "EXTRA_SHOW_CHANGELOG"
    }

    @InjectPresenter
    lateinit var mainPresenter: MainPresenter

    @ProvidePresenter
    fun providePresenter(): MainPresenter = presenterProvider.get()

    @Inject
    lateinit var localNavigatorHolder: NavigatorHolder

    @Inject
    lateinit var githubApi: GithubApi

    private val tabs = arrayOf(
            Tab(R.id.tab_rates, BottomScreens.RATES),
            Tab(R.id.tab_calendar, BottomScreens.CALENDAR),
            Tab(R.id.tab_search, BottomScreens.SEARCH),
            Tab(R.id.tab_main, BottomScreens.MAIN),
            Tab(R.id.tab_more, BottomScreens.MORE)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBottomNav()
        initContainer()
        if (savedInstanceState == null) {
            syncValues()
            checkForUpdate()
        }

        showChangelogIfRequested()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //the notification reuses this activity rather than stacking a second copy of the app
        setIntent(intent)
        showChangelogIfRequested()
    }

    private fun initBottomNav() {
        bottomNav.setOnNavigationItemSelectedListener { item ->
            val tab = tabs.find { it.id == item.itemId }!!
            analyzeNavigation(tab.screenKey)
            presenter.onTabItemSelected(tab.screenKey)
            true
        }
        bottomNav.setOnNavigationItemReselectedListener { item ->
            val tab = tabs.find { it.id == item.itemId }!!
            presenter.onTabItemReselected(tab.screenKey)
        }

        navbarDivider.visibleIf { getCurrentTheme != R.style.ShikimoriAppTheme_Amoled }
    }

    private fun initContainer() {
        val fm = fragmentManager
        val ta = fm.beginTransaction()
        tabs.forEach { tab ->
            var fragment: Fragment? = fm.findFragmentByTag(tab.screenKey)
            if (fragment == null) {
                fragment = BottomTabContainer.newInstance()
                ta.add(R.id.activity_container, fragment, tab.screenKey)
                        .detach(fragment)
                        .commitNow()
            }
        }
        ta.commitNow()
    }

    private fun syncValues() {
        val db = FirebaseFirestore.getInstance()

        db.collection("app")
                .get()
                .addOnSuccessListener {
                    val donationLink = it.documents.firstOrNull()?.data?.get("donationLink") as? String

                    getDefaultSharedPreferences().putString(SettingsExtras.DONATION_LINK, donationLink)
                    getDefaultSharedPreferences().putString(SettingsExtras.SHIKICINEMA_URL, Constants.SHIKICINEMA_URL)
                }.addOnFailureListener { Crashlytics.logException(it) }
    }

    /**
     * The old check compared the version name against `lastVersion` in the upstream firestore,
     * which this fork does not control, so it always reported an update. This asks our own
     * releases instead, and compares properly so only a genuinely newer tag counts.
     */
    private fun checkForUpdate() {
        //the check can be turned off in settings; the badge and anything already stored stay as
        //they are, so an update found earlier is still reachable
        if (!getDefaultSharedPreferences().getBoolean(SettingsExtras.CHECK_UPDATES_ON_START, true)) return

        githubApi.getLatestRelease()
                .subscribeOn(Schedulers.io())
                .subscribe({ onReleaseChecked(it) }, { Crashlytics.logException(it) })
    }

    /**
     * Keeps the newest release in preferences so the changelog dialog and the download work without
     * asking github again - the settings badge can be tapped hours later, possibly offline.
     */
    private fun onReleaseChecked(release: GithubReleaseResponse) {
        val hasUpdate = compareVersions(release.tag, BuildConfig.VERSION_NAME) > 0
        val prefs = getDefaultSharedPreferences()

        prefs.putBoolean(SettingsExtras.NEW_VERSION_AVAILABLE, hasUpdate)

        if (!hasUpdate) {
            //nothing newer - drop what an earlier check left, or the badge would open a stale changelog
            prefs.remove(SettingsExtras.NEW_VERSION_TAG)
            prefs.remove(SettingsExtras.NEW_VERSION_CHANGELOG)
            prefs.remove(SettingsExtras.NEW_VERSION_APK_URL)
            return
        }

        //tags are written as v0.8.10; the app talks about versions, not tags
        val version = release.tag.orEmpty().trimStart('v', 'V')

        prefs.putString(SettingsExtras.NEW_VERSION_TAG, version)
        prefs.putString(SettingsExtras.NEW_VERSION_CHANGELOG, release.body)
        prefs.putString(SettingsExtras.NEW_VERSION_APK_URL, release.apkUrl)

        //this runs on an io thread and can outlive the activity, so the notification uses the app
        AppUpdateService.notifyAvailable(applicationContext, version, release.apkUrl)
    }

    private fun showChangelogIfRequested() {
        if (intent?.getBooleanExtra(EXTRA_SHOW_CHANGELOG, false) != true) return

        //dropped once handled, or a rotation would bring the dialog back
        intent.removeExtra(EXTRA_SHOW_CHANGELOG)
        showChangelog()
    }

    /** Falls back to the releases page when no check has stored a release yet. */
    private fun showChangelog() {
        val dialog = ChangelogDialog.fromPreferences(this)

        if (dialog != null) dialog.show(supportFragmentManager, ChangelogDialog.TAG)
        else startActivity(Intent(Intent.ACTION_VIEW, Constants.GITHUB_RELEASES_URL.toUri()))
    }

    private fun invokeTabRootActionOrClearBackStack(screenKey: String) {
        val fm = fragmentManager
        val fragment: Fragment? = fm.findFragmentByTag(screenKey)
        fragment.ifNotNull {
            if (it is TabContainer && it.invokeTabRootAction())
            else (it as RouterProvider).localRouter.backTo(null)
        }
    }

    private fun clearBackStack(screenKey: String) {
        val fm = fragmentManager
        val fragment: Fragment? = fm.findFragmentByTag(screenKey)
        fragment.ifNotNull {
            (it as RouterProvider).localRouter.backTo(null)
        }
    }

    private fun analyzeNavigation(screenKey: String) {
        presenter.apply {
            when (screenKey) {
                BottomScreens.RATES -> logEvent(AnalyticEvent.NAVIGATION_BOTTOM_RATES)
                BottomScreens.CALENDAR -> logEvent(AnalyticEvent.NAVIGATION_BOTTOM_CALENDAR)
                BottomScreens.SEARCH -> logEvent(AnalyticEvent.NAVIGATION_BOTTOM_SEARCH)
                BottomScreens.MAIN -> logEvent(AnalyticEvent.NAVIGATION_BOTTOM_MAIN)
                BottomScreens.MORE -> logEvent(AnalyticEvent.NAVIGATION_BOTTOM_MORE)
            }
        }
    }

    override fun changeTab(screen: String) {
        val tab = tabs.find { it.screenKey == screen }
        if (tab != null) {
            clearBackStack(tab.screenKey)
            bottomNav.selectedItemId = tab.id
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getLayoutActivity(): Int = R.layout.activity_main

    override val presenter: MainPresenter
        get() = mainPresenter

    override fun getNavigator(): Navigator = object : SupportAppNavigator(this@MainActivity, fragmentManager, R.id.activity_container) {

        override fun createActivityIntent(context: Context?, screenKey: String?, data: Any?): Intent? = null

        override fun unknownScreen(command: Command?) {
            val message = getString(R.string.error_not_realized)
            Log.e("ERR", message)
            showSystemMessage(message)
        }

        override fun createFragment(screenKey: String?, data: Any?): Fragment? = null

        override fun showSystemMessage(message: String?) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }

        override fun replace(command: Replace) {
            val fm = fragmentManager
            val ta = fm.beginTransaction()
            tabs.forEach { tab ->
                val fragment = fm.findFragmentByTag(tab.screenKey)!!
                if (tab.screenKey == command.screenKey) {
                    if (fragment.isDetached) {
                        ta.attach(fragment)
                    }
                    ta.show(fragment)
                } else {
                    ta.detach(fragment)
                }
            }
            ta.commitNow()
        }

        var canExit = false

        override fun exit() {
            if (!canExit) {
                presenter.router.showSystemMessage(getString(R.string.main_exit_message))
                canExit = true
                Handler().postDelayed({ canExit = false }, Constants.EXIT_TIMEOUT)
            } else {
                finish()
            }
        }

        override fun setupFragmentTransactionAnimation(command: Command?, currentFragment: Fragment?, nextFragment: Fragment?, ft: FragmentTransaction?) {
            if (command is Replace) {
                ft?.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
            }
        }
    }

    override fun getNavigatorHolder(): NavigatorHolder = localNavigatorHolder

    override val localRouter: Router
        get() = presenter.router

    override val localNavigator: Navigator
        get() = getNavigator()

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun setTitle(title: String) = Unit

    override fun clearMoreBackStack() = clearBackStack(BottomScreens.MORE)

    override fun clearMainBackStack() = clearBackStack(BottomScreens.MAIN)

    override fun searchActionOrClearSearchBackStack() = invokeTabRootActionOrClearBackStack(BottomScreens.SEARCH)

    override fun clearCalendarBackStack() = clearBackStack(BottomScreens.CALENDAR)

    override fun rateActionOrClearBackStack() = invokeTabRootActionOrClearBackStack(BottomScreens.RATES)


    data class Tab(
            val id: Int,
            val screenKey: String
    )
}
