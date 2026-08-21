package com.abht.manga_dt.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.ConcurrentHashMap

const val APP_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

object AppCookieJar : CookieJar {
    private val allCookies = ConcurrentHashMap<String, Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            val key = "${cookie.domain}::${cookie.name}"
            allCookies[key] = cookie
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()
        val matched = mutableListOf<Cookie>()

        allCookies.values.forEach { cookie ->
            if (cookie.expiresAt >= now) {
                val domain = cookie.domain.removePrefix(".")
                if (host == domain || host.endsWith(".$domain") || domain.endsWith(".$host")) {
                    matched.add(cookie)
                }
            }
        }
        return matched
    }

    fun syncFromAndroidCookieManager(url: String) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val host = httpUrl.host
        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.flush()
        val rawCookies = cookieManager.getCookie(url) ?: cookieManager.getCookie(host) ?: return
        setCookies(host, rawCookies)
    }

    fun setCookies(host: String, rawCookies: String) {
        val cleanHost = host.removePrefix("http://").removePrefix("https://").substringBefore("/").substringBefore(":")
        val baseDomain = cleanHost.removePrefix("www.")
        
        val parts = rawCookies.split(";")
        parts.forEach { part ->
            val trimmed = part.trim()
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val name = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()
                
                val cookie = Cookie.Builder()
                    .domain(baseDomain)
                    .name(name)
                    .value(value)
                    .path("/")
                    .build()
                val key = "$baseDomain::$name"
                allCookies[key] = cookie
            }
        }
    }

    fun clear() {
        allCookies.clear()
    }
}
