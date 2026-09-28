# 利率速查

> 一个朴素的安卓小工具：每天看一眼央行 LPR、五大行存款利率、公积金贷款利率。
> 公众号「读书十页」出品，灵感来自《金钱心理学》。

---

## 设计理念

- **权威数据**：所有数字均来自中国人民银行、全国银行间同业拆借中心、各商业银行官网，不掺一丝自媒体转述。
- **零成本自动化**：GitHub Actions 每日定时抓取 → Cloudflare Pages 静态托管 → APP 拉取 JSON。全程免费。
- **极简、紧凑、不啰嗦**：四个 Tab，存/贷/算/关于，无注册无登录无弹窗。
- **暗色模式与图标**：跟随系统主题，自适应图标含单色层，Android 13+ 主题图标自动染色。
- **"看变化"**：本地 Room 缓存历史快照，每次拉取与上一份对比，自动生成 "工行 1 年期 0.95% → 0.90%（−5BP）" 变动横幅。

## 功能概览

| Tab | 内容 |
|---|---|
| 存款 | 五大行整存整取利率（3 月 / 6 月 / 1 年 / 2 年 / 3 年 / 5 年），期限 Chip 切换，挂牌日期+变动 BP |
| 贷款 | LPR 当前值 + 近 24 期历史走势图；公积金贷款利率（首套/二套 × 5 年上下）；典型房贷执行利率参考 |
| 计算器 | 商贷 / 公积金 / 组合贷 × 等额本息 / 等额本金，输出月供/总利息/还款总额/每月递减 |
| 关于 | 《金钱心理学》主题文案 + 公众号「读书十页」二维码 + 数据源声明 + 免责声明 |

## 目录结构

```
bank/
├── app/                                # Android 主模块
│   ├── src/main/java/com/dushishiyi/lilv/
│   │   ├── LilvApp.kt                  # Application 入口
│   │   ├── MainActivity.kt             # 单 Activity
│   │   ├── data/                       # 数据层
│   │   │   ├── RatesDto.kt             # 远端 JSON schema
│   │   │   ├── RatesRepository.kt      # 仓库 + diff 计算
│   │   │   ├── BankCatalog.kt          # 五大行元数据 + 期限枚举
│   │   │   ├── RateChange.kt           # 变动条目
│   │   │   ├── local/                  # Room 快照表
│   │   │   └── remote/                 # Retrofit 接口
│   │   └── ui/                         # Compose UI
│   │       ├── LilvRoot.kt             # 底部导航 + NavHost
│   │       ├── RatesViewModel.kt       # 存/贷款共享 VM
│   │       ├── theme/                  # Material 3 + 动态色彩 + 暗色
│   │       ├── components/             # 复用组件（折线图、变动横幅等）
│   │       ├── deposit/                # 存款 Tab
│   │       ├── loan/                   # 贷款 Tab
│   │       ├── calculator/             # 计算器 Tab
│   │       └── about/                  # 关于 Tab
│   └── src/main/res/
│       ├── drawable/                   # 自适应图标前景/背景/单色层
│       ├── mipmap-anydpi-v26/          # adaptive-icon 定义
│       ├── values/                     # 浅色主题
│       └── values-night/               # 暗色主题
├── data-pipeline/                      # 数据管线
│   ├── scripts/scraper.py              # 抓取脚本（输出到 ../../docs/rates.json）
│   └── requirements.txt
├── docs/                               # GitHub Pages 发布目录
│   ├── rates.json                      # APP 拉取的就是这个
│   ├── CNAME                           # 自定义子域名 lilv.dafei-python.cn
│   └── .nojekyll                       # 禁用 Jekyll 处理
├── .github/workflows/daily.yml         # 每日 cron 抓取+commit（Pages 自动重新部署）
└── settings.gradle.kts / build.gradle.kts / gradle/libs.versions.toml
```

## 本地构建

```bash
# 1. 用 Android Studio (Hedgehog+) 打开本目录
# 2. 等 Gradle Sync 完成
# 3. 接 Android 设备 / 模拟器（API 26+）
# 4. Run 'app'
```

或命令行：

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 数据管线（一次性配置）

1. **fork 或推到 GitHub**

2. **创建 Cloudflare Pages 项目**：在 Cloudflare Dashboard → Pages → Create project（先空创建一个名为 `lilv` 的项目，构建命令留空，输出目录 `public`）。

3. **在 GitHub 仓库 Settings → Secrets 中添加**：
   - `CLOUDFLARE_API_TOKEN` — Cloudflare API Token（权限：Account → Cloudflare Pages → Edit）
   - `CLOUDFLARE_ACCOUNT_ID` — Cloudflare Account ID
   - `CLOUDFLARE_PROJECT_NAME` — Pages 项目名（如 `lilv`）

4. **手动触发一次 workflow**：GitHub 仓库 → Actions → Daily Rates Refresh → Run workflow。

5. **拿到 Pages 地址**（如 `https://lilv.pages.dev/`），替换 `app/.../data/remote/NetworkProvider.kt` 中的 `BASE_URL`，重新打包。

之后每天北京时间 09:30 GitHub Actions 自动抓取并部署，无需人工干预。

## 替换公众号二维码

把公众号「读书十页」的二维码 PNG 保存为：

```
app/src/main/res/drawable/qrcode_wechat.png
```

然后修改 [app/src/main/java/com/dushishiyi/lilv/ui/about/AboutScreen.kt](app/src/main/java/com/dushishiyi/lilv/ui/about/AboutScreen.kt) 中的 `QrCodePlaceholder()`，按函数顶部注释替换为：

```kotlin
Image(
    painter = painterResource(R.drawable.qrcode_wechat),
    contentDescription = "公众号二维码",
    modifier = Modifier.size(180.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color.White)
        .padding(8.dp),
)
```

## 数据源

| 数据 | 来源 |
|---|---|
| LPR | 全国银行间同业拆借中心 [chinamoney.com.cn/chinese/bklpr](https://www.chinamoney.com.cn/chinese/bklpr/) |
| 公积金贷款利率 | 中国人民银行公告（不定期调整） |
| 五大行存款利率 | 各行官网「人民币存款利率表」 |

## 关于利率的基础知识

- **LPR 多久调一次？** 每月 20 日公布（遇节假日顺延）；但实际可能连续多月不变。
- **贷款利率是否所有银行一样？** LPR 基准全国统一，但**实际执行利率 = LPR ± 基点**，每家银行、每座城市、每个客户类型加点不同。
- **存款利率多久调一次？** 银行不定期调整，通常跟随央行降息/降准动作。
- **公积金贷款利率多久调一次？** 央行调整时才变，频率较低。

## 免责声明

本应用展示的利率数据均来自公开渠道，仅供参考。实际业务利率以银行柜面为准。本应用不构成任何投资建议。

---

由 读书十页 出品 · 灵感来自《金钱心理学》
