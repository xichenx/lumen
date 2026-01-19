plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
}

// 版本配置：优先级 LUMEN_COMPOSE_VERSION > 默认值
val publishVersion: String = run {
    val moduleVersion = project.findProperty("LUMEN_COMPOSE_VERSION") as String?
    
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
logger.info("📦 Publishing lumen-compose version: $publishVersion")
logger.info("📦 Using BOM version: $bomVersion")

// Maven 发布配置
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("io.github.xichenx", "lumen-compose", publishVersion)
    pom {
        name.set("Lumen Compose")
        description.set("Jetpack Compose integration for Lumen - A Kotlin-first Android image loading library")
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
        // 手动添加 BOM 的 dependencyManagement，确保发布的 POM 包含 BOM 引用
        withXml {
            val dependencyManagement = asNode().appendNode("dependencyManagement")
            val dependencies = dependencyManagement.appendNode("dependencies")
            val bomDependency = dependencies.appendNode("dependency")
            bomDependency.appendNode("groupId", "io.github.xichenx")
            bomDependency.appendNode("artifactId", "lumen-bom")
            bomDependency.appendNode("version", bomVersion)
            bomDependency.appendNode("type", "pom")
            bomDependency.appendNode("scope", "import")
        }
    }
}

android {
    namespace = "com.xichen.lumen.compose"
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
    // BOM 平台依赖：构建时使用项目 BOM（因为 BOM 还未发布），发布时 POM 中会包含 BOM 的 dependencyManagement
    api(platform(project(":lumen")))
    
    // Core module - 使用 api 以便依赖传递，版本由 BOM 管理
    // 构建时使用项目依赖，发布时 Gradle 会自动转换为外部依赖（版本由 BOM 管理）
    api(project(":lumen-core"))
    
    // View module (for RequestBuilder) - 使用 api 以便依赖传递，版本由 BOM 管理
    // 构建时使用项目依赖，发布时 Gradle 会自动转换为外部依赖（版本由 BOM 管理）
    api(project(":lumen-view"))
    
    // AndroidX
    implementation(libs.androidx.core.ktx)
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
