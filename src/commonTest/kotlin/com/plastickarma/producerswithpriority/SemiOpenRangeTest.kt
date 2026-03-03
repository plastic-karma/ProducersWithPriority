package com.plastickarma.producerswithpriority

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemiOpenRangeTest {
    @Test
    fun contains_is_inclusive_at_start() {
        assertTrue(0.0 in SemiOpenRange(0.0, 10.0))
    }

    @Test
    fun contains_is_exclusive_at_end() {
        assertFalse(10.0 in SemiOpenRange(0.0, 10.0))
    }

    @Test
    fun contains_value_in_middle() {
        assertTrue(5.0 in SemiOpenRange(0.0, 10.0))
    }

    @Test
    fun contains_value_below_start() {
        assertFalse(-1.0 in SemiOpenRange(0.0, 10.0))
    }

    @Test
    fun split_produces_correct_sub_ranges() {
        val (lower, upper) = SemiOpenRange(0.0, 10.0).split(4.0)
        assertEquals(SemiOpenRange(0.0, 4.0), lower)
        assertEquals(SemiOpenRange(4.0, 10.0), upper)
    }

    @Test
    fun split_at_boundary_throws() {
        assertFails { SemiOpenRange(0.0, 10.0).split(10.0) }
    }

    @Test
    fun split_outside_range_throws() {
        assertFails { SemiOpenRange(0.0, 10.0).split(15.0) }
    }

    @Test
    fun until_infix_creates_semi_open_range() {
        assertEquals(SemiOpenRange(0.0, 10.0), 0.0 until 10.0)
    }
}
