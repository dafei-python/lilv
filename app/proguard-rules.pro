# 利率速查 — R8 保留规则（防止数据类/图标被压缩后运行时崩溃）

# 保留注解信息
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, RuntimeVisibleAnnotations

# Compose Material Icons（我们用 Rounded/Outlined）
-keep class androidx.compose.material.icons.** { *; }

# Room：数据库入口、实体字段、Dao 方法
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class com.dushishiyi.lilv.data.local.SnapshotEntity { <init>(...); }

# Retrofit 接口 + DTO + kotlinx.serialization
-keep interface com.dushishiyi.lilv.data.remote.RatesApi { *; }
-keepclassmembers class com.dushishiyi.lilv.data.** {
    <init>(...);
    kotlinx.serialization.KSerializer **$$serializer;
}
-keep class com.dushishiyi.lilv.data.RatesDto$$serializer { *; }

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# kotlinx.serialization 内部
-keep class kotlinx.serialization.** { *; }
