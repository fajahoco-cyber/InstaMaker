package com.instamaker.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object Exporter {
    private const val JPEG_QUALITY = 95

    /** ذخیره در گالری: Pictures/InstaMaker */
    fun saveToGallery(ctx: Context, bmp: Bitmap): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "InstaMaker_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/InstaMaker")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                ?: throw IllegalStateException("no stream")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    /** نوشتن فایل موقت برای اشتراک‌گذاری */
    fun writeShareFile(ctx: Context, bmp: Bitmap): Uri {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "post_${System.currentTimeMillis()}.jpg")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    }

    /** pkg = null یعنی لیست همه‌ی برنامه‌ها */
    fun share(ctx: Context, uri: Uri, pkg: String?) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (pkg != null) setPackage(pkg)
        }
        try {
            ctx.startActivity(if (pkg == null) Intent.createChooser(send, "اشتراک‌گذاری") else send)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "این برنامه روی گوشی نصب نیست", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearTempFiles(ctx: Context) {
        File(ctx.cacheDir, "shared").deleteRecursively()
        File(ctx.cacheDir, "camera").deleteRecursively()
    }
}
