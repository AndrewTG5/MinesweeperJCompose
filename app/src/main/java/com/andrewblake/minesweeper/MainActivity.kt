package com.andrewblake.minesweeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.andrewblake.minesweeper.ui.theme.MinesweeperTheme

private const val height: Int = 20
private const val width: Int = 10
private const val mines: Int = 35

private var tileIsMine: Array<Array<Boolean>> =
    Array(height) {
        Array(width) {
            false
        }
    }

private var tileNearbyMines: Array<Array<Int>> =
    Array(height) {
        Array(width) {
            0
        }
    }

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // reset tileIsMine and tileNearbyMines, for when the user presses the refresh button
        tileIsMine =
            Array(height) {
                Array(width) {
                    false
                }
            }
        tileNearbyMines =
            Array(height) {
                Array(width) {
                    0
                }
            }

        // populate tiles with mine and number blocks
        var mineCount = mines
        for (i in 0 until mineCount) {
            // randomly place mines
            val x = (0 until width).random()
            val y = (0 until height).random()
            // if the tile is not already a mine, make it a mine
            if (!tileIsMine[y][x]) {
                tileIsMine[y][x] = true
                mineCount--
                // increment nearby tiles
                for (k in -1..1) {
                    for (l in -1..1) {
                        if (y + k in 0 until height && x + l in 0 until width) {
                            tileNearbyMines[y + k][x + l]++
                        }
                    }
                }
            }
        }

        setContent {
            MinesweeperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isDigging by remember { mutableStateOf(true) }
                    var flagsPlaced by remember { mutableStateOf(0) }
                    Scaffold(
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
                                            checked = isDigging,
                                            onCheckedChange = {
                                                isDigging = it
                                            }
                                        )
                                        Text("⛏️")
                                    }
                                },
                                navigationIcon = { // refresh button, to start a new game
                                    IconButton(onClick = {
                                        val mIntent = intent
                                        finish() // restart activity to make a new game
                                        startActivity(mIntent)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "New game"
                                        )
                                    }
                                },
                                actions = {
                                    val remainingMines = mines - flagsPlaced // display remaining mines
                                    Text("🚩 $remainingMines")
                                },
                                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                            )
                        },
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            var tileIsDug = remember {
                                Array(height) {
                                    Array(width) {
                                        mutableStateOf(false)
                                    }
                                }
                            }

                            val tileIsFlagged = remember {
                                Array(height) {
                                    Array(width) {
                                        mutableStateOf(false)
                                    }
                                }
                            }

                            var showDialog by remember { // 0 = none, 1 = win, 2 = lose
                                mutableStateOf(0)
                            }

                            if (showDialog != 0) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showDialog = 0
                                    },
                                    title = {
                                        Text(
                                            text = if (showDialog == 1) {
                                                "You Win!"
                                            } else {
                                                "You Lose!"
                                            }
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = if (showDialog == 1) {
                                                "You have won the game!"
                                            } else {
                                                "You have lost the game!"
                                            }
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                val mIntent = intent
                                                finish()
                                                startActivity(mIntent)
                                            }
                                        ) {
                                            Text(text = "Play Again")
                                        }
                                    }
                                )
                            }

                            Column {
                                for (i in 0 until height) { // create tiles
                                    Row {
                                        for (j in 0 until width) {
                                            Tile(
                                                x = j,
                                                y = i,
                                                isDug = tileIsDug[i][j],
                                                text = {
                                                    if (!tileIsDug[i][j].value) { // display flag or blank
                                                        if (tileIsFlagged[i][j].value) {
                                                            Text(text = "🚩")
                                                        } else {
                                                            Text(text = " ")
                                                        }
                                                    } else {
                                                        if (tileIsMine[i][j]) { // display mine or number
                                                            Text(text = "💣")
                                                        } else if (tileNearbyMines[i][j] == 0) {
                                                            Text(text = " ")
                                                        } else {
                                                            Text(text = tileNearbyMines[i][j].toString())
                                                        }
                                                    }
                                                }
                                            ) { // Tile onClick, to dig or flag
                                                if (isDigging) {
                                                    if (!tileIsFlagged[i][j].value) {
                                                        tileIsDug = digTiles(j, i, tileIsDug)
                                                    }
                                                } else {
                                                    tileIsFlagged[i][j].value = !tileIsFlagged[i][j].value
                                                    if (tileIsFlagged[i][j].value) {
                                                        flagsPlaced++
                                                    } else {
                                                        flagsPlaced--
                                                    }
                                                }
                                                if (checkLost(tileIsDug)) { // check if lost or won
                                                    showDialog = 2
                                                } else if (checkWin(tileIsFlagged)) {
                                                    showDialog = 1
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
        /**
         * @param x X coordinate of tile in the board
         * @param y Y coordinate of tile in the board
         * @param isDug Whether the tile has been dug or not, from tileIsDug
         * @param text Text to display in the tile, either flag or blank, mine or number
         * @param updateState Function to update the state of the tile, either dig or flag
         */
fun Tile(
    x: Int,
    y: Int,
    isDug: MutableState<Boolean>,
    text: @Composable () -> Unit,
    updateState: () -> Unit
) {
    Button(
        onClick = { updateState() },
        shape = RectangleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if ((x + y) % 2 == 0) if (isDug.value) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary else if (isDug.value) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if ((x + y) % 2 == 0) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.size(35.dp)
    ) {
        text()
    }
}

/**
 * @param x X coordinate of tile to dig in the board
 * @param y Y coordinate of tile to dig in the board
 * @param tileIsDug Array of tiles that have been dug
 * @return Array of tiles that have been dug, to update tileIsDug
 */
fun digTiles(
    x: Int,
    y: Int,
    tileIsDug: Array<Array<MutableState<Boolean>>>
): Array<Array<MutableState<Boolean>>> {
    var newtileIsDug = tileIsDug
    return if (tileIsDug[y][x].value) {
        newtileIsDug
    } else {
        tileIsDug[y][x].value = true

        if (tileIsMine[y][x]) {
            // dig every mine
            for (i in 0 until height) {
                for (j in 0 until width) {
                    if (tileIsMine[i][j]) {
                        newtileIsDug[i][j].value = true
                    }
                }
            }
        } else if (tileNearbyMines[y][x] == 0 && !tileIsMine[y][x]) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (y + i in 0 until height && x + j in 0 until width) {
                        newtileIsDug =
                            digTiles(x + j, y + i, newtileIsDug) // recursively dig nearby tiles
                    }
                }
            }
        }
        tileIsDug
    }
}

/**
 * @param tileIsDug Array of tiles that have been dug
 * @return Whether the user has lost or not (dug a mine)
 */
fun checkLost(tileIsDug: Array<Array<MutableState<Boolean>>>): Boolean {
    for (i in 0 until height) {
        for (j in 0 until width) {
            if (tileIsDug[i][j].value && tileIsMine[i][j]) {
                return true
            }
        }
    }
    return false
}

/**
 * @param tileIsFlagged Array of tiles that have been flagged
 * @return Whether the user has won or not (flagged all mines). If the user has flagged a tile that is not a mine, they have not won.
 */
fun checkWin(tileIsFlagged: Array<Array<MutableState<Boolean>>>): Boolean {
    for (i in 0 until height) {
        for (j in 0 until width) {
            if (tileIsMine[i][j] && !tileIsFlagged[i][j].value || !tileIsMine[i][j] && tileIsFlagged[i][j].value) {
                return false
            }
        }
    }
    return true
}
