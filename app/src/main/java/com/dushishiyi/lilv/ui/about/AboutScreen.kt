package com.dushishiyi.lilv.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dushishiyi.lilv.BuildConfig
import com.dushishiyi.lilv.R

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 应用图标（用前景图层）
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.about_app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(20.dp))

        // 引言
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Text(
                text = stringResource(R.string.about_quote),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // 正文
        Text(
            text = stringResource(R.string.about_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        // 公众号卡片
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.about_wechat_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.about_wechat_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                )

                // 公众号「读书十页」二维码
                Image(
                    painter = painterResource(R.drawable.qrcode_wechat),
                    contentDescription = "公众号读书十页二维码",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp),
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "读书十页",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("公众号", "读书十页"))
                        Toast.makeText(context, context.getString(R.string.about_wechat_copied), Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = stringResource(R.string.about_wechat_action),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 数据来源
        AboutInfoCard(
            title = stringResource(R.string.about_data_source_title),
            body = stringResource(R.string.about_data_source_body),
        )

        Spacer(Modifier.height(12.dp))

        // 免责声明
        AboutInfoCard(
            title = stringResource(R.string.about_disclaimer_title),
            body = stringResource(R.string.about_disclaimer_body),
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.about_author),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutInfoCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 二维码占位图（已弃用，保留以备未来需要）。
 *
 * 实际公众号「读书十页」二维码图片已放在
 * res/drawable/qrcode_wechat.jpg，由 AboutScreen 主体直接 Image 引用。
 */
@Composable
private fun QrCodePlaceholder() {
    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = null,
                tint = Color(0xFF2E7D5B),
                modifier = Modifier.size(80.dp),
            )
            Text(
                text = "二维码占位\n请替换为\n读书十页公众号二维码",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B726B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
