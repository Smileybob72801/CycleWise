package com.veleda.cyclewise

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform