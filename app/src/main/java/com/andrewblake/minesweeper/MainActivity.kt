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

        // populate tiles with mine and number blocks
        var mineCount = 35
        for (i in 0 until mineCount) {
            // randomly place mines
            val x = (0 until width).random()
            val y = (0 until height).random()
            // if the tile is not already a mine, make it a mine
            if (!tileIsMine[y][x]) {
                tileIsMine[y][x] = true
                mineCount--
            }
        }
        // populate tiles with number blocks, find all mines and increment nearby tiles
        for (i in 0 until height) {
            for (j in 0 until width) {
                if (tileIsMine[i][j]) {
                    // increment nearby tiles
                    for (k in -1..1) {
                        for (l in -1..1) {
                            if (i + k in 0 until height && j + l in 0 until width) {
                                if (!tileIsMine[i + k][j + l]) {
                                    tileNearbyMines[i + k][j + l]++
                                }
                            }
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
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.primary,
                                ),
                                title = {
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
                                navigationIcon = {
                                    IconButton(onClick = { }) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Localized description"
                                        )
                                    }
                                },
                                actions = {
                                    Text("🚩 35")
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
                            PlayGrid(width, height, isDigging)
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun PlayGrid(x: Int, y: Int, isDigging: Boolean) {

    var tileIsDug = remember {
        Array(y) {
            Array(x) {
                mutableStateOf(false)
            }
        }
    }

    val tileIsFlagged = remember {
        Array(y) {
            Array(x) {
                mutableStateOf(false)
            }
        }
    }

    Column {
        for (i in 0 until y) {
            Row {
                for (j in 0 until x) {
                    Tile(
                        x = j,
                        y = i,
                        isDug = tileIsDug[i][j],
                        text = {
                            if (!tileIsDug[i][j].value) {
                                if (tileIsFlagged[i][j].value) {
                                    Text(text = "🚩")
                                } else {
                                    Text(text = " ")
                                }
                            } else {
                                if (tileIsMine[i][j]) {
                                    Text(text = "💣")
                                } else if (tileNearbyMines[i][j] == 0) {
                                    Text(text = " ")
                                } else {
                                    Text(text = tileNearbyMines[i][j].toString())
                                }
                            }
                        }
                    ) {
                        if (isDigging) {
                            if (!tileIsFlagged[i][j].value) {
                                tileIsDug = digTiles(j, i, tileIsDug)
                            }
                        } else {
                            tileIsFlagged[i][j].value = !tileIsFlagged[i][j].value
                        }
                    }
                }
            }
        }
    }
}

@Composable
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

fun digTiles(x: Int, y: Int, tileIsDug: Array<Array<MutableState<Boolean>>>): Array<Array<MutableState<Boolean>>> {
    var newtileIsDug = tileIsDug
    return if (tileIsDug[y][x].value) {
        newtileIsDug
    } else {
        tileIsDug[y][x].value = true
        if (tileNearbyMines[y][x] == 0 && !tileIsMine[y][x]) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (y + i in 0 until height && x + j in 0 until width) {
                        newtileIsDug = digTiles(x + j, y + i, newtileIsDug) // recursively dig nearby tiles
                    }
                }
            }
        }
        tileIsDug
    }
}
