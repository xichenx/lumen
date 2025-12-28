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
 *    - ORG_GRADLE_PROJECT_mavenCentralUsername: GPG 密钥内容（插件自动读取）
 *    - ORG_GRADLE_PROJECT_signingInMemoryKeyId: GPG 密钥 ID（插件自动读取）
 *    - ORG_GRADLE_PROJECT_mavenCentralPassword: GPG 密钥密码（插件自动读取）
 *    - ORG_GRADLE_PROJECT_signingInMemoryKey
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
mavenPublishing{
    publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.VALIDATE)
    signAllPublications()

    coordinates("io.github.xichenx", "lumen", version)
    pom {
        name.set("Lumen")
        description.set("A description of what my library does.")
        inceptionYear.set("2025")
        url.set("https://github.com/xichenx/lumen/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("xichen")
                name.set("刘明智")
                url.set("https://github.com/xichenx/")
            }
        }
        scm {
            url.set("https://github.com/xichenx/lumen/")
            connection.set("scm:git:git://github.com/xichenx/lumen.git")
            developerConnection.set("scm:git:ssh://git@github.com/xichenx/lumen.git")
        }
    }
}