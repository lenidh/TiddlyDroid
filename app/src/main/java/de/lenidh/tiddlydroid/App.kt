package de.lenidh.tiddlydroid

import android.app.Application
import android.content.Context

/**
 * Global application state.
 */
class App @Deprecated("Instantiated by Android.", level = DeprecationLevel.HIDDEN) constructor()
    : Application() {

    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    companion object {
        private lateinit var instance: App private set

        /**
         * Context of the single, global Application object of the current process.
         *
         * @see getApplicationContext
         */
        val context: Context get() = instance.applicationContext
    }
}
