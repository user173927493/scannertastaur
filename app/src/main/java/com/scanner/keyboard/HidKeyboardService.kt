package com.scanner.keyboard

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.concurrent.Executors

class HidKeyboardService : Service() {

    companion object {
        private const val TAG = "HidKeyboardService"
        const val ACTION_STATUS = "com.scanner.keyboard.STATUS"
        const val EXTRA_STATUS = "status"
        const val CHANNEL_ID = "scanner_keyboard_channel"

        // Standard USB HID Keyboard Descriptor
        private val KEYBOARD_HID_DESCRIPTOR = byteArrayOf(
            0x05.toByte(), 0x01.toByte(), // Usage Page: Generic Desktop Controls
            0x09.toByte(), 0x06.toByte(), // Usage: Keyboard
            0xA1.toByte(), 0x01.toByte(), // Collection: Application
            0x05.toByte(), 0x07.toByte(), //   Usage Page: Keyboard/Keypad
            0x19.toByte(), 0xE0.toByte(), //   Usage Minimum: Keyboard Left Control
            0x29.toByte(), 0xE7.toByte(), //   Usage Maximum: Keyboard Right GUI
            0x15.toByte(), 0x00.toByte(), //   Logical Minimum: 0
            0x25.toByte(), 0x01.toByte(), //   Logical Maximum: 1
            0x75.toByte(), 0x01.toByte(), //   Report Size: 1
            0x95.toByte(), 0x08.toByte(), //   Report Count: 8
            0x81.toByte(), 0x02.toByte(), //   Input: Data, Variable, Absolute (Modifier Keys)
            0x95.toByte(), 0x01.toByte(), //   Report Count: 1
            0x75.toByte(), 0x08.toByte(), //   Report Size: 8
            0x81.toByte(), 0x01.toByte(), //   Input: Constant (Reserved)
            0x95.toByte(), 0x06.toByte(), //   Report Count: 6
            0x75.toByte(), 0x08.toByte(), //   Report Size: 8
            0x15.toByte(), 0x00.toByte(), //   Logical Minimum: 0
            0x25.toByte(), 0x65.toByte(), //   Logical Maximum: 101
            0x05.toByte(), 0x07.toByte(), //   Usage Page: Keyboard/Keypad
            0x19.toByte(), 0x00.toByte(), //   Usage Minimum: 0
            0x29.toByte(), 0x65.toByte(), //   Usage Maximum: 101
            0x81.toByte(), 0x00.toByte(), //   Input: Data, Array (Key Array)
            0xC0.toByte()                  // End Collection
        )

        // HID Keycodes für ASCII-Zeichen (US-Layout)
        // Pair: (keycode, needsShift)
        private val ASCII_TO_HID = mapOf(
            ' ' to Pair(0x2C, false),
            '!' to Pair(0x1E, true),
            '"' to Pair(0x1F, true),
            '#' to Pair(0x20, true),
            '$' to Pair(0x21, true),
            '%' to Pair(0x22, true),
            '&' to Pair(0x24, true),
            '\'' to Pair(0x34, false),
            '(' to Pair(0x26, true),
            ')' to Pair(0x27, true),
            '*' to Pair(0x25, true),
            '+' to Pair(0x2E, true),
            ',' to Pair(0x36, false),
            '-' to Pair(0x2D, false),
            '.' to Pair(0x37, false),
            '/' to Pair(0x38, false),
            '0' to Pair(0x27, false),
            '1' to Pair(0x1E, false),
            '2' to Pair(0x1F, false),
            '3' to Pair(0x20, false),
            '4' to Pair(0x21, false),
            '5' to Pair(0x22, false),
            '6' to Pair(0x23, false),
            '7' to Pair(0x24, false),
            '8' to Pair(0x25, false),
            '9' to Pair(0x26, false),
            ':' to Pair(0x33, true),
            ';' to Pair(0x33, false),
            '<' to Pair(0x36, true),
            '=' to Pair(0x2E, false),
            '>' to Pair(0x37, true),
            '?' to Pair(0x38, true),
            '@' to Pair(0x1F, true),
            'A' to Pair(0x04, true),
            'B' to Pair(0x05, true),
            'C' to Pair(0x06, true),
            'D' to Pair(0x07, true),
            'E' to Pair(0x08, true),
            'F' to Pair(0x09, true),
            'G' to Pair(0x0A, true),
            'H' to Pair(0x0B, true),
            'I' to Pair(0x0C, true),
            'J' to Pair(0x0D, true),
            'K' to Pair(0x0E, true),
            'L' to Pair(0x0F, true),
            'M' to Pair(0x10, true),
            'N' to Pair(0x11, true),
            'O' to Pair(0x12, true),
            'P' to Pair(0x13, true),
            'Q' to Pair(0x14, true),
            'R' to Pair(0x15, true),
            'S' to Pair(0x16, true),
            'T' to Pair(0x17, true),
            'U' to Pair(0x18, true),
            'V' to Pair(0x19, true),
            'W' to Pair(0x1A, true),
            'X' to Pair(0x1B, true),
            'Y' to Pair(0x1C, true),
            'Z' to Pair(0x1D, true),
            'a' to Pair(0x04, false),
            'b' to Pair(0x05, false),
            'c' to Pair(0x06, false),
            'd' to Pair(0x07, false),
            'e' to Pair(0x08, false),
            'f' to Pair(0x09, false),
            'g' to Pair(0x0A, false),
            'h' to Pair(0x0B, false),
            'i' to Pair(0x0C, false),
            'j' to Pair(0x0D, false),
            'k' to Pair(0x0E, false),
            'l' to Pair(0x0F, false),
            'm' to Pair(0x10, false),
            'n' to Pair(0x11, false),
            'o' to Pair(0x12, false),
            'p' to Pair(0x13, false),
            'q' to Pair(0x14, false),
            'r' to Pair(0x15, false),
            's' to Pair(0x16, false),
            't' to Pair(0x17, false),
            'u' to Pair(0x18, false),
            'v' to Pair(0x19, false),
            'w' to Pair(0x1A, false),
            'x' to Pair(0x1B, false),
            'y' to Pair(0x1C, false),
            'z' to Pair(0x1D, false),
            '\n' to Pair(0x28, false), // Enter
            '\t' to Pair(0x2B, false)  // Tab
        )
    }

