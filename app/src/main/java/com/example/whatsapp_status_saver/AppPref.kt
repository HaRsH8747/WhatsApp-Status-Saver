package com.example.whatsapp_status_saver

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class AppPref(context: Context) {

    fun getString(key: String?): String? {
        return appSharedPref.getString(key, "")
    }

    fun setString(key: String?, value: String?) {
        prefEditor.putString(key, value).apply()
    }

    fun clearString(key: String?) {
        setString(key, "")
    }

    companion object{
        const val WHATSAPP_STATUS_SAVER = "WHATSAPP_STATUS_SAVER"
        lateinit var appSharedPref: SharedPreferences
        lateinit var prefEditor: SharedPreferences.Editor
        const val FAVOURITE_ITEMS = "FAVOURITE_ITEMS"
        const val PATH = "PATH"
    }
    init {
        appSharedPref = context.getSharedPreferences(WHATSAPP_STATUS_SAVER, Activity.MODE_PRIVATE)
        prefEditor = appSharedPref.edit()
    }
}