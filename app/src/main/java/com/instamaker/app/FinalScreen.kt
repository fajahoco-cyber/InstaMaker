package com.instamaker.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinalScreen(vm: EditorViewModel) {
    val ctx = LocalContext.current
    val img = vm.finalImage
    val format = vm.state.format

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", { vm.back() })
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text("پیش‌نمایش نهایی", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Palette.Ink)
                Text(
                    "${format.label} • ${format.w}×${format.h}",
                    fontSize = 11.sp,
                    color = Palette.Muted
                )
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (img == null) {
                CircularProgressIndicator(color = Palette.Gold)
            } else {
                val shape = RoundedCornerShape(24.dp)
                Image(
                    bitmap = img,
                    contentDescription = "پیش‌نمایش نهایی",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .aspectRatio(format.aspect)
                        .softShadow(shape, 14.dp)
                        .clip(shape)
                )
            }
        }

        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoldButton(
                text = "ذخیره در گالری",
                onClick = { vm.save() },
                enabled = img != null,
                icon = Icons.Default.Download,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton(
                    "اینستاگرام", { vm.shareVia(ctx, "com.instagram.android") },
                    Modifier.weight(1f), enabled = img != null, fontSize = 13
                )
                SoftButton(
                    "واتساپ", { vm.shareVia(ctx, "com.whatsapp") },
                    Modifier.weight(1f), enabled = img != null, fontSize = 13
                )
                SoftButton(
                    "تلگرام", { vm.shareVia(ctx, "org.telegram.messenger") },
                    Modifier.weight(1f), enabled = img != null, fontSize = 13
                )
            }

            SoftButton(
                "اشتراک‌گذاری با برنامه‌های دیگر",
                { vm.shareVia(ctx, null) },
                Modifier.fillMaxWidth(),
                icon = Icons.Default.Share,
                enabled = img != null
            )

            TextButton(
                onClick = { vm.newPost() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ساخت پست جدید", color = Palette.Bronze, fontWeight = FontWeight.Medium) }
        }
    }
}
