package com.andrewblake.minesweeper

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.core.content.edit

class MinesweeperViewModel(application: Application) : AndroidViewModel(application) {
    private val _minesweeperUiState = MutableStateFlow(MinesweeperUiState())
    val minesweeperUiState: StateFlow<MinesweeperUiState> = _minesweeperUiState.asStateFlow()
    
    private val sharedPreferences: SharedPreferences = application.getSharedPreferences(
        "minesweeper_preferences", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    init {
        loadPresets()
    }

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

        // Record the start time when the board is initialized
        _minesweeperUiState.update {
            it.copy(gameStartTime = System.currentTimeMillis())
        }
        
        updateTileList(tiles)
        updateState(0)
        digTile(X, Y)
    }

    fun newGame(height: Int, width: Int, mines: Int, presetName: String? = null) {
        _minesweeperUiState.update {
            it.copy(
                height = height, 
                width = width, 
                mines = mines,
                currentPresetName = presetName
            )
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
        // Check if all non-mine tiles are dug
        val win = minesweeperUiState.value.tiles.flatten().all { tile ->
            tile.isMine || tile.isDug // tile is either a mine or is dug
        }
        if (win) {
            // Calculate completion time
            val currentTime = System.currentTimeMillis()
            val elapsedTimeSeconds = ((currentTime - minesweeperUiState.value.gameStartTime) / 1000).toInt()
            
            // Check if this game was played with a preset
            val presetName = minesweeperUiState.value.currentPresetName
            if (presetName != null) {
                updateBestTime(presetName, elapsedTimeSeconds)
            }
            
            updateState(1)
        }
    }

    private fun updateBestTime(presetName: String, timeSeconds: Int) {
        val updatedPresets = minesweeperUiState.value.presets.map { preset ->
            if (preset.name == presetName) {
                // Update only if it's a new best time or there was no previous best time
                if (preset.bestTimeSeconds == null || timeSeconds < preset.bestTimeSeconds) {
                    preset.copy(bestTimeSeconds = timeSeconds)
                } else {
                    preset
                }
            } else {
                preset
            }
        }
        
        // Save updated presets to SharedPreferences
        val presetsJson = gson.toJson(updatedPresets)
        sharedPreferences.edit { putString("difficulty_presets", presetsJson) }
        
        // Update UI state
        _minesweeperUiState.update {
            it.copy(presets = updatedPresets)
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

    // Presets functions
    private fun loadPresets() {
        val presetsJson = sharedPreferences.getString("difficulty_presets", null)
        if (presetsJson != null) {
            val type = object : TypeToken<List<DifficultyPreset>>() {}.type
            val loadedPresets = gson.fromJson<List<DifficultyPreset>>(presetsJson, type)
            _minesweeperUiState.update {
                it.copy(presets = loadedPresets)
            }
        }
    }

    fun savePreset(name: String, width: Int, height: Int, mines: Int) {
        // Find existing preset to preserve best time if it exists
        val existingPreset = _minesweeperUiState.value.presets.find { it.name == name }
        val bestTime = existingPreset?.bestTimeSeconds
        
        val newPreset = DifficultyPreset(name, width, height, mines, bestTime)
        val updatedPresets = _minesweeperUiState.value.presets.toMutableList()
        
        // Replace existing preset with same name or add new one
        val existingIndex = updatedPresets.indexOfFirst { it.name == name }
        if (existingIndex >= 0) {
            updatedPresets[existingIndex] = newPreset
        } else {
            updatedPresets.add(newPreset)
        }
        
        // Update SharedPreferences
        val presetsJson = gson.toJson(updatedPresets)
        sharedPreferences.edit { putString("difficulty_presets", presetsJson) }
        
        // Update UI state
        _minesweeperUiState.update {
            it.copy(presets = updatedPresets)
        }
    }

    fun deletePreset(name: String) {
        val updatedPresets = _minesweeperUiState.value.presets.filter { it.name != name }
        
        // Update SharedPreferences
        val presetsJson = gson.toJson(updatedPresets)
        sharedPreferences.edit { putString("difficulty_presets", presetsJson) }
        
        // Update UI state
        _minesweeperUiState.update {
            it.copy(presets = updatedPresets)
        }
    }
}
