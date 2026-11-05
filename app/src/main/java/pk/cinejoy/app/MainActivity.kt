package pk.cinejoy.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import java.io.ByteArrayInputStream
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adnxs.com", "taboola.com", "outbrain.com",
        "popads.net", "popcash.net", "propellerads.com", "exoclick.com",
        "juicyads.com", "adsterra.com", "pushengage.com"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = WebView(this)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(progress, FrameLayout.LayoutParams(-1, dp(3)))
        setContentView(root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            loadsImagesAutomatically = true
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
        }
        webView.setBackgroundColor(Color.BLACK)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme?.lowercase(Locale.ROOT)
                return scheme != "https" && scheme != "http"
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val host = request.url.host?.lowercase(Locale.ROOT) ?: return null
                if (blockedHosts.any { host == it || host.endsWith(".$it") }) {
                    return WebResourceResponse("text/plain", "utf-8", 204, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) { callback.onCustomViewHidden(); return }
                customView = view
                customViewCallback = callback
                originalOrientation = requestedOrientation
                (webView.parent as? ViewGroup)?.addView(view, ViewGroup.LayoutParams(-1, -1))
                webView.visibility = View.GONE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            override fun onHideCustomView() { hideCustomView() }
        }

        if (savedInstanceState == null) webView.loadUrl("https://cinejoy.pk/")
        else webView.restoreState(savedInstanceState)
    }

    private fun hideCustomView() {
        val view = customView ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        customView = null
        webView.visibility = View.VISIBLE
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        requestedOrientation = originalOrientation
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (customView != null) { hideCustomView(); return }
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) { webView.stopLoading(); webView.destroy() }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
