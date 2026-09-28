package com.dushishiyi.lilv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 拉取快照：每次成功拉取 rates.json 后存一份完整 JSON。
 * 用于本地有"上次的值"，从而显示"工行 1 年期 0.95% → 0.90%"这种变动。
 *
 * 保留最近 30 天，自动清理（见 [SnapshotDao.trim]）。
 */
@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val fetchedAt: Long,    // 毫秒时间戳
    val payload: String,                // 完整 JSON
)
