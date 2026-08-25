package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.PulsePcNavGraph
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberBackground
                ) {
                    val navController = rememberNavController()
                    PulsePcNavGraph(
                        navController = navController,
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
    }
}
