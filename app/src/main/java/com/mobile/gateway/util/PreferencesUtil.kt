package com.mobile.gateway.util

import android.content.Context
import com.google.gson.Gson
import com.mobile.gateway.extension.toDataClass
import com.mobile.gateway.server.iso8583.ISO8583ResponseConfig
import com.mobile.gateway.server.iso8583.ISO8583ServerProfile
import java.util.Locale

object PreferencesUtil {
    fun clearPreferenceData(context: Context, path: String) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.remove(path)?.apply()
    }

    fun saveLocale(context: Context?, locale: Locale) {
        val localPref = context?.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.apply {
            putString(localeLanguagePrefKey, locale.language)
            putString(localeCountryPrefKey, locale.country)
            apply()
        }
    }

    fun getLocale(context: Context?): Locale {
        if (context == null) return Locale.getDefault()
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val localeLanguage = localPref.getString(localeLanguagePrefKey, "").orEmpty()
        val localeCountry = localPref.getString(localeCountryPrefKey, "").orEmpty()
        return if (localeLanguage.isEmpty() && localeCountry.isEmpty()) {
            val deviceLocale = context.resources.configuration.locales.get(0)
            Locale(deviceLocale.language, deviceLocale.country)
        } else {
            Locale(localeLanguage, localeCountry)
        }
    }


    fun getLocaleInfo(context: Context?, prefKey: String): String {
        if (context == null) return ""
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        return localPref.getString(prefKey, "").orEmpty()
    }

    fun saveLogFontSize(context: Context, fontSize: Float) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.putFloat(prefLogFontSize, fontSize)?.apply()
    }

    fun getLogFontSize(context: Context): Float {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        return localPref.getFloat(prefLogFontSize, 10F)
    }

    fun saveISO8583ResponseConfig(context: Context, config: ISO8583ResponseConfig) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = Gson().toJson(config)
        localPref?.edit()?.putString(prefISO8583ResponseConfig, jsonStr)?.apply()
    }

    fun getISO8583ResponseConfig(context: Context): ISO8583ResponseConfig {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = localPref.getString(prefISO8583ResponseConfig, null)
        return jsonStr?.toDataClass<ISO8583ResponseConfig>() ?: AssetsUtil.readFile(context, assetPathDefaultISO8385ResponseConfig)
    }

    fun saveISO8583ServerProfile(context: Context, config: ISO8583ServerProfile) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = Gson().toJson(config)
        localPref?.edit()?.putString(prefISO8583ServerProfile, jsonStr)?.apply()
    }

    fun getISO8583ServerProfile(context: Context): ISO8583ServerProfile {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        val jsonStr = localPref.getString(prefISO8583ServerProfile, null)
        return jsonStr?.toDataClass<ISO8583ServerProfile>() ?: AssetsUtil.readFile(context, assetPathDefaultISO8385ServerProfile)
    }

    fun saveDefaultPortNo(context: Context, portNo: String) {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        localPref?.edit()?.putString(prefDefaultPortNo, portNo)?.apply()
    }

    fun getDefaultPortNo(context: Context): String {
        val localPref = context.getSharedPreferences(localPrefFileName, Context.MODE_PRIVATE)
        return localPref.getString(prefDefaultPortNo, "10004") ?: "10004"
    }
}