package com.segmentify.segmentifyandroidsdk.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

abstract class PreferencesManager(targetContext: Context) {
    private val defaultPackageName = "AppcentPackageSettings"
    private val emptyString = ""
    private var preferenceName: String = ""
    private var targetPreferences: SharedPreferences
    private val gson = Gson()

    init {
        targetPreferences = targetContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    }

    fun clearKey(key: String) {
        if (targetPreferences.contains(key)) {
            putString(key, null)
        }
    }

    fun getBooleanValue(key: String): Boolean {
        return targetPreferences.getBoolean(key, false)
    }

    fun getBooleanValue(key: String, defaultValue: Boolean): Boolean {
        return targetPreferences.getBoolean(key, defaultValue)
    }

    fun getFloat(key: String): Float {
        return targetPreferences.getFloat(key, 0f)
    }

    fun getInt(key: String): Int {
        return targetPreferences.getInt(key, 0)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return targetPreferences.getInt(key, defaultValue)
    }

    fun getLong(key: String): Long {
        return targetPreferences.getLong(key, 0)
    }

    fun getPreferenceName(): String {
        return preferenceName
    }

    fun getString(key: String): String? {
        return getString(key, emptyString)
    }

    fun getString(key: String, defaultValue: String): String? {
        return targetPreferences.getString(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean): Boolean {
        val targetEditor = targetPreferences.edit()
        targetEditor.putBoolean(key, value)
        return targetEditor.commit()
    }

    fun putFloat(key: String, targetValue: Float): Boolean {
        val targetEditor = targetPreferences.edit()
        return targetEditor.putFloat(key, targetValue).commit()
    }

    fun putInt(key: String, targetValue: Int): Boolean {
        val targetEditor = targetPreferences.edit()
        return targetEditor.putInt(key, targetValue).commit()
    }

    fun putLong(key: String, targetValue: Long): Boolean {
        val targetEditor = targetPreferences.edit()
        return targetEditor.putLong(key, targetValue).commit()
    }

    /** Like [putLong], but writes to disk in the background: for values written on every event. */
    fun putLongInBackground(key: String, targetValue: Long) {
        targetPreferences.edit().putLong(key, targetValue).apply()
    }

    fun putObject(key: String, targetObject: Any) {
        putString(key, gson.toJson(targetObject))
    }

    fun putString(keys: Array<String>, values: Array<String>) {
        val targetEditor = targetPreferences.edit()
        var counter = 0
        for (key in keys) {
            targetEditor.putString(key, values[counter++])
        }
        targetEditor.apply()
    }

    fun putString(key: String, value: String?) {
        val targetEditor = targetPreferences.edit()
        targetEditor.putString(key, value)
        targetEditor.apply()
    }


}