package com.codetrio.spatialflow.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object ColorSchemeCache {
    private val memoryCache = LruCache<String, ColorSchemePair>(30)
    private val mutex = Mutex()
    private val inProgress = mutableSetOf<String>()
    fun get(key: String): ColorSchemePair? = memoryCache.get(key)
    fun put(key: String, v: ColorSchemePair) { memoryCache.put(key, v) }
    fun evictAll() = memoryCache.evictAll()
    suspend fun markInProgress(uri: String): Boolean = mutex.withLock {
        if (inProgress.contains(uri)) false else { inProgress.add(uri); true }
    }
    suspend fun markComplete(uri: String) = mutex.withLock { inProgress.remove(uri) }
    suspend fun loadBitmapForExtraction(ctx: Context, uri: Any): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val req = ImageRequest.Builder(ctx).data(uri).allowHardware(false)
                .size(Size(128,128)).bitmapConfig(Bitmap.Config.ARGB_8888).build()
            val d = ctx.imageLoader.execute(req).drawable ?: return@withContext null
            Bitmap.createBitmap(
                d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            ).also { b -> Canvas(b).let { c -> d.setBounds(0,0,c.width,c.height); d.draw(c) } }
        } catch (_: Exception) { null }
    }
}
