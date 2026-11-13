package com.rsdgt.mihon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rsdgt.mihon.ui.navigation.AppNavigation
import com.rsdgt.mihon.ui.theme.MihonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MihonTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    MihonTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation()
        }
    }
}
