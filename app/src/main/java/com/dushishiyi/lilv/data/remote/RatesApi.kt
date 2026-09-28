package com.dushishiyi.lilv.data.remote

import com.dushishiyi.lilv.data.RatesDto
import retrofit2.http.GET

/**
 * 远端 rates.json 接口。
 * baseUrl 由 [NetworkProvider] 配置为 GitHub Pages 自定义子域名。
 */
interface RatesApi {
    @GET("rates.json")
    suspend fun fetchRates(): RatesDto
}
