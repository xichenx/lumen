package com.xichen.lumen.compose

import android.graphics.Bitmap
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.xichen.lumen.core.ImageData
import com.xichen.lumen.core.ImageState
import com.xichen.lumen.core.Lumen
import com.xichen.lumen.view.RequestBuilder
import com.xichen.lumen.view.RequestBuilderScope

/**
 * Lumen Compose 图片加载组件
 * 
 * 使用示例：
 * ```
 * LumenImage(
 *     url = "https://example.com/image.jpg",
 *     modifier = Modifier.size(200.dp),
 *     contentDescription = "示例图片"
 * )
 * ```
 * 
 * @param url 图片 URL
 * @param modifier Modifier
 * @param contentDescription 内容描述（用于无障碍）
 * @param contentScale 图片缩放模式
 * @param block 请求配置块
 */
@Composable
fun LumenImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    block: RequestBuilderScope? = null
) {
    LumenImage(
        data = com.xichen.lumen.core.ImageData.Url(url),
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
        block = block
    )
}

/**
 * Lumen Compose 图片加载组件（通用版本）
 */
@Composable
fun LumenImage(
    data: ImageData,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    block: RequestBuilderScope? = null
) {
    val context = LocalContext.current
    val lumen = remember { Lumen.with(context) }
    
    var imageState by remember(data) { mutableStateOf<ImageState>(ImageState.Loading) }
    
    LaunchedEffect(data) {
        val builder = RequestBuilder(lumen, data)
        block?.invoke(builder)
        val request = builder.build(context)
        
        lumen.load(request).collect { state ->
            imageState = state
        }
    }
    
    when (val state = imageState) {
        is ImageState.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ImageState.Progressive -> {
            // 显示渐进式加载的预览图
            Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        is ImageState.Success -> {
            Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        is ImageState.SuccessAnimated -> {
            val drawable = state.drawable
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setImageDrawable(drawable)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                            drawable is AnimatedImageDrawable) {
                            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                                override fun onViewAttachedToWindow(v: View) {
                                    drawable.start()
                                }
                                override fun onViewDetachedFromWindow(v: View) {
                                    drawable.stop()
                                }
                            })
                        }
                    }
                },
                modifier = modifier,
                onRelease = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        drawable is AnimatedImageDrawable) {
                        drawable.stop()
                    }
                }
            )
        }
        is ImageState.Error -> {
            // 显示错误占位符（可以自定义）
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                // 这里可以显示错误图标或占位符
            }
        }
        is ImageState.Fallback -> {
            // 显示兜底 UI（可以自定义）
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                // 这里可以显示兜底 UI
            }
        }
    }
}

/**
 * 记住 Lumen 图片加载状态
 * 
 * 用于更细粒度的状态控制
 * 
 * @param data 图片数据
 * @param block 请求配置块
 * @return 图片加载状态
 */
@Composable
fun rememberLumenState(
    data: ImageData,
    block: RequestBuilderScope? = null
): ImageState {
    val context = LocalContext.current
    val lumen = remember { Lumen.with(context) }
    
    var imageState by remember(data) { mutableStateOf<ImageState>(ImageState.Loading) }
    
    LaunchedEffect(data) {
        val builder = RequestBuilder(lumen, data)
        block?.invoke(builder)
        val request = builder.build(context)
        
        lumen.load(request).collect { state ->
            imageState = state
        }
    }
    
    return imageState
}

