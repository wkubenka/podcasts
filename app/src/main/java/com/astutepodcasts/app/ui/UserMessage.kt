package com.astutepodcasts.app.ui

import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Exception.toUserMessage(): String = when (this) {
    is UnknownHostException -> "No internet connection. Check your network and try again."
    is SocketTimeoutException -> "Connection timed out. Please try again."
    else -> message ?: "Something went wrong. Please try again."
}
