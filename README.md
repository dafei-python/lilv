# 利率速查

一个朴素的安卓小工具：每天看一眼央行 LPR、五大行存款利率、公积金贷款利率。
公众号「读书十页」出品，灵感来自《金钱心理学》。

## 功能

四个 Tab，无注册无登录：

- **存款**：工/农/中/建/邮储五大行整存整取利率（3 月～5 年），期限切换，变动提示
- **贷款**：LPR 当前值与历史走势、公积金贷款利率（首套/二套）、典型房贷参考
- **计算器**：商贷 / 公积金 / 组合贷 × 等额本息 / 等额本金
- **关于**：《金钱心理学》文案 + 公众号「读书十页」二维码

Material 3 动态色彩 + 暗色模式，自适应图标。

## 数据怎么更新（全自动，免费）

```
GitHub Actions（每天 09:30）→ 抓数据 commit 到 docs/rates.json
        → GitHub Pages 自动部署 → https://lilv.dafei-python.cn/rates.json
        → APP 打开时自动拉取（断网用内置缓存）
```

零服务器、零 secrets、零费用。公开仓库的 Actions 和 Pages 额度无限，本项目用量可忽略。
仓库保持 **Public** 即可，改成 Private 会停用 Pages。

## 目录

```
app/                     Android APP（Kotlin + Compose）
data-pipeline/           Python 抓取脚本
docs/                    GitHub Pages 发布目录（rates.json + CNAME）
.github/workflows/       每日定时任务
```

## 构建 APK

需要 JDK 21 + Android SDK 35：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

安装到手机：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 数据源

| 数据 | 来源 |
|---|---|
| LPR | 全国银行间同业拆借中心（每月 20 日公布） |
| 公积金贷款利率 | 中国人民银行公告 |
| 五大行存款利率 | 各行官网人民币存款利率表 |

## 免责声明

数据来自公开渠道，仅供参考，实际利率以银行柜面为准，不构成投资建议。

---

由 读书十页 出品 · 灵感来自《金钱心理学》
