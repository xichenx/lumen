/**
 * 发布任务优化配置
 * 创建聚合任务以优化发布流程
 * 
 * 优化策略：
 * 1. 使用 Gradle 的并行执行（--parallel）
 * 2. 利用任务依赖系统自动处理依赖顺序
 * 3. 构建阶段完全并行，发布阶段在满足依赖的前提下并行
 * 4. 添加发布前验证和清理任务
 */

import org.gradle.api.publish.PublishingExtension

// 发布前验证任务：检查所有模块是否已正确配置
tasks.register("validatePublishConfiguration") {
    group = "publishing"
    description = "Validate publish configuration for all modules"
    
    doLast {
        val modules = listOf(":lumen-core", ":lumen-transform", ":lumen-view", ":lumen")
        val versionName = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"
        
        println("🔍 Validating publish configuration...")
        println("Version: $versionName")
        
        modules.forEach { modulePath ->
            val module = project.findProject(modulePath)
            if (module == null) {
                throw GradleException("Module $modulePath not found")
            }
            
            val publishing = module.extensions.findByType<PublishingExtension>()
            if (publishing == null) {
                throw GradleException("PublishingExtension not found for $modulePath")
            }
            
            val publication = publishing.publications.findByName("release")
            if (publication == null) {
                throw GradleException("Release publication not found for $modulePath")
            }
            
            println("  ✅ $modulePath: configured")
        }
        
        println("✅ All modules are properly configured for publishing")
    }
}

// 清理发布产物任务
tasks.register("cleanPublishArtifacts") {
    group = "publishing"
    description = "Clean publish artifacts from all modules"
    
    doLast {
        val modules = listOf(":lumen-core", ":lumen-transform", ":lumen-view", ":lumen")
        
        println("🧹 Cleaning publish artifacts...")
        
        modules.forEach { modulePath ->
            val module = project.findProject(modulePath)
            if (module != null) {
                val buildDir = module.buildDir
                val publicationsDir = buildDir.resolve("publications")
                
                if (publicationsDir.exists()) {
                    publicationsDir.deleteRecursively()
                    println("  ✅ Cleaned $modulePath publications")
                }
            }
        }
        
        println("✅ All publish artifacts cleaned")
    }
}

// 创建发布所有模块的聚合任务
// 注意：必须在 afterEvaluate 中配置，因为发布任务是在子项目的 afterEvaluate 中创建的
afterEvaluate {
    tasks.register("publishAllToMavenCentral") {
        group = "publishing"
        description = "Publish all modules to Maven Central with optimized parallel execution"
        
        // 发布前验证
        dependsOn("validatePublishConfiguration")
        
        // 查找所有发布任务（使用延迟查找）
        val publishCore = tasks.findByPath(":lumen-core:publishReleasePublicationToMavenCentralRepository")
        val publishTransform = tasks.findByPath(":lumen-transform:publishReleasePublicationToMavenCentralRepository")
        val publishView = tasks.findByPath(":lumen-view:publishReleasePublicationToMavenCentralRepository")
        val publishLumen = tasks.findByPath(":lumen:publishReleasePublicationToMavenCentralRepository")
        
        // 只依赖存在的任务
        val publishTasks = listOfNotNull(publishCore, publishTransform, publishView, publishLumen)
        
        if (publishTasks.isEmpty()) {
            logger.warn("⚠️  No publish tasks found. Make sure publish configuration is correct.")
            doLast {
                logger.warn("⚠️  No modules to publish. Check publish configuration.")
            }
        } else {
            // 依赖所有找到的发布任务
            dependsOn(publishTasks)
            
            // 明确指定执行顺序，确保依赖关系
            if (publishCore != null && publishTransform != null) {
                // lumen-transform 必须在 lumen-core 之后
                publishTransform.mustRunAfter(publishCore)
            }
            
            if (publishCore != null && publishTransform != null && publishView != null) {
                // lumen-view 必须在 lumen-core 和 lumen-transform 之后
                publishView.mustRunAfter(publishCore, publishTransform)
            }
            
            if (publishCore != null && publishTransform != null && publishView != null && publishLumen != null) {
                // lumen 聚合模块必须在所有子模块之后
                publishLumen.mustRunAfter(publishCore, publishTransform, publishView)
            }
            
            doLast {
                val versionName = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"
                println("✅ All modules published to Maven Central (version: $versionName)")
                println("📋 Next steps:")
                println("   1. Check Sonatype Staging Repository")
                println("   2. Close and release the staging repository")
                println("   3. Wait for sync to Maven Central")
            }
        }
    }
}

// 创建本地发布的聚合任务（用于测试）
tasks.register("publishAllToMavenLocal") {
    group = "publishing"
    description = "Publish all modules to local Maven repository"
    
    // 发布前验证
    dependsOn("validatePublishConfiguration")
    
    dependsOn(
        ":lumen-core:publishToMavenLocal",
        ":lumen-transform:publishToMavenLocal",
        ":lumen-view:publishToMavenLocal",
        ":lumen:publishToMavenLocal"
    )
    
    doLast {
        val versionName = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"
        println("✅ All modules published to local Maven repository (version: $versionName)")
        println("📦 Local repository: ${System.getProperty("user.home")}/.m2/repository")
    }
}

// 验证发布产物任务
tasks.register("verifyPublishArtifacts") {
    group = "publishing"
    description = "Verify publish artifacts for all modules"
    
    doLast {
        val modules = listOf(":lumen-core", ":lumen-transform", ":lumen-view", ":lumen")
        val versionName = project.findProperty("VERSION_NAME") as String? ?: "1.0.0"
        val groupId = if (System.getenv("JITPACK") == "true") {
            "com.github.XichenX"
        } else {
            "io.github.xichenx"
        }
        
        println("🔍 Verifying publish artifacts...")
        
        modules.forEach { modulePath ->
            val module = project.findProject(modulePath)
            if (module != null) {
                val artifactId = module.name
                val buildDir = module.buildDir
                val publicationsDir = buildDir.resolve("publications/release")
                
                val requiredFiles = listOf(
                    "pom-default.xml",
                    "${artifactId}-${versionName}.aar",
                    "${artifactId}-${versionName}-sources.jar",
                    "${artifactId}-${versionName}-javadoc.jar"
                )
                
                var allFound = true
                requiredFiles.forEach { fileName ->
                    val file = publicationsDir.resolve(fileName)
                    if (!file.exists()) {
                        println("  ⚠️  $modulePath: Missing $fileName")
                        allFound = false
                    }
                }
                
                if (allFound) {
                    println("  ✅ $modulePath: All artifacts present")
                }
            }
        }
        
        println("✅ Artifact verification completed")
    }
}

