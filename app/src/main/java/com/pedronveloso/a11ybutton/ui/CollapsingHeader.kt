/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Collapses a header as a list below it scrolls down and brings it back on any scroll up. The
 * header moves alongside the list rather than consuming scroll, so the list is never held back.
 */
@Stable
class CollapsingHeaderState {
  private var heightPx by mutableFloatStateOf(0f)

  /** Between `-height` (fully collapsed) and 0 (fully shown). */
  var offsetPx by mutableFloatStateOf(0f)
    private set

  val nestedScrollConnection =
      object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
          onScroll(available.y)
          return Offset.Zero
        }
      }

  fun onScroll(deltaY: Float) {
    offsetPx = (offsetPx + deltaY).coerceIn(-heightPx, 0f)
  }

  fun expand() {
    offsetPx = 0f
  }

  internal fun updateHeight(px: Float) {
    heightPx = px
    offsetPx = offsetPx.coerceIn(-px, 0f)
  }
}

/** Shrinks this layout's height by the state's collapsed amount, clipping what slides away. */
fun Modifier.collapsing(state: CollapsingHeaderState): Modifier =
    this.clipToBounds().layout { measurable, constraints ->
      val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
      state.updateHeight(placeable.height.toFloat())
      val visibleHeight = (placeable.height + state.offsetPx).roundToInt().coerceAtLeast(0)
      layout(placeable.width, visibleHeight) { placeable.place(0, state.offsetPx.roundToInt()) }
    }
