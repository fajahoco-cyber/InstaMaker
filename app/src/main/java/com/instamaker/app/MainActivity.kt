package com.instamaker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    private val vm: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaTheme {
                // رابط کاربری فارسی است، پس راست‌به‌چپ
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    App(vm)
                }
            }
        }
    }
}

@Composable
fun App(vm: EditorViewModel) {
    val snackbar = remember { SnackbarHostState() }

    BackHandler(enabled = vm.screen != Screen.HOME) { vm.back() }

    LaunchedEffect(vm.message) {
        val m = vm.message
        if (m != null) {
            snackbar.showSnackbar(m)
            vm.message = null
        }
    }

    AppBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = Color.Transparent
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (vm.screen) {
                    Screen.HOME -> HomeScreen(vm)
                    Screen.EDITOR -> EditorScreen(vm)
                    Screen.FINAL -> FinalScreen(vm)
                }
                if (vm.busy) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                            .pointerInput(Unit) {}, // لمس‌ها را هنگام پردازش می‌بلعد
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Palette.Gold) }
                }
            }
        }
    }
}
