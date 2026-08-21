package com.abht.manga_dt.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abht.manga_dt.data.AppCookieJar
import com.abht.manga_dt.data.Strings

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun CaptchaWebViewDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit,
    onSolved: () -> Unit
) {
    val strings = Strings.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentProgress by remember { mutableStateOf(0) }
    var isLoadingPage by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                        }

                        // Refresh button
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = strings.close)
                        }
                    }
                }

                // Progress Indicator
                if (isLoadingPage && currentProgress < 100) {
                    LinearProgressIndicator(
                        progress = { currentProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Info Banner
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.captchaHelpText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Interactive WebView Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                webViewInstance = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    userAgentString = com.abht.manga_dt.data.APP_USER_AGENT
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        currentProgress = newProgress
                                        isLoadingPage = newProgress < 100
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, targetUrl: String?, favicon: Bitmap?) {
                                        isLoadingPage = true
                                    }

                                    override fun onPageFinished(view: WebView?, targetUrl: String?) {
                                        isLoadingPage = false
                                        if (targetUrl != null) {
                                            AppCookieJar.syncFromAndroidCookieManager(targetUrl)
                                            val cookies = cookieManager.getCookie(targetUrl) ?: ""
                                            if (cookies.contains("cf_clearance") || cookies.contains("__cf_bm")) {
                                                AppCookieJar.syncFromAndroidCookieManager(targetUrl)
                                            }
                                        }
                                    }
                                }

                                loadUrl(url)
                            }
                        },
                        update = { view ->
                            webViewInstance = view
                        }
                    )
                }

                // Bottom Action Bar: "I have solved it"
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text(strings.cancel)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val currentUrl = webViewInstance?.url ?: url
                                AppCookieJar.syncFromAndroidCookieManager(currentUrl)
                                onSolved()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.iHaveSolvedIt, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
