plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

// 统一版本号管理：确保 JitPack 和 Maven Central 使用相同的版本号
// 优先级：JitPack 传递的 version > LIBRARY_VERSION_NAME > VERSION_NAME > 默认值
val publishVersion: String = run {
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

// 设置项目版本
version = publishVersion
logger.info("📦 Publishing version: $publishVersion for ${project.name}")

// 配置 Maven 发布（直接在这里配置，可以访问插件类型）
mavenPublishing {
    // 只在非 JitPack 环境发布到 Maven Central（JitPack 不需要）
    val isJitPack = System.getenv("JITPACK") == "true"
    if (!isJitPack) {
        publishToMavenCentral(automaticRelease = true)
        // 只在 Maven Central 发布时启用签名（JitPack 不需要签名）
        signAllPublications()
    }

    coordinates("io.github.xichenx", "lumen", publishVersion)
    pom {
        name.set("Lumen")
        description.set("A description of what my library does.")
        inceptionYear.set("2025")
        url.set("https://github.com/xichenx/lumen/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
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
            developerConnection.set("scm:git:ssh://git@github.com:xichenx/lumen.git")
        }
    }
}

android {
    namespace = "com.xichen.lumen.library"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

dependencies {
    // 聚合所有子模块
    api(project(":lumen-core"))
    api(project(":lumen-view"))
    api(project(":lumen-transform"))
    
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

