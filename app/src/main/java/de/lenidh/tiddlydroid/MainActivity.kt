package de.lenidh.tiddlydroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.JsonReader
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import de.lenidh.tiddlydroid.databinding.ActivityMainBinding
import de.lenidh.tiddlydroid.databinding.NavHeaderMainBinding
import java.io.FileOutputStream
import java.io.StringReader
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val TAG = this.javaClass.simpleName

    private var activeUri: Uri = Uri.EMPTY
    private lateinit var descriptors: WikiDescriptors
    private lateinit var descriptorMenuItems: MutableMap<WikiDescriptor, MenuItem>

    private lateinit var binding: ActivityMainBinding
    private val toolbar: Toolbar get() = binding.appBarMain.toolbar
    private val drawerLayout: DrawerLayout get() = binding.drawerLayout
    private val navView: NavigationView get() = binding.navView
    private val webView: WebView get() = binding.appBarMain.contentMain.webView
    private val progressBar: ProgressBar get() = binding.appBarMain.contentMain.progressBar

    private lateinit var navHeaderMainBinding: NavHeaderMainBinding
    private val wikiTitleView: TextView get() = navHeaderMainBinding.wikiTitleView
    private val wikiSubtitleView: TextView get() = navHeaderMainBinding.wikiSubtitleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        navHeaderMainBinding = NavHeaderMainBinding.bind(binding.navView.getHeaderView(0))

        setContentView(binding.root)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        loadPreferences()

        // Generate menu items for each known TiddlyWiki (metadata stored in preferences).
        descriptorMenuItems = HashMap()
        descriptors.forEach { addMenuItemFor(it) }

        initWikiView()
        updateWikiView()
    }

    override fun onPause() {
        savePreferences()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == ACTIVITY_RESULT_CHOOSE_WIKI && resultCode == Activity.RESULT_OK
                && data != null) {
            addWiki(WikiDescriptor(data.data))
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_add_wiki -> startWikiChooser()
            R.id.nav_remove_wiki -> removeCurrentWiki()
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadPreferences() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        activeUri = Uri.parse(sharedPrefs.getString(R.string.pref_key_active_wiki))

        val set = sharedPrefs.getStringSet(R.string.pref_key_wiki_descriptors)
        descriptors = WikiDescriptors.fromStringSet(set)
    }

    private fun savePreferences() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        val editor = sharedPrefs.edit()
        editor.putString(R.string.pref_key_active_wiki, activeUri.toString())
        editor.putStringSet(R.string.pref_key_wiki_descriptors, descriptors.toStringSet())
        editor.apply()
    }

    @SuppressLint("SetJavaScriptEnabled") // TiddlyWiki requires JavaScript engine
    private fun initWikiView() {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(AppJavascriptInterface(), "app")
        webView.webViewClient = WikiViewClient(webView)
        webView.webChromeClient = WikiChromeClient()
    }

    private fun updateWikiView() {
        webView.visibility = if(activeUri == Uri.EMPTY) View.INVISIBLE else View.VISIBLE
        webView.loadUrl(activeUri.toString())
    }

    private fun startWikiChooser() {
        val intent = createWikiChooserIntent()
        startActivityForResult(intent, ACTIVITY_RESULT_CHOOSE_WIKI)
    }

    private fun createWikiChooserIntent(): Intent {
        val intent: Intent
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            // LEGACY: ACTION_OPEN_DOCUMENT is not available before KitKat and seems to be broken on
            //         KitKat.
            intent = Intent(Intent.ACTION_GET_CONTENT)
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/html"
        return intent
    }

    private fun selectWiki(descriptor: WikiDescriptor) {
        if(activeUri != descriptor.uri) {
            activeUri = descriptor.uri
            updateWikiView()
            wikiTitleView.text = descriptor.title
            wikiSubtitleView.text = descriptor.subtitle
        }
    }

    private fun deselectWiki() {
        if(activeUri != Uri.EMPTY) {
            activeUri = Uri.EMPTY
            updateWikiView()
            wikiTitleView.text = ""
            wikiSubtitleView.text = ""
        }
    }

    private fun addWiki(descriptor: WikiDescriptor) {
        // proceed only if the specified wiki is not already registered
        if(descriptors[descriptor.uri] != null) {
            // TODO: Display message
            return
        }

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) { // LEGACY: Must not be executed
            // persist file permissions across device boots
            val mode = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(descriptor.uri, mode)
        }


        // register wiki and initialize dependent state
        descriptors.put(descriptor)
        addMenuItemFor(descriptor)

        selectWiki(descriptor)
    }

    private fun removeWiki(descriptor: WikiDescriptor) {
        // if the specified wiki is the currently selected one, deselect it
        if(descriptor.uri == activeUri) {
            deselectWiki()
        }

        // remove references and dependent state
        removeMenuItemFor(descriptor)
        descriptors.remove(descriptor.uri)

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) { // LEGACY: Must not be executed
            // release file permissions
            val mode = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.releasePersistableUriPermission(descriptor.uri, mode)
        }
    }

    private fun removeCurrentWiki() {
        val descriptor = descriptors[activeUri]
        if(descriptor != null) removeWiki(descriptor)
    }

    private fun addMenuItemFor(descriptor: WikiDescriptor) {
        val item = navView.menu.add(R.id.nav_wiki_list, Menu.NONE, Menu.NONE, descriptor.title)
        descriptorMenuItems[descriptor] = item
        item.setOnMenuItemClickListener {
            selectWiki(descriptor)
            false // allow onNavigationItemSelected to handle common behaviour
        }
    }

    private fun removeMenuItemFor(descriptor: WikiDescriptor) {
        val item = descriptorMenuItems.remove(descriptor)
        if(item != null) {
            navView.menu.removeItem(item.itemId)
        }
    }

    private fun enableProgressBar() {
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE
    }

    private fun disableProgressBar() {
        progressBar.visibility = View.INVISIBLE
        progressBar.progress = 0
    }

    private inner class WikiChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progressBar.progress = newProgress
        }
    }

    private inner class WikiViewClient constructor(view: WebView) : WebViewClient() {
        private val TAG = this.javaClass.simpleName

        private val pageNameScript: String by lazy { loadAsset("js/get-application-name.js") }
        private val saveSupportScript: String by lazy { loadAsset("js/save-support.js") }
        private val wikiPropertiesScript: String by lazy { loadAsset("js/get-wiki-properties.js") }

        private val jsInjector: JsInjector = JsInjector(view)

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest)
                : WebResourceResponse? {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "TiddlyWiki resource request: ${request.method} ${request.url} { isForMainFrame: ${request.isForMainFrame} }")
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            Log.d(TAG, "Started loading URI $url")
            if(url.startsWith("data:text/html")) {
                return;
            }
            enableProgressBar()
        }

        override fun onPageFinished(view: WebView, url: String) {
            Log.d(TAG, "Finished loading URI $url")
            if(url.startsWith("data:text/html")) {
                return;
            }
            injectScript(pageNameScript, { pageName ->
                if(pageName in listOf("TiddlyWiki", "\"TiddlyWiki\"", "'TiddlyWiki'")) {
                    injectScript(saveSupportScript)
                    injectScript(wikiPropertiesScript, { onWikiPropertiesAvailable(it) })
                } else {
                    Log.e(TAG, "Page is not a TiddlyWiki (application-name: $pageName)")
                }
            })
            disableProgressBar()
        }

        private fun onWikiPropertiesAvailable(properties: String) {
            var title = ""
            var subtitle = ""

            JsonReader(StringReader(properties)).use { reader ->
                reader.beginObject()
                while(reader.hasNext()) {
                    val name = reader.nextName()
                    when(name) {
                        "title" -> title = reader.nextString()
                        "subtitle" -> subtitle = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }

            wikiTitleView.text = title
            wikiSubtitleView.text = subtitle

            val descriptor = descriptors[activeUri]
            if(descriptor != null) {
                descriptor.title = title
                descriptor.subtitle = subtitle
                descriptorMenuItems[descriptor]?.title = title
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest,
                                     error: WebResourceError) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.e(TAG, "Failed to load resource: ${error.description} (URL: ${request.url}, Code: ${error.errorCode})")
            }
            disableProgressBar()
            super.onReceivedError(view, request, error)
        }

        private fun injectScript(script: String) {
            injectScript(script, {})
        }

        private fun injectScript(script: String, callback: (String) -> Unit) {
            jsInjector.injectScript(script, callback)
        }

        private fun loadAsset(name: String): String {
            return assets.open(name).bufferedReader().use { it.readText() }
        }
    }

    @Suppress("unused") // Members are used by JavaScript code
    private inner class AppJavascriptInterface {

        @JavascriptInterface
        fun currentPath(): String {
            return activeUri.toString()
        }

        @JavascriptInterface
        fun write(path: String, content: String): Boolean {
            val uri = Uri.parse(path)
            if (!requireCurrentUri(uri)) {
                return false
            }

            contentResolver.openFileDescriptor(uri, "w").use {
                FileOutputStream(it.fileDescriptor).bufferedWriter(Charsets.UTF_8).use {
                    it.write(content)
                    Log.i(TAG, "TiddlyWiki successfully saved")
                }
            }

            return true
        }

        private fun requireCurrentUri(uri: Uri): Boolean {
            val expected = activeUri
            return if(expected == uri) { true } else {
                Log.e(TAG, "Unexpected URI (expected: $expected, actual: $uri)")
                false
            }
        }
    }

    companion object {
        private const val ACTIVITY_RESULT_CHOOSE_WIKI: Int = 1
    }
}
