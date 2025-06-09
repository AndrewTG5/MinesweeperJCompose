package com.andrewblake.minesweeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.andrewblake.minesweeper.ui.theme.MinesweeperTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MinesweeperViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MinesweeperTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
