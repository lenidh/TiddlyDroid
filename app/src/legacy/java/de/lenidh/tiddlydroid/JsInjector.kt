package de.lenidh.tiddlydroid

import android.annotation.SuppressLint
import android.os.Build
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

@SuppressLint("AddJavascriptInterface")
internal class JsInjector constructor(private val view: WebView) {

    private val callbacks: MutableMap<Int, (String) -> Unit> = HashMap()
    private var nextId: Int = 0
        get() = field++
        set(_) = throw NotImplementedError()

    init {
        view.addJavascriptInterface(CallbackInterface(), "appCallback")
    }

    fun injectScript(script: String, callback: (String) -> Unit) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(script, callback)
        } else {
            requireUiThread()

            val id = nextId
            Log.i(TAG, "Inject script ($id)")
            callbacks[id] = callback
            view.loadUrl(formatScript(id, script))
        }
    }

    private fun formatScript(id: Int, script: String): String {
        var result = script
        result = result.replace("//.*".toRegex(), "") // remove line comments
        result = result.replace("\\", "\\\\") // mask backslash
        result = result.replace("\"", "\\\"") // mask double quote
        result = result.replace("\r\n|\n".toRegex(), "\\\\n") // mask line delimiter
        result = ARMOR.format(id, result)
        Log.d(TAG, "Formatted script ($id): $result")
        return result
    }

    private fun requireUiThread() {
        val thread = Thread.currentThread()
        if(thread != Looper.getMainLooper().thread) {
            throw RuntimeException("Must be called on UI thread (thread was '${thread.name}').")
        }
    }

    private inner class CallbackInterface {

        @JavascriptInterface
        @Suppress("unused")
        fun result(id: Int, result: String) {
            Log.d(TAG, "Script ($id) result: $result")

            // invoke callback and clean up callback registration
            callbacks.remove(id)?.let { cb ->
                view.activity?.runOnUiThread { cb.invoke(result) }
                        ?: Log.w(TAG, "Unable to invoke callback: WebView is not attached to an activity.")
            }
        }

        @JavascriptInterface
        @Suppress("unused")
        fun error(id: Int, message: String) {
            Log.e(TAG, "Script ($id) error: $message")
            callbacks.remove(id) // clean up callback registration
        }
    }

    companion object {
        private val TAG = JsInjector::class.java.name
        private const val ARMOR = """javascript:(function(){"use strict";var id = %d;try{var result = eval("%s");appCallback.result(id,JSON.stringify(result));}catch(e){appCallback.error(id,e.message);}})();"""
    }
}
