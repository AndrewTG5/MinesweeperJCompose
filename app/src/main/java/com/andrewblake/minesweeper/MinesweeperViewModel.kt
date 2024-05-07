package com.andrewblake.minesweeper

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MinesweeperViewModel : ViewModel() {
    private val _minesweeperUiState = MutableStateFlow(MinesweeperUiState())
    val minesweeperUiState: StateFlow<MinesweeperUiState> = _minesweeperUiState.asStateFlow()

    fun initBoard(X: Int, Y: Int) {
        val tiles = MutableList(minesweeperUiState.value.height) { MutableList(minesweeperUiState.value.width) { Tile() } }
        val positions = mutableListOf<Pair<Int, Int>>()

        // Generate all possible positions
        for (x in 0 until minesweeperUiState.value.width) {
            for (y in 0 until minesweeperUiState.value.height) {
                if (!(x in X - 1..X + 1 && y in Y - 1..Y + 1)) { // Exclude tiles within 1 of the first click
                    positions.add(Pair(x, y))
                }
            }
        }

        // Shuffle the positions
        positions.shuffle()

        // Place the mines
        for (i in 0 until minesweeperUiState.value.mines) {
            val (x, y) = positions[i]
            tiles[y][x] = tiles[y][x].copy(isMine = true)

            // Increment nearbyMines for all surrounding tiles
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    if (x + dx in 0 until minesweeperUiState.value.width && y + dy in 0 until minesweeperUiState.value.height) {
                        tiles[y + dy][x + dx] = tiles[y + dy][x + dx].copy(nearbyMines = tiles[y + dy][x + dx].nearbyMines + 1)
                    }
                }
            }
        }

        updateTileList(tiles)
        updateState(0)
        digTile(X, Y)
    }

    fun newGame(height: Int, width: Int, mines: Int) {
        _minesweeperUiState.update {
            it.copy(height = height, width = width, mines = mines)
        }
        updateState(-1)
        updateTileList(List(height) { List(width) { Tile() } })
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
        var newTiles = tiles
        newTiles[y][x] = newTiles[y][x].copy(isDug = true)
        if (tiles[y][x].nearbyMines == 0) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (y + i in 0 until minesweeperUiState.value.height && x + j in 0 until minesweeperUiState.value.width && !tiles[y + i][x + j].isDug && !tiles[y + i][x + j].isFlagged) {
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