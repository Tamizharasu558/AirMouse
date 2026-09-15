package com.airmouse.diagnostics

import com.airmouse.diagnostics.diagnostics.CapabilityChecks
import com.airmouse.diagnostics.diagnostics.CheckStatus
import org.junit.Assert.*
import org.junit.Test

/** Tests the real production decision logic (CapabilityChecks), not a copy of it. */
class CapabilityChecksTest {

    @Test
    fun gyro_passesWhenPresent_failsWhenMissing() {
        assertEquals(CheckStatus.PASS, CapabilityChecks.gyro(true).status)
        assertEquals(CheckStatus.FAIL, CapabilityChecks.gyro(false).status)
    }

    @Test
    fun accel_passesWhenPresent_failsWhenMissing() {
        assertEquals(CheckStatus.PASS, CapabilityChecks.accel(true).status)
        assertEquals(CheckStatus.FAIL, CapabilityChecks.accel(false).status)
    }

    @Test
    fun gravity_warnsWhenMissing() {
        assertEquals(CheckStatus.PASS, CapabilityChecks.gravity(true).status)
        assertEquals(CheckStatus.WARN, CapabilityChecks.gravity(false).status)
    }

    @Test
    fun magnetometer_warnsWhenMissing() {
        assertEquals(CheckStatus.PASS, CapabilityChecks.magnetometer(true).status)
        assertEquals(CheckStatus.WARN, CapabilityChecks.magnetometer(false).status)
    }

    @Test
    fun bluetooth_failsWithoutAdapter_warnsIfDisabled_passesIfReady() {
        assertEquals(CheckStatus.FAIL, CapabilityChecks.bluetooth(false, false).status)
        assertEquals(CheckStatus.WARN, CapabilityChecks.bluetooth(true, false).status)
        assertEquals(CheckStatus.PASS, CapabilityChecks.bluetooth(true, true).status)
    }

    @Test
    fun ir_unknownWhenServiceMissing_warnsWhenNoEmitter_passesWithEmitter() {
        assertEquals(CheckStatus.UNKNOWN, CapabilityChecks.ir(false, false).status)
        assertEquals(CheckStatus.WARN, CapabilityChecks.ir(false, true).status)
        assertEquals(CheckStatus.PASS, CapabilityChecks.ir(true, true).status)
    }

    @Test
    fun hidSupport_requiresApi28AndBluetooth() {
        val (ok1, reason1) = CapabilityChecks.hidSupport(27, true)
        assertFalse(ok1); assertNotNull(reason1)

        val (ok2, reason2) = CapabilityChecks.hidSupport(28, false)
        assertFalse(ok2); assertNotNull(reason2)

        val (ok3, reason3) = CapabilityChecks.hidSupport(34, true)
        assertTrue(ok3); assertNull(reason3)
    }
}
