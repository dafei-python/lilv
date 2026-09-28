package com.dushishiyi.lilv

import android.app.Application
import com.dushishiyi.lilv.data.RatesRepository
import com.dushishiyi.lilv.data.local.LilvDatabase
import com.dushishiyi.lilv.data.remote.NetworkProvider
import kotlinx.serialization.json.Json

/**
 * Application 入口：构建全局单例（Repository / Json）。
 *
 * 不引入 Hilt，避免给"自用 + 分享 APK"的小项目增加编译期成本。
 * 简单 `object : Application()` + 全局单例足够。
 */
class LilvApp : Application() {

    val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    /**
     * 内置 fallback 数据：打包在 res/raw/rates_fallback.json。
     * 用于首次启动尚未拉到远端数据时展示，避免空白页。
     */
    private val fallbackJson: String by lazy {
        runCatching {
            resources.openRawResource(R.raw.rates_fallback).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    val ratesRepository: RatesRepository by lazy {
        RatesRepository(
            api = NetworkProvider.api,
            dao = LilvDatabase.get(this).snapshotDao(),
            json = json,
            fallbackJson = fallbackJson,
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LilvApp
            private set
    }
}
