package com.instamaker.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/** قالب‌های PNG که کاربر اضافه می‌کند در حافظه‌ی داخلی برنامه نگه‌داری می‌شوند. */
class TemplateStore(private val ctx: Context) {

    sealed interface ImportResult {
        data class Ok(val template: Template.Custom) : ImportResult
        data class Error(val message: String) : ImportResult
    }

    private val dir = File(ctx.filesDir, "templates").apply { mkdirs() }

    /** جدیدترین قالب اول */
    fun list(): List<Template.Custom> =
        (dir.listFiles { f -> f.extension.equals("png", ignoreCase = true) } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .mapNotNull { toTemplate(it) }

    private fun toTemplate(f: File): Template.Custom? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, o)
        if (o.outWidth <= 0 || o.outHeight <= 0) return null
        return Template.Custom(
            id = f.nameWithoutExtension,
            name = "قالب من",
            file = f,
            aspect = o.outWidth.toFloat() / o.outHeight
        )
    }

    fun import(uri: Uri): ImportResult {
        val tmp = File(dir, "${UUID.randomUUID()}.tmp")
        return try {
            val input = ctx.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("فایل خوانده نشد")
            input.use { src -> tmp.outputStream().use { dst -> src.copyTo(dst) } }

            val small = ImageUtils.decodeFile(tmp, 400)
            if (small == null) {
                tmp.delete()
                return ImportResult.Error("این فایل یک تصویر معتبر نیست")
            }
            if (!small.hasAlpha()) {
                tmp.delete()
                return ImportResult.Error(
                    "این تصویر پس‌زمینه‌ی شفاف ندارد و عکس شما را کاملاً می‌پوشاند. " +
                        "یک فایل PNG با پس‌زمینه‌ی شفاف انتخاب کنید."
                )
            }
            val dest = File(dir, "${tmp.nameWithoutExtension}.png")
            if (!tmp.renameTo(dest)) {
                tmp.delete()
                return ImportResult.Error("ذخیره‌ی قالب ناموفق بود")
            }
            val t = toTemplate(dest) ?: return ImportResult.Error("قالب خوانده نشد")
            ImportResult.Ok(t)
        } catch (e: Exception) {
            tmp.delete()
            ImportResult.Error("افزودن قالب ناموفق بود")
        }
    }

    fun delete(t: Template.Custom) {
        t.file.delete()
    }
}
