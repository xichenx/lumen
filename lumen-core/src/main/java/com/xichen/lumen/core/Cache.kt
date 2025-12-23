package com.xichen.lumen.core

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Lumen 内存缓存管理器
 * 
 * 使用 LruCache 实现内存缓存，自动管理 Bitmap 内存占用
 */
class MemoryCache(
    maxSize: Int = calculateDefaultMaxSize()
) {
    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(maxSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            // 计算 Bitmap 占用的内存大小（字节）
            return value.byteCount
        }
    }

    /**
     * 获取缓存的 Bitmap
     */
    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    /**
     * 存储 Bitmap 到缓存
     */
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    /**
     * 移除缓存
     */
    fun remove(key: String) {
        cache.remove(key)
    }

    /**
     * 清空缓存
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * 获取当前缓存大小
     */
    fun size(): Int {
        return cache.size()
    }

    /**
     * 获取最大缓存大小
     */
    fun maxSize(): Int {
        return cache.maxSize()
    }

    companion object {
        /**
         * 计算默认最大缓存大小
         * 使用可用内存的 1/8
         */
        private fun calculateDefaultMaxSize(): Int {
            val maxMemory = Runtime.getRuntime().maxMemory()
            return (maxMemory / 8).toInt()
        }
    }
}

/**
 * Lumen 磁盘缓存管理器
 * 
 * 使用文件系统实现磁盘缓存，存储原始字节数据
 * 采用 LRU 策略管理缓存大小
 */
class DiskCache(
    private val context: Context,
    private val maxSizeBytes: Long = 50 * 1024 * 1024 // 默认 50MB
) {
    private val cacheDir: File by lazy {
        File(context.cacheDir, "lumen_disk_cache").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val accessTimes = mutableMapOf<String, Long>()
    // 文件到 key 的反向映射，用于快速查找
    private val fileToKey = mutableMapOf<String, String>()

    /**
     * 获取缓存的字节数据
     */
    suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = getCacheFile(key)
        if (!file.exists()) {
            return@withContext null
        }

        try {
            // 更新访问时间
            accessTimes[key] = System.currentTimeMillis()
            // 更新反向映射
            fileToKey[file.name] = key
            
            FileInputStream(file).use { input ->
                input.readBytes()
            }
        } catch (e: Exception) {
            android.util.Log.e("Lumen", "Failed to read disk cache for key: $key", e)
            // 如果读取失败，删除损坏的文件
            file.delete()
            accessTimes.remove(key)
            fileToKey.remove(file.name)
            null
        }
    }

    /**
     * 存储字节数据到磁盘缓存
     */
    suspend fun put(key: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            // 检查缓存大小，如果超过限制则清理
            ensureCacheSize(data.size.toLong())
            
            val file = getCacheFile(key)
            FileOutputStream(file).use { output ->
                output.write(data)
            }
            
            // 更新访问时间
            accessTimes[key] = System.currentTimeMillis()
            // 更新反向映射
            fileToKey[file.name] = key
        } catch (e: Exception) {
            android.util.Log.e("Lumen", "Failed to write disk cache for key: $key", e)
        }
    }

    /**
     * 移除缓存
     */
    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        val file = getCacheFile(key)
        file.delete()
        accessTimes.remove(key)
        fileToKey.remove(file.name)
    }

    /**
     * 清空缓存
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        accessTimes.clear()
        fileToKey.clear()
    }

    /**
     * 获取当前缓存大小（字节）
     */
    suspend fun size(): Long = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * 获取最大缓存大小（字节）
     */
    fun maxSize(): Long = maxSizeBytes

    /**
     * 根据 key 生成缓存文件
     */
    private fun getCacheFile(key: String): File {
        // 使用 MD5 哈希生成文件名，避免特殊字符问题
        val fileName = md5(key)
        return File(cacheDir, fileName)
    }

    /**
     * 确保缓存大小不超过限制
     * 使用 LRU 策略删除最久未访问的文件
     */
    private suspend fun ensureCacheSize(newDataSize: Long) = withContext(Dispatchers.IO) {
        val currentSize = size()
        if (currentSize + newDataSize <= maxSizeBytes) {
            return@withContext
        }

        // 需要清理的空间
        val needToFree = currentSize + newDataSize - maxSizeBytes

        // 获取所有文件及其访问时间
        val files = cacheDir.listFiles()?.mapNotNull { file ->
            val key = fileToKey[file.name]
            if (key != null) {
                Triple(file, accessTimes[key] ?: file.lastModified(), file.length())
            } else {
                // 如果找不到 key，使用文件修改时间
                Triple(file, file.lastModified(), file.length())
            }
        } ?: emptyList()

        // 按访问时间排序（最久未访问的在前）
        val sortedFiles = files.sortedBy { it.second }

        // 删除最久未访问的文件，直到释放足够的空间
        var freed = 0L
        for ((file, _, size) in sortedFiles) {
            if (freed >= needToFree) {
                break
            }
            if (file.delete()) {
                freed += size
                // 从映射中移除
                fileToKey[file.name]?.let { key ->
                    accessTimes.remove(key)
                }
                fileToKey.remove(file.name)
            }
        }
    }

    /**
     * MD5 哈希函数
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        /**
         * 计算默认最大缓存大小
         * 使用 50MB
         */
        private const val DEFAULT_MAX_SIZE_BYTES = 50 * 1024 * 1024L
    }
}

