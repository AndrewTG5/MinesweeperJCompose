package com.andrewblake.minesweeper

data class MinesweeperUiState(
    val tiles: List<List<Tile>> = emptyList(),
    val state: Int = 0 // 0 = playing, 1 = won, 2 = lost
)
