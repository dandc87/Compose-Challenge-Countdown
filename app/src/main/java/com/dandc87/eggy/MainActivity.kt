/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dandc87.eggy

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandc87.eggy.ui.theme.MyTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                MyApp()
            }
        }
    }
}

// Start building your app here!
@Composable
fun MyApp(
    tickSpacing: Dp = 32.dp,
    millisPerTick: Int = 1000 * 60,
) {
    val tickSpacingPx = with(LocalDensity.current) { tickSpacing.toPx() }
    val countdownMillis = remember { mutableStateOf(0) }
    val currentCountdown = remember { mutableStateOf<Job?>(null) }

    val dragState = rememberDraggableState(
        onDelta = {
            val deltaInTicks = (it / tickSpacingPx)
            val deltaInMillis = millisPerTick * deltaInTicks
            countdownMillis.value = countdownMillis.value + deltaInMillis.roundToInt()
        }
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.app_name))
                },
            )
        },
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        reverseDirection = true,
                        startDragImmediately = true,
                        onDragStarted = {
                            currentCountdown.value?.cancel()
                        },
                        onDragStopped = { v ->
                            currentCountdown.value = launch {
                                val startTime = withFrameMillis { it }
                                val startCountdown = countdownMillis.value
                                do {
                                    val delta = withFrameMillis { it } - startTime
                                    countdownMillis.value = (startCountdown - delta.toInt())
                                        .coerceAtLeast(0)
                                } while (countdownMillis.value > 0)
                            }
                        },
                    ),
            ) {
                EggTimer(
                    tickSpacing = tickSpacing,
                    millisPerTick = millisPerTick,
                    milliseconds = countdownMillis.value,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                )
                Icon(
                    imageVector = Icons.Default.ArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colors.secondary,
                )
                Text(
                    text = formatTime(countdownMillis.value.coerceAtLeast(0)),
                    style = MaterialTheme.typography.h4,
                )
            }
        }
    )
}

private fun formatTime(durationMillis: Int): String {
    val minutes = durationMillis / 60_000
    val seconds = (durationMillis % 60_000) / 1000
    val millis = durationMillis % 60_000 % 1000
    return String.format("%d:%02d:%03d", minutes, seconds, millis)
}

@Composable
private fun EggTimer(
    milliseconds: Int,
    millisPerTick: Int,
    tickSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    val yDelta: Dp = (tickSpacing * milliseconds / millisPerTick)
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd,
    ) {
        val midHeight = maxHeight / 2
        val centeredYDelta = midHeight - yDelta
        val offset = centeredYDelta - (tickSpacing / 2)
        val numTicks: Int = ceil(maxHeight / tickSpacing).roundToInt()
        val firstTick: Float = if (centeredYDelta >= 0.dp) {
            0f
        } else {
            -centeredYDelta / tickSpacing
        }
        val invisibleTicks = tickSpacing * floor(firstTick)
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .wrapContentHeight(unbounded = true, align = Alignment.Top)
                .offset(y = offset + invisibleTicks)
        ) {
            repeat(numTicks + 1) { index ->
                val tick = index + floor(firstTick).roundToInt()
                if (tick % 5 == 0) {
                    BigTick(
                        name = tick.toString(),
                        spacing = tickSpacing,
                    )
                } else {
                    SmallTick(spacing = tickSpacing)
                }
            }
        }
    }
}

@Composable
private fun BigTick(
    name: String,
    spacing: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.requiredHeight(spacing),
    ) {
        Text(text = name)
        Icon(imageVector = Icons.Default.ArrowRight, contentDescription = null)
    }
}

@Composable
private fun SmallTick(
    spacing: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.requiredHeight(spacing),
    ) {
        Icon(imageVector = Icons.Default.Remove, contentDescription = null)
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        MyApp()
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        MyApp()
    }
}
