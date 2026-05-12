// BOM 模块：用于版本协调（约束中的版本字符串与工程内 project 依赖无强绑定，仅保留坐标形态）
plugins {
    `java-platform`
}

val bomLibraryVersion = "1.0.0"

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api("io.github.xichenx:lumen-core:$bomLibraryVersion")
        api("io.github.xichenx:lumen-transform:$bomLibraryVersion")
        api("io.github.xichenx:lumen-view:$bomLibraryVersion")
        api("io.github.xichenx:lumen-compose:$bomLibraryVersion")
    }
}
