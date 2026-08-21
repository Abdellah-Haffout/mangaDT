package com.abht.manga_dt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform