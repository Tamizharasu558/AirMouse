package com.airmouse.diagnostics.hid

/**
 * HID report map for a 3-button mouse with relative X/Y axes and wheel.
 *
 * Single input report with NO Report ID item: per the HID spec a device with one
 * report may omit the Report ID, so reports go on the wire as 4 raw bytes and
 * BluetoothHidDevice.sendReport() is called with id = 0.
 *
 * Wire format (4 bytes): [buttons bitfield][x int8][y int8][wheel int8]
 */
object HidReportDescriptor {
    const val REPORT_SIZE = 4

    val MOUSE = byteArrayOf(
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x02, // Usage (Mouse)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x09, 0x01, //   Usage (Pointer)
        0xA1.toByte(), 0x00, //   Collection (Physical)
        0x05, 0x09, //     Usage Page (Button)
        0x19, 0x01, //     Usage Minimum (1)
        0x29.toByte(), 0x03, //     Usage Maximum (3)
        0x15, 0x00, //     Logical Minimum (0)
        0x25.toByte(), 0x01, //     Logical Maximum (1)
        0x95.toByte(), 0x03, //     Report Count (3)
        0x75.toByte(), 0x01, //     Report Size (1)
        0x81.toByte(), 0x02, //     Input (Data, Variable, Absolute)
        0x95.toByte(), 0x01, //     Report Count (1)
        0x75.toByte(), 0x05, //     Report Size (5)
        0x81.toByte(), 0x03, //     Input (Constant, Variable, Absolute) - button padding
        0x05, 0x01, //     Usage Page (Generic Desktop)
        0x09, 0x30, //     Usage (X)
        0x09, 0x31, //     Usage (Y)
        0x09, 0x38, //     Usage (Wheel)
        0x15, 0x81.toByte(), //     Logical Minimum (-127)
        0x25.toByte(), 0x7F, //     Logical Maximum (127)
        0x75.toByte(), 0x08, //     Report Size (8)
        0x95.toByte(), 0x03, //     Report Count (3)
        0x81.toByte(), 0x06, //     Input (Data, Variable, Relative)
        0xC0.toByte(), //   End Collection (Physical)
        0xC0.toByte()  // End Collection (Application)
    )
}