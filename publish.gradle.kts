/**
 * 简化的发布配置
 * 使用 com.vanniktech.maven.publish 插件的标准 DSL
 * 
 * 参考: https://vanniktech.github.io/gradle-maven-publish-plugin/central/
 * 
 * 使用方法：
 * 1. 在子模块的 build.gradle.kts 中应用插件：alias(libs.plugins.maven.publish)
 * 2. 应用此脚本：apply(from = rootProject.file("publish.gradle.kts"))
 * 3. 配置凭证（通过环境变量或 gradle.properties）：
 *    - mavenCentralUsername: Sonatype 用户名（插件自动读取）
 *    - mavenCentralPassword: Sonatype 密码（插件自动读取）
 *    - signingInMemoryKey: GPG 密钥内容（插件自动读取）
 *    - signingInMemoryKeyId: GPG 密钥 ID（插件自动读取）
 *    - signingInMemoryKeyPassword: GPG 密钥密码（插件自动读取）
 *    或者通过 SIGNING_SECRET_KEY_RING_FILE 指定密钥文件路径（脚本会自动读取并设置）
 */

// 统一版本号管理：确保 JitPack 和 Maven Central 使用相同的版本号
// 优先级：JitPack 传递的 version > LIBRARY_VERSION_NAME > VERSION_NAME > 默认值
// 这与 build.gradle.kts 中的版本管理保持一致
val versionName: String = run {
    // JitPack 通过 -Pversion=xxx 传递版本号，优先使用
    val jitpackVersion = project.findProperty("version") as String?
    val libraryVersion = project.findProperty("LIBRARY_VERSION_NAME") as String?
    val fallbackVersion = project.findProperty("VERSION_NAME") as String?
    
    // 优先级：JitPack version > LIBRARY_VERSION_NAME > VERSION_NAME > 默认值
    when {
        !jitpackVersion.isNullOrBlank() -> jitpackVersion.trim()
        !libraryVersion.isNullOrBlank() -> libraryVersion.trim()
        !fallbackVersion.isNullOrBlank() -> fallbackVersion.trim()
        else -> "1.0.0"
    }
}
val isJitPack = System.getenv("JITPACK") == "true"

// 根据发布方式选择不同的 groupId
// 确保 GitHub 用户名使用小写（JitPack 要求小写）
val githubUser = (project.findProperty("GITHUB_USER") as String? ?: "xichenx").lowercase()
val publishGroupId = if (isJitPack) {
    "com.github.$githubUser"
} else {
    "io.github.$githubUser"
}

// 设置项目版本（JitPack 和 Maven Central 使用相同的版本号）
version = versionName
logger.info("📦 Publishing version: $versionName for ${project.name}")


