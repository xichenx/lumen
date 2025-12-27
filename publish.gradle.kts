/**
 * 简化的发布配置
 * 使用 com.vanniktech.maven.publish 插件的标准 DSL
 * 
 * 参考: https://vanniktech.github.io/gradle-maven-publish-plugin/central/
 * 
 * 使用方法：
 * 1. 在子模块的 build.gradle.kts 中应用插件：alias(libs.plugins.maven.publish)
 * 2. 应用此脚本：apply(from = rootProject.file("publish.gradle.kts"))
 * 3. 配置凭证（gradle.properties 或环境变量）：
 *    - mavenCentralUsername / SONATYPE_USERNAME
 *    - mavenCentralPassword / SONATYPE_PASSWORD
 *    - signingInMemoryKey / SIGNING_IN_MEMORY_KEY
 *    - signingInMemoryKeyId / SIGNING_KEY_ID (可选)
 *    - signingInMemoryKeyPassword / SIGNING_PASSWORD (可选)
 */

// 统一版本号管理：确保 JitPack 和 Maven Central 使用相同的版本号
// 优先使用 LIBRARY_VERSION_NAME（组件版本），如果没有则使用 VERSION_NAME
// 这与 build.gradle.kts 中的版本管理保持一致
val versionName: String = project.findProperty("LIBRARY_VERSION_NAME") as String?
    ?: project.findProperty("VERSION_NAME") as String?
    ?: "1.0.0"
val isJitPack = System.getenv("JITPACK") == "true"

// 根据发布方式选择不同的 groupId
val githubUser = project.findProperty("GITHUB_USER") as String? ?: "xichenx"
val publishGroupId = if (isJitPack) {
    "com.github.$githubUser"
} else {
    "io.github.$githubUser"
}

// 设置项目版本（JitPack 和 Maven Central 使用相同的版本号）
version = versionName

// 配置发布（仅在非 JitPack 时使用 Maven Central）
if (!isJitPack && project.plugins.hasPlugin("com.vanniktech.maven.publish")) {
    // 映射凭证到插件需要的属性名（为了兼容性）
    val sonatypeUsername = project.findProperty("SONATYPE_USERNAME") as String?
        ?: System.getenv("SONATYPE_USERNAME")
    val sonatypePassword = project.findProperty("SONATYPE_PASSWORD") as String?
        ?: System.getenv("SONATYPE_PASSWORD")
    
    if (sonatypeUsername != null && sonatypePassword != null) {
        project.setProperty("mavenCentralUsername", sonatypeUsername.trim())
        project.setProperty("mavenCentralPassword", sonatypePassword.trim())
    }
    
    // 配置 GPG 签名（使用内存密钥）
    val signingKey = project.findProperty("SIGNING_IN_MEMORY_KEY") as String?
        ?: System.getenv("SIGNING_IN_MEMORY_KEY")
        ?: project.findProperty("SIGNING_SECRET_KEY_RING_FILE") as String?
        ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    
    if (signingKey != null) {
        val keyContent = if (file(signingKey).exists()) {
            file(signingKey).readText().trim()
        } else {
            signingKey.trim()
        }
        
        if (keyContent.isNotBlank()) {
            project.setProperty("signingInMemoryKey", keyContent)
            
            val signingKeyId = project.findProperty("SIGNING_KEY_ID") as String?
                ?: System.getenv("SIGNING_KEY_ID")
            val signingPassword = project.findProperty("SIGNING_PASSWORD") as String?
                ?: System.getenv("SIGNING_PASSWORD")
            
            if (signingKeyId != null) {
                project.setProperty("signingInMemoryKeyId", signingKeyId.trim())
            }
            if (signingPassword != null) {
                project.setProperty("signingInMemoryKeyPassword", signingPassword.trim())
            }
        }
    }
    
    // 配置 mavenPublishing（使用 afterEvaluate 确保插件已初始化）
    afterEvaluate {
        // 使用简化的配置方式
        val mavenPublishing = extensions.findByName("mavenPublishing")
        if (mavenPublishing != null) {
            try {
                // 设置坐标
                mavenPublishing.javaClass.getMethod(
                    "coordinates",
                    String::class.java,
                    String::class.java,
                    String::class.java
                ).invoke(mavenPublishing, publishGroupId, project.name, versionName)
                
                // 配置 Maven Central
                mavenPublishing.javaClass.getMethod("publishToMavenCentral").invoke(mavenPublishing)
                
                // 启用签名
                mavenPublishing.javaClass.getMethod("signAllPublications").invoke(mavenPublishing)
                
                logger.info("✅ mavenPublishing configured for ${project.name}")
            } catch (e: Exception) {
                logger.warn("⚠️  Could not configure mavenPublishing: ${e.message}")
            }
        }
    }
} else if (isJitPack) {
    // JitPack 模式：使用标准的 maven-publish
    // 注意：版本号与 Maven Central 保持一致（使用相同的 versionName）
    if (!project.plugins.hasPlugin("maven-publish")) {
        project.plugins.apply("maven-publish")
    }
    
    afterEvaluate {
        extensions.configure<org.gradle.api.publish.PublishingExtension>("publishing") {
            publications {
                create<org.gradle.api.publish.maven.MavenPublication>("release") {
                    from(components["release"])
                    groupId = publishGroupId
                    artifactId = project.name
                    // 使用与 Maven Central 相同的版本号
                    version = versionName
                }
            }
        }
    }
} else {
    // 非 JitPack 但插件未应用，只记录警告
    logger.warn("⚠️  com.vanniktech.maven.publish plugin not applied to ${project.name}, skipping Maven Central configuration")
}
