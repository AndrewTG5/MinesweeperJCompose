package com.andrewblake.minesweeper

enum class State {
    LevelSelect, NewGame, Playing, Won, Lost
}

data class MinesweeperUiState(
    val tiles: List<List<Tile>> = emptyList(),
    val state: State = State.LevelSelect,
    val height: Int = 0,
    val width: Int = 0,
    val mines: Int = 0,
    val presets: List<DifficultyPreset> = emptyList(),
    val gameStartTime: Long = 0,
    val currentPresetName: String? = null
)

data class DifficultyPreset(
    val name: String,
    val width: Int,
    val height: Int,
    val mines: Int,
    val bestTimeSeconds: Int? = null
)
