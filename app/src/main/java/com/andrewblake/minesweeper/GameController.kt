package com.andrewblake.minesweeper

class GameController(width: Int, height: Int, mines: Int) {
    // The grid of tiles. 0 = no nearby mines, 1-8 = number of nearby mines, 9 = mine
    private var tiles: Array<Array<Int>> = Array(height) { Array(width) {0} }

    init {
        // populate tiles with mine and number blocks
        var mineCount = mines
        for (i in 0 until mineCount) {
            // randomly place mines
            val x = (0 until width).random()
            val y = (0 until height).random()
            // if the tile is not already a mine, make it a mine
            if (tiles[y][x] != 9) {
                tiles[y][x] = 9
                mineCount--
            }
        }
        // populate tiles with number blocks, find all mines and increment nearby tiles
        for (i in 0 until height) {
            for (j in 0 until width) {
                if (tiles[i][j] == 9) {
                    // increment nearby tiles
                    for (k in -1..1) {
                        for (l in -1..1) {
                            if (i + k in 0 until height && j + l in 0 until width) {
                                if (tiles[i + k][j + l] != 9) {
                                    tiles[i + k][j + l]++
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getTile(x: Int, y: Int): Int {
        return tiles[y][x]
    }

    fun getTileText(x: Int, y: Int): String {
        return when (tiles[y][x]) {
            0 -> ""
            9 -> "💣"
            else -> tiles[y][x].toString()
        }
    }

}