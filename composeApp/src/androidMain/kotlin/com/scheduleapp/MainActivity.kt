package com.scheduleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.scheduleapp.core.database.initDatabaseContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDatabaseContext(this)
        com.scheduleapp.core.platform.initPlatformContext(this)
        setContent {
            App()
        }
    }
}
