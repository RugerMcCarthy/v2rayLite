package com.thoughtcrime.v2raylite.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class CardPlatformRepo(private val client: OkHttpClient) {

    suspend fun getPointConfigByUid(uuid: String): Response {
        val url = "http://103.193.150.125:10101/api/stats/$uuid"
        val request = Request.Builder()
            .url(url)
            .build()
        return client.newCall(request).execute()
    }
}