package com.needed.books

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Utility for persisting and applying application locale (English & Bangla) in Kotlin.
 * Fully guarded against early attach lifecycle issues and class resolution errors.
 */
object LocaleHelper {
    private const val PREFS_NAME = "needed_books_prefs"
    const val KEY_LANGUAGE = "app_language_pref"
    const val LANG_ENGLISH = "en"
    const val LANG_BANGLA = "bn"

    @JvmStatic
    fun onAttach(context: Context): Context {
        return try {
            val lang = getPersistedLanguage(context, LANG_ENGLISH)
            updateResources(context, lang)
        } catch (e: Throwable) {
            context
        }
    }

    @JvmStatic
    fun getLanguage(context: Context): String {
        return try {
            getPersistedLanguage(context, LANG_ENGLISH)
        } catch (e: Throwable) {
            LANG_ENGLISH
        }
    }

    @JvmStatic
    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return try {
            updateResources(context, language)
        } catch (e: Throwable) {
            context
        }
    }

    @JvmStatic
    fun getPersistedLanguage(context: Context, defaultLanguage: String): String {
        return try {
            val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            preferences.getString(KEY_LANGUAGE, defaultLanguage) ?: defaultLanguage
        } catch (e: Throwable) {
            defaultLanguage
        }
    }

    private fun persist(context: Context, language: String) {
        try {
            val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            preferences.edit().putString(KEY_LANGUAGE, language).apply()
        } catch (e: Throwable) {
            // Ignore safely
        }
    }

    @JvmStatic
    fun applyLocale(context: Context) {
        try {
            val lang = getLanguage(context)
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val resources = context.resources
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                config.setLocales(localeList)
            }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (e: Throwable) {
            // Ignore safely
        }
    }

    @JvmStatic
    fun updateResources(context: Context, language: String): Context {
        return try {
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)

            val resources = context.resources
            val configuration = Configuration(resources.configuration)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocale(locale)
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
                context.createConfigurationContext(configuration)
            } else {
                @Suppress("DEPRECATION")
                configuration.locale = locale
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
                context
            }
        } catch (e: Throwable) {
            context
        }
    }
}
