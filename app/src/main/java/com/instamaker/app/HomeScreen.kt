package com.instamaker.app

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun HomeScreen(vm: EditorViewModel) {
    val ctx = LocalContext.current

    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) vm.loadPhoto(uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.loadPhoto(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // دو خط طلایی کوچک بالای صفحه، مثل تصویر نمونه
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.width(26.dp).height(3.dp).clip(CircleShape).background(Palette.Gold))
            Box(Modifier.width(16.dp).height(3.dp).clip(CircleShape).background(Palette.GoldLight))
        }
        Spacer(Modifier.height(28.dp))

        Text(
            "سلام!",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.Ink
        )
        Text(
            "پست یا استوری بعدی‌ات را همین‌جا آماده کن",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.Muted
        )
        Spacer(Modifier.height(24.dp))

        // انتخاب اندازه
        val pill = RoundedCornerShape(50)
        Row(
            Modifier
                .fillMaxWidth()
                .softShadow(pill, 6.dp)
                .clip(pill)
                .background(Palette.Cream)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Format.values().forEach { f ->
                val sel = vm.state.format == f
                Column(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(pill)
                        .background(
                            if (sel) Brush.horizontalGradient(listOf(Palette.GoldLight, Palette.Gold))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { vm.update { it.copy(format = f) } },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        f.short,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (sel) Color.White else Palette.Ink
                    )
                    Text(
                        f.label,
                        fontSize = 11.sp,
                        color = if (sel) Color(0xDDFFFFFF) else Palette.Muted
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        Text(
            "شروع کنید",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Palette.Ink
        )
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StartCard(
                icon = Icons.Default.PhotoCamera,
                title = "دوربین",
                sub = "عکس تازه بگیر",
                dark = true,
                modifier = Modifier.weight(1f)
            ) {
                try {
                    val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                    val file = File(dir, "cap_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                    cameraUri = uri
                    takePicture.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    vm.message = "برنامه‌ی دوربین پیدا نشد"
                }
            }
            StartCard(
                icon = Icons.Default.PhotoLibrary,
                title = "گالری",
                sub = "از عکس‌هایت انتخاب کن",
                dark = false,
                modifier = Modifier.weight(1f)
            ) {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "قالب PNG و فونت دلخواه‌ات را داخل ویرایشگر اضافه کن.",
            fontSize = 12.sp,
            color = Palette.Muted
        )
    }
}

/** کارت بلند با گوشه‌های بسیار گرد، مثل کارت «اتاق‌ها» در تصویر نمونه */
@Composable
private fun StartCard(
    icon: ImageVector,
    title: String,
    sub: String,
    dark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(36.dp)
    val bg = if (dark) Brush.verticalGradient(listOf(Palette.BronzeLight, Palette.Bronze))
    else Brush.verticalGradient(listOf(Palette.Cream, Palette.Sand))
    Column(
        modifier
            .aspectRatio(0.66f)
            .softShadow(shape, 12.dp)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (dark) Color.White else Palette.Bronze),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (dark) Palette.Gold else Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Column {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = if (dark) Color.White else Palette.Ink
            )
            Spacer(Modifier.height(2.dp))
            Text(
                sub,
                fontSize = 12.sp,
                color = if (dark) Color(0xCCFFFFFF) else Palette.Muted
            )
        }
    }
}
