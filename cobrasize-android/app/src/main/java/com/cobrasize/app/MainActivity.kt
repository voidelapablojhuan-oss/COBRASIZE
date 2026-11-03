package com.cobrasize.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class MainActivity : Activity() {

    private var webView: WebView? = null
    private var receiverRegistered = false

    // Usuário tocou em "Desativar" na notificação: desativa também na interface
    private val stoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            webView?.post { webView?.evaluateJavascript("setActive(false)", null) }
        }
    }

    // Qualquer exceção solta numa função da ponte derruba o app, então tudo fica protegido
    inner class Bridge {
        @JavascriptInterface
        fun setActive(on: Boolean) {
            try {
                runOnUiThread {
                    if (on) this@MainActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else this@MainActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                val intent = Intent(this@MainActivity, ActiveService::class.java)
                if (on) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                    else startService(intent)
                } else stopService(intent)
            } catch (e: Exception) {
            }
        }

        @JavascriptInterface
        fun applyTweaks(sensi: Int, gpu: String, fps: Int): String {
            val msg = try {
                Tweaker.apply(this@MainActivity, sensi, gpu, fps)
            } catch (e: Exception) {
                "Não foi possível aplicar no sistema."
            }
            return JSONObject().put("message", msg).toString()
        }

        @JavascriptInterface
        fun restoreTweaks() {
            try {
                Tweaker.restore(this@MainActivity)
            } catch (e: Exception) {
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { Tweaker.restore(this) } catch (e: Exception) { } // desfaz sobras caso o app tenha sido morto antes

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val view = WebView(this)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        // A ponte só pode ser usada pela página local: links externos abrem no navegador
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                if (request.url.scheme == "file") return false
                try { startActivity(Intent(Intent.ACTION_VIEW, request.url)) } catch (e: Exception) { }
                return true
            }
        }
        view.addJavascriptInterface(Bridge(), "AndroidBridge")
        view.loadUrl("file:///android_asset/index.html")
        webView = view
        setContentView(view)

        val filter = IntentFilter(ActiveService.ACTION_STOPPED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stoppedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stoppedReceiver, filter)
        }
        receiverRegistered = true
    }

    // Fechar o app (voltar / deslizar) encerra o serviço e desativa tudo
    override fun onDestroy() {
        try { stopService(Intent(this, ActiveService::class.java)) } catch (e: Exception) { }
        try { Tweaker.restore(this) } catch (e: Exception) { }
        if (receiverRegistered) {
            try { unregisterReceiver(stoppedReceiver) } catch (e: Exception) { }
            receiverRegistered = false
        }
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
