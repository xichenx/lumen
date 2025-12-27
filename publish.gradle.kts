/**
 * 统一发布配置
 * 支持 Maven Central 和 JitPack 两种发布方式
 * 
 * 特性：
 * - 子模块独立发布
 * - lumen 模块聚合所有子模块，依赖自动传递
 * - 支持 GitHub Actions 自动发布
 * 
 * 注意：此脚本通过 apply(from = ...) 应用
 * Android Library 插件已自动应用 maven-publish 插件
 * 需要手动应用 signing 插件
 */

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

// 应用必要的插件
// 注意：Android Library 插件在某些版本中可能不会自动应用 maven-publish
if (!project.plugins.hasPlugin("maven-publish")) {
    project.plugins.apply("maven-publish")
}
if (!project.plugins.hasPlugin("signing")) {
    project.plugins.apply("signing")
}

// 从 gradle.properties 读取版本号
val versionName: String = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"
val isJitPack = System.getenv("JITPACK") == "true"

// 根据发布方式选择不同的 groupId
// GitHub 仓库: https://github.com/XichenX/Lumen
// 注意：Group ID 必须使用小写，与 Central Portal 中注册的命名空间完全匹配
val githubUser = project.findProperty("GITHUB_USER") as String? ?: "xichenx" // 使用小写
val publishGroupId = if (isJitPack) {
    // JitPack 格式: com.github.用户名
    "com.github.$githubUser"
} else {
    // Maven Central 格式: io.github.用户名 (GitHub 域名格式，必须小写)
    "io.github.$githubUser"
}

// 设置项目版本
version = versionName

