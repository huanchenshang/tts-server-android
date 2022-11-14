package com.github.jing332.tts_server_android.ui.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.jing332.tts_server_android.App
import com.github.jing332.tts_server_android.databinding.FragmentServerWebBinding
import com.github.jing332.tts_server_android.service.TtsIntentService
import com.github.jing332.tts_server_android.util.toastOnUi


class ServerWebFragment : Fragment() {
    private val binding by lazy { FragmentServerWebBinding.inflate(layoutInflater) }
    private val viewModel: ServerWebViewFragmentModel by viewModels()

    private val mReceiver by lazy { MyReceiver() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true

        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                binding.swipeRefresh.isRefreshing = false
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                try {
                    if (request?.url?.scheme?.startsWith("http") == false) {
                        val intent = Intent(Intent.ACTION_VIEW, request.url)
                        startActivity(intent)
                        return true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    toastOnUi("跳转APP失败！")
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        App.localBroadcast.registerReceiver(
            mReceiver,
            IntentFilter(TtsIntentService.ACTION_ON_STARTED)
        )

        val port = TtsIntentService.instance?.cfg?.port ?: 1233
        binding.webView.loadUrl("http://localhost:${port}")
        if (TtsIntentService.instance?.isRunning != true) {
            val i = Intent(App.context, TtsIntentService::class.java)
            requireContext().startService(i)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.localBroadcast.unregisterReceiver(mReceiver)
        binding.webView.clearHistory()
    }


    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TtsIntentService.ACTION_ON_STARTED -> {
                    binding.webView.reload()
                }
            }
        }
    }
}