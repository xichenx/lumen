package com.xichen.lumen.view

import android.graphics.Outline
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.xichen.lumen.core.ImageRequest
import com.xichen.lumen.core.ImageState
import com.xichen.lumen.core.Lumen
import com.xichen.lumen.transform.RoundedCornersTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * ImageView Target
 * 
 * 负责将图片加载结果应用到 ImageView
 * 支持 RecyclerView 场景：自动取消旧任务、占位图立即显示
 */
class ImageViewTarget(
    private val imageView: ImageView,
    private val lumen: Lumen
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentJob: Job? = null

    init {
        // 绑定 ImageViewTarget 到 View tag，用于 RecyclerView 场景
        imageView.setTag(R.id.lumen_load_job, this)
    }

    /**
     * 加载图片
     */
    fun load(request: ImageRequest) {
        // 取消之前的加载任务
        cancel()

        // 检测是否有圆角转换器
        val roundedCornersTransformer = request.transformers.firstOrNull { 
            it is RoundedCornersTransformer 
        } as? RoundedCornersTransformer
        
        // 如果存在圆角转换器，根据 scaleType 决定是否在 View 层面应用圆角
        if (roundedCornersTransformer != null) {
            applyRoundedCornersToView(roundedCornersTransformer)
        } else {
            // 清除之前的圆角设置
            clearRoundedCornersFromView()
        }

        // 立即显示占位图
        request.placeholder?.let {
            imageView.setImageDrawable(it)
        }

        // 开始加载
        currentJob = scope.launch {
            try {
                lumen.load(request)
                    .collect { state ->
                        when (state) {
                            is ImageState.Loading -> {
                                // 加载中，占位图已在上面显示
                                android.util.Log.d("Lumen", "Loading image: ${request.data.key}")
                            }
                            is ImageState.Success -> {
                                android.util.Log.d("Lumen", "Successfully loaded image: ${request.data.key}, size: ${state.bitmap.width}x${state.bitmap.height}")
                                imageView.setImageBitmap(state.bitmap)
                                // 图片加载成功后，如果需要在 View 层面应用圆角，需要等待 View 布局完成
                                if (roundedCornersTransformer != null) {
                                    imageView.post {
                                        applyRoundedCornersToView(roundedCornersTransformer)
                                    }
                                }
                            }
                            is ImageState.Error -> {
                                // 打印错误日志
                                android.util.Log.e("Lumen", "Failed to load image: ${request.data.key}", state.throwable)
                                // 显示错误图片
                                request.error?.let {
                                    imageView.setImageDrawable(it)
                                }
                            }
                            is ImageState.Fallback -> {
                                // 显示兜底 UI（由外部处理）
                                android.util.Log.w("Lumen", "Fallback for image: ${request.data.key}")
                            }
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("Lumen", "Exception in image loading", e)
                request.error?.let {
                    imageView.setImageDrawable(it)
                }
            }
        }
    }
    
    /**
     * 在 View 层面应用圆角裁剪
     * 只对会裁剪图片的 scaleType 应用，对于 fitStart/fitEnd/fitCenter 等保持 Bitmap 层面的圆角
     */
    private fun applyRoundedCornersToView(transformer: RoundedCornersTransformer) {
        val scaleType = imageView.scaleType
        
        // 对于会裁剪图片的 scaleType，需要在 View 层面应用圆角
        // centerCrop, fitXY, matrix 等会裁剪，需要 View 层面圆角
        // fitStart, fitEnd, fitCenter, center, centerInside 等不会裁剪，保持 Bitmap 层面圆角即可
        val needsViewLevelRoundedCorners = when (scaleType) {
            ImageView.ScaleType.CENTER_CROP,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.MATRIX -> true
            else -> false
        }
        
        if (needsViewLevelRoundedCorners) {
            // 获取圆角半径（统一圆角或使用最大圆角值）
            val radius = if (transformer.radius > 0) {
                transformer.radius
            } else {
                maxOf(
                    transformer.topLeft,
                    transformer.topRight,
                    transformer.bottomRight,
                    transformer.bottomLeft
                )
            }
            
            // 使用 ViewOutlineProvider 在 View 层面应用圆角
            imageView.clipToOutline = true
            imageView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    // 对于 centerCrop/fitXY，图片会填满整个 View，所以直接裁剪整个 View
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
        } else {
            // 对于不会裁剪的 scaleType，清除 View 层面的圆角设置
            // Bitmap 层面的圆角已经足够
            clearRoundedCornersFromView()
        }
    }
    
    /**
     * 清除 View 层面的圆角设置
     */
    private fun clearRoundedCornersFromView() {
        imageView.clipToOutline = false
        imageView.outlineProvider = ViewOutlineProvider.BACKGROUND
    }

    /**
     * 取消加载
     */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        cancel()
        scope.cancel()
        imageView.setTag(R.id.lumen_load_job, null)
    }

    companion object {
        /**
         * 从 View 获取绑定的 ImageViewTarget
         */
        @JvmStatic
        fun from(view: View): ImageViewTarget? {
            return view.getTag(R.id.lumen_load_job) as? ImageViewTarget
        }
    }
}

