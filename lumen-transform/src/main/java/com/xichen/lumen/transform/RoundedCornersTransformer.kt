package com.xichen.lumen.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.xichen.lumen.core.BitmapTransformer

/**
 * Lumen 圆角转换器
 * 
 * 对 Bitmap 进行圆角处理，支持统一圆角或分别设置四个角的圆角
 * 
 * 使用示例：
 * ```
 * RoundedCornersTransformer(12f) // 统一圆角 12px
 * RoundedCornersTransformer(topLeft = 12f, topRight = 12f) // 只设置上边两个角
 * ```
 */
class RoundedCornersTransformer(
    val radius: Float = 0f,
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f
) : BitmapTransformer {

    init {
        require(radius >= 0 || (topLeft >= 0 && topRight >= 0 && bottomRight >= 0 && bottomLeft >= 0)) {
            "Radius values must be non-negative"
        }
    }

    override fun transform(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        
        // 使用统一圆角或分别设置的圆角
        val tl = if (radius > 0) radius else topLeft
        val tr = if (radius > 0) radius else topRight
        val br = if (radius > 0) radius else bottomRight
        val bl = if (radius > 0) radius else bottomLeft
        
        // 如果所有圆角都是 0，直接返回原图
        if (tl == 0f && tr == 0f && br == 0f && bl == 0f) {
            return source
        }
        
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // 如果所有圆角相同，使用简单的圆角矩形
        if (tl == tr && tr == br && br == bl) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(rect, tl, tl, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(source, 0f, 0f, paint)
        } else {
            // 分别设置四个角的圆角（使用 Path）
            val path = android.graphics.Path().apply {
                val radii = floatArrayOf(
                    tl, tl,  // top-left
                    tr, tr,  // top-right
                    br, br,  // bottom-right
                    bl, bl   // bottom-left
                )
                addRoundRect(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    radii,
                    android.graphics.Path.Direction.CW
                )
            }
            canvas.clipPath(path)
            canvas.drawBitmap(source, 0f, 0f, null)
        }
        
        return output
    }

    override val key: String
        get() = buildString {
            append("roundedCorners")
            if (radius > 0) {
                append("_r").append(radius)
            } else {
                append("_tl").append(topLeft)
                append("_tr").append(topRight)
                append("_br").append(bottomRight)
                append("_bl").append(bottomLeft)
            }
        }
}

