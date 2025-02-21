package com.github.jing332.tts_server_android.compose.systts.plugin.login

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.drake.net.utils.withIO
import com.drake.net.utils.withMain
import com.github.jing332.common.utils.fromCookie
import com.github.jing332.common.utils.longToast
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class PluginLoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "PluginLoginActivity"
        private val logger = KotlinLogging.logger(TAG)

        const val ARG_LOGIN_URL = "PluginLoginActivity.login_url"
        const val ARG_BINDING = "PluginLoginActivity.binding"
        const val ARG_DESC = "PluginLoginActivity.description"
        const val RESULT = "PluginLoginActivity.result"
        const val OK: Int = 1
    }

    private var cookie: String = ""

    private suspend fun WebView.evaluateJavascript(script: String): String = coroutineScope {
        val mutex = Mutex(true)
        var result = ""
        withMain {
            evaluateJavascript(script, object : ValueCallback<String> {
                override fun onReceiveValue(value: String?) {
                    result = value ?: ""
                    mutex.unlock()
                }

            })
        }

        logger.info { "js result: $result" }
        mutex.lock()
        result
    }

    private suspend fun parseBinding(webView: WebView, binding: String): String = withIO {
        val split = binding.trim().split('.')
        if (split.size != 2) {
            logger.error { "binding is invalid: $binding" }
            return@withIO ""
        }

        val start = split[0]
        val end = split[1]

        when (start) {
            "cookies" -> {
                val cookies = webView.evaluateJavascript("document.cookie").fromCookie()
                cookies[end] ?: ""
            }

            "locals" -> {
                webView.evaluateJavascript("localStorage.getItem('${start}')")
            }

            "sessions" -> {
                webView.evaluateJavascript("sessionStorage.getItem('${start}')")
            }

            else -> {
                ""
            }
        }

    }

    private suspend fun finished(webView: WebView, binding: String) {
        val binding = parseBinding(webView, binding).ifBlank { finish();return }
        logger.info { "binding: $binding" }
        withMain {
            setResult(OK, Intent().apply {
                putExtra(RESULT, binding)
            })
            finish()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loginUrl = intent.getStringExtra(ARG_LOGIN_URL)
        val binding = intent.getStringExtra(ARG_BINDING)
        val description = intent.getStringExtra(ARG_DESC) ?: ""
        if (loginUrl == null || binding == null) {
            logger.error { "loginUrl or binding is null" }
            finish()
            return
        }

        var webview: WebView? = null

        setContent {
            AppTheme {
                Scaffold(topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    stringResource(R.string.login),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    description,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                lifecycleScope.launch {
                                    finished(
                                        webview ?: return@launch, binding
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Save, stringResource(R.string.save))
                            }
                        },
                    )
                }) { padding ->
                    LoginScreen(
                        modifier = Modifier.padding(padding),
                        loginUrl,
                        onCreated = { webview = it }
                    )
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun LoginScreen(
        modifier: Modifier = Modifier,
        loginUrl: String,
        userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0",
        onCreated: (WebView) -> Unit,
    ) {
        val state = rememberWebViewState(
            loginUrl,
            mapOf("User-Agent" to userAgent)
        )
        val navigator = rememberWebViewNavigator()

        val context = LocalContext.current
        val chromeClient = remember {
            object : AccompanistWebChromeClient() {
                override fun onJsConfirm(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?,
                ): Boolean {
                    if (result == null) return false
                    context.longToast(message)
                    return super.onJsConfirm(view, url, message, result)
                }

                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?,
                ): Boolean {
                    context.longToast(message)

                    return super.onJsAlert(view, url, message, result)
                }
            }
        }


        val client = remember {
            object : AccompanistWebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    view.evaluateJavascript(
                        """document.querySelector('meta[name="viewport"]').setAttribute('content', 'width=1024px, height=1024px,initial-scale=' + (document.documentElement.clientWidth / 1024));""",
                        null
                    );
//                    cookie = CookieManager.getInstance().getCookie(url)

                    super.onPageFinished(view, url)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    runCatching {
                        if (request?.url?.scheme?.startsWith("http") == false) {
                            val intent = Intent(Intent.ACTION_VIEW, request.url)
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    request.url.toString()
                                )
                            )
                            return true
                        }
                    }.onFailure {
                        context.longToast("跳转APP失败: ${request?.url}")
                    }

                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        Column(modifier) {
            WebView(
                state = state,
                modifier = Modifier
                    .fillMaxSize(),
                navigator = navigator,
                onCreated = {
                    it.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
                    it.setScrollbarFadingEnabled(true);
                    it.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true


                        databaseEnabled = true
                        userAgentString = userAgent

                        loadWithOverviewMode = true;
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN;
                        loadWithOverviewMode = true;
                        useWideViewPort = true;
                    }

                    onCreated(it)
                },
                client = client,
                chromeClient = chromeClient,
            )
        }

    }
}