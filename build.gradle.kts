// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

// 统一版本管理
// 优先使用LIBRARY_VERSION_NAME（组件版本），如果没有则使用VERSION_NAME
val versionName: String = project.findProperty("LIBRARY_VERSION_NAME") as String?
    ?: project.findProperty("VERSION_NAME") as String?
    ?: "1.0.0"

// 为所有子项目应用版本
subprojects {
    version = versionName
}

// 应用发布任务优化配置
apply(from = "publish-tasks.gradle.kts")