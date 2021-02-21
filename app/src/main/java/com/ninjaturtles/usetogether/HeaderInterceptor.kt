package com.ninjaturtles.usetogether

import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder()
                .addHeader("Authorization", "34f436faeccee77ec0f1b445a77a245c5e8a3e1")
                .build()
        )
    }
}