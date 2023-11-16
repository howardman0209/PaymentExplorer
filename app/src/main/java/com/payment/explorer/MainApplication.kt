package com.payment.explorer

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import androidx.room.RoomDatabase
import com.akexorcist.localizationactivity.core.LocalizationApplicationDelegate
import com.payment.explorer.model.Category
import com.payment.explorer.model.NavigationMenuData
import com.payment.explorer.model.Tool
import com.payment.explorer.util.DebugPanelManager
import com.payment.explorer.util.LIFECYCLE
import com.payment.explorer.util.PreferencesUtil


class MainApplication : Application(), ActivityLifecycleCallbacks {

    companion object {
        var activitiesAlive = ArrayList<String>()

        inline fun <reified T : RoomDatabase> getDatabase(
            context: Context,
            databaseName: String,
        ): T {
            val dbBuilder = Room.databaseBuilder(context, T::class.java, databaseName).fallbackToDestructiveMigration()
            return dbBuilder.build()
        }

        fun getNavigationMenuData(): NavigationMenuData {
            return NavigationMenuData(
                data = hashMapOf(
                    Category.GENERIC to listOf(
                        Tool.TLV_PARSER,
                        Tool.DES,
                        Tool.HASH,
                        Tool.BITWISE,
                        Tool.MAC,
                        Tool.CONVERTER,
                        Tool.RSA,
                    ),
                    Category.EMV to listOf(
                        Tool.CARD_SIMULATOR,
                        Tool.EMV_KERNEL,
                        Tool.ARQC,
                        Tool.ODA,
                        Tool.PIN_BLOCK
                    ),
                    Category.ACQUIRER to listOf(
                        Tool.HOST
                    )
                )
            )
        }
    }

    private var currentActiveActivity: Activity? = null
    private val localizationDelegate = LocalizationApplicationDelegate()

    override fun onCreate() {
        super.onCreate()
        Log.d(LIFECYCLE, "app onCreate")
        registerActivityLifecycleCallbacks(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        DebugPanelManager.initDebugPanel(applicationContext)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(LIFECYCLE, "${activity.javaClass.simpleName} onCreated")
        if (!activitiesAlive.contains(activity.javaClass.name)) {
            activitiesAlive.add(activity.javaClass.name)
            Log.d(LIFECYCLE, activitiesAlive.toString())
        }
    }

    override fun onActivityStarted(activity: Activity) {
//        Log.d(LIFECYCLE, "${activity.javaClass.name} onStarted")
    }

    override fun onActivityResumed(activity: Activity) {
//        Log.d(LIFECYCLE, "${activity.javaClass.name} onResumed")
        currentActiveActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
//        Log.d(LIFECYCLE, "${activity.javaClass.name} onPaused")
        currentActiveActivity = null
    }

    override fun onActivityStopped(activity: Activity) {
//        Log.d(LIFECYCLE, "${activity.javaClass.name} onStopped")
    }

    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(LIFECYCLE, "${activity.javaClass.simpleName} onDestroyed")
        activitiesAlive.remove(activity.javaClass.name)
        Log.d(LIFECYCLE, activitiesAlive.toString())
    }

    override fun attachBaseContext(base: Context) {
        localizationDelegate.setDefaultLanguage(base, PreferencesUtil.getLocale(base))
        super.attachBaseContext(localizationDelegate.attachBaseContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localizationDelegate.onConfigurationChanged(this)
    }

    override fun getApplicationContext(): Context {
        return localizationDelegate.getApplicationContext(super.getApplicationContext())
    }
}