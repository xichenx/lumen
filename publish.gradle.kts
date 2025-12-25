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
val githubUser = project.findProperty("GITHUB_USER") as String? ?: "XichenX"
val publishGroupId = if (isJitPack) {
    // JitPack 格式: com.github.用户名
    "com.github.$githubUser"
} else {
    // Maven Central 格式: io.github.用户名 (GitHub 域名格式)
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
    
    val publishing = extensions.findByType<PublishingExtension>()
    if (publishing == null) {
        logger.warn("PublishingExtension not found, skipping publishing configuration")
        return@afterEvaluate
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
    if (!isJitPack) {
        publishing.repositories {
            maven {
                name = "MavenCentral"
                val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                url = uri(if (versionName.endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                
                credentials {
                    username = project.findProperty("SONATYPE_USERNAME") as String?
                        ?: System.getenv("SONATYPE_USERNAME")
                    password = project.findProperty("SONATYPE_PASSWORD") as String?
                        ?: System.getenv("SONATYPE_PASSWORD")
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

// 添加发布前验证任务
afterEvaluate {
    // 验证所有发布相关的任务
    tasks.matching { task ->
        task.name.startsWith("publish") && 
        (task.name.contains("Release") || task.name == "publish" || task.name == "publishToMavenLocal")
    }.configureEach { task ->
        task.doFirst {
            val publishing = extensions.findByType<PublishingExtension>()
            if (publishing == null) {
                throw GradleException("PublishingExtension not found for ${project.name}")
            }
            
            val publication = publishing.publications.findByName("release")
            if (publication == null) {
                throw GradleException("Release publication not found for ${project.name}")
            }
            
            // 验证版本号
            if (version == "unspecified" || version.toString().isBlank()) {
                throw GradleException("Version is not set for ${project.name}")
            }
            
            // 验证 groupId
            val groupId = (publication as? MavenPublication)?.groupId
            if (groupId.isNullOrBlank()) {
                throw GradleException("GroupId is not set for ${project.name}")
            }
            
            logger.info("✅ Publishing ${project.name} version $version to group $groupId")
        }
    }
}

