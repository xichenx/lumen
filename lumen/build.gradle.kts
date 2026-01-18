// BOM 模块：用于版本协调
plugins {
    `java-platform`
    alias(libs.plugins.maven.publish)
}

// BOM 版本管理：用于版本协调
val bomVersion: String = run {
    val bomVersionProperty = project.findProperty("LUMEN_BOM_VERSION") as String?
    
    when {
        !bomVersionProperty.isNullOrBlank() -> bomVersionProperty.trim()
        else -> "1.0.0"
    }
}

// 读取各模块版本
val coreVersion: String = project.findProperty("LUMEN_CORE_VERSION") as String? ?: bomVersion
val transformVersion: String = project.findProperty("LUMEN_TRANSFORM_VERSION") as String? ?: bomVersion
val viewVersion: String = project.findProperty("LUMEN_VIEW_VERSION") as String? ?: bomVersion
val composeVersion: String = project.findProperty("LUMEN_COMPOSE_VERSION") as String? ?: bomVersion

version = bomVersion
logger.info("📦 Publishing BOM version: $bomVersion")
logger.info("   - lumen-core: $coreVersion")
logger.info("   - lumen-transform: $transformVersion")
logger.info("   - lumen-view: $viewVersion")
logger.info("   - lumen-compose: $composeVersion")

// 配置 Maven 发布
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    
    coordinates("io.github.xichenx", "lumen-bom", bomVersion)
    pom {
        name.set("Lumen BOM")
        description.set("Bill of Materials (BOM) for Lumen - A Kotlin-first Android image loading library")
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

// BOM 定义：版本约束
javaPlatform {
    allowDependencies()
}

dependencies {
    // 定义版本约束
    constraints {
        api("io.github.xichenx:lumen-core:$coreVersion")
        api("io.github.xichenx:lumen-transform:$transformVersion")
        api("io.github.xichenx:lumen-view:$viewVersion")
        api("io.github.xichenx:lumen-compose:$composeVersion")
    }
}
