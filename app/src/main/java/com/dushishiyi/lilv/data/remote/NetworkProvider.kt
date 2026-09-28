package com.dushishiyi.lilv.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * 网络层单例。
 *
 * baseUrl 指向 GitHub Pages 自定义子域名（CNAME 到 dafei-python.github.io）。
 * HTTPS 证书由 GitHub 自动签发（Let's Encrypt）。
 */
object NetworkProvider {

    const val BASE_URL = "https://lilv.dafei-python.cn/"

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val api: RatesApi by lazy { buildApi() }

    private fun buildApi(): RatesApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RatesApi::class.java)
    }
}
