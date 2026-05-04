package com.openclaw.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Handles image operations: loading from URI, compression, and base64 encoding.
 */
class ImageRepository(private val context: Context) {

    companion object {
        private const val MAX_IMAGE_SIZE = 1024 // Max dimension in pixels
        private const val JPEG_QUALITY = 80
        private const val MAX_BASE64_SIZE = 20 * 1024 * 1024 // 20MB limit
    }

    /**
     * Load image from URI, compress it, and return base64 data URI.
     */
    fun encodeImageToBase64(uri: Uri): Result<String> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法读取图片"))

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                return Result.failure(Exception("无法解码图片"))
            }

            // Resize if too large
            val resizedBitmap = resizeBitmap(originalBitmap)

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }
            originalBitmap.recycle()

            val bytes = outputStream.toByteArray()
            if (bytes.size > MAX_BASE64_SIZE) {
                return Result.failure(Exception("图片太大（超过20MB）"))
            }

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64"
            Result.success(dataUri)
        } catch (e: Exception) {
            Result.failure(Exception("图片处理失败: ${e.message}"))
        }
    }

    /**
     * Get image dimensions without loading the full bitmap.
     */
    fun getImageDimensions(uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get a human-readable file size string.
     */
    fun getImageSize(uri: Uri): Long {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val size = inputStream?.available()?.toLong() ?: 0L
            inputStream?.close()
            size
        } catch (e: Exception) {
            0L
        }
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap
        }

        val ratio = minOf(
            MAX_IMAGE_SIZE.toFloat() / width,
            MAX_IMAGE_SIZE.toFloat() / height
        )
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
