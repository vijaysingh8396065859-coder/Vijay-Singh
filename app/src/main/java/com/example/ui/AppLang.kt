package com.example.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App ki bhasha: Hindi ya English.
 * Choice phone par save rehti hai (SharedPreferences), Settings se badalti hai.
 * Koi bhi screen AppLang.s("हिंदी text", "English text") se current bhasha ka
 * text leti hai — AppLang.current ek Compose state hai, isliye bhasha badalte hi
 * poori UI khud refresh ho jati hai.
 */
enum class AppLanguage {
    HINDI,
    ENGLISH
}

object AppLang {
    private const val PREFS_NAME = "milk_diary_settings"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_DAIRY_NAME = "dairy_name"

    var current by mutableStateOf(AppLanguage.HINDI)
        private set

    var dairyName by mutableStateOf("")
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        current = if (prefs.getString(KEY_LANGUAGE, AppLanguage.HINDI.name) ==
            AppLanguage.ENGLISH.name
        ) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.HINDI
        }
        dairyName = prefs.getString(KEY_DAIRY_NAME, "") ?: ""
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        current = language
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    fun setDairyName(context: Context, name: String) {
        dairyName = name
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DAIRY_NAME, name)
            .apply()
    }

    /** Current bhasha ka text: Hindi mode me pehla, English mode me dusra। */
    fun s(hindi: String, english: String): String =
        if (current == AppLanguage.ENGLISH) english else hindi

    fun isEnglish(): Boolean = current == AppLanguage.ENGLISH
}
