package com.example.dragtoreorderwithcomposedemo

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.lang.IndexOutOfBoundsException
import kotlin.math.roundToInt
import kotlin.math.sign

@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun <T> Modifier.dragToReorder(
    item: T,
    itemList: List<T>,
    itemHeight: Int,
    updateSlideState: (item: T, slideState: SlideState) -> Unit,
    onStartDrag: (currIndex: Int) -> Unit = {},
    onStopDrag: (currIndex: Int, destIndex: Int) -> Unit // Call invoked when drag is finished
): Modifier = composed {
// Keep track of the of the vertical drag offset smoothly
    val offsetY = remember { Animatable(0f) }

    pointerInput(Unit) {
        // Wrap in a coroutine scope to use suspend functions for touch events and animation.
        coroutineScope {
            val itemIndex = itemList.indexOf(item)
            // Threshold for when an item should be considered as moved to a new position in the list
            // Needs to be at least a half of the height of the item but this can be modified as needed
            val offsetToSlide = itemHeight / 2
            var numberOfSlidItems = 0
            var previousNumberOfItems: Int
            var listOffset = 0

            val onDragStart = {
                // Interrupt any ongoing animation of other items.
                launch {
                    offsetY.stop()
                }
                onStartDrag(itemIndex)
            }
            val onDragging = { change: PointerInputChange ->

                val verticalDragOffset = offsetY.value + change.positionChange().y

                launch {
                    offsetY.snapTo(verticalDragOffset)
                    val offsetSign = offsetY.value.sign.toInt()

                    previousNumberOfItems = numberOfSlidItems
                    numberOfSlidItems = calculateNumberOfSlidItems(
                        offsetY.value * offsetSign,
                        itemHeight,
                        offsetToSlide,
                        previousNumberOfItems
                    )

                    if (previousNumberOfItems > numberOfSlidItems) {
                        updateSlideState(
                            itemList[itemIndex + previousNumberOfItems * offsetSign],
                            SlideState.NONE
                        )
                    } else if (numberOfSlidItems != 0) {
                        try {
                            updateSlideState(
                                itemList[itemIndex + numberOfSlidItems * offsetSign],
                                if (offsetSign == 1) SlideState.UP else SlideState.DOWN
                            )
                        } catch (e: IndexOutOfBoundsException) {
                            numberOfSlidItems = previousNumberOfItems
                        }
                    }
                    listOffset = numberOfSlidItems * offsetSign
                }
                // Consume the gesture event, not passed to external
                if (change.positionChange() != androidx.compose.ui.geometry.Offset.Zero) change.consume()
            }
            val onDragEnd = {
                launch {
                    offsetY.animateTo(itemHeight * numberOfSlidItems * offsetY.value.sign)
                    onStopDrag(itemIndex, itemIndex + listOffset)
                }
            }
            detectDragGesturesAfterLongPress(
                onDragStart = { onDragStart() },
                onDrag = { change, _ -> onDragging(change) },
                onDragEnd = { onDragEnd() }
            )
        }
    }.offset {
        IntOffset(0, offsetY.value.roundToInt())
    }
}

enum class SlideState { NONE, UP, DOWN }

private fun calculateNumberOfSlidItems(
    offsetY: Float,
    itemHeight: Int,
    offsetToSlide: Int,
    previousNumberOfItems: Int
): Int {
    val numberOfItemsInOffset = (offsetY / itemHeight).toInt()
    val numberOfItemsPlusOffset = ((offsetY + offsetToSlide) / itemHeight).toInt()
    val numberOfItemsMinusOffset = ((offsetY - offsetToSlide - 1) / itemHeight).toInt()

    return when {
        offsetY - offsetToSlide - 1 < 0 -> 0
        numberOfItemsPlusOffset > numberOfItemsInOffset -> numberOfItemsPlusOffset
        numberOfItemsMinusOffset < numberOfItemsInOffset -> numberOfItemsInOffset
        else -> previousNumberOfItems
    }
}