package de.lenidh.tiddlydroid

import android.app.Activity
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.view.View
import java.util.*

/**
 * Retrieve a string value from the preferences.
 *
 * @param [keyId] Resource id for the name of the preference to retrieve.
 * @param [defaultValue] Value to return if this preference does not exist.
 * @return The preference value if it exists, or `defaultValue`.
 * @throws [ClassCastException] When there is a preference with this name that is not a string.
 */
fun SharedPreferences.getString(keyId: Int, defaultValue: String = ""): String {
    val key = App.context.getString(keyId)
    return this.getString(key, defaultValue)
}

/**
 * Retrieve a set of string value from the preferences.
 *
 * @param [keyId] Resource id for the name of the preference to retrieve.
 * @param [defaultValue] Value to return if this preference does not exist.
 * @return The preference value if it exists, or `defaultValue`.
 * @throws [ClassCastException] When there is a preference with this name that is not a set.
 */
fun SharedPreferences.getStringSet(keyId: Int, defaultValue: Set<String> = Collections.emptySet())
        : Set<String> {
    val key = App.context.getString(keyId)
    return this.getStringSet(key, defaultValue)
}

/**
 * Set a string value in the preferences editor, to be written back once
 * [commit()][SharedPreferences.Editor.commit] or [apply()][SharedPreferences.Editor.apply] are
 * called.
 *
 * @param [keyId] Resource id for the name of the preference to modify.
 * @param [value] The new value for the preference.
 * @return A reference to the same Editor object, so you can chain put calls together.
 */
fun SharedPreferences.Editor.putString(keyId: Int, value: String): SharedPreferences.Editor {
    val key = App.context.getString(keyId)
    this.putString(key, value)
    return this
}

/**
 * Set a set of string value in the preferences editor, to be written back once
 * [commit()][SharedPreferences.Editor.commit] or [apply()][SharedPreferences.Editor.apply] are
 * called.
 *
 * @param [keyId] Resource id for the name of the preference to modify.
 * @param [value] The new value for the preference.
 * @return A reference to the same Editor object, so you can chain put calls together.
 */
fun SharedPreferences.Editor.putStringSet(keyId: Int, value: Set<String>)
        : SharedPreferences.Editor {
    val key = App.context.getString(keyId)
    this.putStringSet(key, value)
    return this
}

/**
 * Gets the activity the view is attached to.
 *
 * @return An activity or `null` if this view is not attached to an activity.
 */
val View.activity: Activity? get() {
    var context = this.context
    while (context is ContextWrapper) {
        if (context is Activity) { return context }
        context = context.baseContext
    }
    return null
}
