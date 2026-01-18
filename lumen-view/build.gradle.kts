plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// 版本配置：优先级 LUMEN_VIEW_VERSION > LIBRARY_VERSION_NAME > VERSION_NAME > 默认值
val publishVersion: String = run {
    val moduleVersion = project.findProperty("LUMEN_VIEW_VERSION") as String?
    val libraryVersion = project.findProperty("LIBRARY_VERSION_NAME") as String?
    val versionName = project.findProperty("VERSION_NAME") as String?
    
    when {
        !moduleVersion.isNullOrBlank() -> moduleVersion.trim()
        !libraryVersion.isNullOrBlank() -> libraryVersion.trim()
        !versionName.isNullOrBlank() -> versionName.trim()
        else -> "1.0.0"
    }
}

version = publishVersion
logger.info("📦 Publishing lumen-view version: $publishVersion")

// Maven 发布配置
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("io.github.xichenx", "lumen-view", publishVersion)
    pom {
        name.set("Lumen View")
        description.set("XML View integration for Lumen - A Kotlin-first Android image loading library")
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
    namespace = "com.xichen.lumen.view"
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
    // Core module - 使用 api 以便依赖传递，但版本由 BOM 管理
    api(project(":lumen-core"))
    
    // Transform module (for transformers) - 使用 api 以便依赖传递，但版本由 BOM 管理
    api(project(":lumen-transform"))
    
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}