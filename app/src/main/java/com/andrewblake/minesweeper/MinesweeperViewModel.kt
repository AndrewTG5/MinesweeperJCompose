package com.andrewblake.minesweeper

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MinesweeperViewModel : ViewModel() {
    private val _minesweeperUiState = MutableStateFlow(MinesweeperUiState())
    val minesweeperUiState: StateFlow<MinesweeperUiState> = _minesweeperUiState.asStateFlow()

    val HEIGHT: Int = 20
    val WIDTH: Int = 10
    val MINES: Int = 35

    init {
        newGame()
    }

    fun initBoard(X: Int, Y: Int) {
        var minesLeft = MINES // Number of mines left to place
        val tiles = MutableList(HEIGHT) { MutableList(WIDTH) { Tile() } }

        while (minesLeft > 0) {
            var x: Int
            var y: Int

            do {
                x = (0 until WIDTH).random()
                y = (0 until HEIGHT).random()
            } while (x in X - 1..X + 1 && y in Y - 1..Y + 1) // make sure there are no mines within 1 tile of the first click

            if (!tiles[y][x].isMine) { // If the tile is not already a mine
                tiles[y][x] = tiles[y][x].copy(isMine = true)
                minesLeft--
                // Increment nearbyMines for all surrounding tiles
                for (i in -1..1) {
                    for (j in -1..1) {
                        if (i == 0 && j == 0) continue
                        if (x + i in 0 until WIDTH && y + j in 0 until HEIGHT) {
                            tiles[y + j][x + i] = tiles[y + j][x + i].copy(nearbyMines = tiles[y + j][x + i].nearbyMines + 1)
                        }
                    }
                }
            }
        }

        updateTileList(tiles)
        updateState(0)
        digTile(X, Y)
    }


    fun newGame() {
        updateState(-1)
        updateTileList(List(HEIGHT) { List(WIDTH) { Tile() } })
    }

    fun flagTile(x: Int, y: Int) {
        // make a new list with the tile at x, y flagged
        val newList = minesweeperUiState.value.tiles.mapIndexed { i, row ->
            row.mapIndexed { j, tile ->
                if (i == y && j == x) {
                    tile.copy(isFlagged = !tile.isFlagged)
                } else {
                    tile
                }
            }
        }
        updateTileList(newList)
        checkWin()
    }

    fun digTile(x: Int, y: Int) {
        // check if the tile was a mine
        if (minesweeperUiState.value.tiles[y][x].isFlagged) {
            return
        }
        if (minesweeperUiState.value.tiles[y][x].isMine) {
            updateState(2)
            // dig every mine
            val newList = minesweeperUiState.value.tiles.map { row ->
                row.map { tile ->
                    tile.copy(isDug = true)
                }
            }
            updateTileList(newList)
        } else {
            // dig the tile
            val newList = minesweeperUiState.value.tiles.toMutableList().map { it.toMutableList() }
            updateTileList(digTiles(x, y, newList))
            checkWin()
        }
    }

    private fun digTiles(x: Int, y: Int, tiles: List<MutableList<Tile>>): List<MutableList<Tile>> {
        if (tiles[y][x].isDug) {
            return tiles
        }
        var newTiles = tiles
        newTiles[y][x] = newTiles[y][x].copy(isDug = true)
        if (tiles[y][x].nearbyMines == 0) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (y + i in 0 until HEIGHT && x + j in 0 until WIDTH) {
                        newTiles = digTiles(x + j, y + i, newTiles) // recursively dig surrounding tiles
                    }
                }
            }
        }
        return newTiles
    }


    private fun checkWin() {
        // Check if all mines are flagged
        val win = minesweeperUiState.value.tiles.flatten().all { tile ->
            (tile.isMine && tile.isFlagged) || (!tile.isMine && !tile.isFlagged && tile.isDug) // mine is flagged or tile is not a mine and is dug
        }
        if (win) {
            updateState(1)
        }
    }


    private fun updateTileList(newList: List<List<Tile>>) {
        _minesweeperUiState.update {
            it.copy(tiles = newList)
        }
    }

    fun updateState(newState: Int) {
        _minesweeperUiState.update {
            it.copy(state = newState)
        }
    }

}