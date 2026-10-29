package com.instamaker.app

import android.graphics.Color
import java.io.File

/** اندازه‌های رایج اینستاگرام */
enum class Format(val label: String, val short: String, val w: Int, val h: Int) {
    SQUARE("پست مربعی", "۱:۱", 1080, 1080),
    PORTRAIT("پست عمودی", "۴:۵", 1080, 1350),
    STORY("استوری", "۹:۱۶", 1080, 1920);

    val aspect: Float get() = w.toFloat() / h

    /** فاصله‌ی امن بالا/پایین: در استوری نوار اینستاگرام بخشی از تصویر را می‌پوشاند */
    val safeTop: Float get() = if (this == STORY) 230f else 70f
    val safeBottom: Float get() = if (this == STORY) 250f else 70f
}

/** سبک قالب‌های آماده (بدون نیاز به فایل) */
enum class BuiltInStyle(val usesAccent: Boolean) {
    NONE(false),
    BOTTOM_GRADIENT(false),
    TOP_GRADIENT(false),
    CARD(false),
    BAND(true),
    FRAME(true)
}

sealed interface Template {
    val id: String
    val name: String

    data class BuiltIn(
        override val id: String,
        override val name: String,
        val style: BuiltInStyle
    ) : Template

    /** قالب PNG که کاربر اضافه کرده است */
    data class Custom(
        override val id: String,
        override val name: String,
        val file: File,
        val aspect: Float
    ) : Template
}

object Templates {
    val builtIn: List<Template.BuiltIn> = listOf(
        Template.BuiltIn("b_grad_bottom", "گرادیان پایین", BuiltInStyle.BOTTOM_GRADIENT),
        Template.BuiltIn("b_grad_top", "گرادیان بالا", BuiltInStyle.TOP_GRADIENT),
        Template.BuiltIn("b_card", "کارت", BuiltInStyle.CARD),
        Template.BuiltIn("b_band", "نوار رنگی", BuiltInStyle.BAND),
        Template.BuiltIn("b_frame", "قاب", BuiltInStyle.FRAME),
        Template.BuiltIn("b_plain", "بدون قالب", BuiltInStyle.NONE)
    )

    /** فاصله‌ی عمودی مرکز تیتر تا مرکز توضیحات (واحد: پیکسل بوم ۱۰۸۰) */
    private const val GAP = 130f

    /**
     * جای پیش‌فرض تیتر و توضیحات برای هر قالب (کسری از ارتفاع بوم).
     * خروجی: (cy تیتر، cy توضیحات)
     */
    fun presetPositions(template: Template, format: Format): Pair<Float, Float> {
        val h = format.h.toFloat()
        val style = (template as? Template.BuiltIn)?.style ?: BuiltInStyle.BOTTOM_GRADIENT
        val title = when (style) {
            BuiltInStyle.TOP_GRADIENT -> format.safeTop + 70f
            BuiltInStyle.CARD, BuiltInStyle.BAND -> h * 0.60f
            else -> h - format.safeBottom - 60f - GAP
        }
        return (title / h) to ((title + GAP) / h)
    }
}

val TextPalette: List<Int> = listOf(
    Color.WHITE,
    0xFF1E1B18.toInt(),
    0xFFFFD54F.toInt(),
    0xFFE9B872.toInt(),
    0xFFFF6E6E.toInt(),
    0xFF7FD6FF.toInt(),
    0xFF8CE99A.toInt()
)

val AccentPalette: List<Int> = listOf(
    0xFFB8832A.toInt(),
    0xFF6A4B33.toInt(),
    0xFFE1306C.toInt(),
    0xFF833AB4.toInt(),
    0xFF2979FF.toInt(),
    0xFF111111.toInt(),
    0xFFFFFFFF.toInt()
)

const val FONT_TITLE_DEFAULT = "res:vz_bold"
const val FONT_BODY_DEFAULT = "res:vz_regular"

enum class TextTarget(val label: String) {
    TITLE("تیتر"),
    BODY("توضیحات")
}

/** یک لایه‌ی متن؛ تیتر و توضیحات هرکدام لایه‌ی مستقل با جا، اندازه، رنگ و فونت خودشان دارند. */
data class TextLayer(
    val text: String,
    /** اندازه‌ی فونت نسبت به عرض بوم */
    val size: Float,
    val color: Int,
    val fontId: String,
    /** مرکز متن، کسری از عرض و ارتفاع بوم (۰ تا ۱) */
    val cx: Float = 0.5f,
    val cy: Float = 0.7f,
    /** عرض کادر شکستن خطوط، کسری از عرض بوم */
    val width: Float = 0.84f
)

/** همه‌ی چیزهایی که کاربر در ویرایشگر تنظیم می‌کند */
data class EditState(
    val format: Format = Format.PORTRAIT,
    val template: Template = Templates.builtIn.first(),
    val title: TextLayer = TextLayer("عنوان شما", 0.085f, Color.WHITE, FONT_TITLE_DEFAULT),
    val body: TextLayer = TextLayer("متن توضیحات را اینجا بنویسید", 0.045f, Color.WHITE, FONT_BODY_DEFAULT),
    /** اگر کاربر متن را جابه‌جا کرده باشد، تغییر قالب یا اندازه جای متن را دست نمی‌زند */
    val textsMoved: Boolean = false,
    val accent: Int = 0xFFB8832A.toInt(),
    /** بزرگ‌نمایی و جابجایی عکس (واحد: پیکسل بوم ۱۰۸۰) */
    val photoScale: Float = 1f,
    val photoOffX: Float = 0f,
    val photoOffY: Float = 0f,
    /** تیره کردن عکس، بین ۰ تا ۰٫۶ */
    val photoDim: Float = 0f
) {
    fun layer(t: TextTarget): TextLayer = if (t == TextTarget.TITLE) title else body

    fun withLayer(t: TextTarget, block: (TextLayer) -> TextLayer): EditState =
        if (t == TextTarget.TITLE) copy(title = block(title)) else copy(body = block(body))
}

enum class Screen { HOME, EDITOR, FINAL }

enum class EditorTab(val label: String) {
    TEMPLATE("قالب"),
    TEXT("متن"),
    PHOTO("عکس")
}
