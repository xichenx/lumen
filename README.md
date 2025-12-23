<div align="center">

# Lumen


</div>


<div align="center">

![Lumen Logo](https://img.shields.io/badge/Lumen-Image%20Loader-blue?style=flat)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=flat&logo=kotlin)
![Android](https://img.shields.io/badge/Android-API%2021+-green?style=flat&logo=android)
![License](https://img.shields.io/badge/License-Apache%202.0-yellow?style=flat)

**A Kotlin-first Android image loading library for business-friendly, AI scenarios, and list scenarios**

[中文文档](README-zh.md) • [Quick Start](#-quick-start) • [Features](#-features) • [Comparison](#-comparison-with-glide--coil) • [Documentation](#-documentation)

</div>

---

## 🤔 Why

While there are excellent image loading libraries like Glide and Coil in the Android ecosystem, we encountered the following pain points in real-world development:

1. **Opaque State Management**: Difficult to precisely control loading states (Loading / Success / Error / Fallback), insufficient flexibility for custom UI
2. **Black Box Pipeline**: Loading pipeline is not transparent enough, making debugging and customization difficult (e.g., encrypted images, custom decoding)
3. **Insufficient RecyclerView Optimization**: Image flickering and memory leaks are common in list scenarios
4. **Underutilized Kotlin Features**: Existing libraries are mostly Java-designed, not fully leveraging Kotlin's DSL, coroutines, etc.
5. **Insufficient AI Scenario Support**: Not friendly enough for AI-related scenarios requiring decryption, custom decoding, etc.

**Lumen's Positioning**: Not another Glide clone, but a modern Android image loading library designed for "real business + AI scenarios".

---

## ✨ Features

### Core Features

- ✅ **State Control**: Clear loading states (Loading / Success / Error / Fallback) with custom UI support
  - Sealed class-based state model for type-safe state handling
  - Flow-based reactive state updates
  - Support for custom state UI rendering
  
- ✅ **Transparent Pipeline**: Every step is pluggable (Fetcher → Decryptor → Decoder → Transformer → Cache)
  - Custom Fetcher for different data sources (Network, File, Uri, Resource)
  - Optional Decryptor for encrypted images (AI scenarios)
  - Pluggable Decoder with BitmapFactory integration
  - Chainable Transformers (rounded corners, rotation, crop, blur)
  - Memory cache with automatic LruCache management
  
- ✅ **Kotlin-first**: Fully leverages modern Kotlin features like DSL, coroutines, Flow
  - DSL-style API for request configuration
  - Coroutine-based asynchronous loading
  - Flow for reactive state updates
  - Type-safe sealed classes and data classes
  
- ✅ **RecyclerView Optimization**: Automatically cancels loading tasks for recycled views, preventing memory leaks and image flickering
  - Automatic job cancellation on view recycling
  - View tag-based target management
  - Immediate placeholder display
  
- ✅ **Image Transformations**: Rounded corners, rotation, cropping, blur, etc. (applied directly to Bitmap, not View)
  - Transformations applied to Bitmap pixels directly
  - Support for chained transformations
  - Smart View-level clipping for certain scaleTypes (centerCrop, fitXY)
  
- ✅ **Multiple Data Sources**: Supports URL, File, Uri, Resource ID, Video
  - Network URL loading with HttpURLConnection
  - Local file system access
  - Android ContentProvider Uri support
  - Android resource ID support
  - Video file frame extraction (File and Uri)
  
- ✅ **Compose Support**: Native Jetpack Compose components and state management
  - `LumenImage` composable for easy integration
  - `rememberLumenState` for fine-grained state control
  - Automatic state management with LaunchedEffect
  
- ✅ **Memory Cache**: Automatic memory cache based on LruCache
  - Default cache size: 1/8 of available memory
  - Automatic cache key generation (includes data, decryptor, transformers)
  - Thread-safe cache operations
  
- ✅ **Disk Cache**: Automatic disk cache for raw image data
  - Default cache size: 50MB
  - LRU-based cache eviction
  - Stores encrypted data (supports "no plaintext on disk" principle)
  - Automatic cache key generation based on data source
  
- ✅ **GIF Animation Support**: Automatic GIF detection and playback
  - Full animation support on API 28+ (using ImageDecoder)
  - Automatic fallback to static image (first frame) on API < 28
  - Auto-start animation playback
  - Seamless integration with existing API
  
- ✅ **Video Frame Extraction**: Extract frames from video files
  - Support for File and Uri sources
  - Extract frame at any time point (in microseconds)
  - All transformers supported (rounded corners, blur, etc.)
  - Automatic memory caching for extracted frames

### Technical Highlights

- 🔄 **Coroutine-driven**: Based on Kotlin Coroutines and Flow
  - All I/O operations on `Dispatchers.IO`
  - Image processing on `Dispatchers.Default`
  - UI updates on `Dispatchers.Main`
  - Flow-based reactive state emission
  
- 🎭 **State Management**: Sealed Class for loading states
  - `ImageState.Loading`: Loading in progress
  - `ImageState.Success(bitmap)`: Loaded successfully (static images)
  - `ImageState.SuccessAnimated(drawable)`: Loaded successfully (GIF animations)
  - `ImageState.Error(throwable)`: Load failed
  - `ImageState.Fallback`: Fallback state for custom handling
  
- 🧩 **Modular Design**: Core logic separated from UI (`lumen-core` has no Android UI dependencies)
  - `lumen-core`: Pure business logic, no Android UI dependencies
  - `lumen-view`: ImageView and ViewTarget support
  - `lumen-transform`: Image transformation implementations
  - `lumen`: Aggregated module for convenience
  
- 🛡️ **Type Safety**: Fully leverages Kotlin's type system
  - Sealed classes for data sources (`ImageData`)
  - Sealed classes for states (`ImageState`)
  - Type-safe DSL API
  - Immutable data classes for requests

---

## 🚀 Quick Start

### 1. Add Dependencies

**Simple way (recommended):** Just add one dependency to get all features:

```kotlin
dependencies {
    implementation("com.xichen.lumen:lumen:1.0.0")
}
```

**Modular way (optional):** If you only need specific modules:

```kotlin
dependencies {
    implementation("com.xichen.lumen:lumen-core:1.0.0")      // Core only
    implementation("com.xichen.lumen:lumen-view:1.0.0")      // View support
    implementation("com.xichen.lumen:lumen-transform:1.0.0") // Transform support
}
```

### 2. Add Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 3. Use It (10 lines of code)

```kotlin
Lumen.with(context)
    .load("https://example.com/image.jpg") {
        placeholder(R.drawable.placeholder)
        error(R.drawable.error)
        roundedCorners(12f)
    }
    .into(imageView)
```

**That's it!** 🎉

---

## 📊 Comparison with Mainstream Libraries

### Feature Comparison Table

| Feature | Lumen | Glide | Coil | Fresco | Picasso |
|---------|-------|-------|------|--------|---------|
| **Kotlin-first** | ✅ Native Kotlin, DSL, coroutines, Flow | ❌ Java-designed, limited Kotlin extensions | ✅ Kotlin-first, coroutines | ❌ Java-designed | ❌ Java-designed |
| **State Transparency** | ✅ Sealed Class, clear states (Loading/Success/Error/Fallback) | ⚠️ States not transparent enough | ⚠️ States not transparent enough | ⚠️ States not transparent enough | ⚠️ States not transparent enough |
| **Pluggable Pipeline** | ✅ Every step customizable (Fetcher→Decryptor→Decoder→Transformer→Cache) | ⚠️ Partially customizable | ⚠️ Partially customizable | ⚠️ Partially customizable | ⚠️ Limited customization |
| **RecyclerView Optimization** | ✅ Auto-cancel, prevents flickering | ✅ Supported | ✅ Supported | ✅ Supported | ⚠️ Manual cancellation needed |
| **Transform Applied to** | ✅ Bitmap (direct pixel manipulation) | ❌ View (applied to ImageView) | ✅ Bitmap | ✅ Bitmap | ❌ View |
| **Compose Support** | ✅ Native Compose components | ⚠️ Requires adaptation | ✅ Native support | ❌ No official support | ❌ No official support |
| **Encrypted Image Support** | ✅ Built-in Decryptor interface | ❌ Requires custom implementation | ❌ Requires custom implementation | ❌ Requires custom implementation | ❌ Requires custom implementation |
| **Memory Management** | ✅ LruCache, automatic memory management | ✅ Advanced memory management | ✅ Automatic memory management | ✅ Ashmem (Android <5.0), advanced | ⚠️ Basic memory management |
| **Disk Cache** | ✅ Automatic disk cache (50MB default) | ✅ Automatic disk cache | ✅ Automatic disk cache | ✅ Automatic disk cache | ✅ Automatic disk cache |
| **GIF Support** | ✅ Full support (API 28+), fallback on <28 | ✅ Full support | ✅ Full support | ✅ Full support | ❌ Not supported |
| **Video Frame** | ✅ Extract frames from video | ❌ Not supported | ❌ Not supported | ❌ Not supported | ❌ Not supported |
| **WebP Support** | ✅ Supported | ✅ Supported | ✅ Supported | ✅ Supported | ✅ Supported |
| **Progressive Loading** | ❌ Not supported | ✅ Supported | ✅ Supported | ✅ Supported | ❌ Not supported |
| **Learning Curve** | ⭐⭐ Simple and intuitive | ⭐⭐⭐ Complex features | ⭐⭐ Relatively simple | ⭐⭐⭐ Complex setup | ⭐ Simple |
| **Package Size** | 📦 Small (~50KB core, modular) | 📦📦 Medium (~475KB) | 📦 Small (~200KB) | 📦📦📦 Large (~3.4MB) | 📦 Small (~120KB) |
| **API Design** | ✅ Modern DSL, type-safe | ⚠️ Builder pattern | ✅ Modern Kotlin API | ⚠️ Complex API | ✅ Simple API |
| **Coroutine Support** | ✅ Native Flow-based | ⚠️ Limited support | ✅ Native support | ❌ No support | ❌ No support |
| **Maturity** | 🆕 New project | ✅ Very mature (2014) | ✅ Mature (2019) | ✅ Very mature (2015) | ✅ Very mature (2013) |
| **Community** | 🆕 Growing | ✅ Large community | ✅ Active community | ✅ Large community | ⚠️ Less active |

### Detailed Comparison

#### **Lumen vs Glide**

| Aspect | Lumen | Glide |
|--------|-------|-------|
| **Architecture** | Kotlin-first, Flow-based, modular design | Java-based, mature but complex |
| **State Management** | Sealed Class with explicit states | Implicit state handling |
| **Customization** | Every pipeline step is pluggable | Limited customization points |
| **Best For** | Kotlin projects, AI scenarios, state control | GIF support, mature ecosystem, Java projects |

#### **Lumen vs Coil**

| Aspect | Lumen | Coil |
|--------|-------|------|
| **State Management** | Sealed Class with Fallback state | Basic state handling |
| **Pipeline Transparency** | Fully transparent, every step customizable | Partially transparent |
| **Encryption Support** | Built-in Decryptor interface | Requires custom implementation |
| **Best For** | AI scenarios, encrypted images, state control | General Kotlin projects, Compose apps |

#### **Lumen vs Fresco**

| Aspect | Lumen | Fresco |
|--------|-------|--------|
| **Package Size** | Small (~50KB core) | Large (~3.4MB) |
| **Memory Management** | LruCache-based | Advanced Ashmem (Android <5.0) |
| **Kotlin Support** | Native Kotlin-first | Java-based |
| **Compose Support** | Native support | No official support |
| **Best For** | Modern Kotlin apps, Compose projects | Large-scale apps, complex memory scenarios |

#### **Lumen vs Picasso**

| Aspect | Lumen | Picasso |
|--------|-------|---------|
| **Modern Features** | Kotlin-first, coroutines, Flow | Java-based, simple API |
| **State Management** | Explicit sealed class states | Basic callback-based |
| **Transform** | Applied to Bitmap | Applied to View |
| **Best For** | Modern Kotlin projects, state control | Simple projects, minimal dependencies |

### Recommendation

- **Choose Lumen**: 
  - ✅ Need precise state control (Loading/Success/Error/Fallback)
  - ✅ Need transparent, pluggable pipeline
  - ✅ AI scenario support (encrypted images, custom decoding)
  - ✅ Kotlin-first experience with DSL and coroutines
  - ✅ Jetpack Compose projects
  - ✅ Want small package size with modular design
  - ✅ Need GIF animation support (API 28+)
  - ✅ Need video frame extraction
  - ✅ Need disk cache with "no plaintext on disk" support

- **Choose Glide**: 
  - ✅ Need GIF animation support on older Android versions (< API 28)
  - ✅ Need very mature ecosystem with many plugins
  - ✅ Java projects or mixed Java/Kotlin projects
  - ✅ Need advanced caching strategies

- **Choose Coil**: 
  - ✅ Need lightweight library with Compose support
  - ✅ Modern Kotlin API with coroutines
  - ✅ General-purpose image loading

- **Choose Fresco**: 
  - ✅ Large-scale apps with complex memory requirements
  - ✅ Need progressive image loading
  - ✅ Need advanced memory management (especially for Android <5.0)
  - ✅ Can accept large library size (~3.4MB)

- **Choose Picasso**: 
  - ✅ Simple projects with minimal requirements
  - ✅ Want smallest possible library size
  - ✅ Don't need GIF or advanced features

---

## 🎯 Use Cases

### ✅ Suitable Scenarios

1. **Business-friendly Scenarios**
   - Need precise control over loading states (Loading / Success / Error / Fallback)
   - Need custom UI display (e.g., skeleton screens, custom error UI)
   - Need clear error handling and fallback mechanisms

2. **AI Scenarios**
   - Encrypted image loading (built-in Decryptor interface)
   - Custom decoding logic
   - Image preprocessing and post-processing

3. **List Scenarios**
   - Image loading in RecyclerView
   - Need to prevent image flickering and memory leaks
   - Need automatic cancellation of loading tasks for recycled views

4. **Kotlin Projects**
   - Pure Kotlin projects
   - Using Jetpack Compose
   - Need modern Kotlin API (DSL, coroutines, Flow)

5. **Image Transformation Scenarios**
   - Need transformations like rounded corners, rotation, cropping, blur
   - Need transformations applied directly to Bitmap (not View)
   - Need chained transformations

### ❌ Unsuitable Scenarios

1. **Complex Animations**
   - Does not support image loading animations (e.g., fade in/out)
   - Does not support transition animations

2. **Progressive Loading**
   - Does not support progressive JPEG loading
   - Does not support streaming image loading

4. **Java Projects**
   - Can be used in Java but experience is not as good as Kotlin
   - Recommend using Glide or Coil

5. **Need Many Third-party Plugins**
   - Relatively new ecosystem, fewer third-party plugins
   - Recommend using Glide if you need a rich ecosystem

---

## 📝 Usage Examples

### Basic Usage

```kotlin
// Simplest usage
Lumen.with(context)
    .load("https://example.com/image.jpg")
    .into(imageView)

// With placeholder and error handling
Lumen.with(context)
    .load("https://example.com/image.jpg") {
        placeholder(R.drawable.placeholder)
        error(R.drawable.error)
    }
    .into(imageView)
```

### Image Transformations

```kotlin
// Rounded corners
Lumen.with(context)
    .load("https://example.com/image.jpg") {
        roundedCorners(20f)
    }
    .into(imageView)

// Chained transformations
Lumen.with(context)
    .load("https://example.com/image.jpg") {
        roundedCorners(30f)
        rotate(90f)
        blur(radius = 15f)
    }
    .into(imageView)
```

### Jetpack Compose

```kotlin
import com.xichen.lumen.view.compose.LumenImage

@Composable
fun ImageScreen() {
    LumenImage(
        url = "https://example.com/image.jpg",
        modifier = Modifier.size(200.dp),
        contentDescription = "Example image",
        block = {
            placeholder(R.drawable.placeholder)
            roundedCorners(20f)
        }
    )
}
```

### RecyclerView Optimization

```kotlin
class ImageAdapter : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Lumen automatically handles cancellation
        Lumen.with(holder.itemView.context)
            .load(images[position]) {
                roundedCorners(12f)
            }
            .into(holder.imageView)
    }
    
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Optional: Manual cancellation (Lumen already handles it)
        holder.itemView.cancelLumenLoad()
    }
}
```

### Advanced Usage: Custom Decryptor

```kotlin
class CustomDecryptor : ImageDecryptor {
    override fun decrypt(input: ByteArray): ByteArray {
        // Custom decryption logic
        return decryptedData
    }
    
    override val key: String = "custom_decryptor_v1"
}

Lumen.with(context)
    .load("https://example.com/encrypted-image.jpg") {
        decryptor(CustomDecryptor())
    }
    .into(imageView)
```

### GIF Animation

```kotlin
// Automatic GIF detection and playback (API 28+)
Lumen.with(context)
    .load("https://example.com/animation.gif")
    .into(imageView)
// Animation automatically starts on API 28+

// In Compose
LumenImage(
    url = "https://example.com/animation.gif",
    modifier = Modifier.size(200.dp)
)
```

### Video Frame Extraction

```kotlin
// Extract first frame from video file
Lumen.with(context)
    .loadVideo(videoFile)
    .into(imageView)

// Extract frame at specific time point (5 seconds)
val timeUs = 5_000_000L // 5 seconds = 5,000,000 microseconds
Lumen.with(context)
    .loadVideo(videoFile, timeUs)
    .into(imageView)

// Extract from video Uri
Lumen.with(context)
    .loadVideo(videoUri, timeUs)
    .into(imageView)

// Video frame with transformations
Lumen.with(context)
    .loadVideo(videoFile) {
        roundedCorners(16f)
        blur(10f)
    }
    .into(imageView)
```

### Disk Cache Management

```kotlin
// Clear disk cache
lifecycleScope.launch {
    Lumen.with(context).clearDiskCache()
}

// Clear all caches (memory + disk)
lifecycleScope.launch {
    Lumen.with(context).clearCache()
}

// Clear only memory cache
Lumen.with(context).clearMemoryCache()
```

---

## 🏗️ Architecture

### Core Loading Pipeline

```
ImageRequest (immutable data class)
   ↓
[1] Memory Cache Check → If hit, return cached Bitmap/Drawable
   ↓
[2] Disk Cache Check (for raw data) → If hit, skip fetching
   ↓
[3] Fetcher (Network / File / Uri / Resource / Video)
   - NetworkFetcher: HttpURLConnection-based network loading
   - FileFetcher: Local file system access
   - UriFetcher: ContentProvider access
   - ResourceFetcher: Android resource access
   - Video: Direct frame extraction via VideoFrameExtractor
   ↓
[4] Disk Cache Store (for raw data, before decryption)
   - Stores encrypted data (supports "no plaintext on disk")
   - LRU-based eviction when cache size exceeded
   ↓
[5] Decryptor (Optional)
   - Custom ImageDecryptor interface
   - Supports encrypted images for AI scenarios
   - Decryption happens in memory (no disk I/O)
   ↓
[6] Decoder (BitmapFactory / ImageDecoder)
   - Uses Android BitmapFactory for static images
   - Uses ImageDecoder for GIF animations (API 28+)
   - Automatic GIF detection
   - Supports custom BitmapFactory.Options
   - Automatic error handling
   ↓
[7] Transformer (Optional: rounded corners, rotation, crop, blur, etc.)
   - Applied directly to Bitmap pixels
   - Supports chained transformations
   - Smart View-level clipping for certain scaleTypes
   - Note: Transformers only apply to static images, not GIF animations
   ↓
[8] Memory Cache (LruCache)
   - Stores transformed Bitmap (for static images)
   - GIF animations not cached (Drawable not cacheable)
   - Automatic cache key generation
   - Thread-safe operations
   - Configurable cache size
   ↓
[9] Target (ImageView / Compose / Custom)
   - ImageViewTarget: Automatic RecyclerView optimization
   - LumenImage: Compose composable
   - Custom targets via Flow collection
```

**Core Principle: Every step is pluggable and transparent**

- Each step is an interface that can be customized
- Pipeline is fully observable via Flow
- Error handling at each step with clear error states
- No black box operations - everything is traceable

### Module Structure

```
Lumen/
 ├── lumen-core        // Core loading logic (no Android UI dependencies)
 │   ├── Lumen.kt              // Main loader class
 │   ├── ImageRequest.kt       // Request model
 │   ├── ImageState.kt         // State model (Sealed Class)
 │   ├── Fetcher.kt            // Data fetching (Network/File/Uri/Resource)
 │   ├── ImageDecryptor.kt     // Decryption interface
 │   ├── Decoder.kt             // Bitmap decoding (static + GIF)
 │   ├── BitmapTransformer.kt  // Transformation interface
 │   ├── Cache.kt               // Memory cache (LruCache) + Disk cache
 │   └── VideoFrameExtractor.kt // Video frame extraction
 │
 ├── lumen-view        // ImageView / ViewTarget / Compose support
 │   ├── RequestBuilder.kt     // DSL API builder
 │   ├── ImageViewTarget.kt    // ImageView integration
 │   ├── RecyclerViewExtensions.kt  // RecyclerView optimization
 │   └── compose/
 │       └── LumenImage.kt      // Compose composable
 │
 ├── lumen-transform   // Image transformers
 │   ├── RoundedCornersTransformer.kt  // Rounded corners
 │   ├── RotateTransformer.kt          // Rotation
 │   ├── CropTransformer.kt            // Cropping
 │   └── BlurTransformer.kt            // Blur effect
 │
 ├── lumen             // Aggregated module (convenience)
 └── app               // Sample application
```

### State Model

```kotlin
sealed class ImageState {
    object Loading : ImageState()
    data class Success(val bitmap: Bitmap) : ImageState()              // Static images
    data class SuccessAnimated(val drawable: Drawable) : ImageState()  // GIF animations
    data class Error(val throwable: Throwable? = null) : ImageState()
    object Fallback : ImageState()
}
```

---

## 📚 Documentation

### API Documentation

- [Core API](docs/api-core.md)
- [View API](docs/api-view.md)
- [Compose API](docs/api-compose.md)
- [Transform API](docs/api-transform.md)

### More Examples

Check the [sample-app](app/) module for complete example code.

## 💡 Best Practices

### 1. Disk Cache Strategy

- **Storage**: Disk cache stores raw data (may be encrypted) before decryption
- **Security**: Supports "no plaintext on disk" principle - decrypted data never touches disk
- **Performance**: Automatic LRU eviction when cache size exceeds limit (default 50MB)
- **Customization**: Can configure cache size when creating `DiskCache` instance

```kotlin
// Custom disk cache size
val diskCache = DiskCache(context, maxSizeBytes = 100 * 1024 * 1024) // 100MB
val lumen = Lumen.create(context, diskCache = diskCache)
```

### 2. GIF Animation Best Practices

- **API Compatibility**: 
  - API 28+: Full animation support with `ImageDecoder`
  - API < 28: Automatic fallback to static image (first frame)
- **Memory**: GIF animations are not cached in memory (Drawable is not cacheable)
- **Transformers**: Transformers do not apply to GIF animations (only to static images)
- **Auto-play**: Animations automatically start, no manual call needed

### 3. Video Frame Extraction Best Practices

- **Time Unit**: Use microseconds (1 second = 1,000,000 microseconds)
- **Performance**: Frame extraction runs on IO thread, results are cached
- **Transformers**: All transformers work with extracted frames
- **Caching**: Extracted frames are cached in memory for performance

```kotlin
// Extract frame at 5 seconds
val timeUs = 5_000_000L // 5 seconds

// Extract frame at 30% of video duration
val duration = VideoFrameExtractor.getDuration(context, videoUri)
val timeUs = (duration * 0.3).toLong()
```

### 4. Cache Management

```kotlin
// Clear memory cache (synchronous)
Lumen.with(context).clearMemoryCache()

// Clear disk cache (suspend function)
lifecycleScope.launch {
    Lumen.with(context).clearDiskCache()
}

// Clear all caches
lifecycleScope.launch {
    Lumen.with(context).clearCache()
}
```

### 5. RecyclerView Optimization

- Lumen automatically cancels loading tasks when views are recycled
- No manual cancellation needed in most cases
- Placeholder images are shown immediately

```kotlin
// Automatic - no extra code needed
override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    Lumen.with(holder.itemView.context)
        .load(images[position])
        .into(holder.imageView)
}
```

### 6. Error Handling

```kotlin
// Handle different states
Lumen.with(context)
    .load(url)
    .into(imageView) // Automatic error handling with error drawable

// Or use Flow for custom handling
Lumen.with(context)
    .load(request)
    .collect { state ->
        when (state) {
            is ImageState.Success -> { /* Show image */ }
            is ImageState.SuccessAnimated -> { /* Show GIF */ }
            is ImageState.Error -> { /* Handle error */ }
            is ImageState.Loading -> { /* Show loading */ }
            is ImageState.Fallback -> { /* Show fallback UI */ }
        }
    }
```

---

## 🤝 Contributing

We welcome all forms of contributions!

### How to Contribute

1. Fork this repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines

1. Follow Kotlin coding conventions
2. Add necessary unit tests
3. Update relevant documentation
4. Ensure all tests pass

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

Thanks to all developers who contributed to Lumen!

Special thanks to the Glide and Coil projects for their tremendous contributions to the Android image loading field.

---

## 📞 Contact

- **Issues**: [GitHub Issues](https://github.com/your-username/lumen/issues)
- **Email**: your-email@example.com

---

<div align="center">

**If this project helps you, please give it a ⭐ Star!**

Made with ❤️ by Lumen Team

</div>