    private val binder = LocalBinder()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var hidDevice: BluetoothHidDevice? = null
    var connectedHost: BluetoothDevice? = null
        private set
    var isRegistered = false
        private set

    private val executor = Executors.newSingleThreadExecutor()

    inner class LocalBinder : Binder() {
        fun getService(): HidKeyboardService = this@HidKeyboardService
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered")
            isRegistered = registered
            broadcastStatus(if (registered) "Bereit – warte auf PC-Verbindung..." else "Nicht registriert")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    broadcastStatus("Verbunden mit: ${device?.name ?: device?.address}")
                    Log.d(TAG, "Connected to ${device?.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    broadcastStatus("Verbindung getrennt")
                    Log.d(TAG, "Disconnected")
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.replyReport(device, type, id, ByteArray(8))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification("Scanner Keyboard läuft"))

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        val sdpRecord = BluetoothHidDeviceAppSdpSettings(
            "Scanner Keyboard",
            "Barcode Scanner HID Keyboard",
            "Honeywell EDA52",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            KEYBOARD_HID_DESCRIPTOR
        )

        bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            @SuppressLint("MissingPermission")
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidDevice = proxy as BluetoothHidDevice
                    hidDevice?.registerApp(sdpRecord, null, null, executor, hidCallback)
                    Log.d(TAG, "HID Profile connected, app registered")
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hidDevice = null
                Log.d(TAG, "HID Profile disconnected")
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    override fun onBind(intent: Intent): IBinder = binder

    @SuppressLint("MissingPermission")
    fun sendString(text: String) {
        val device = connectedHost ?: run {
            Log.w(TAG, "Kein Gerät verbunden")
            return
        }
        executor.execute {
            for (char in text) {
                val hid = ASCII_TO_HID[char] ?: continue
                val keycode = hid.first
                val shift = hid.second

                // Taste drücken
                val pressReport = ByteArray(8)
                pressReport[0] = if (shift) 0x02.toByte() else 0x00.toByte() // Modifier
                pressReport[2] = keycode.toByte()
                hidDevice?.sendReport(device, 0, pressReport)

                // Taste loslassen
                Thread.sleep(20)
                hidDevice?.sendReport(device, 0, ByteArray(8))
                Thread.sleep(20)
            }
            // Enter am Ende senden
            val enterPress = ByteArray(8)
            enterPress[2] = 0x28.toByte()
            hidDevice?.sendReport(device, 0, enterPress)
            Thread.sleep(20)
            hidDevice?.sendReport(device, 0, ByteArray(8))
        }
    }

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_STATUS).putExtra(EXTRA_STATUS, status)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Scanner Keyboard", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Scanner Keyboard")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .build()

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        hidDevice?.unregisterApp()
        super.onDestroy()
    }
}
