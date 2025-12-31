/**
 * 简化的发布任务配置
 * 创建聚合任务以简化发布流程
 */

// 发布所有模块到 Maven Central 的聚合任务
tasks.register("publishAllToMavenCentral") {
    group = "publishing"
    description = "Publish all modules to Maven Central"
    
    // 依赖所有模块的发布任务
    dependsOn(
        ":lumen-core:publishToMavenCentral",
        ":lumen-transform:publishToMavenCentral",
        ":lumen-view:publishToMavenCentral",
        ":lumen-compose:publishToMavenCentral",
        ":lumen:publishToMavenCentral"
    )
    
    doLast {
        println("✅ All modules published to Maven Central")
        println("📋 Next steps:")
        println("   1. Check Central Portal: https://central.sonatype.com/")
        println("   2. Wait for sync to Maven Central (10-30 minutes)")
    }
}

// 发布所有模块到本地 Maven 仓库的聚合任务（用于测试）
tasks.register("publishAllToMavenLocal") {
    group = "publishing"
    description = "Publish all modules to local Maven repository"
    
    dependsOn(
        ":lumen-core:publishToMavenLocal",
        ":lumen-transform:publishToMavenLocal",
        ":lumen-view:publishToMavenLocal",
        ":lumen-compose:publishToMavenLocal",
        ":lumen:publishToMavenLocal"
    )
    
    doLast {
        println("✅ All modules published to local Maven repository")
        println("📦 Local repository: ${System.getProperty("user.home")}/.m2/repository")
    }
}
