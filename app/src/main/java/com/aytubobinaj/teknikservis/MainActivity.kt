package com.aytubobinaj.teknikservis

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var offlinePanel: LinearLayout
    private lateinit var message: TextView
    private val startUrl = "https://aytubobinaj.com/teknikservis/"
    private val allowedHost = "aytubobinaj.com"
    private var lastMainUrl = startUrl

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 42, 37)
        window.navigationBarColor = android.graphics.Color.rgb(16, 42, 37)
        CookieManager.getInstance().setAcceptCookie(true)
        buildUi()
        webView.settings.apply {
            javaScriptEnabled = true // Existing portal uses WordPress AJAX.
            domStorageEnabled = true
            databaseEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            loadsImagesAutomatically = true
            userAgentString = "$userAgentString AYTU-Bobinaj-Android/1.0"
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = PortalWebViewClient()
        if (savedInstanceState != null) webView.restoreState(savedInstanceState) else openPortal()
    }


    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFF5F7F6.toInt()) }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true; visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        webView = WebView(this)
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        offlinePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(28), dp(28), dp(28), dp(28)); visibility = View.GONE
        }
        message = TextView(this).apply { text = "İnternet bağlantısı kurulamıyor. Bağlantınızı kontrol edip yeniden deneyin."; textSize = 17f; gravity = Gravity.CENTER }
        val retry = Button(this).apply { text = "Yeniden dene"; setOnClickListener { openPortal() } }
        offlinePanel.addView(message); offlinePanel.addView(retry)
        root.addView(offlinePanel, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun openPortal() {
        if (!isOnline()) { showOffline("İnternet bağlantısı yok. Wi-Fi veya mobil verinizi açıp yeniden deneyin."); return }
        offlinePanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(startUrl)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showOffline(text: String) {
        message.text = text; webView.visibility = View.GONE; offlinePanel.visibility = View.VISIBLE; progress.visibility = View.GONE
    }

    private fun allowedWebUrl(uri: Uri): Boolean {
        if (uri.scheme != "https" || !uri.host.equals(allowedHost, ignoreCase = true) || uri.port !in listOf(-1, 443)) return false
        val path = uri.path ?: "/"
        if (path == "/teknikservis" || path == "/teknikservis/") return true
        // WordPress may route login/logout through wp-login.php. Only authentication actions are allowed.
        if (path == "/wp-login.php") {
            val action = uri.getQueryParameter("action") ?: "login"
            return action in setOf("login", "logout", "lostpassword", "rp", "resetpass", "confirm_admin_email")
        }
        // Keep WordPress AJAX available for the existing portal's same-origin fetch() requests.
        if (path == "/wp-admin/admin-ajax.php") return true
        return false
    }

    private fun launchExternal(uri: Uri) {
        val scheme = uri.scheme?.lowercase()
        if (scheme != "tel" && scheme != "sms" && scheme != "smsto") {
            Toast.makeText(this, "Bu bağlantı uygulama içinde açılamaz.", Toast.LENGTH_SHORT).show(); return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, if (scheme == "tel") "Arama uygulaması bulunamadı." else "SMS uygulaması bulunamadı.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Bağlantı açılamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class PortalWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val uri = request?.url ?: return true
            if (uri.scheme.equals("tel", true) || uri.scheme.equals("sms", true) || uri.scheme.equals("smsto", true)) {
                launchExternal(uri); return true
            }
            if (!allowedWebUrl(uri)) {
                Toast.makeText(this@MainActivity, "Güvenlik nedeniyle bu sayfaya izin verilmiyor.", Toast.LENGTH_SHORT).show()
                return true
            }
            lastMainUrl = uri.toString()
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            progress.visibility = View.VISIBLE
            if (!isOnline()) showOffline("Bağlantı kesildi. İnternet bağlantınızı kontrol edin.")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            progress.visibility = View.GONE
            if (url != null && allowedWebUrl(Uri.parse(url))) lastMainUrl = url
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            if (request?.isForMainFrame == true) showOffline(if (isOnline()) "Sayfa yüklenemedi. Biraz sonra tekrar deneyin." else "İnternet bağlantısı yok. Bağlantınızı kontrol edip yeniden deneyin.")
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.cancel() // Never bypass certificate errors.
            showOffline("Güvenli HTTPS bağlantısı doğrulanamadı. Lütfen daha sonra tekrar deneyin.")
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            val history = webView.copyBackForwardList()
            val targetIndex = history.currentIndex - 1
            if (targetIndex >= 0) {
                val target = history.getItemAtIndex(targetIndex).url
                if (runCatching { allowedWebUrl(Uri.parse(target)) }.getOrDefault(false)) webView.goBack()
                else Toast.makeText(this, "Önceki sayfaya güvenli şekilde dönülemiyor.", Toast.LENGTH_SHORT).show()
            } else super.onBackPressed()
        } else if (offlinePanel.visibility == View.VISIBLE) openPortal() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); if (::webView.isInitialized) webView.saveState(outState) }
    override fun onDestroy() { if (::webView.isInitialized) webView.destroy(); super.onDestroy() }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
