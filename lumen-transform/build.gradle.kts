plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// 版本配置：优先级 LUMEN_TRANSFORM_VERSION > 默认值
val publishVersion: String = run {
    val moduleVersion = project.findProperty("LUMEN_TRANSFORM_VERSION") as String?
    
    when {
        !moduleVersion.isNullOrBlank() -> moduleVersion.trim()
        else -> "1.0.0"
    }
}

// BOM 版本（用于引用 BOM 进行版本管理）
val bomVersion: String = run {
    val bomVersionProperty = project.findProperty("LUMEN_BOM_VERSION") as String?
    when {
        !bomVersionProperty.isNullOrBlank() -> bomVersionProperty.trim()
        else -> publishVersion // 如果没有 BOM 版本，使用当前模块版本
    }
}

version = publishVersion
logger.info("📦 Publishing lumen-transform version: $publishVersion")
logger.info("📦 Using BOM version: $bomVersion")

// Maven 发布配置
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("io.github.xichenx", "lumen-transform", publishVersion)
    pom {
        name.set("Lumen Transform")
        description.set("Image transformation utilities for Lumen - A Kotlin-first Android image loading library")
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
    namespace = "com.xichen.lumen.transform"
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
}

dependencies {
    // BOM 平台依赖：确保发布的 POM 包含 dependencyManagement 引用 BOM
    api(platform("io.github.xichenx:lumen-bom:$bomVersion"))
    
    // Core module - 使用 api 以便依赖传递，版本由 BOM 管理
    // 本地构建时使用项目依赖，发布时 Gradle 会自动转换为外部依赖（版本由 BOM 管理）
    api(project(":lumen-core"))
    
    // AndroidX
    implementation(libs.androidx.core.ktx)
    
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
