package com.instamaker.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.roundToInt

/** کش کوچک برای StaticLayout تا هنگام کشیدن متن، هر فریم از نو ساخته نشود. فقط از یک ترد استفاده شود. */
class LayoutCache {
    private data class Key(
        val text: String,
        val size: Float,
        val color: Int,
        val font: String,
        val width: Int
    )

    private val map = object : LinkedHashMap<Key, StaticLayout>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, StaticLayout>?): Boolean =
            size > 24
    }

    fun get(layer: TextLayer, widthPx: Int, build: () -> StaticLayout): StaticLayout {
        val key = Key(layer.text, layer.size, layer.color, layer.fontId, widthPx)
        return map.getOrPut(key, build)
    }
}

/** نشانه‌هایی که فقط در پیش‌نمایش رسم می‌شوند (کادر انتخاب و خطوط راهنما) و در خروجی نیستند. */
class UiMarks(
    val selected: TextTarget?,
    /** ضخامت خط به واحد بوم ۱۰۸۰؛ باید با مقیاس نمایش هماهنگ باشد */
    val stroke: Float,
    val guideV: Boolean,
    val guideH: Boolean
)

/**
 * یک تابع واحد هم برای پیش‌نمایش و هم برای خروجی نهایی؛
 * بنابراین چیزی که می‌بینید دقیقاً همان چیزی است که ذخیره می‌شود.
 * همه‌چیز در فضای مختصات «۱۰۸۰ پیکسل عرض» رسم می‌شود؛ مقیاس را فراخوان‌کننده روی Canvas می‌دهد.
 */
object PostRenderer {
    const val BASE_W = 1080f

    private const val SELECTION_GOLD = 0xFFFFC857.toInt()

    /** مستطیل جای عکس (پوشاندن کامل بوم + بزرگ‌نمایی + جابجایی با محدودیت) */
    fun photoRect(
        bw: Int, bh: Int, w: Float, h: Float,
        scale: Float, offX: Float, offY: Float
    ): RectF {
        val s = max(w / bw, h / bh) * scale
        val dw = bw * s
        val dh = bh * s
        val maxX = max(0f, (dw - w) / 2f)
        val maxY = max(0f, (dh - h) / 2f)
        val cx = w / 2f + offX.coerceIn(-maxX, maxX)
        val cy = h / 2f + offY.coerceIn(-maxY, maxY)
        return RectF(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f)
    }

    // ------------------------------------------------------------ متن

    private fun buildLayout(layer: TextLayer, fonts: FontStore, widthPx: Int): StaticLayout {
        val isLight = ColorUtils.calculateLuminance(layer.color) > 0.5
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.typeface = fonts.typeface(layer.fontId)
        tp.textSize = layer.size * BASE_W
        tp.color = layer.color
        tp.setShadowLayer(10f, 0f, 3f, if (isLight) 0x99000000.toInt() else 0x55FFFFFF)
        return StaticLayout.Builder.obtain(layer.text, 0, layer.text.length, tp, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .build()
    }

    fun layoutFor(layer: TextLayer, fonts: FontStore, cache: LayoutCache?): StaticLayout? {
        if (layer.text.isBlank()) return null
        val widthPx = (layer.width * BASE_W).toInt().coerceAtLeast(40)
        return if (cache != null) cache.get(layer, widthPx) { buildLayout(layer, fonts, widthPx) }
        else buildLayout(layer, fonts, widthPx)
    }

    /** مستطیل فشرده‌ی متن (به‌اندازه‌ی طولانی‌ترین خط) در مختصات بوم؛ برای کادر انتخاب و لمس. */
    fun textRect(layer: TextLayer, layout: StaticLayout, h: Float): RectF {
        var maxLine = 0f
        for (i in 0 until layout.lineCount) maxLine = max(maxLine, layout.getLineWidth(i))
        val cx = layer.cx * BASE_W
        val cy = layer.cy * h
        return RectF(
            cx - maxLine / 2f, cy - layout.height / 2f,
            cx + maxLine / 2f, cy + layout.height / 2f
        )
    }

    /** مستطیل لمس هر متن (با کمی حاشیه)؛ برای متن خالی null. */
    fun hitRect(layer: TextLayer, fonts: FontStore, cache: LayoutCache?, h: Float): RectF? {
        val l = layoutFor(layer, fonts, cache) ?: return null
        return textRect(layer, l, h).apply { inset(-36f, -36f) }
    }

    // ------------------------------------------------------------ رسم

    fun draw(
        c: Canvas,
        photo: Bitmap?,
        state: EditState,
        fonts: FontStore,
        overlay: Bitmap?,
        cache: LayoutCache?,
        ui: UiMarks?
    ) {
        val w = BASE_W
        val h = state.format.h.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // ۱) پس‌زمینه و عکس
        paint.color = 0xFF1C1C22.toInt()
        c.drawRect(0f, 0f, w, h, paint)
        if (photo != null) {
            val r = photoRect(
                photo.width, photo.height, w, h,
                state.photoScale, state.photoOffX, state.photoOffY
            )
            c.drawBitmap(photo, null, r, paint)
        }
        if (state.photoDim > 0f) {
            paint.color = Color.argb((state.photoDim.coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
            c.drawRect(0f, 0f, w, h, paint)
        }

        // ۲) چیدمان متن (قبل از قالب، چون بعضی قالب‌ها دور متن شکل می‌کشند)
        val titleLayout = layoutFor(state.title, fonts, cache)
        val bodyLayout = layoutFor(state.body, fonts, cache)
        val titleRect = titleLayout?.let { textRect(state.title, it, h) }
        val bodyRect = bodyLayout?.let { textRect(state.body, it, h) }
        val group: RectF? = when {
            titleRect != null && bodyRect != null -> RectF(titleRect).apply { union(bodyRect) }
            titleRect != null -> RectF(titleRect)
            bodyRect != null -> RectF(bodyRect)
            else -> null
        }

        // ۳) قالب
        when (val t = state.template) {
            is Template.BuiltIn -> drawBuiltIn(c, t.style, w, h, group, state.accent)
            is Template.Custom -> if (overlay != null) drawCenterCrop(c, overlay, w, h, paint)
        }

        // ۴) متن‌ها
        drawLayout(c, titleLayout, state.title, h)
        drawLayout(c, bodyLayout, state.body, h)

        // ۵) فقط پیش‌نمایش: خطوط راهنما و کادر انتخاب
        if (ui != null) {
            val sw = ui.stroke
            val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = sw
                color = SELECTION_GOLD
            }
            if (ui.guideV) c.drawLine(w / 2f, 0f, w / 2f, h, guide)
            if (ui.guideH) c.drawLine(0f, h / 2f, w, h / 2f, guide)

            val sel = ui.selected
            if (sel != null) {
                val rect = if (sel == TextTarget.TITLE) titleRect else bodyRect
                if (rect != null) {
                    val r = RectF(rect).apply { inset(-16f, -12f) }
                    val box = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = sw
                        color = SELECTION_GOLD
                        pathEffect = DashPathEffect(floatArrayOf(sw * 3f, sw * 2f), 0f)
                    }
                    c.drawRect(r, box)
                    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SELECTION_GOLD }
                    val rad = sw * 1.8f
                    c.drawCircle(r.left, r.top, rad, dot)
                    c.drawCircle(r.right, r.top, rad, dot)
                    c.drawCircle(r.left, r.bottom, rad, dot)
                    c.drawCircle(r.right, r.bottom, rad, dot)
                }
            }
        }
    }

