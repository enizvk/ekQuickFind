package com.ekquickfind.app.ui.search

import com.ekquickfind.data.prefs.BarAnimation
import com.ekquickfind.data.prefs.ItemAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveHighlightTest {
    @Test
    fun borderAnimationsReuseTheSearchBarGlowOfTheSameName() {
        assertEquals(BarAnimation.LightSweep, ItemAnimation.LightSweep.borderGlow())
        assertEquals(BarAnimation.ColourPulse, ItemAnimation.ColourPulse.borderGlow())
        assertEquals(BarAnimation.ShimmerRing, ItemAnimation.ShimmerRing.borderGlow())
        assertEquals(BarAnimation.Breathe, ItemAnimation.Breathe.borderGlow())
        assertEquals(BarAnimation.TwinLights, ItemAnimation.TwinLights.borderGlow())
    }

    @Test
    fun settleAndLiftDrawNoBorderGlow() {
        assertNull(ItemAnimation.Settle.borderGlow())
        assertNull(ItemAnimation.Lift.borderGlow())
    }
}
