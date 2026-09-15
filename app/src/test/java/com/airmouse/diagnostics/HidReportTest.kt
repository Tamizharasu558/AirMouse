package com.airmouse.diagnostics

import com.airmouse.diagnostics.hid.HidReportDescriptor
import com.airmouse.diagnostics.hid.MouseReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HidReportTest {

    @Test
    fun `descriptor is a well-formed mouse report map with no report id`() {
        val d = HidReportDescriptor.MOUSE
        // Usage Page (Generic Desktop) then Usage (Mouse)
        assertEquals(0x05, d[0].toInt() and 0xFF)
        assertEquals(0x01, d[1].toInt() and 0xFF)
        assertEquals(0x09, d[2].toInt() and 0xFF)
        assertEquals(0x02, d[3].toInt() and 0xFF)
        // No Report ID item (0x85) anywhere — reports must go out with id = 0
        assertTrue(d.none { it == 0x85.toByte() })
        // Ends with End Collection x2 (Application + Physical)
        assertEquals(0xC0.toByte(), d[d.size - 1])
        assertEquals(0xC0.toByte(), d[d.size - 2])
    }

    @Test
    fun `zero report is 4 bytes of zeros`() {
        val r = MouseReport.release()
        assertEquals(4, r.size)
        assertTrue(r.all { it.toInt() == 0 })
    }

    @Test
    fun `button bits map to byte0`() {
        assertEquals(1, MouseReport.press(MouseReport.BUTTON_LEFT)[0].toInt())
        assertEquals(2, MouseReport.press(MouseReport.BUTTON_RIGHT)[0].toInt())
        assertEquals(
            5,
            MouseReport.press(MouseReport.BUTTON_LEFT or MouseReport.BUTTON_MIDDLE)[0].toInt()
        )
    }

    @Test
    fun `movement encodes int8 clamped deltas`() {
        val r = MouseReport.move(10, -5)
        assertEquals(0, r[0].toInt())
        assertEquals(10, r[1].toInt())
        assertEquals(-5, r[2].toInt())
        // Overflow clamps to the int8 HID range
        val big = MouseReport.move(500, -500)
        assertEquals(127, big[1].toInt())
        assertEquals(-127, big[2].toInt())
    }

    @Test
    fun `wheel amount lands in byte3`() {
        val r = MouseReport.wheel(-1)
        assertEquals(-1, r[3].toInt())
        assertEquals(0, r[1].toInt())
        assertEquals(0, r[2].toInt())
    }
}