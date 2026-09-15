package com.airmouse.diagnostics

import com.airmouse.diagnostics.hid.PointerAccumulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PointerAccumulatorTest {

    @Test
    fun `fast drag emits integer chunk immediately`() {
        val acc = PointerAccumulator()
        // 40 dp x 2.5 = 100 counts
        val chunk = acc.add(40f, 0f)
        assertNotNull(chunk)
        assertEquals(100, chunk!!.first)
        assertEquals(0, chunk.second)
    }

    @Test
    fun `slow drag carries fraction until 1 count accumulates`() {
        val acc = PointerAccumulator()
        // 0.1 dp x 2.5 = 0.25 counts → nothing yet
        assertNull(acc.add(0.1f, 0f))
        // +0.3 dp → total 1.0 counts → emits 1 and keeps 0 remainder
        val chunk = acc.add(0.3f, 0f)
        assertNotNull(chunk)
        assertEquals(1, chunk!!.first)
        // following micro-move restarts from zero carry
        assertNull(acc.add(0.1f, 0f))
    }

    @Test
    fun `negative movement emits negative counts`() {
        val acc = PointerAccumulator()
        val chunk = acc.add(-10f, -4f)
        assertEquals(-25, chunk!!.first)
        assertEquals(-10, chunk.second)
    }

    @Test
    fun `overflowing movement clamps to int8 hid range`() {
        val acc = PointerAccumulator()
        val chunk = acc.add(200f, -200f) // 500 counts each way
        assertEquals(127, chunk!!.first)
        assertEquals(-127, chunk.second)
    }

    @Test
    fun `reset drops pending carry`() {
        val acc = PointerAccumulator()
        assertNull(acc.add(0.2f, 0.2f))
        acc.reset()
        assertNull(acc.add(0.2f, 0.2f)) // carry was discarded, still below 1 count
    }
}