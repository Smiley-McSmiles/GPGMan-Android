package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Standard Android-style continuous drag Pattern Lock component.
 * Allows drawing a continuous line by dragging across the 3x3 dot grid (nodes 0..8)
 * and automatically triggers [onPatternComplete] when the user lifts their finger.
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 280.dp,
    isError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit,
    onPatternChange: (List<Int>) -> Unit = {},
    activeColor: Color = MaterialTheme.colorScheme.primary,
    errorColor: Color = MaterialTheme.colorScheme.error,
    dotNormalColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    lineColor: Color = activeColor.copy(alpha = 0.85f),
    testTag: String = "pattern_lock_view"
) {
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }

    val currentColor = if (isError) errorColor else activeColor
    val currentLineColor = if (isError) errorColor.copy(alpha = 0.85f) else lineColor

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(sizeDp)
                .pointerInput(isError) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        selectedNodes.clear()
                        currentTouchPos = down.position

                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val dotCenters = calculateDotCenters(canvasWidth, canvasHeight)
                        val hitRadius = canvasWidth / 6f // generous hit target for smooth gestures

                        findNearestDot(down.position, dotCenters, hitRadius)?.let { node ->
                            selectedNodes.add(node)
                            onPatternChange(selectedNodes.toList())
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                break
                            }

                            currentTouchPos = change.position

                            findNearestDot(change.position, dotCenters, hitRadius)?.let { node ->
                                if (!selectedNodes.contains(node)) {
                                    selectedNodes.add(node)
                                    onPatternChange(selectedNodes.toList())
                                }
                            }
                            change.consume()
                        }

                        // Finger lifted (gesture complete)
                        currentTouchPos = null
                        if (selectedNodes.isNotEmpty()) {
                            onPatternComplete(selectedNodes.toList())
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val dotCenters = calculateDotCenters(canvasWidth, canvasHeight)

            val normalDotRadius = 6.dp.toPx()
            val selectedDotRadius = 10.dp.toPx()
            val ringRadius = 24.dp.toPx()
            val strokeWidth = 5.dp.toPx()

            // 1. Draw connecting lines between already selected nodes
            for (i in 0 until selectedNodes.size - 1) {
                val start = dotCenters[selectedNodes[i]]
                val end = dotCenters[selectedNodes[i + 1]]
                drawLine(
                    color = currentLineColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 2. Draw live rubber-band line from last selected node to finger position
            val touch = currentTouchPos
            if (touch != null && selectedNodes.isNotEmpty()) {
                val lastDotCenter = dotCenters[selectedNodes.last()]
                drawLine(
                    color = currentLineColor.copy(alpha = 0.65f),
                    start = lastDotCenter,
                    end = touch,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw the 9 nodes (0 to 8)
            for (index in 0..8) {
                val center = dotCenters[index]
                val isSelected = selectedNodes.contains(index)

                if (isSelected) {
                    // Outer translucent halo ring
                    drawCircle(
                        color = currentColor.copy(alpha = 0.2f),
                        radius = ringRadius,
                        center = center
                    )
                    // Border ring
                    drawCircle(
                        color = currentColor,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Inner filled core
                    drawCircle(
                        color = currentColor,
                        radius = selectedDotRadius,
                        center = center
                    )
                } else {
                    // Unselected subtle dot
                    drawCircle(
                        color = dotNormalColor,
                        radius = normalDotRadius,
                        center = center
                    )
                }
            }
        }
    }
}

/**
 * Calculates (x, y) center coordinates for each of the 9 nodes on a 3x3 grid.
 */
private fun calculateDotCenters(width: Float, height: Float): List<Offset> {
    val centers = ArrayList<Offset>(9)
    val colSpacing = width / 3f
    val rowSpacing = height / 3f
    for (row in 0..2) {
        for (col in 0..2) {
            val cx = (col + 0.5f) * colSpacing
            val cy = (row + 0.5f) * rowSpacing
            centers.add(Offset(cx, cy))
        }
    }
    return centers
}

/**
 * Checks whether a given touch position is within [hitRadius] of any node center.
 */
private fun findNearestDot(pos: Offset, dotCenters: List<Offset>, hitRadius: Float): Int? {
    for (i in dotCenters.indices) {
        val center = dotCenters[i]
        val dx = pos.x - center.x
        val dy = pos.y - center.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= hitRadius) {
            return i
        }
    }
    return null
}
