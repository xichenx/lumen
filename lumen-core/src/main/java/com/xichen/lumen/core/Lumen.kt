package com.xichen.lumen.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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
    fun load(request: ImageRequest): Flow<ImageState> = channelFlow {
        // 1. 检查内存缓存（仅对静态图片）
        val cachedBitmap = memoryCache.get(request.cacheKey)
        if (cachedBitmap != null) {
            send(ImageState.Success(cachedBitmap))
            return@channelFlow
        }

        // 2. 发送加载中状态
        send(ImageState.Loading)

        try {
            when (val data = request.data) {
                // 视频数据源：直接提取帧
                is ImageData.Video -> {
                    var bitmap = withContext(Dispatchers.IO) {
                        VideoFrameExtractor.extractFrame(data.file, data.timeUs)
                    }

                    // 应用转换器
                    request.transformers.forEach { transformer ->
                        bitmap = withContext(Dispatchers.Default) {
                            transformer.transform(bitmap)
                        }
                    }

                    // 存入内存缓存
                    memoryCache.put(request.cacheKey, bitmap)
                    send(ImageState.Success(bitmap))
                }

                is ImageData.VideoUri -> {
                    var bitmap = withContext(Dispatchers.IO) {
                        VideoFrameExtractor.extractFrame(context, data.uri, data.timeUs)
                    }

                    // 应用转换器
                    request.transformers.forEach { transformer ->
                        bitmap = withContext(Dispatchers.Default) {
                            transformer.transform(bitmap)
                        }
                    }

                    // 存入内存缓存
                    memoryCache.put(request.cacheKey, bitmap)
                    send(ImageState.Success(bitmap))
                }

                // 其他数据源：图片文件（可能包含 GIF）
                else -> {
                    // 3. 检查磁盘缓存（使用数据源 key，不包含解密器和转换器）
                    val dataSourceCacheKey = data.key
                    val rawData: ByteArray
                    val useProgressiveLoading = request.progressiveLoading && data is ImageData.Url

                    if (useProgressiveLoading && diskCache.get(dataSourceCacheKey) == null) {
                        // 渐进式加载：流式读取并逐步解码
                        val fetcher = FetcherFactory.create(context, data) as? NetworkFetcher
                            ?: throw IllegalStateException("Progressive loading only supports network URLs")

                        // 使用流式获取和渐进式解码
                        rawData = fetcher.fetchStream { partialData, progress ->
                            // 尝试渐进式解码部分数据
                            try {
                                // 检测是否为渐进式 JPEG 或数据量足够大
                                if (Decoder.isProgressiveJpeg(partialData) || partialData.size > 1024) {
                                    val previewBitmap = withContext(Dispatchers.Default) {
                                        try {
                                            val options = BitmapFactory.Options().apply {
                                                inJustDecodeBounds = false
                                                inSampleSize = if (partialData.size < 50000) 4 else 2
                                            }
                                            BitmapFactory.decodeByteArray(partialData, 0, partialData.size, options)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    previewBitmap?.let { bitmap ->
                                        send(ImageState.Progressive(bitmap, progress))
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.d("Lumen", "Progressive decode failed: ${e.message}")
                            }
                        }

                        // 存入磁盘缓存
                        diskCache.put(dataSourceCacheKey, rawData)
                    } else {
                        // 普通加载：从缓存或数据源获取
                        rawData = diskCache.get(dataSourceCacheKey) ?: run {
                            // 4. 如果磁盘缓存未命中，从数据源获取原始数据
                            val fetcher = FetcherFactory.create(context, data)
                                ?: throw IllegalStateException("Unsupported data source: $data")
                            val fetchedData = withContext(Dispatchers.IO) {
                                fetcher.fetch()
                            }

                            // 5. 存入磁盘缓存（存储原始数据，可能是加密的）
                            diskCache.put(dataSourceCacheKey, fetchedData)
                            fetchedData
                        }
                    }

                    // 6. 解密（如果需要）
                    var decryptedData = rawData
                    request.decryptor?.let { decryptor ->
                        decryptedData = withContext(Dispatchers.Default) {
                            decryptor.decrypt(rawData)
                        }
                    }

                    // 7. 检测是否为 GIF
                    if (Decoder.isGif(decryptedData)) {
                        // GIF 动画：使用 decodeAnimated
                        val drawable = withContext(Dispatchers.Default) {
                            Decoder.decodeAnimated(context, decryptedData)
                        }
                        send(ImageState.SuccessAnimated(drawable))
                    } else {
                        // 静态图片：使用 decode
                        var bitmap = withContext(Dispatchers.Default) {
                            Decoder.decode(decryptedData)
                        }

                        // 8. 转换（如果需要）
                        request.transformers.forEach { transformer ->
                            bitmap = withContext(Dispatchers.Default) {
                                transformer.transform(bitmap)
                            }
                        }

                        // 9. 存入内存缓存
                        memoryCache.put(request.cacheKey, bitmap)
                        send(ImageState.Success(bitmap))
                    }
                }
            }
        } catch (e: Exception) {
            send(ImageState.Error(e))
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

