package com.dushishiyi.lilv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dushishiyi.lilv.ui.LilvRoot
import com.dushishiyi.lilv.ui.theme.LilvTheme

/**
 * 入口 Activity。
 * 启动屏由 Manifest 中的 Theme.Lilv.Splash 提供（窗口背景色避免冷启动白闪）。
 * 内容加载后由 Compose 接管，主题在 setContent 内由 [LilvTheme] 应用。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LilvTheme {
                LilvRoot()
            }
        }
    }
}