// 配置发布：统一使用 com.vanniktech.maven.publish 插件
// 在 JitPack 和 Maven Central 模式下都使用这个插件，只是配置不同的坐标
if (project.plugins.hasPlugin("com.vanniktech.maven.publish")) {
    // 注意：com.vanniktech.maven.publish 插件会自动从以下位置读取凭证：
    // 1. gradle.properties 文件中的 mavenCentralUsername 和 mavenCentralPassword
    // 2. 环境变量 mavenCentralUsername 和 mavenCentralPassword
    // 我们不需要手动设置这些属性，插件会自动读取
    
    // 配置 GPG 签名（使用内存密钥）
    // 需要显式配置 Gradle 签名插件以使用内存中的 GPG 密钥
    afterEvaluate {
        // 读取 GPG 签名配置
        val signingKeyFile = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
            ?: project.findProperty("SIGNING_SECRET_KEY_RING_FILE") as String?
        val signingKeyId = System.getenv("signingInMemoryKeyId")
            ?: System.getenv("SIGNING_KEY_ID")
            ?: project.findProperty("signingInMemoryKeyId") as String?
            ?: project.findProperty("SIGNING_KEY_ID") as String?
        val signingPassword = System.getenv("signingInMemoryKeyPassword")
            ?: System.getenv("SIGNING_PASSWORD")
            ?: project.findProperty("signingInMemoryKeyPassword") as String?
            ?: project.findProperty("SIGNING_PASSWORD") as String?
        
        // 如果提供了密钥文件路径，读取密钥内容
        val signingKeyContent = if (signingKeyFile != null) {
            try {
                val keyFile = file(signingKeyFile)
                if (keyFile.exists()) {
                    val content = keyFile.readText().trim()
                    if (content.isNotBlank()) {
                        logger.info("✅ GPG key loaded from file: $signingKeyFile")
                        content
                    } else null
                } else null
            } catch (e: Exception) {
                logger.warn("⚠️  Failed to read GPG key file: ${e.message}")
                null
            }
        } else {
            // 或者从环境变量直接读取密钥内容
            System.getenv("signingInMemoryKey")
                ?: System.getenv("SIGNING_IN_MEMORY_KEY")
                ?: project.findProperty("signingInMemoryKey") as String?
                ?: project.findProperty("SIGNING_IN_MEMORY_KEY") as String?
        }
        
        // 配置签名插件（如果所有必需的签名信息都可用）
        if (signingKeyContent != null && signingKeyId != null && signingPassword != null) {
            try {
                // 应用签名插件（如果尚未应用）
                if (!project.plugins.hasPlugin("signing")) {
                    project.plugins.apply("signing")
                }
                
                // 配置签名插件使用内存中的密钥
                extensions.configure<org.gradle.plugins.signing.SigningExtension>("signing") {
                    useInMemoryPgpKeys(signingKeyId, signingKeyContent, signingPassword)
                }
                
                logger.info("✅ GPG signing configured for ${project.name}")
            } catch (e: Exception) {
                logger.warn("⚠️  Failed to configure GPG signing: ${e.message}")
            }
        } else {
            val missing = mutableListOf<String>()
            if (signingKeyContent == null) missing.add("signingInMemoryKey")
            if (signingKeyId == null) missing.add("signingInMemoryKeyId")
            if (signingPassword == null) missing.add("signingInMemoryKeyPassword")
            logger.warn("⚠️  GPG signing not configured for ${project.name}, missing: ${missing.joinToString(", ")}")
        }
    }
    
    // 配置 mavenPublishing（使用 afterEvaluate 确保插件已初始化）
    afterEvaluate {
        // artifactId 统一使用小写
        val artifactId = project.name.lowercase()
        
        // 检查是否有冲突的 publication（保险起见）
        if (project.plugins.hasPlugin("maven-publish")) {
            extensions.configure<org.gradle.api.publish.PublishingExtension>("publishing") {
                val publications = publications.withType<org.gradle.api.publish.maven.MavenPublication>()
                val conflictingPubs = publications.filter { 
                    it.groupId != publishGroupId || it.artifactId != artifactId 
                }
                if (conflictingPubs.isNotEmpty()) {
                    logger.warn("⚠️  Found ${conflictingPubs.size} conflicting publication(s) for ${project.name}, removing them")
                    conflictingPubs.forEach { pub ->
                        publications.remove(pub)
                        logger.warn("   Removed publication: ${pub.name} (${pub.groupId}:${pub.artifactId}:${pub.version})")
                    }
                }
            }
        }
        
        // 使用简化的配置方式
        val mavenPublishing = extensions.findByName("mavenPublishing")
        if (mavenPublishing != null) {
            try {
                // 验证版本号不为空
                if (versionName.isBlank()) {
                    logger.error("❌ Version name is empty for ${project.name}, cannot publish")
                    return@afterEvaluate
                }
                
                logger.info("🔧 Configuring mavenPublishing for ${project.name}: groupId=$publishGroupId, artifactId=$artifactId, version=$versionName")
                
                // 设置坐标
                mavenPublishing.javaClass.getMethod(
                    "coordinates",
                    String::class.java,
                    String::class.java,
                    String::class.java
                ).invoke(mavenPublishing, publishGroupId, artifactId, versionName)
                
                // 仅在非 JitPack 模式下配置 Maven Central 和签名
                if (!isJitPack) {
                    // 配置 Maven Central
                    // 注意：这可能会在清理时产生警告，但不会影响实际的发布
                    // 原因：com.vanniktech.maven.publish 插件在构建服务清理时，
                    // 尝试访问 centralPortal 属性，但该属性在某些情况下可能未初始化
                    // 这不会影响实际的发布过程，因为发布已经在清理阶段之前完成
                    mavenPublishing.javaClass.getMethod("publishToMavenCentral").invoke(mavenPublishing)
                    
                    // 启用签名（仅 Maven Central 需要签名）
                    mavenPublishing.javaClass.getMethod("signAllPublications").invoke(mavenPublishing)
                    
                    logger.info("✅ Maven Central publishing and signing configured for ${project.name}")
                } else {
                    logger.info("ℹ️  JitPack mode detected, skipping Maven Central publishing and signing configuration for ${project.name}")
                }
                
                logger.info("✅ mavenPublishing configured for ${project.name}")
            } catch (e: Exception) {
                logger.warn("⚠️  Could not configure mavenPublishing: ${e.message}")
            }
        }
    }
} else {
    // 插件未应用，只记录警告
    logger.warn("⚠️  com.vanniktech.maven.publish plugin not applied to ${project.name}, skipping publish configuration")
}
