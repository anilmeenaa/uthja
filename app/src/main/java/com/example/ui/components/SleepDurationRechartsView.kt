package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.WeeklySleepSummary
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SleepDurationRechartsView(
    summary: WeeklySleepSummary,
    onDaySelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isChartReady by remember { mutableStateOf(false) }

    // When summary changes, inject updated data into the Recharts component
    LaunchedEffect(summary, isChartReady) {
        webViewRef?.let { wv ->
            val jsonPayload = summary.toJsonString()
            val escapedJson = JSONObject.quote(jsonPayload)
            val jsCode = "if (window.updateChartData) { window.updateChartData($escapedJson); }"
            wv.post {
                wv.evaluateJavascript(jsCode, null)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x1A785CD8),
                ambientColor = Color(0x0D785CD8)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(22.dp))
            .padding(2.dp)
            .testTag("sleep_duration_recharts_card")
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .testTag("recharts_webview"),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false

                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onDaySelected(dayIndex: Int) {
                                post {
                                    onDaySelected(dayIndex)
                                }
                            }

                            @JavascriptInterface
                            fun onChartReady() {
                                post {
                                    isChartReady = true
                                    val jsonPayload = summary.toJsonString()
                                    val escapedJson = JSONObject.quote(jsonPayload)
                                    evaluateJavascript("if (window.updateChartData) { window.updateChartData($escapedJson); }", null)
                                }
                            }
                        },
                        "AndroidBridge"
                    )

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            return true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isChartReady = true
                            val jsonPayload = summary.toJsonString()
                            val escapedJson = JSONObject.quote(jsonPayload)
                            evaluateJavascript("if (window.updateChartData) { window.updateChartData($escapedJson); }", null)
                        }
                    }

                    loadUrl("file:///android_asset/sleep_recharts.html")
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
                if (isChartReady) {
                    val jsonPayload = summary.toJsonString()
                    val escapedJson = JSONObject.quote(jsonPayload)
                    wv.evaluateJavascript("if (window.updateChartData) { window.updateChartData($escapedJson); }", null)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}
