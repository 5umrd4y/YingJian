# Keep Room entities
-keep class com.yingjian.core.data.database.** { *; }
# Keep kotlinx.serialization models
-keep class com.yingjian.feature.photobook.model.** { *; }
-keep class com.yingjian.core.util.** { *; }
# Keep serialization annotations
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontwarn kotlinx.serialization.**
-dontwarn org.jetbrains.annotations.**
