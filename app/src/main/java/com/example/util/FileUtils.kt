package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    /**
     * Retrieves the file size of a content Uri safely.
     */
    fun getUriSize(context: Context, uri: Uri): Long {
        var size = 0L
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback method
        if (size <= 0L) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { fd ->
                    size = fd.length
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return size
    }

    /**
     * Compresses an image asynchronously in the background thread.
     * Prevents OutOfMemoryError by decoding with inSampleSize if the image is too large.
     */
    suspend fun compressImageFileAsync(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Target dimensions
            val reqWidth = 1200
            val reqHeight = 1200
            var inSampleSize = 1

            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            // Create output file
            val compressedFile = File(context.cacheDir, "compressed_upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            bitmap.recycle()

            if (compressedFile.exists() && compressedFile.length() > 0) {
                compressedFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
