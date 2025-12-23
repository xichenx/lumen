package com.xichen.lumen.core

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Lumen 图片加载器核心类
 * 
 * Lumen 是一个 Kotlin-first 的图片加载库，面向业务友好、AI 场景、列表场景。
 * 强调状态可控、兜底清晰、链路透明。
 * 
 * 核心职责：
 * - 调度加载流程（Fetcher → Decryptor → Decoder → Transformer → Cache）
 * - 管理协程生命周期
 * - 缓存协调（内存缓存 + 磁盘缓存）
 * 
 * 使用示例：
 * ```
 * val lumen = Lumen.with(context)
 * lumen.load(request).collect { state ->
 *     when (state) {
 *         is ImageState.Success -> imageView.setImageBitmap(state.bitmap)
 *         is ImageState.Error -> // 处理错误
 *         else -> // 处理其他状态
 *     }
 * }
 * ```
 */
class Lumen private constructor(
    private val context: Context,
    private val memoryCache: MemoryCache,
    private val diskCache: DiskCache
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 加载图片
     * @param request 图片加载请求
     * @return 图片状态 Flow
     */
    fun load(request: ImageRequest): Flow<ImageState> = flow {
        // 1. 检查内存缓存
        val cachedBitmap = memoryCache.get(request.cacheKey)
        if (cachedBitmap != null) {
            emit(ImageState.Success(cachedBitmap))
            return@flow
        }

        // 2. 发送加载中状态
        emit(ImageState.Loading)

        try {
            // 3. 检查磁盘缓存（使用数据源 key，不包含解密器和转换器）
            // 磁盘缓存存储原始数据（可能是加密的），保证"不落明文磁盘"原则
            val dataSourceCacheKey = request.data.key
            var rawData: ByteArray = diskCache.get(dataSourceCacheKey) ?: run {
                // 4. 如果磁盘缓存未命中，从数据源获取原始数据
                val fetcher = FetcherFactory.create(context, request.data)
                val fetchedData = withContext(Dispatchers.IO) {
                    fetcher.fetch()
                }

                // 5. 存入磁盘缓存（存储原始数据，可能是加密的）
                // 不存储解密后的数据，保证安全性和扩展性
                diskCache.put(dataSourceCacheKey, fetchedData)
                
                fetchedData
            }

            // 6. 解密（如果需要）- 在从磁盘缓存读取后解密
            // 这样既保证了"不落明文磁盘"，又支持用户自定义任何解密算法
            var data = rawData
            request.decryptor?.let { decryptor ->
                data = withContext(Dispatchers.Default) {
                    decryptor.decrypt(rawData)
                }
            }

            // 7. 解码
            var bitmap = withContext(Dispatchers.Default) {
                Decoder.decode(data)
            }

            // 8. 转换（如果需要）
            request.transformers.forEach { transformer ->
                bitmap = withContext(Dispatchers.Default) {
                    transformer.transform(bitmap)
                }
            }

            // 9. 存入内存缓存（存储转换后的 Bitmap）
            memoryCache.put(request.cacheKey, bitmap)

            // 10. 发送成功状态
            emit(ImageState.Success(bitmap))
        } catch (e: Exception) {
            // 11. 发送错误状态
            emit(ImageState.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 取消所有加载任务
     */
    fun cancelAll() {
        scope.cancel()
    }

    /**
     * 清空内存缓存
     */
    fun clearMemoryCache() {
        memoryCache.clear()
    }

    /**
     * 清空磁盘缓存
     */
    suspend fun clearDiskCache() {
        diskCache.clear()
    }

    /**
     * 清空所有缓存（内存 + 磁盘）
     */
    suspend fun clearCache() {
        memoryCache.clear()
        diskCache.clear()
    }

    companion object {
        @Volatile
        private var INSTANCE: Lumen? = null

        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): Lumen {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Lumen(
                    context.applicationContext,
                    MemoryCache(),
                    DiskCache(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }

        /**
         * 创建新实例（用于测试或特殊场景）
         */
        fun create(
            context: Context,
            memoryCache: MemoryCache = MemoryCache(),
            diskCache: DiskCache = DiskCache(context.applicationContext)
        ): Lumen {
            return Lumen(context.applicationContext, memoryCache, diskCache)
        }

        /**
         * 便捷方法：获取 Lumen 实例
         * 用于 DSL API：Lumen.with(context)
         */
        fun with(context: Context): Lumen {
            return getInstance(context)
        }
    }
}

