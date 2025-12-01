package com.misterjerry.test01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.misterjerry.test01.ui.theme.Test01Theme
import com.misterjerry.test01.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Test01Theme {
                MainScreen()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        com.misterjerry.test01.data.SoundEventBus.isForeground = true
    }

    override fun onPause() {
        super.onPause()
        com.misterjerry.test01.data.SoundEventBus.isForeground = false
    }
}