/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CollapsingHeaderStateTest {
  private fun stateWithHeight(px: Float) = CollapsingHeaderState().also { it.updateHeight(px) }

  @Test
  fun onScroll_collapsesWhileScrollingDown_andStopsAtHeight() {
    val state = stateWithHeight(100f)

    state.onScroll(-40f)
    assertEquals(-40f, state.offsetPx, 0f)

    state.onScroll(-200f)
    assertEquals(-100f, state.offsetPx, 0f)
  }

  @Test
  fun onScroll_expandsOnAnyScrollUp_andStopsAtZero() {
    val state = stateWithHeight(100f)
    state.onScroll(-100f)

    state.onScroll(15f)
    assertEquals(-85f, state.offsetPx, 0f)

    state.onScroll(500f)
    assertEquals(0f, state.offsetPx, 0f)
  }

  @Test
  fun expand_showsTheFullHeader() {
    val state = stateWithHeight(100f)
    state.onScroll(-100f)

    state.expand()

    assertEquals(0f, state.offsetPx, 0f)
  }

  @Test
  fun updateHeight_keepsOffsetWithinNewBounds() {
    val state = stateWithHeight(100f)
    state.onScroll(-100f)

    state.updateHeight(60f)

    assertEquals(-60f, state.offsetPx, 0f)
  }

  @Test
  fun onScroll_doesNothing_beforeHeightIsKnown() {
    val state = CollapsingHeaderState()

    state.onScroll(-50f)

    assertEquals(0f, state.offsetPx, 0f)
  }
}
