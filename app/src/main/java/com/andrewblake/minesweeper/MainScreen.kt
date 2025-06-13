package com.andrewblake.minesweeper

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MinesweeperViewModel) {
    val uiState by viewModel.minesweeperUiState.collectAsState()
    var isDigging by remember { mutableStateOf(true) }
    var currentTimeMillis by remember { mutableLongStateOf(0L) }

    // Timer effect
    LaunchedEffect(uiState.gameStartTime, uiState.state) {
        // Only run timer when game is in progress
        if (uiState.gameStartTime > 0 && uiState.state == 0) {
            while (true) {
                currentTimeMillis = System.currentTimeMillis()
                delay(1000) // Update every second
            }
        }
    }

    // Calculate elapsed time
    val elapsedSeconds = remember(currentTimeMillis, uiState.gameStartTime) {
        if (uiState.gameStartTime > 0 && uiState.state == 0) {
            ((currentTimeMillis - uiState.gameStartTime) / 1000).toInt()
        } else if (uiState.state > 0 && uiState.gameStartTime > 0) {
            // Keep the final time if game ended
            ((System.currentTimeMillis() - uiState.gameStartTime) / 1000).toInt()
        } else {
            0
        }
    }

    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { // toggle between digging and flagging
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚩")
                        Switch(
                            modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp),
                            checked = isDigging,
                            onCheckedChange = {
                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                                isDigging = it
                            }
                        )
                        Text("⛏️")
                        // Timer display
                        if (uiState.gameStartTime > 0) {
                            Spacer(modifier = Modifier.width(32.dp))
                            Text(
                                text = formatTime(elapsedSeconds),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                navigationIcon = { // refresh button, to start a new game
                    IconButton(onClick = {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                        viewModel.updateState(-2)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "New game",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    val count = uiState.mines - uiState.tiles.flatten().count { it.isFlagged }
                    Text(modifier = Modifier.padding(end = 16.dp), text = "🚩 $count", color = if (count < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer) // display remaining flags to place
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        },
    ) { innerPadding ->

        if (uiState.state == -2) {
            DifficultyDialog(
                vibrator = vibrator, 
                presets = uiState.presets,
                onConfirm = { height, width, mines, presetName ->
                    viewModel.newGame(height, width, mines, presetName)
                },
                onSavePreset = { name, width, height, mines ->
                    viewModel.savePreset(name, width, height, mines)
                },
                onDeletePreset = { name ->
                    viewModel.deletePreset(name)
                }
            )
        }

        if (uiState.state > 0) {
            AlertDialog(
                onDismissRequest = { viewModel.updateState(0) },
                title = {
                    Text(
                        text = if (uiState.state == 1) {
                            "You Win!"
                        } else {
                            "You Lose!"
                        }
                    )
                },
                text = {
                    Text(
                        text = if (uiState.state == 1) {
                            "You have won the game!"
                        } else {
                            "You have lost the game!"
                        }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateState(-2)
                        }
                    ) {
                        Text(text = "Play Again")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val size = minOf(LocalConfiguration.current.screenWidthDp.dp / (uiState.width + 2), LocalConfiguration.current.screenHeightDp.dp / (uiState.height + 3)) // size of tile. extra tiles for padding
            for (i in 0 until uiState.height) { // create tiles
                Row {
                    for (j in 0 until uiState.width) {
                        Tile(
                            x = j,
                            y = i,
                            isDug = uiState.tiles[i][j].isDug,
                            text = if (uiState.tiles[i][j].isDug) { // display flag or blank
                                if (uiState.tiles[i][j].isMine) { // display mine or number
                                    "💣"
                                } else if (uiState.tiles[i][j].nearbyMines == 0) {
                                    " "
                                } else {
                                    uiState.tiles[i][j].nearbyMines.toString()
                                }
                            } else {
                                if (uiState.tiles[i][j].isFlagged) {
                                    "🚩"
                                } else {
                                    " "
                                }
                            },
                            size = size,
                            onClick = {
                                if (isDigging) {
                                    if (!uiState.tiles[i][j].isFlagged) {
                                        if (uiState.tiles[i][j].isMine) {
                                            vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD).compose())
                                        } else {
                                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                                        }
                                    }
                                    if (uiState.state == -1) {
                                        viewModel.initBoard(j, i)
                                    } else {
                                        viewModel.digTile(j, i)
                                    }
                                } else {
                                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                                    viewModel.flagTile(j, i)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * @param x X coordinate of tile in the board
 * @param y Y coordinate of tile in the board
 * @param isDug Whether the tile has been dug or not
 * @param text String to display in the tile, either flag or blank, mine or number
 * @param onClick Function to update the state of the tile, either dig or flag
 */
@Composable
fun Tile(
    x: Int,
    y: Int,
    isDug: Boolean,
    text: String,
    size: Dp,
    onClick: () -> Unit
) {
    Button(
        onClick = { if (!isDug) onClick() },
        shape = RectangleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if ((x + y) % 2 == 0) if (isDug) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary else if (isDug) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if ((x + y) % 2 == 0) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.size(size)
    ) {
        Text(text)
    }
}

@Composable
fun DifficultyDialog(
    vibrator: Vibrator,
    presets: List<DifficultyPreset>,
    onConfirm: (Int, Int, Int, String?) -> Unit,
    onSavePreset: (String, Int, Int, Int) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    var height by remember { mutableIntStateOf(20) }
    var width by remember { mutableIntStateOf(10) }
    var mines by remember { mutableIntStateOf(35) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var selectedPresetName: String? by remember { mutableStateOf(null) }

    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Width: $width, Height: $height", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                
                Slider(
                    value = width.toFloat(),
                    onValueChange = {
                        if (width != it.toInt()) {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                            width = it.toInt()
                            height = it.toInt() * 2
                            mines = (width*height)/5
                            selectedPresetName = null // Clear selected preset when manually changing values
                        }
                    },
                    valueRange = 5f..15f,
                    steps = 9,
                )
                
                Text("Mines: $mines", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                
                Slider(
                    value = mines.toFloat(),
                    onValueChange = {
                        if (mines != it.toInt()) {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                            mines = it.toInt()
                            selectedPresetName = null // Clear selected preset when manually changing values
                        }
                    },
                    valueRange = ((width*height)/10).toFloat()..((width*height)/2).toFloat(),
                    steps = {
                        val maxMines = (width*height)/2
                        val minMines = (width*height)/10
                        val totalRange = maxMines - minMines
                        val stepSize = when {
                            maxMines > 100 -> 5
                            maxMines > 50 -> 2
                            else -> 1
                        }
                        totalRange / stepSize
                    }()
                )
                
                // Presets section
                if (presets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saved Presets:", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        items(presets) { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    TextButton(
                                        onClick = {
                                            width = preset.width
                                            height = preset.height
                                            mines = preset.mines
                                            selectedPresetName = preset.name
                                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${preset.name} (${preset.width}×${preset.height}, ${preset.mines} mines)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            // Display best time if it exists
                                            preset.bestTimeSeconds?.let { time ->
                                                Text(
                                                    text = formatTime(time),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                IconButton(
                                    onClick = {
                                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                                        onDeletePreset(preset.name)
                                        if (selectedPresetName == preset.name) {
                                            selectedPresetName = null
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete preset",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            showSaveDialog = true
                        }
                    ) {
                        Text("Save Preset")
                    }
                    
                    Button(
                        onClick = {
                            onConfirm(height, width, mines, selectedPresetName)
                        }
                    ) {
                        Text("Start Game")
                    }
                }
            }
        }
    }
    
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSavePreset(presetName, width, height, mines)
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            showSaveDialog = false
                            selectedPresetName = presetName
                            presetName = ""
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper function to format time from seconds to MM:SS format
@Composable
fun formatTime(timeSeconds: Int): String {
    val minutes = timeSeconds / 60
    val seconds = timeSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
