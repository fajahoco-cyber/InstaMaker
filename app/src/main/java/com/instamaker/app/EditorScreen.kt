package com.instamaker.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(vm: EditorViewModel) {
    var tab by rememberSaveable { mutableStateOf(EditorTab.TEMPLATE) }
    var target by rememberSaveable { mutableStateOf(TextTarget.TITLE) }

    // با لمس یک متن روی پیش‌نمایش، تب متن باز می‌شود و همان متن برای ویرایش انتخاب می‌شود
    LaunchedEffect(vm.selected) {
        val s = vm.selected
        if (s != null) {
            target = s
            tab = EditorTab.TEXT
        }
    }

    Column(Modifier.fillMaxSize()) {
        // نوار بالا
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", { vm.back() })
            Text(
                "ویرایش",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Palette.Ink
            )
            GoldButton(
                text = "ادامه",
                onClick = { vm.goFinal() },
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                height = 44.dp
            )
        }

        // پیش‌نمایش زنده
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            PreviewCanvas(vm)
        }

        Text(
            "روی متن بزنید و بکشید؛ با دو انگشت اندازه‌ی آن را عوض کنید. بیرون از متن، عکس جابه‌جا می‌شود.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = Palette.Muted
        )

        // پنل پایین، شبیه کارت شیشه‌ای تصویر نمونه
        val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .softShadow(sheetShape, 18.dp)
                .clip(sheetShape)
                .background(Palette.Cream)
                .padding(top = 14.dp)
        ) {
            PillTabs(
                labels = EditorTab.values().map { it.label },
                selected = tab.ordinal,
                onSelect = { tab = EditorTab.values()[it] },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(PANEL_HEIGHT)
            ) {
                when (tab) {
                    EditorTab.TEMPLATE -> TemplatePanel(vm)
                    EditorTab.TEXT -> TextPanel(vm, target) { t ->
                        target = t
                        vm.select(t)
                    }
                    EditorTab.PHOTO -> PhotoPanel(vm)
                }
            }
        }
    }
}

private val PANEL_HEIGHT = 250.dp

// ------------------------------------------------------------------ پیش‌نمایش و لمس

/**
 * پیش‌نمایش مستقیم روی Canvas رسم می‌شود (بدون ساختن بیت‌مپ جدید در هر تغییر)،
 * به همین دلیل کشیدن متن و عکس روان است.
 */
@Composable
private fun PreviewCanvas(vm: EditorViewModel) {
    // فقط وقتی اندازه عوض شود دوباره ترکیب می‌شود، نه با هر حرکت
    val format by remember { derivedStateOf { vm.state.format } }
    val shape = RoundedCornerShape(24.dp)

    Canvas(
        Modifier
            .aspectRatio(format.aspect)
            .softShadow(shape, 14.dp)
            .clip(shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val unit = PostRenderer.BASE_W / size.width
                    val h = vm.state.format.h.toFloat()

                    val hit = vm.hitTest(down.position.x * unit, down.position.y * unit)
                    vm.select(hit)
                    if (hit != null) vm.beginTextDrag(hit)

                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (zoom != 1f || pan != Offset.Zero) {
                            if (hit != null) {
                                vm.dragText(
                                    hit,
                                    pan.x * unit / PostRenderer.BASE_W,
                                    pan.y * unit / h
                                )
                                vm.scaleText(hit, zoom)
                            } else {
                                vm.transformPhoto(pan.x * unit, pan.y * unit, zoom)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    vm.endTextDrag()
                }
            }
    ) {
        val s = vm.state
        vm.overlayTick // برای رسم دوباره بعد از آماده‌شدن قالب PNG
        val tpl = s.template
        val overlay = if (tpl is Template.Custom) vm.overlayFor(tpl.id) else null
        val k = size.width / PostRenderer.BASE_W
        val ui = UiMarks(
            selected = vm.selected,
            stroke = 2.2f * density / k,
            guideV = vm.guideV,
            guideH = vm.guideH
        )
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.scale(k, k)
            PostRenderer.draw(nc, vm.photoPreview, s, vm.fonts, overlay, vm.layoutCache, ui)
            nc.restore()
        }
    }
}

