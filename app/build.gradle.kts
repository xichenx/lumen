plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.xichen.lumen"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.xichen.lumen"
        minSdk = 24
        targetSdk = 36
        
        // 从 gradle.properties 读取版本信息，如果没有则使用默认值
        val appVersionName = project.findProperty("APP_VERSION_NAME") as String?
            ?: project.findProperty("VERSION_NAME") as String?
            ?: "1.0"
        val appVersionCode = (project.findProperty("APP_VERSION_CODE") as String?)?.toIntOrNull()
            ?: 1
        
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 从 gradle.properties 或环境变量读取签名配置
            val keystoreFile = project.findProperty("KEYSTORE_FILE") as String?
                ?: System.getenv("KEYSTORE_FILE")
            val keystorePassword = project.findProperty("KEYSTORE_PASSWORD") as String?
                ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = project.findProperty("KEY_ALIAS") as String?
                ?: System.getenv("KEY_ALIAS")
            val keyPassword = project.findProperty("KEY_PASSWORD") as String?
                ?: System.getenv("KEY_PASSWORD")
            
            // 调试信息
            if (keystoreFile != null) {
                println("🔍 Checking keystore file: $keystoreFile")
                println("   Project root: ${project.rootDir}")
                println("   Project dir: ${project.projectDir}")
            }
            
            // 只有当所有签名信息都配置完整时才设置签名配置
            if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                // 尝试多种路径解析方式
                val keystorePath = when {
                    // 绝对路径（Unix/Linux/Mac）或 Windows 绝对路径
                    keystoreFile.startsWith("/") || (keystoreFile.length >= 2 && keystoreFile[1] == ':') -> {
                        file(keystoreFile)
                    }
                    // 相对于项目根目录
                    else -> {
                        // 先尝试相对于项目根目录
                        val rootPath = File(project.rootDir, keystoreFile)
                        if (rootPath.exists()) {
                            file(rootPath)
                        } else {
                            // 再尝试相对于当前模块目录
                            val modulePath = File(project.projectDir, keystoreFile)
                            file(modulePath)
                        }
                    }
                }
                
                // 检查文件是否存在
                if (keystorePath.exists()) {
                    storeFile = keystorePath
                    storePassword = keystorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                    println("✅ Release signing configured with keystore: ${keystorePath.absolutePath}")
                } else {
                    println("⚠️  Keystore file not found: $keystoreFile")
                    println("   Tried paths:")
                    println("     - ${File(project.rootDir, keystoreFile).absolutePath}")
                    println("     - ${File(project.projectDir, keystoreFile).absolutePath}")
                    println("   Current working directory: ${System.getProperty("user.dir")}")
                    println("   Release build will be unsigned")
                }
            } else {
                val missing = mutableListOf<String>()
                if (keystoreFile == null) missing.add("KEYSTORE_FILE")
                if (keystorePassword == null) missing.add("KEYSTORE_PASSWORD")
                if (keyAlias == null) missing.add("KEY_ALIAS")
                if (keyPassword == null) missing.add("KEY_PASSWORD")
                println("⚠️  Signing credentials not configured, release build will be unsigned")
                println("   Missing: ${missing.joinToString(", ")}")
                println("   Set KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD in gradle.properties or environment variables")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 只有当签名配置的 storeFile 存在时才应用签名配置
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null && releaseSigningConfig.storeFile!!.exists()) {
                signingConfig = releaseSigningConfig
            } else {
                // 不设置 signingConfig，使用默认的未签名构建
            }
        }
        debug {
            // Debug 配置（使用默认debug签名，无需配置）
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

// 配置APK输出名称为 Lumen.apk
// 在构建任务完成后重命名 APK
// 支持 AGP 8.0+ 的新输出路径（app/release, app/debug）和传统路径（build/outputs/apk/...）
afterEvaluate {
    // 查找 APK 输出目录的辅助函数
    fun findApkOutputDir(variant: String): File? {
        // AGP 8.0+ 新路径：app/release 或 app/debug
        val newPath = file(variant)
        if (newPath.exists() && newPath.isDirectory) {
            return newPath
        }
        // 传统路径：build/outputs/apk/release 或 build/outputs/apk/debug
        val legacyPath = file("build/outputs/apk/$variant")
        if (legacyPath.exists() && legacyPath.isDirectory) {
            return legacyPath
        }
        return null
    }

    tasks.named("assembleRelease") {
        doLast {
            val releaseDir = findApkOutputDir("release")
            if (releaseDir != null && releaseDir.exists()) {
                releaseDir.listFiles()?.forEach { apkFile ->
                    if (apkFile.isFile && apkFile.name.endsWith(".apk") && apkFile.name != "Lumen.apk") {
                        val targetApk = File(releaseDir, "Lumen.apk")
                        // 如果目标文件已存在，先删除
                        if (targetApk.exists()) {
                            targetApk.delete()
                        }
                        if (apkFile.renameTo(targetApk)) {
                            println("✅ Renamed APK to: ${targetApk.name} (from ${apkFile.name})")
                        } else {
                            println("⚠️  Failed to rename ${apkFile.name} to Lumen.apk")
                        }
                    }
                }
            } else {
                println("⚠️  Release APK output directory not found")
            }
        }
    }

    tasks.named("assembleDebug") {
        doLast {
            val debugDir = findApkOutputDir("debug")
            if (debugDir != null && debugDir.exists()) {
                debugDir.listFiles()?.forEach { apkFile ->
                    if (apkFile.isFile && apkFile.name.endsWith(".apk") && apkFile.name != "Lumen-debug.apk") {
                        val targetApk = File(debugDir, "Lumen-debug.apk")
                        // 如果目标文件已存在，先删除
                        if (targetApk.exists()) {
                            targetApk.delete()
                        }
                        if (apkFile.renameTo(targetApk)) {
                            println("✅ Renamed APK to: ${targetApk.name} (from ${apkFile.name})")
                        } else {
                            println("⚠️  Failed to rename ${apkFile.name} to Lumen-debug.apk")
                        }
                    }
                }
            } else {
                println("⚠️  Debug APK output directory not found")
            }
        }
    }

}

dependencies {
    // Lumen - 聚合模块（包含所有子模块）
    implementation(project(":lumen"))
    
    // 或者单独使用子模块（可选）
    // implementation(project(":lumen-core"))
    // implementation(project(":lumen-view"))
    // implementation(project(":lumen-transform"))
    
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.activity.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}