package dev.toastbits.composekit.platform.composable

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
actual fun ScrollBarLazyRow(
    modifier: Modifier,
    state: LazyListState,
    show_scrollbar: Boolean,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    horizontalArrangement: Arrangement.Horizontal,
    verticalAlignment: Alignment.Vertical,
    flingBehavior: FlingBehavior,
    userScrollEnabled: Boolean,
    scrollBarColour: Color,
    horizontalAlignment: Alignment.Horizontal,
    reverseScrollBarLayout: Boolean,
    scrollBarSpacing: Dp,
    rowModifier: Modifier,
    content: LazyListScope.() -> Unit
) {
    LazyRow(modifier.then(rowModifier), state, contentPadding, reverseLayout, horizontalArrangement, verticalAlignment, flingBehavior, userScrollEnabled, content)
}
