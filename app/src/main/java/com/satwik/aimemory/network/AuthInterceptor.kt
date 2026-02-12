package com.satwik.aimemory.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds Bearer token authentication
 * to every outgoing request.
 */
class AuthInterceptor(private val token: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
