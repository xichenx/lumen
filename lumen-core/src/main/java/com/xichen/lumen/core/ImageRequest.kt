package com.xichen.lumen.core

import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File

/**
 * Lumen 图片加载请求
 * 
 * 描述一次图片加载请求的所有参数，采用 immutable 设计
 * 
 * @param data 图片数据源（URL、File、Uri、Resource ID）
 * @param placeholder 占位图 Drawable
 * @param error 错误时显示的 Drawable
 * @param decryptor 解密器（可选）
 * @param transformers 转换器列表（可选）
 * @param priority 加载优先级
 */
data class ImageRequest(
    val data: ImageData,
    val placeholder: Drawable? = null,
    val error: Drawable? = null,
    val decryptor: ImageDecryptor? = null,
    val transformers: List<BitmapTransformer> = emptyList(),
    val priority: Priority = Priority.NORMAL
) {
    /**
     * 生成缓存 Key（包含转换器）
     * 用于内存缓存，包含 data、decryptor 和 transformers 的信息
     */
    val cacheKey: String
        get() = buildString {
            append(data.key)
            decryptor?.let { append("_decrypt:").append(it.key) }
            transformers.forEachIndexed { index, transformer ->
                append("_transform$index:").append(transformer.key)
            }
        }

    /**
     * 生成原始数据缓存 Key（仅包含数据源信息）
     * 用于磁盘缓存，只包含 data 的信息，不包含 decryptor
     * 
     * 设计原则：
     * - 磁盘缓存存储原始数据（可能是加密的），保证"不落明文磁盘"
     * - 解密在从磁盘缓存读取后进行，支持用户自定义任何解密算法
     * - 同一个数据源使用不同的解密器时，共享同一个磁盘缓存条目
     */
    val rawDataCacheKey: String
        get() = data.key

    /**
     * 加载优先级
     */
    enum class Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }
}

/**
 * Lumen 图片数据源
 * 
 * 支持多种数据源类型：网络 URL、本地文件、Android Uri、资源 ID
 */
sealed class ImageData {
    abstract val key: String

    /**
     * 网络 URL
     */
    data class Url(val url: String) : ImageData() {
        override val key: String = "url:$url"
    }

    /**
     * 文件路径
     */
    data class File(val file: java.io.File) : ImageData() {
        override val key: String = "file:${file.absolutePath}"
    }

    /**
     * Android Uri
     */
    data class Uri(val uri: android.net.Uri) : ImageData() {
        override val key: String = "uri:$uri"
    }

    /**
     * 资源 ID
     */
    data class Resource(val resId: Int) : ImageData() {
        override val key: String = "res:$resId"
    }
}

