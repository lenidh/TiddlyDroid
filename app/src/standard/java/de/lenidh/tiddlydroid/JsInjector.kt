package de.lenidh.tiddlydroid

import android.webkit.WebView

internal class JsInjector constructor(private val view: WebView) {

    fun injectScript(script: String, callback: (String) -> Unit) {
        view.evaluateJavascript(script, callback)
    }
}
