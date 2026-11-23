package com.smartexpense.domain.network

/**
 * Thin abstraction over device connectivity so ViewModels can explain an
 * offline failure without depending on Android framework classes.
 */
fun interface NetworkStatusProvider {
    fun isConnected(): Boolean
}

/** Used in tests and previews where no device connectivity is available. */
object AlwaysConnectedNetworkStatus : NetworkStatusProvider {
    override fun isConnected(): Boolean = true
}

/** Single, user-facing wording for connectivity problems across the app. */
const val NO_INTERNET_MESSAGE =
    "No internet connection. Please check your connection and try again."
