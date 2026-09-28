package com.dushishiyi.lilv.data

import com.dushishiyi.lilv.data.local.SnapshotDao
import com.dushishiyi.lilv.data.local.SnapshotEntity
import com.dushishiyi.lilv.data.remote.RatesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 利率数据仓库。
 *
 * - [refresh] 从远端拉取最新 rates.json，落库为快照
 * - [observeLatest] / [getCached] 提供本地最近一份快照
 * - [computeChanges] 计算与"上一份快照"的差异
 *
 * 设计原则：远端永远是权威源，本地仅作为缓存与"上次值"参考。
 */
class RatesRepository(
    private val api: RatesApi,
    private val dao: SnapshotDao,
    private val json: Json,
    private val fallbackJson: String = "",
) {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("Asia/Shanghai") }

    /** 拉取远端最新数据。成功后落库。 */
    suspend fun refresh(): Result<RatesDto> = runCatching {
        val dto = api.fetchRates()
        dao.insert(
            SnapshotEntity(
                fetchedAt = System.currentTimeMillis(),
                payload = json.encodeToString(RatesDto.serializer(), dto),
            )
        )
        dao.trim(KEEP_SNAPSHOTS)
        dto
    }

    /** 同步取得本地最新一份；若无快照，返回打包在 res/raw 中的 fallback。 */
    suspend fun getCached(): RatesDto? {
        val snap = dao.getLatest()
        if (snap != null) {
            return runCatching { json.decodeFromString(RatesDto.serializer(), snap.payload) }.getOrNull()
        }
        if (fallbackJson.isNotEmpty()) {
            return runCatching { json.decodeFromString(RatesDto.serializer(), fallbackJson) }.getOrNull()
        }
        return null
    }

    /** 监听本地最新快照（库变化时自动推送）。 */
    fun observeLatest(): Flow<RatesDto?> = flow {
        emit(getCached())
    }

    /** 计算当前数据相对上一份快照的变动。 */
    suspend fun computeChanges(current: RatesDto): List<RateChange> {
        val recent = dao.getRecent(2)  // 最近两份
        if (recent.size < 2) return emptyList()
        val previous = runCatching {
            json.decodeFromString(RatesDto.serializer(), recent[1].payload)
        }.getOrNull() ?: return emptyList()

        return diff(previous, current)
    }

    /** 公开 diff 逻辑，便于单测。 */
    fun diff(previous: RatesDto, current: RatesDto): List<RateChange> {
        val changes = mutableListOf<RateChange>()

        // 存款利率变动
        val prevMap = previous.deposit.banks.associateBy { it.code }
        for (bank in current.deposit.banks) {
            val prev = prevMap[bank.code] ?: continue
            DepositTerm.all.forEach { term ->
                val old = term.dtoSelector(prev.rates)
                val new = term.dtoSelector(bank.rates)
                if (old != new) {
                    val meta = BankCatalog.byCode(bank.code)
                    changes += RateChange(
                        scope = "deposit",
                        title = "${meta?.shortName ?: bank.name} ${term.label}",
                        oldValue = old,
                        newValue = new,
                        date = bank.updatedAt,
                    )
                }
            }
        }

        // LPR 变动
        if (previous.lpr.current.lpr1y != current.lpr.current.lpr1y) {
            changes += RateChange(
                scope = "lpr",
                title = "LPR 1 年期",
                oldValue = previous.lpr.current.lpr1y,
                newValue = current.lpr.current.lpr1y,
                date = current.lpr.current.date,
            )
        }
        if (previous.lpr.current.lpr5y != current.lpr.current.lpr5y) {
            changes += RateChange(
                scope = "lpr",
                title = "LPR 5 年期以上",
                oldValue = previous.lpr.current.lpr5y,
                newValue = current.lpr.current.lpr5y,
                date = current.lpr.current.date,
            )
        }

        // 公积金利率变动
        if (previous.fund.first5yAbove != current.fund.first5yAbove) {
            changes += RateChange(
                scope = "fund",
                title = "公积金 首套 5 年以上",
                oldValue = previous.fund.first5yAbove,
                newValue = current.fund.first5yAbove,
                date = current.fund.effectiveSince,
            )
        }
        if (previous.fund.second5yAbove != current.fund.second5yAbove) {
            changes += RateChange(
                scope = "fund",
                title = "公积金 二套 5 年以上",
                oldValue = previous.fund.second5yAbove,
                newValue = current.fund.second5yAbove,
                date = current.fund.effectiveSince,
            )
        }

        return changes
    }

    /** 把 fetchedAt 时间戳格式化为"今天 09:30"风格。 */
    fun formatFetchedAt(epochMillis: Long): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("Asia/Shanghai") }
        return sdf.format(Date(epochMillis))
    }

    companion object {
        private const val KEEP_SNAPSHOTS = 30  // 保留最近 30 份
    }
}
