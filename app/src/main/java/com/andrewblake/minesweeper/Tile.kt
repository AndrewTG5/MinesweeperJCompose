package com.andrewblake.minesweeper

data class Tile(
    val isDug: Boolean = false,
    val isFlagged: Boolean = false,
    val isMine: Boolean = false,
    val nearbyMines: Int = 0,
)
