package com.localdiary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.localdiary.app.ui.DiaryAppRoot
import com.localdiary.app.ui.theme.DiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DiaryApplication).appContainer
        setContent {
            DiaryTheme {
                Surface {
                    DiaryAppRoot(container = container)
                }
            }
        }
    }
}
