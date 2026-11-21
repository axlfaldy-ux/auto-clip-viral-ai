package com.autoclip.viralai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.autoclip.viralai.ui.AutoClipApp
import com.autoclip.viralai.ui.theme.AutoClipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AutoClipTheme { AutoClipApp() } }
    }
}
