package com.github.jing332.tts_server_android.compose.systts.plugin.login

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.drake.net.utils.withIO
import com.drake.net.utils.withMain
import com.github.jing332.common.utils.fromCookie
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.widgets.AsyncCircleImage
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.text.StringEscapeUtils

class PluginLoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "PluginLoginActivity"
        private val logger = KotlinLogging.logger(TAG)

        const val ARG_LOGIN_URL = "PluginLoginActivity.login_url"
        const val ARG_BINDING = "PluginLoginActivity.binding"
        const val ARG_DESC = "PluginLoginActivity.description"
        const val ARG_UA = "PluginLoginActivity.ua"

        const val RESULT = "PluginLoginActivity.result"
        const val OK: Int = 1

        private const val UA_PC =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0"
    }

    private var binding = ""
    private var loginUrl = ""

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
        StringEscapeUtils.unescapeJson(result).trimStart('"').trimEnd('"')
    }

    private suspend fun parseBinding(webView: WebView, binding: String): String = withIO {
        val split = binding.trim().split('.')

        val start = split[0]
        val end: String = split.getOrElse(1) { "" }
        val all = end.isBlank()

        when (start) {
            "cookies" -> {
                val cookies = CookieManager.getInstance().getCookie(loginUrl)
//                val cookies = webView.evaluateJavascript("document.cookie") // Deprecated; incomplete cookie.”
                if (all) cookies else cookies.fromCookie()[end] ?: ""
            }

            "locals" -> {
                webView.evaluateJavascript("localStorage.getItem('${end}')")
            }

            "sessions" -> {
                webView.evaluateJavascript("sessionStorage.getItem('${end}')")
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

        loginUrl = intent.getStringExtra(ARG_LOGIN_URL) ?: ""
        binding = intent.getStringExtra(ARG_BINDING) ?: ""
        val description = intent.getStringExtra(ARG_DESC) ?: ""
        val ua = intent.getStringExtra(ARG_UA) ?: ""

        val userAgent = when (ua.lowercase()) {
            "pc", "" -> UA_PC
            "mobile" -> ""
            "android" -> ""
            else -> ua
        }

        if (loginUrl.isBlank() || binding.isBlank()) {
            longToast("loginUrl or binding is null")
            finish()
            return
        }
        logger.debug { "loginUrl: $loginUrl, binding: $binding, ua: $ua, userAgent: ${userAgent} desc: $description" }

        var webview: WebView? = null

        setContent {
            AppTheme {
                var title by remember { mutableStateOf(getString(R.string.login)) }
                var icon by remember { mutableStateOf<Bitmap?>(null) }
                Scaffold(topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                icon?.let {
                                    AsyncCircleImage(
                                        it,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(title, maxLines = 1)

                                    Text(
                                        description,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 3
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                webview?.reload()
                            }) {
                                Icon(Icons.Default.Refresh, stringResource(R.string.reload))
                            }


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
                        loginUrl = loginUrl,
                        userAgent = userAgent,
                        onTitleUpdate = { title = it },
                        onIconUpdate = { icon = it },
                        onCreated = { webview = it }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun LoginScreen(
        modifier: Modifier = Modifier,
        loginUrl: String,
        userAgent: String = "",
        onTitleUpdate: (String) -> Unit,
        onIconUpdate: (Bitmap) -> Unit,
        onCreated: (WebView) -> Unit,
    ) {
        val state = rememberWebViewState(
            loginUrl,
            if (userAgent.isBlank()) emptyMap() else mapOf("User-Agent" to userAgent)
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
                    val regex = Regex("android|mobile", RegexOption.IGNORE_CASE)
                    val isMobile = userAgent.isBlank() || regex.containsMatchIn(userAgent)
                    if (!isMobile)
                        view.evaluateJavascript(
                            """document.querySelector('meta[name="viewport"]').setAttribute('content', 'width=1024px, height=auto, initial-scale=' + (document.documentElement.clientWidth / 1024));""",
                            null
                        );

                    super.onPageFinished(view, url)

                    onTitleUpdate(view.title ?: "")
                    onIconUpdate(view.favicon ?: return)
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
            val process =
                if (state.loadingState is LoadingState.Loading) (state.loadingState as LoadingState.Loading).progress else 0f

            AnimatedVisibility(process > 0) {
                LinearProgressIndicator(
                    progress = { process },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LaunchedEffect(state.pageIcon) {
                onIconUpdate(state.pageIcon ?: return@LaunchedEffect)
            }

            val refreshState = rememberPullToRefreshState()

            PullToRefreshBox(
                state.loadingState != LoadingState.Finished,
                state = refreshState,
                onRefresh = {},
            ) {
                WebView(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
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
                            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL;
                            loadWithOverviewMode = true;
                            useWideViewPort = true;
                            setSupportZoom(true)
                            builtInZoomControls = true
                        }

                        onCreated(it)
                    },
                    client = client,
                    chromeClient = chromeClient,
                )
            }
        }

    }
}