package com.example.dragtoreorderwithcomposedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.dragtoreorderwithcomposedemo.ui.theme.DragToReorderWithComposeDemoTheme
import kotlinx.coroutines.launch
import java.util.Collections

private val itemHeightDp = 50.dp
private var itemHeightPx = 0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DragToReorderWithComposeDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyListComposable(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private val myList = buildList {
    for (i in 0..20) {
        add("Item $i")
    }
}.toMutableList()

@Composable
fun MyListComposable(modifier: Modifier) {
    val listState = rememberLazyListState()
    val isPlaced = remember { mutableStateOf(false) }
    val currentIndex = remember { mutableIntStateOf(-1) }
    val destinationIndex = remember { mutableIntStateOf(0) }
    val slideStates = remember {
        mutableStateMapOf<String, SlideState>()
            .apply {
                myList.associateWith { SlideState.NONE }.also { putAll(it) }
            }
    }

    LaunchedEffect(isPlaced.value) {
        if (isPlaced.value) {
            launch {
                if (currentIndex.intValue != destinationIndex.intValue) {
                    Collections.swap(myList, currentIndex.intValue, destinationIndex.intValue)
                    slideStates.apply {
                        myList.associateWith { SlideState.NONE }.also { putAll(it) }
                    }
                }
                isPlaced.value = false
            }
        }
    }

    with(LocalDensity.current) {
        itemHeightPx = itemHeightDp.toPx().toInt()
    }

    LazyColumn(modifier = modifier.fillMaxSize(), state = listState) {
        items(myList.size) { idx ->

            val myItem = myList.getOrNull(idx) ?: return@items

            val slideState = slideStates[myItem] ?: SlideState.NONE

            val verticalTranslation by animateIntAsState(
                targetValue = when (slideState) {
                    SlideState.UP -> -itemHeightPx
                    SlideState.DOWN -> itemHeightPx
                    else -> 0
                },
                label = "drag_to_reorder_vertical_translation"
            )

            key(myItem) {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(itemHeightDp)
                        .padding(horizontal = 12.dp)
                        .dragToReorder(
                            item = myItem,
                            itemList = myList,
                            itemHeight = itemHeightPx,
                            updateSlideState = { param: String, state: SlideState -> slideStates[param] = state },
                            onStartDrag = { index -> currentIndex.intValue = index },
                            onStopDrag = { currIndex: Int, destIndex: Int ->
                                isPlaced.value = true
                                currentIndex.intValue = currIndex
                                destinationIndex.intValue = destIndex
                            })
                        .offset { IntOffset(0, verticalTranslation) }
                ) {
                    Text(
                        text = myItem, modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
