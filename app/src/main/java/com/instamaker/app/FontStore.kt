package com.instamaker.app

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class FontOption(val id: String, val name: String, val typeface: Typeface, val isUser: Boolean)

/**
 * فونت‌های داخلی (وزیرمتن و فونت‌های سیستم) و فونت‌هایی که کاربر خودش اضافه می‌کند
 * (فایل TTF یا OTF). فایل‌ها داخل حافظه‌ی خصوصی برنامه ذخیره می‌شوند.
 */
class FontStore(private val ctx: Context) {

    sealed interface ImportResult {
        data class Ok(val font: FontOption) : ImportResult
        data class Error(val message: String) : ImportResult
    }

    private val dir = File(ctx.filesDir, "fonts").apply { mkdirs() }
    private val cache = ConcurrentHashMap<String, Typeface>()

    private val fallback: Typeface = Typeface.DEFAULT

    private val bundled: List<FontOption> by lazy {
        fun res(id: String, name: String, resId: Int): FontOption? {
            val tf = try {
                ResourcesCompat.getFont(ctx, resId)
            } catch (e: Exception) {
                null
            } ?: return null
            cache[id] = tf
            return FontOption(id, name, tf, false)
        }
        listOfNotNull(
            res("res:vz_regular", "وزیرمتن", R.font.vazirmatn_regular),
            res("res:vz_light", "وزیرمتن سبک", R.font.vazirmatn_light),
            res("res:vz_medium", "وزیرمتن متوسط", R.font.vazirmatn_medium),
            res("res:vz_bold", "وزیرمتن ضخیم", R.font.vazirmatn_bold),
            res("res:vz_black", "وزیرمتن سیاه", R.font.vazirmatn_black),
            FontOption("sys:serif", "Serif", Typeface.SERIF, false),
            FontOption("sys:sans", "Sans", Typeface.SANS_SERIF, false),
            FontOption("sys:mono", "Mono", Typeface.MONOSPACE, false)
        ).also { list -> list.filter { it.id.startsWith("sys:") }.forEach { cache[it.id] = it.typeface } }
    }

    fun bundledFonts(): List<FontOption> = bundled

    /** جدیدترین فونت اول */
    fun userFonts(): List<FontOption> =
        (dir.listFiles { f -> f.extension.lowercase() in EXTENSIONS } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .mapNotNull { toOption(it) }

    private fun idOf(f: File) = "user:${f.name}"

    private fun toOption(f: File): FontOption? {
        val id = idOf(f)
        val tf = cache[id] ?: loadFile(f)?.also { cache[id] = it } ?: return null
        val name = f.nameWithoutExtension.substringAfter("__", f.nameWithoutExtension)
        return FontOption(id, name, tf, true)
    }

    private fun loadFile(f: File): Typeface? = try {
        Typeface.Builder(f).build()
    } catch (e: Exception) {
        null
    }

    /** اگر فونتی پیدا نشد (مثلاً حذف شده باشد) فونت پیش‌فرض برگردانده می‌شود. */
    fun typeface(id: String): Typeface {
        cache[id]?.let { return it }
        bundledFonts() // تنبل: اولین بار فونت‌های داخلی را در cache می‌گذارد
        cache[id]?.let { return it }
        if (id.startsWith("user:")) {
            val f = File(dir, id.removePrefix("user:"))
            if (f.exists()) loadFile(f)?.let { cache[id] = it; return it }
        }
        return cache[FONT_BODY_DEFAULT] ?: fallback
    }

    fun import(uri: Uri): ImportResult {
        val displayName = queryName(uri) ?: "font"
        val ext = displayName.substringAfterLast('.', "").lowercase()
        if (ext !in EXTENSIONS) {
            return ImportResult.Error("فقط فایل فونت با پسوند ttf یا otf پشتیبانی می‌شود")
        }
        val clean = displayName.substringBeforeLast('.')
            .replace(Regex("[^\\p{L}\\p{N} _-]"), "")
            .trim()
            .ifEmpty { "font" }
            .take(40)
        val dest = File(dir, "${UUID.randomUUID().toString().take(8)}__$clean.$ext")
        return try {
            val input = ctx.contentResolver.openInputStream(uri)
                ?: return ImportResult.Error("فایل خوانده نشد")
            input.use { src -> dest.outputStream().use { dst -> src.copyTo(dst) } }
            val tf = loadFile(dest)
            if (tf == null) {
                dest.delete()
                return ImportResult.Error("این فایل یک فونت معتبر نیست")
            }
            cache[idOf(dest)] = tf
            ImportResult.Ok(FontOption(idOf(dest), clean, tf, true))
        } catch (e: Exception) {
            dest.delete()
            ImportResult.Error("افزودن فونت ناموفق بود")
        }
    }

    fun delete(id: String) {
        if (!id.startsWith("user:")) return
        cache.remove(id)
        File(dir, id.removePrefix("user:")).delete()
    }

    private fun queryName(uri: Uri): String? = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (e: Exception) {
        null
    } ?: uri.lastPathSegment?.substringAfterLast('/')

    private companion object {
        val EXTENSIONS = setOf("ttf", "otf")
    }
}
