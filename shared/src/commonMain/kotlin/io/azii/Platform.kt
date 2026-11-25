package io.azii

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform