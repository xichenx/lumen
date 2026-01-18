plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
}

// 版本配置：优先级 LUMEN_CORE_VERSION > 默认值
val publishVersion: String = run {
    val moduleVersion = project.findProperty("LUMEN_CORE_VERSION") as String?
    
    when {
        !moduleVersion.isNullOrBlank() -> moduleVersion.trim()
        else -> "1.0.0"
    }
}

version = publishVersion
logger.info("📦 Publishing lumen-core version: $publishVersion")

// Maven 发布配置
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("io.github.xichenx", "lumen-core", publishVersion)
    pom {
        name.set("Lumen Core")
        description.set("Core loading logic for Lumen - A Kotlin-first Android image loading library")
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
    namespace = "com.xichen.lumen.core"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
