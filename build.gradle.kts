// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.maven.publish) apply false
}

// 注意：各模块独立管理版本和发布
// 每个模块可以通过以下方式设置版本：
// 1. 模块特定版本属性（如 LUMEN_CORE_VERSION, LUMEN_TRANSFORM_VERSION 等）
// 2. 默认值 "1.0.0"
// 
// BOM 模块（lumen）用于版本协调，通过 LUMEN_BOM_VERSION 设置 BOM 版本，
// 通过 constraints 定义兼容的版本组合

// 应用发布任务优化配置
apply(from = "publish-tasks.gradle.kts")