// ------------------------------------------------------------------ تب قالب

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplatePanel(vm: EditorViewModel) {
    val s = vm.state
    var deleting by remember { mutableStateOf<Template.Custom?>(null) }

    // فقط PNG؛ قالب باید پس‌زمینه‌ی شفاف داشته باشد
    val pickPng = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importTemplate(uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Format.values().forEach { f ->
                FormatPill(f, s.format == f, Modifier.weight(1f)) { vm.update { it.copy(format = f) } }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "add") { AddCard(s.format.aspect) { pickPng.launch(arrayOf("image/png")) } }
            items(vm.customTemplates, key = { it.id }) { t ->
                TemplateCard(
                    t, vm.thumbs[t.id], s.format.aspect, s.template.id == t.id,
                    onClick = { vm.selectTemplate(t) },
                    onLongClick = { deleting = t }
                )
            }
            items(Templates.builtIn, key = { it.id }) { t ->
                TemplateCard(
                    t, vm.thumbs[t.id], s.format.aspect, s.template.id == t.id,
                    onClick = { vm.selectTemplate(t) },
                    onLongClick = null
                )
            }
        }

        val tpl = s.template
        if (tpl is Template.BuiltIn && tpl.style.usesAccent) {
            Text("رنگ قالب", Modifier.padding(horizontal = 20.dp), fontSize = 13.sp, color = Palette.Ink)
            ColorRow(AccentPalette, s.accent) { c -> vm.update { it.copy(accent = c) } }
        }

        Text(
            "قالب PNG با پس‌زمینه‌ی شفاف را با + اضافه کنید. برای حذف، روی قالب اضافه‌شده نگه دارید.",
            Modifier.padding(horizontal = 20.dp),
            fontSize = 11.sp,
            color = Palette.Muted
        )
    }

    deleting?.let { t ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف قالب؟") },
            text = { Text("این قالب از برنامه پاک می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTemplate(t)
                    deleting = null
                }) { Text("حذف", color = Palette.Gold) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("انصراف", color = Palette.Muted) }
            }
        )
    }
}

@Composable
private fun FormatPill(f: Format, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(Palette.GoldLight, Palette.Gold))
                else Brush.horizontalGradient(listOf(Palette.Sand, Palette.Sand))
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AspectGlyph(f.aspect, if (selected) Color.White else Palette.Bronze, Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            f.short,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selected) Color.White else Palette.Ink
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateCard(
    t: Template,
    thumb: androidx.compose.ui.graphics.ImageBitmap?,
    aspect: Float,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            Modifier
                .height(112.dp)
                .aspectRatio(aspect)
                .softShadow(shape, if (selected) 8.dp else 3.dp)
                .clip(shape)
                .background(Palette.Sand)
                .border(
                    if (selected) 3.dp else 0.dp,
                    if (selected) Palette.Gold else Color.Transparent,
                    shape
                )
        ) {
            if (thumb != null) {
                Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            t.name,
            fontSize = 11.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Palette.Gold else Palette.Ink
        )
    }
}

/** دکمه‌ی دایره‌ای طلایی «+» مثل تصویر نمونه */
@Composable
private fun AddCard(aspect: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .height(112.dp)
                .width(80.dp)
                .clip(shape)
                .background(Palette.Sand),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .softShadow(CircleShape, 6.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Palette.GoldLight, Palette.Gold))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن قالب", tint = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("افزودن PNG", fontSize = 11.sp, color = Palette.Ink)
    }
}

