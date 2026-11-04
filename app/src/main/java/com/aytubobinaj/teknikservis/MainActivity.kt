
package com.aytubobinaj.teknikservis

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var offlinePanel: FrameLayout
    private lateinit var message: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var retryEnabled = false
    private var destroyed = false

    private val startUrl = "https://aytubobinaj.com/teknikservis/"
    private val allowedHost = "aytubobinaj.com"
    private var lastMainUrl = startUrl

    private var loadId = 0

    companion object {
        private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        private const val WHATSAPP_NORMAL = "com.whatsapp"
    }

    private val retryRunnable = object : Runnable {
        override fun run() {
            if (destroyed || !retryEnabled) return

            if (isOnline()) {
                openPortal()
            } else {
                handler.postDelayed(this, 5000)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(16, 42, 37)
        window.navigationBarColor = Color.rgb(16, 42, 37)

        CookieManager.getInstance().setAcceptCookie(true)

        buildUi()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode =
                android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            loadsImagesAutomatically = true
            userAgentString = "$userAgentString AYTU-Bobinaj-Android/1.0"
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = PortalWebViewClient()

        openPortal()
    }

    private fun buildUi() {

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        webView = WebView(this)

        root.addView(
            webView,
            FrameLayout.LayoutParams(-1, -1)
        )

        progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            isIndeterminate = true
            visibility = View.GONE
        }

        root.addView(
            progress,
            FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP)
        )

        offlinePanel = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
        }

        // Atölye arka planı
        val background = ImageView(this).apply {
            setImageResource(R.drawable.offline_workshop)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = null
        }

        offlinePanel.addView(
            background,
            FrameLayout.LayoutParams(-1, -1)
        )

        // Okunabilirlik için koyu katman
        val shade = View(this).apply {
            setBackgroundColor(0x990A1B1D.toInt())
        }

        offlinePanel.addView(
            shade,
            FrameLayout.LayoutParams(-1, -1)
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(30), dp(24), dp(30), dp(24))
        }

        // Şeffaf AYTU logosu - orijinal oran korunur
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aytu_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = "AYTU Bobinaj"
        }

        content.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(235),
                dp(100)
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(30)
            }
        )

        val heading = TextView(this).apply {
            text = "Bağlantı Kurulamıyor"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        content.addView(heading)

        message = TextView(this).apply {
            text = "İnternet bağlantınızı veya sunucu erişimini kontrol ediyoruz."
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(12))
        }

        content.addView(message)

        val spinner = ProgressBar(this).apply {
            isIndeterminate = true
        }

        content.addView(
            spinner,
            LinearLayout.LayoutParams(dp(45), dp(45)).apply {
                gravity = Gravity.CENTER
                topMargin = dp(12)
                bottomMargin = dp(12)
            }
        )

        val waiting = TextView(this).apply {
            text = "Lütfen bekleyiniz..."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        content.addView(waiting)

        val info = TextView(this).apply {
            text = "Bağlantı sağlandığında uygulamanız otomatik olarak açılacaktır."
            textSize = 13f
            setTextColor(0xFFE0E8E5.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(25))
        }

        content.addView(info)

        val retry = Button(this).apply {
            text = "YENİDEN DENE"
            setOnClickListener {
                handler.removeCallbacks(retryRunnable)
                retryEnabled = true
                openPortal()
            }
        }

        content.addView(
            retry,
            LinearLayout.LayoutParams(-1, dp(52))
        )

        offlinePanel.addView(
            content,
            FrameLayout.LayoutParams(-1, -1)
        )

        root.addView(
            offlinePanel,
            FrameLayout.LayoutParams(-1, -1)
        )

        setContentView(root)
    }

    private fun openPortal() {

        if (!isOnline()) {
            showOffline("İnternet bağlantınızı kontrol ediyoruz.")
            return
        }

        loadId++

        handler.removeCallbacks(retryRunnable)
        retryEnabled = false

        offlinePanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE

        webView.loadUrl(lastMainUrl)
    }

    private fun isOnline(): Boolean {

        val cm = getSystemService(CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return caps.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && caps.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    private fun showOffline(
        text: String,
        autoRetry: Boolean = true
    ) {

        message.text = text

        loadId++

        webView.stopLoading()
        webView.visibility = View.GONE
        progress.visibility = View.GONE
        offlinePanel.visibility = View.VISIBLE

        handler.removeCallbacks(retryRunnable)

        retryEnabled = autoRetry

        if (autoRetry) {
            handler.postDelayed(retryRunnable, 5000)
        }
    }

    private fun isWhatsAppUrl(uri: Uri): Boolean {

        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()

        if (scheme == "whatsapp") return true

        return (scheme == "https" || scheme == "http") &&
            host in setOf(
                "wa.me",
                "api.whatsapp.com",
                "www.whatsapp.com",
                "whatsapp.com"
            )
    }

    private fun launchWhatsApp(uri: Uri) {

        for (packageName in listOf(
            WHATSAPP_BUSINESS,
            WHATSAPP_NORMAL
        )) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(packageName)
                }

                startActivity(intent)
                return

            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: Exception) {
                continue
            }
        }

        Toast.makeText(
            this,
            "WhatsApp uygulaması bulunamadı.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun allowedWebUrl(uri: Uri): Boolean {

        if (
            uri.scheme != "https" ||
            !uri.host.equals(allowedHost, true) ||
            uri.port !in listOf(-1, 443)
        ) return false

        val path = uri.path ?: "/"

        if (path == "/teknikservis" || path == "/teknikservis/") {
            return true
        }

        if (path == "/wp-login.php") {
            val action = uri.getQueryParameter("action") ?: "login"

            return action in setOf(
                "login",
                "logout",
                "lostpassword",
                "rp",
                "resetpass",
                "confirm_admin_email"
            )
        }

        if (path == "/wp-admin/admin-ajax.php") return true

        return false
    }

    private fun launchExternal(uri: Uri) {

        val scheme = uri.scheme?.lowercase()

        if (scheme !in listOf("tel", "sms", "smsto")) return

        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Bağlantı açılamadı.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private inner class PortalWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {

            val uri = request?.url ?: return true

            if (isWhatsAppUrl(uri)) {
                launchWhatsApp(uri)
                return true
            }

            if (uri.scheme?.lowercase() in listOf("tel", "sms", "smsto")) {
                launchExternal(uri)
                return true
            }

            if (!allowedWebUrl(uri)) {
                Toast.makeText(
                    this@MainActivity,
                    "Güvenlik nedeniyle bu sayfaya izin verilmiyor.",
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }

            lastMainUrl = uri.toString()
            return false
        }

        override fun onPageStarted(
            view: WebView?,
            url: String?,
            favicon: Bitmap?
        ) {
            progress.visibility = View.VISIBLE
        }

        override fun onPageFinished(
            view: WebView?,
            url: String?
        ) {
            progress.visibility = View.GONE

            if (url != null && allowedWebUrl(Uri.parse(url))) {
                lastMainUrl = url
                handler.removeCallbacks(retryRunnable)
                retryEnabled = false
                offlinePanel.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if (request?.isForMainFrame == true) {
                showOffline(
                    if (isOnline())
                        "Sunucuya şu anda ulaşılamıyor. Yeniden bağlanmayı deniyoruz."
                    else
                        "İnternet bağlantınızı kontrol ediyoruz."
                )
            }
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: android.webkit.WebResourceResponse?
        ) {
            if (
                request?.isForMainFrame == true &&
                (errorResponse?.statusCode ?: 200) >= 500
            ) {
                showOffline(
                    "Sunucuda geçici bir sorun oluştu. Yeniden bağlanmayı deniyoruz."
                )
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            handler?.cancel()

            showOffline(
                "Güvenli bağlantı doğrulanamadı. Lütfen daha sonra tekrar deneyin.",
                autoRetry = false
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (offlinePanel.visibility == View.VISIBLE) {
            openPortal()
            return
        }

        if (::webView.isInitialized && webView.canGoBack()) {

            val history = webView.copyBackForwardList()
            val targetIndex = history.currentIndex - 1

            if (targetIndex >= 0) {
                val target = history.getItemAtIndex(targetIndex).url

                if (
                    runCatching {
                        allowedWebUrl(Uri.parse(target))
                    }.getOrDefault(false)
                ) {
                    webView.goBack()
                } else {
                    Toast.makeText(
                        this,
                        "Önceki sayfaya güvenli şekilde dönülemiyor.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                super.onBackPressed()
            }

        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        handler.removeCallbacks(retryRunnable)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::offlinePanel.isInitialized &&
            offlinePanel.visibility == View.VISIBLE &&
            retryEnabled
        ) {
            handler.removeCallbacks(retryRunnable)
            handler.postDelayed(retryRunnable, 5000)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacksAndMessages(null)

        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }

        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