    private fun drawLayout(c: Canvas, layout: StaticLayout?, layer: TextLayer, h: Float) {
        if (layout == null) return
        c.save()
        c.translate(layer.cx * BASE_W - layout.width / 2f, layer.cy * h - layout.height / 2f)
        layout.draw(c)
        c.restore()
    }

    /** خروجی نهایی یا بند‌انگشتی: بیت‌مپ جدید می‌سازد و همان draw را صدا می‌زند. */
    fun render(
        photo: Bitmap?,
        state: EditState,
        outW: Int,
        fonts: FontStore,
        overlay: Bitmap?
    ): Bitmap {
        val scale = outW / BASE_W
        val outH = (state.format.h * scale).roundToInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(scale, scale)
        draw(c, photo, state, fonts, overlay, null, null)
        return bmp
    }

    private fun drawCenterCrop(c: Canvas, b: Bitmap, w: Float, h: Float, paint: Paint) {
        val s = max(w / b.width, h / b.height)
        val dw = b.width * s
        val dh = b.height * s
        c.drawBitmap(b, null, RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f), paint)
    }

    private fun drawBuiltIn(
        c: Canvas,
        style: BuiltInStyle,
        w: Float,
        h: Float,
        group: RectF?,
        accent: Int
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val clear = Color.argb(0, 0, 0, 0)
        val dark = Color.argb(205, 0, 0, 0)

        fun bottomGradient() {
            p.shader = LinearGradient(0f, h * 0.40f, 0f, h, clear, dark, Shader.TileMode.CLAMP)
            c.drawRect(0f, h * 0.40f, w, h, p)
            p.shader = null
        }

        when (style) {
            BuiltInStyle.NONE -> Unit
            BuiltInStyle.BOTTOM_GRADIENT -> bottomGradient()
            BuiltInStyle.TOP_GRADIENT -> {
                p.shader = LinearGradient(0f, h * 0.60f, 0f, 0f, clear, dark, Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w, h * 0.60f, p)
                p.shader = null
            }
            BuiltInStyle.CARD -> if (group != null) {
                p.color = Color.argb(180, 0, 0, 0)
                val r = RectF(group).apply { inset(-50f, -42f) }
                c.drawRoundRect(r, 44f, 44f, p)
            }
            BuiltInStyle.BAND -> if (group != null) {
                p.color = accent
                c.drawRect(0f, group.top - 56f, w, group.bottom + 56f, p)
            }
            BuiltInStyle.FRAME -> {
                bottomGradient()
                val sw = w * 0.035f
                p.style = Paint.Style.STROKE
                p.strokeWidth = sw
                p.color = accent
                c.drawRect(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f, p)
            }
        }
    }
}
