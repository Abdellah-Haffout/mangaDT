package com.abht.manga_dt

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

fun main() {
    val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val host = original.url.host
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "${original.url.scheme}://$host/")
                .build()
            chain.proceed(request)
        }
        .build()

    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }

    application {
        val state = rememberWindowState(placement = WindowPlacement.Maximized)
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kotatsu Desktop - MangaDT",
            state = state,
            onKeyEvent = { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F11) {
                    state.placement = if (state.placement == WindowPlacement.Fullscreen) {
                        WindowPlacement.Maximized
                    } else {
                        WindowPlacement.Fullscreen
                    }
                    true
                } else {
                    false
                }
            }
        ) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                window.extendedState = java.awt.Frame.MAXIMIZED_BOTH
            }
            App()
        }
    }
}


