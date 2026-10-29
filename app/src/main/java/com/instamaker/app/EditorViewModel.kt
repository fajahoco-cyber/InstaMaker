package com.instamaker.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val store = TemplateStore(app)

    /** فقط از ترد اصلی استفاده شود (رسم پیش‌نمایش و لمس) */
    val layoutCache = LayoutCache()
    val fonts = FontStore(app)
    val bundledFonts: List<FontOption> = fonts.bundledFonts()

    // ---- وضعیتی که UI می‌خواند ----
    var screen by mutableStateOf(Screen.HOME)
        private set
    var state by mutableStateOf(applyPreset(EditState()))
        private set
    var selected by mutableStateOf<TextTarget?>(null)
        private set
    var guideV by mutableStateOf(false)
        private set
    var guideH by mutableStateOf(false)
        private set
    var customTemplates by mutableStateOf(store.list())
        private set
    var userFonts by mutableStateOf(fonts.userFonts())
        private set
    /** نسخه‌ی سبک عکس برای پیش‌نمایش */
    var photoPreview by mutableStateOf<Bitmap?>(null)
        private set
    /** با هر بار آماده‌شدن یک قالب PNG زیاد می‌شود تا پیش‌نمایش دوباره رسم شود */
    var overlayTick by mutableIntStateOf(0)
        private set
    val thumbs = mutableStateMapOf<String, ImageBitmap>()
    var finalImage by mutableStateOf<ImageBitmap?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    // ---- داخلی ----
    private var photo: Bitmap? = null // نسخه‌ی بزرگ برای خروجی
    private var finalBitmap: Bitmap? = null
    private var sharedUri: Uri? = null
    private var thumbJob: Job? = null
    private val overlays = ConcurrentHashMap<String, Bitmap>()

    private var dragX = 0f
    private var dragY = 0f

    fun overlayFor(id: String): Bitmap? = overlays[id]

    // ---------------------------------------------------------------- عکس

    fun loadPhoto(uri: Uri) {
        viewModelScope.launch {
            busy = true
            val big = withContext(Dispatchers.IO) { ImageUtils.decodeUri(app, uri, 2400) }
            if (big == null) {
                busy = false
                message = "باز کردن عکس ناموفق بود"
                return@launch
            }
            val small = withContext(Dispatchers.Default) { ImageUtils.scaleDown(big, 1280) }
            photo = big
            photoPreview = small
            state = state.copy(photoScale = 1f, photoOffX = 0f, photoOffY = 0f, photoDim = 0f)
            selected = null
            busy = false
            screen = Screen.EDITOR
            renderThumbs()
        }
    }

    fun transformPhoto(dx: Float, dy: Float, zoom: Float) {
        val p = photoPreview ?: return
        val s = state
        val newScale = (s.photoScale * zoom).coerceIn(1f, 5f)
        val w = PostRenderer.BASE_W
        val h = s.format.h.toFloat()
        val r = PostRenderer.photoRect(p.width, p.height, w, h, newScale, s.photoOffX + dx, s.photoOffY + dy)
        state = s.copy(
            photoScale = newScale,
            photoOffX = r.centerX() - w / 2f,
            photoOffY = r.centerY() - h / 2f
        )
    }

    fun resetPhoto() {
        state = state.copy(photoScale = 1f, photoOffX = 0f, photoOffY = 0f, photoDim = 0f)
    }

    // ---------------------------------------------------------------- ویرایش

    private fun applyPreset(s: EditState): EditState {
        val (ty, bodyCy) = Templates.presetPositions(s.template, s.format)
        return s.copy(
            title = s.title.copy(cx = 0.5f, cy = ty),
            body = s.body.copy(cx = 0.5f, cy = bodyCy),
            textsMoved = false
        )
    }

    fun update(block: (EditState) -> EditState) {
        val old = state
        var new = block(old)
        val layoutChanged = old.format != new.format || old.template.id != new.template.id
        if (layoutChanged && !new.textsMoved) new = applyPreset(new)
        state = new
        if (old.format != new.format || old.accent != new.accent) renderThumbs()
    }

    fun updateText(t: TextTarget, block: (TextLayer) -> TextLayer) {
        state = state.withLayer(t, block)
    }

    fun select(t: TextTarget?) {
        selected = t
    }

    fun resetTextPositions() {
        state = applyPreset(state)
    }

    // ---- لمس و جابه‌جایی آزاد متن ----

    /** x و y در مختصات بوم ۱۰۸۰ */
    fun hitTest(x: Float, y: Float): TextTarget? {
        val s = state
        val h = s.format.h.toFloat()
        val order = (listOfNotNull(selected) + TextTarget.values().reversed()).distinct()
        for (t in order) {
            val r = PostRenderer.hitRect(s.layer(t), fonts, layoutCache, h)
            if (r != null && r.contains(x, y)) return t
        }
        return null
    }

    fun beginTextDrag(t: TextTarget) {
        val l = state.layer(t)
        dragX = l.cx
        dragY = l.cy
    }

    /** dx و dy کسری از عرض و ارتفاع بوم؛ نزدیک وسط بوم به‌صورت خودکار می‌چسبد. */
    fun dragText(t: TextTarget, dx: Float, dy: Float) {
        dragX = (dragX + dx).coerceIn(0f, 1f)
        dragY = (dragY + dy).coerceIn(0f, 1f)
        val snapX = abs(dragX - 0.5f) < SNAP_X
        val snapY = abs(dragY - 0.5f) < SNAP_Y
        val nx = if (snapX) 0.5f else dragX
        val ny = if (snapY) 0.5f else dragY
        guideV = snapX
        guideH = snapY
        state = state.withLayer(t) { it.copy(cx = nx, cy = ny) }.copy(textsMoved = true)
    }

    fun endTextDrag() {
        guideV = false
        guideH = false
    }

    fun scaleText(t: TextTarget, zoom: Float) {
        if (zoom == 1f) return
        state = state.withLayer(t) { it.copy(size = (it.size * zoom).coerceIn(0.03f, 0.22f)) }
    }

    // ---------------------------------------------------------------- قالب‌های PNG

    fun selectTemplate(t: Template) {
        update {
            it.copy(
                template = t,
                format = if (t is Template.Custom) closestFormat(t.aspect, it.format) else it.format
            )
        }
        if (t is Template.Custom) ensureOverlay(t)
    }

    /** اگر نسبت قالب PNG به یکی از اندازه‌های اینستاگرام نزدیک بود، همان انتخاب می‌شود. */
    private fun closestFormat(aspect: Float, current: Format): Format {
        val best = Format.values().minByOrNull { abs(it.aspect - aspect) } ?: return current
        return if (abs(best.aspect - aspect) < 0.04f) best else current
    }

    private fun ensureOverlay(t: Template.Custom) {
        if (overlays.containsKey(t.id)) return
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) { ImageUtils.decodeFile(t.file, OVERLAY_MAX) }
            if (bmp != null) {
                overlays[t.id] = bmp
                overlayTick++
            }
        }
    }

    fun importTemplate(uri: Uri) {
        viewModelScope.launch {
            busy = true
            val res = withContext(Dispatchers.IO) { store.import(uri) }
            busy = false
            when (res) {
                is TemplateStore.ImportResult.Ok -> {
                    customTemplates = store.list()
                    selectTemplate(res.template)
                    renderThumbs()
                    message = "قالب اضافه شد"
                }
                is TemplateStore.ImportResult.Error -> message = res.message
            }
        }
    }

    fun deleteTemplate(t: Template.Custom) {
        store.delete(t)
        overlays.remove(t.id)
        thumbs.remove(t.id)
        customTemplates = store.list()
        if (state.template.id == t.id) selectTemplate(Templates.builtIn.first())
    }

    // ---------------------------------------------------------------- فونت

    fun importFont(uri: Uri, target: TextTarget) {
        viewModelScope.launch {
            busy = true
            val res = withContext(Dispatchers.IO) { fonts.import(uri) }
            busy = false
            when (res) {
                is FontStore.ImportResult.Ok -> {
                    userFonts = fonts.userFonts()
                    updateText(target) { it.copy(fontId = res.font.id) }
                    message = "فونت اضافه شد"
                }
                is FontStore.ImportResult.Error -> message = res.message
            }
        }
    }

    fun deleteFont(id: String) {
        fonts.delete(id)
        userFonts = fonts.userFonts()
        var s = state
        if (s.title.fontId == id) s = s.withLayer(TextTarget.TITLE) { it.copy(fontId = FONT_TITLE_DEFAULT) }
        if (s.body.fontId == id) s = s.withLayer(TextTarget.BODY) { it.copy(fontId = FONT_BODY_DEFAULT) }
        state = s
    }

    // ---------------------------------------------------------------- بندانگشتی قالب‌ها

    /** فقط وقتی عکس، اندازه، رنگ قالب یا فهرست قالب‌ها عوض شود اجرا می‌شود، نه با هر حرکت متن. */
    private fun renderThumbs() {
        thumbJob?.cancel()
        val p = photoPreview ?: return
        val s = state
        val list: List<Template> = customTemplates + Templates.builtIn
        thumbJob = viewModelScope.launch(Dispatchers.Default) {
            for (t in list) {
                ensureActive()
                var overlay: Bitmap? = null
                if (t is Template.Custom) {
                    overlay = overlays[t.id]
                    if (overlay == null) {
                        overlay = ImageUtils.decodeFile(t.file, OVERLAY_MAX)
                        if (overlay != null) {
                            overlays[t.id] = overlay
                            withContext(Dispatchers.Main) { overlayTick++ }
                        }
                    }
                }
                val (ty, bodyCy) = Templates.presetPositions(t, s.format)
                val st = s.copy(
                    template = t,
                    title = s.title.copy(text = "عنوان", cx = 0.5f, cy = ty),
                    body = s.body.copy(text = "متن نمونه", cx = 0.5f, cy = bodyCy),
                    photoOffX = 0f,
                    photoOffY = 0f,
                    photoScale = 1f
                )
                val bmp = PostRenderer.render(p, st, THUMB_WIDTH, fonts, overlay)
                withContext(Dispatchers.Main) { thumbs[t.id] = bmp.asImageBitmap() }
            }
        }
    }

    // ---------------------------------------------------------------- صفحه‌ها

    fun goFinal() {
        selected = null
        screen = Screen.FINAL
        finalImage = null
        finalBitmap = null
        sharedUri = null
        val s = state
        val p = photo
        viewModelScope.launch(Dispatchers.Default) {
            val tpl = s.template
            val overlay = if (tpl is Template.Custom) ImageUtils.decodeFile(tpl.file, 2200) else null
            val bmp = PostRenderer.render(p, s, PostRenderer.BASE_W.toInt(), fonts, overlay)
            withContext(Dispatchers.Main) {
                finalBitmap = bmp
                finalImage = bmp.asImageBitmap()
            }
        }
    }

    fun back() {
        screen = when (screen) {
            Screen.FINAL -> Screen.EDITOR
            Screen.EDITOR -> Screen.HOME
            Screen.HOME -> Screen.HOME
        }
        selected = null
    }

    fun newPost() {
        photo = null
        photoPreview = null
        finalBitmap = null
        finalImage = null
        sharedUri = null
        selected = null
        thumbs.clear()
        state = applyPreset(
            EditState(format = state.format, template = state.template, accent = state.accent)
        )
        screen = Screen.HOME
        viewModelScope.launch(Dispatchers.IO) { Exporter.clearTempFiles(app) }
    }

    // ---------------------------------------------------------------- خروجی

    fun save() {
        val bmp = finalBitmap ?: return
        viewModelScope.launch {
            busy = true
            val uri = withContext(Dispatchers.IO) { Exporter.saveToGallery(app, bmp) }
            busy = false
            message = if (uri != null) "در گالری ذخیره شد (پوشه‌ی InstaMaker)" else "ذخیره ناموفق بود"
        }
    }

    /** pkg = null: انتخاب از لیست همه‌ی برنامه‌ها */
    fun shareVia(ctx: Context, pkg: String?) {
        val bmp = finalBitmap ?: return
        viewModelScope.launch {
            val uri = sharedUri ?: withContext(Dispatchers.IO) { Exporter.writeShareFile(app, bmp) }
                .also { sharedUri = it }
            Exporter.share(ctx, uri, pkg)
        }
    }

    private companion object {
        const val THUMB_WIDTH = 216
        const val OVERLAY_MAX = 1280
        const val SNAP_X = 0.02f
        const val SNAP_Y = 0.015f
    }
}