// 配置发布
// 注意：需要在 afterEvaluate 中配置，确保 Android 插件已配置完成
afterEvaluate {
    // 确保 maven-publish 插件已应用
    if (!project.plugins.hasPlugin("maven-publish")) {
        project.plugins.apply("maven-publish")
    }
    
    // 等待一下，确保插件完全初始化
    // 然后查找 PublishingExtension
    val publishing = extensions.findByType<PublishingExtension>()
    if (publishing == null) {
        // 如果 PublishingExtension 不存在，尝试强制创建
        // 这通常不应该发生，但如果发生了，我们需要处理
        logger.error("❌ PublishingExtension not found for ${project.name}")
        logger.error("   maven-publish plugin applied: ${project.plugins.hasPlugin("maven-publish")}")
        logger.error("   This is a critical error. Publishing configuration cannot proceed.")
        throw GradleException(
            "PublishingExtension not found for ${project.name}. " +
            "This usually means the maven-publish plugin was not properly initialized. " +
            "Please check your build configuration."
        )
    }
    
    publishing.publications {
        create<MavenPublication>("release") {
            // 发布 Android 库
            from(components["release"])
            
            groupId = publishGroupId
            artifactId = project.name
            version = versionName
            
            pom {
                name.set("Lumen ${project.name}")
                description.set("A Kotlin-first Android image loading library")
                url.set("https://github.com/XichenX/Lumen")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("XichenX")
                        name.set("Lumen Team")
                        email.set("3108531642@qq.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/XichenX/Lumen.git")
                    developerConnection.set("scm:git:ssh://github.com:XichenX/Lumen.git")
                    url.set("https://github.com/XichenX/Lumen")
                }
            }
        }
    }
    
    // Maven Central 仓库配置（仅非 JitPack 时使用）
    // 使用 Central Publishing Portal（OSSRH 将于 2025-06-30 停止服务）
    // 参考：https://central.sonatype.org/news/20250326_ossrh_sunset/
    if (!isJitPack) {
        publishing.repositories {
            maven {
                name = "MavenCentral"
                
                // Central Publishing Portal URL
                // 注意：新的 Central Publishing Portal API 端点
                // 如果标准的 maven-publish 插件不支持 REST API，可能需要使用社区插件
                // 或者回退到 OSSRH URL（使用 Central Token 认证）
                val useNewApi = project.findProperty("USE_CENTRAL_PORTAL_API") as String?
                    ?: System.getenv("USE_CENTRAL_PORTAL_API")
                    ?: "true" // 默认尝试新 API
                
                val releasesRepoUrl = if (useNewApi == "true") {
                    // 新的 Central Publishing Portal REST API 端点
                    "https://central.sonatype.com/api/v1/publisher/"
                } else {
                    // 回退到 OSSRH URL（使用 Central Token 认证）
                    // 注意：OSSRH 已停止，但如果新 API 不兼容，可以临时使用此选项
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                }
                
                val snapshotsRepoUrl = if (useNewApi == "true") {
                    "https://central.sonatype.com/api/v1/publisher/" // SNAPSHOT 也使用相同端点
                } else {
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                }
                
                url = uri(if (versionName.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                
                credentials {
                    // Central Publishing Portal 使用 Token 认证
                    // 注意：变量名保持 SONATYPE_USERNAME 和 SONATYPE_PASSWORD 以保持兼容性
                    // SONATYPE_USERNAME: Central Portal 的用户名（短用户名）
                    // SONATYPE_PASSWORD: Central Portal 的专属 Token（虽然叫 PASSWORD，但实际是 Token）
                    val sonatypeUsername = project.findProperty("SONATYPE_USERNAME") as String?
                        ?: System.getenv("SONATYPE_USERNAME")
                    val sonatypeToken = project.findProperty("SONATYPE_PASSWORD") as String?
                        ?: System.getenv("SONATYPE_PASSWORD")
                    
                    // 清理 Token（移除可能的空格或换行符）
                    val cleanToken = sonatypeToken?.trim()
                    val cleanUsername = sonatypeUsername?.trim()
                    
                    username = cleanUsername
                    password = cleanToken
                    
                    // 详细的调试信息（不打印实际 Token）
                    if (cleanUsername.isNullOrBlank() || cleanToken.isNullOrBlank()) {
                        logger.error("❌ Central Publishing Portal credentials are missing or empty")
                        logger.error("   SONATYPE_USERNAME: ${if (cleanUsername.isNullOrBlank()) "not set" else "set (${cleanUsername.length} chars)"}")
                        logger.error("   SONATYPE_PASSWORD (Central Token): ${if (cleanToken.isNullOrBlank()) "not set" else "set (${cleanToken.length} chars)"}")
                        logger.error("   Make sure SONATYPE_USERNAME and SONATYPE_PASSWORD (Central Token) are set in GitHub Secrets")
                        logger.error("   Get your Central Token from: https://central.sonatype.com/ → Profile → User Token")
                    } else {
                        logger.info("✅ Central Publishing Portal credentials configured")
                        logger.info("   Username: ${cleanUsername.take(3)}*** (length: ${cleanUsername.length})")
                        logger.info("   Central Token: ${"*".repeat(cleanToken.length)} (length: ${cleanToken.length})")
                        logger.info("   Repository URL: ${url}")
                        logger.info("   Group ID: $publishGroupId (must match Central Portal namespace exactly, case-sensitive)")
                        logger.info("   Using Central Publishing Portal API (https://central.sonatype.com/api/v1/publisher/)")
                        
                        // 验证 Token 格式（通常 Central Token 是较长的字符串）
                        if (cleanToken.length < 20) {
                            logger.warn("⚠️  Warning: Central Token seems too short (${cleanToken.length} chars)")
                            logger.warn("   Central Tokens are usually longer. Please verify you're using the correct Token.")
                        }
                        
                        // 检查命名空间提示
                        logger.info("   ⚠️  Make sure namespace '$publishGroupId' is registered and verified in Central Portal")
                        logger.info("   ⚠️  If you get 401 errors, verify:")
                        logger.info("      1. Token is correct and not expired")
                        logger.info("      2. Username matches the Token owner")
                        logger.info("      3. Namespace '$publishGroupId' is verified in Central Portal")
                    }
                }
            }
        }
    }
}

// Maven Central 签名配置（仅非 JitPack 时使用）
if (!isJitPack) {
    afterEvaluate {
        val signing = extensions.findByType<SigningExtension>()
        val publishing = extensions.findByType<PublishingExtension>()
        
        if (signing == null || publishing == null) {
            logger.warn("SigningExtension or PublishingExtension not found, skipping signing configuration")
            return@afterEvaluate
        }
        
        val signingKeyId = project.findProperty("SIGNING_KEY_ID") as String?
            ?: System.getenv("SIGNING_KEY_ID")
        val signingPassword = project.findProperty("SIGNING_PASSWORD") as String?
            ?: System.getenv("SIGNING_PASSWORD")
        val signingSecretKeyRingFile = project.findProperty("SIGNING_SECRET_KEY_RING_FILE") as String?
            ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")
        
        if (signingKeyId != null && signingPassword != null && signingSecretKeyRingFile != null) {
            try {
                val secretKeyRingFile = file(signingSecretKeyRingFile)
                if (secretKeyRingFile.exists()) {
                    val secretKey = secretKeyRingFile.readText()
                    if (secretKey.isBlank()) {
                        logger.warn("GPG secret key file is empty: $signingSecretKeyRingFile, skipping signing")
                        return@afterEvaluate
                    }
                    
                    signing.useInMemoryPgpKeys(signingKeyId, secretKey, signingPassword)
                    signing.sign(publishing.publications["release"])
                    logger.info("✅ GPG signing configured for ${project.name}")
                } else {
                    logger.warn("⚠️  GPG secret key file not found: $signingSecretKeyRingFile, skipping signing")
                }
            } catch (e: Exception) {
                logger.error("❌ Failed to configure GPG signing: ${e.message}", e)
                throw e
            }
        } else {
            logger.warn("⚠️  GPG signing credentials not fully configured, skipping signing")
            logger.debug("  SIGNING_KEY_ID: ${if (signingKeyId != null) "set" else "not set"}")
            logger.debug("  SIGNING_PASSWORD: ${if (signingPassword != null) "set" else "not set"}")
            logger.debug("  SIGNING_SECRET_KEY_RING_FILE: ${if (signingSecretKeyRingFile != null) "set" else "not set"}")
        }
    }
}

// 注意：移除了发布前验证任务
// 原因：
// 1. Gradle 的发布任务会自动检查 PublishingExtension 和 publication 是否存在
// 2. 如果配置有问题，Gradle 会在任务执行时自动报错，并提供更清晰的错误信息
// 3. 额外的验证可能导致时序问题（如当前遇到的 PublishingExtension 找不到的问题）
// 4. 我们已经有了 validatePublishConfiguration 任务在配置阶段验证
// 
// 如果需要验证，可以在发布前手动运行：
//   ./gradlew validatePublishConfiguration

