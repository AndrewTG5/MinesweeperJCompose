package com.andrewblake.minesweeper

data class MinesweeperUiState(
    val tiles: List<List<Tile>> = emptyList(),
    val state: Int = -2, // -2 = level select, -1 = new game, 0 = playing, 1 = won, 2 = lost
    val height: Int = 0,
    val width: Int = 0,
    val mines: Int = 0,
    val presets: List<DifficultyPreset> = emptyList()
)

data class DifficultyPreset(
    val name: String,
    val width: Int,
    val height: Int,
    val mines: Int
)