// ------------------------------------------------------------------ تب متن

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextPanel(vm: EditorViewModel, target: TextTarget, onTarget: (TextTarget) -> Unit) {
    val s = vm.state
    val layer = s.layer(target)
    var deletingFont by remember { mutableStateOf<FontOption?>(null) }

    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFont(uri, target)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // انتخاب بین تیتر و توضیحات؛ هرکدام جدا جابه‌جا و تنظیم می‌شوند
        PillTabs(
            labels = TextTarget.values().map { it.label },
            selected = target.ordinal,
            onSelect = { onTarget(TextTarget.values()[it]) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        OutlinedTextField(
            value = layer.text,
            onValueChange = { v -> vm.updateText(target) { it.copy(text = v) } },
            label = { Text(target.label) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            minLines = 1,
            maxLines = 4,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Gold,
                unfocusedBorderColor = Palette.Line,
                focusedLabelColor = Palette.Gold,
                cursorColor = Palette.Gold,
                focusedContainerColor = Palette.Cream,
                unfocusedContainerColor = Palette.Cream
            )
        )

        Text("فونت", Modifier.padding(horizontal = 20.dp), fontSize = 13.sp, color = Palette.Ink)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "add_font") {
                Row(
                    Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Palette.GoldLight, Palette.Gold)))
                        .clickable { pickFont.launch(arrayOf("*/*")) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("افزودن فونت", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            items(vm.userFonts + vm.bundledFonts, key = { it.id }) { f ->
                FontChip(
                    f, layer.fontId == f.id,
                    onClick = { vm.updateText(target) { it.copy(fontId = f.id) } },
                    onLongClick = if (f.isUser) ({ deletingFont = f }) else null
                )
            }
        }

        LabeledSlider("اندازه", layer.size, 0.025f..0.2f) { v ->
            vm.updateText(target) { it.copy(size = v) }
        }
        LabeledSlider("عرض کادر", layer.width, 0.3f..1f) { v ->
            vm.updateText(target) { it.copy(width = v) }
        }

        Text("رنگ", Modifier.padding(horizontal = 20.dp), fontSize = 13.sp, color = Palette.Ink)
        ColorRow(TextPalette, layer.color) { c -> vm.updateText(target) { it.copy(color = c) } }

        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SoftButton(
                "جای پیش‌فرض متن‌ها", { vm.resetTextPositions() },
                Modifier.weight(1f), height = 42.dp, fontSize = 13
            )
        }
    }

    deletingFont?.let { f ->
        AlertDialog(
            onDismissRequest = { deletingFont = null },
            title = { Text("حذف فونت؟") },
            text = { Text("فونت «${f.name}» از برنامه پاک می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteFont(f.id)
                    deletingFont = null
                }) { Text("حذف", color = Palette.Gold) }
            },
            dismissButton = {
                TextButton(onClick = { deletingFont = null }) { Text("انصراف", color = Palette.Muted) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FontChip(
    f: FontOption,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .height(42.dp)
            .clip(shape)
            .background(if (selected) Color(0xFFF3E3C4) else Palette.Sand)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Palette.Gold else Palette.Line,
                shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            f.name,
            fontFamily = FontFamily(f.typeface),
            fontSize = 15.sp,
            maxLines = 1,
            color = Palette.Ink
        )
    }
}

// ------------------------------------------------------------------ تب عکس

@Composable
private fun PhotoPanel(vm: EditorViewModel) {
    val s = vm.state
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "برای جابه‌جایی عکس، بیرون از متن بکشید و با دو انگشت بزرگ و کوچک کنید.",
            Modifier.padding(horizontal = 20.dp),
            fontSize = 12.sp,
            color = Palette.Muted
        )
        LabeledSlider("بزرگ‌نمایی", s.photoScale, 1f..5f) { v ->
            vm.transformPhoto(0f, 0f, v / s.photoScale)
        }
        LabeledSlider("تیره کردن", s.photoDim, 0f..0.6f) { v ->
            vm.update { it.copy(photoDim = v) }
        }
        Row(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SoftButton("بازنشانی عکس", { vm.resetPhoto() }, Modifier.weight(1f), height = 42.dp, fontSize = 13)
            SoftButton("تغییر عکس", { vm.back() }, Modifier.weight(1f), height = 42.dp, fontSize = 13)
        }
    }
}

// ------------------------------------------------------------------ اجزای مشترک

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(
        Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.width(84.dp), fontSize = 13.sp, color = Palette.Ink)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = goldSliderColors(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ColorRow(colors: List<Int>, selected: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { c ->
            val isSel = c == selected
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(
                        if (isSel) 3.dp else 1.dp,
                        if (isSel) Palette.Gold else Palette.Line,
                        CircleShape
                    )
                    .clickable { onPick(c) }
            )
        }
    }
}
