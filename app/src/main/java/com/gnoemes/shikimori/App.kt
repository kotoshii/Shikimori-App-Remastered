package com.gnoemes.shikimori

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import com.gnoemes.shikimori.di.app.component.DaggerAppComponent
import dagger.android.*
import io.fabric.sdk.android.Fabric
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import net.danlew.android.joda.JodaTimeAndroid
import java.io.IOException
import javax.inject.Inject

class App : Application(), HasActivityInjector, HasServiceInjector, HasBroadcastReceiverInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var serviceDispatchingAndroidInjector: DispatchingAndroidInjector<Service>

    @Inject
    lateinit var broadcastReceiverDispatchingAndroidInjector: DispatchingAndroidInjector<BroadcastReceiver>

    override fun onCreate() {
        super.onCreate()
        RxJavaPlugins.setErrorHandler(::logUndeliverable)
        Fabric.with(this, Crashlytics())
        JodaTimeAndroid.init(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        DaggerAppComponent.builder().create(this).inject(this)
    }

    /**
     * RxJava sends an error here when its subscriber is already gone. That happens routinely in
     * this app: `BaseNetworkPresenter.onDestroy` clears its `CompositeDisposable` while parses are
     * still in flight, so leaving a screen mid-parse orphans the error. With no handler installed
     * RxJava rethrows it on the scheduler thread and the **process dies** - which is why a
     * third-party site changing its html could take the whole app down.
     *
     * Nothing is rethrown here. That is a deliberate departure from RxJava's own recommended
     * handler, which rethrows `NullPointerException` as "likely a bug in the application": in this
     * app the commonest undeliverable NPE is a parser returning null into `map` because a site
     * changed, which is precisely the case this exists to survive.
     *
     * This only affects errors that could **not** be delivered. An error arriving while its screen
     * is alive still goes to `BaseNetworkPresenter.processErrors` and is shown to the user as
     * before - this changes nothing there.
     *
     * Runs on arbitrary threads, so it must only log: no ui, no navigation.
     */
    private fun logUndeliverable(error: Throwable) {
        when (val cause = (error as? UndeliverableException)?.cause ?: error) {
            //expected when a subscription is disposed mid-flight and the socket closes under it
            is IOException, is InterruptedException -> Log.d(TAG_RX, "dropped after dispose: $cause")
            //a parser giving up, or a real bug. Either way it must not kill the app - but be loud,
            //because with the crash gone this log line is the only trace left.
            else -> Log.e(TAG_RX, "undeliverable error, dropped", cause)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        private const val TAG_RX = "RxUndeliverable"
    }

    override fun activityInjector(): AndroidInjector<Activity> = dispatchingAndroidInjector
    override fun serviceInjector(): AndroidInjector<Service> = serviceDispatchingAndroidInjector
    override fun broadcastReceiverInjector(): AndroidInjector<BroadcastReceiver> = broadcastReceiverDispatchingAndroidInjector
}