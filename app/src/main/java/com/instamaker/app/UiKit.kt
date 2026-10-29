package com.instamaker.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** سایه‌ی نرم و گرم شبیه کارت‌های تصویر نمونه */
fun Modifier.softShadow(shape: Shape, elevation: Dp = 10.dp): Modifier =
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x22000000),
        spotColor = Color(0x556A4B33)
    )

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.BgTop, Palette.BgBottom))),
        content = content
    )
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp
) {
    val shape = RoundedCornerShape(50)
    val brush = if (enabled) Brush.horizontalGradient(listOf(Palette.GoldLight, Palette.Gold))
    else Brush.horizontalGradient(listOf(Palette.Line, Palette.Line))
    Row(
        modifier
            .height(height)
            .softShadow(shape, if (enabled) 10.dp else 0.dp)
            .clip(shape)
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    fontSize: Int = 14
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(height)
            .softShadow(shape, 4.dp)
            .clip(shape)
            .background(Palette.Cream)
            .border(1.dp, Palette.Line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                icon, contentDescription = null,
                tint = if (enabled) Palette.Gold else Palette.Line,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = if (enabled) Palette.Ink else Palette.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize.sp,
            maxLines = 1
        )
    }
}

/** دکمه‌ی گرد کوچک با آیکون، مثل دکمه‌ی «بازگشت» در تصویر نمونه */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .size(46.dp)
            .softShadow(shape, 6.dp)
            .clip(shape)
            .background(Color(0xCCFFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Palette.Bronze, modifier = Modifier.size(22.dp))
    }
}

/** تب‌های قرصی با انتخاب طلایی */
@Composable
fun PillTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val outer = RoundedCornerShape(50)
    Row(
        modifier
            .clip(outer)
            .background(Palette.Sand)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(outer)
                    .background(
                        if (sel) Brush.horizontalGradient(listOf(Palette.GoldLight, Palette.Gold))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (sel) Color.White else Palette.Muted,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/** نماد نسبت تصویر: یک مستطیل گرد هم‌نسبت با اندازه‌ی انتخابی */
@Composable
fun AspectGlyph(aspect: Float, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(26.dp)) {
        val maxSide = size.minDimension * 0.9f
        val w = if (aspect >= 1f) maxSide else maxSide * aspect
        val h = if (aspect >= 1f) maxSide / aspect else maxSide
        drawRoundRect(
            color = tint,
            topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/** قرص بلند با دایره‌ی سفید و آیکون در بالا و برچسب در پایین؛ مثل دکمه‌های دستگاه در تصویر نمونه */
@Composable
fun TallChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sub: String? = null,
    glyph: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val bg = if (selected) Brush.verticalGradient(listOf(Palette.BronzeLight, Palette.Bronze))
    else Brush.verticalGradient(listOf(Palette.Sand, Palette.Sand))
    Column(
        modifier
            .softShadow(shape, if (selected) 8.dp else 2.dp)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) { glyph() }
        Text(
            label,
            color = if (selected) Color.White else Palette.Ink,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (sub != null) {
            Text(
                sub,
                color = if (selected) Color(0xCCFFFFFF) else Palette.Muted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun goldSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Palette.Gold,
    activeTrackColor = Palette.Gold,
    inactiveTrackColor = Palette.Line
)
