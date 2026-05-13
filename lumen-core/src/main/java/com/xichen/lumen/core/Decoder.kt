package com.xichen.lumen.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Lumen 图片解码器
 * 
 * 负责将字节数组解码为 Bitmap 或 Drawable，支持静态图片和 GIF 动画
 */
object Decoder {
    /**
     * 解码字节数组为 Bitmap（静态图片）
     * @param data 图片字节数据
     * @param options 解码选项（可选）
     * @return 解码后的 Bitmap
     */
    suspend fun decode(
        data: ByteArray,
        options: BitmapFactory.Options? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = options?.let {
            BitmapFactory.decodeByteArray(data, 0, data.size, it)
        } ?: BitmapFactory.decodeByteArray(data, 0, data.size)

        bitmap ?: throw IllegalStateException("Failed to decode bitmap from byte array")
    }

    /**
     * 解码字节数组为 Drawable（支持 GIF 动画）
     * @param context Context
     * @param data 图片字节数据
     * @return 解码后的 Drawable（如果是 GIF，返回 AnimatedImageDrawable）
     */
    suspend fun decodeAnimated(
        context: Context,
        data: ByteArray
    ): Drawable = withContext(Dispatchers.Default) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+ 使用 ImageDecoder 支持 GIF 动画
            val byteBuffer = ByteBuffer.allocateDirect(data.size).apply {
                put(data)
                rewind()
            }
            val source = ImageDecoder.createSource(byteBuffer)
            ImageDecoder.decodeDrawable(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            // API < 28 降级为静态图片（第一帧）
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: throw IllegalStateException("Failed to decode image from byte array")
            android.graphics.drawable.BitmapDrawable(
                context.resources,
                bitmap
            )
        }
    }

    /**
     * 检测是否为 GIF 格式
     * @param data 图片字节数据
     * @return 是否为 GIF
     */
    fun isGif(data: ByteArray): Boolean {
        if (data.size < 6) return false
        // GIF 文件头：GIF87a 或 GIF89a
        val header = String(data, 0, 6)
        return header == "GIF87a" || header == "GIF89a"
    }

    /**
     * 创建默认解码选项
     * @param inSampleSize 采样率（用于内存优化）
     * @return 解码选项
     */
    fun createOptions(inSampleSize: Int = 1): BitmapFactory.Options {
        return BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
    }

    /**
     * 渐进式解码（从流中逐步解码 JPEG 图片）
     * 支持渐进式 JPEG，在数据流式下载时逐步显示从低质量到高质量的图片
     * 
     * @param inputStream 输入流（图片数据流）
     * @param onProgress 进度回调，返回当前解码的 Bitmap 和进度（0.0-1.0）
     * @param options 解码选项（可选）
     * @return 最终解码后的完整 Bitmap
     */
    suspend fun decodeProgressive(
        inputStream: InputStream,
        onProgress: suspend (Bitmap, Float) -> Unit,
        options: BitmapFactory.Options? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val buffer = mutableListOf<Byte>()
        val chunkSize = 8192 // 8KB 块大小
        val tempBuffer = ByteArray(chunkSize)
        var totalBytesRead = 0
        var contentLength: Long = -1
        
        // 尝试从流中读取数据并逐步解码
        var lastDecodedBitmap: Bitmap? = null
        var bytesRead: Int
        
        while (inputStream.read(tempBuffer).also { bytesRead = it } != -1) {
            buffer.addAll(tempBuffer.sliceArray(0 until bytesRead).toList())
            totalBytesRead += bytesRead
            
            // 尝试解码当前缓冲区
            val currentData = buffer.toByteArray()
            val bitmap = try {
                val decodeOptions = options?.let { 
                    BitmapFactory.Options().apply {
                        // 复制选项，但允许部分解码
                        inSampleSize = it.inSampleSize
                        inJustDecodeBounds = false
                    }
                } ?: BitmapFactory.Options()
                
                // 尝试解码（对于渐进式 JPEG，即使数据不完整也可能返回部分解码的图片）
                BitmapFactory.decodeByteArray(currentData, 0, currentData.size, decodeOptions)
            } catch (e: Exception) {
                null
            }
            
            // 如果成功解码，更新预览图
            if (bitmap != null) {
                lastDecodedBitmap = bitmap
                // 计算进度（如果知道总大小，使用总大小；否则使用已读取的字节数估算）
                val progress = if (contentLength > 0) {
                    (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f)
                } else {
                    // 估算进度：假设 JPEG 文件通常不会超过 10MB
                    (totalBytesRead.toFloat() / (10 * 1024 * 1024)).coerceIn(0f, 0.95f)
                }
                onProgress(bitmap, progress)
            }
        }
        
        // 确保流已关闭
        inputStream.close()
        
        // 返回最终解码的图片（如果最后解码成功，使用最后的；否则尝试完整解码）
        lastDecodedBitmap ?: run {
            val finalData = buffer.toByteArray()
            val finalOptions = options ?: BitmapFactory.Options()
            BitmapFactory.decodeByteArray(finalData, 0, finalData.size, finalOptions)
                ?: throw IllegalStateException("Failed to decode progressive bitmap")
        }
    }

    /**
     * 检测是否为渐进式 JPEG
     * @param data 图片字节数据（至少需要前几个字节）
     * @return 是否为渐进式 JPEG
     */
    fun isProgressiveJpeg(data: ByteArray): Boolean {
        if (data.size < 4) return false
        // JPEG 文件头：FF D8
        if (data[0] != 0xFF.toByte() || data[1] != 0xD8.toByte()) return false
        
        // 检查是否包含渐进式标记（SOF2 - Start of Frame, Progressive）
        // 在 JPEG 中查找 0xFF 0xC2（SOF2）
        for (i in 0 until data.size - 1) {
            if (data[i] == 0xFF.toByte() && data[i + 1] == 0xC2.toByte()) {
                return true
            }
        }
        return false
    }
}

