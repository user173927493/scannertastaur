package com.scanner.keyboard

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scanner.keyboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var hidService: HidKeyboardService? = null
    private var serviceBound = false

    // DataWedge Intent-Aktion (Honeywell Standard)
    companion object {
        const val DW_ACTION = "com.honeywell.aidc.action.ACTION_AIDC_BARCODE_DATA"
        const val DW_EXTRA_DATA = "com.honeywell.aidc.extra.EXTRA_AIDC_BARCODE_DATA"
        // Fallback für ältere DataWedge-Versionen
        const val DW_ACTION_ALT = "com.symbol.datawedge.api.RESULT_ACTION"
        const val DW_EXTRA_ALT = "com.symbol.datawedge.data_string"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            hidService = (binder as HidKeyboardService.LocalBinder).getService()
            serviceBound = true
            updateStatus("Service gestartet – koppele jetzt den PC per Bluetooth")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            serviceBound = false
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra(HidKeyboardService.EXTRA_STATUS) ?: return
            updateStatus(status)
        }
    }

    // DataWedge Scan-Empfänger
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val scannedData = intent?.getStringExtra(DW_EXTRA_DATA)
                ?: intent?.getStringExtra(DW_EXTRA_ALT)
                ?: return

            onBarcodeScanned(scannedData)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Service starten
        val serviceIntent = Intent(this, HidKeyboardService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Bluetooth prüfen
        val bt = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (bt == null || !bt.isEnabled) {
            updateStatus("⚠ Bluetooth ist deaktiviert!")
        }

        // Manuell senden (Test-Button)
        binding.btnSend.setOnClickListener {
            val text = binding.etManual.text.toString()
            if (text.isNotEmpty()) {
                hidService?.sendString(text)
                addLog("Manuell gesendet: $text")
                binding.etManual.text.clear()
            }
        }

        // DataWedge auch über onNewIntent abfangen
        handleScanIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(statusReceiver, IntentFilter(HidKeyboardService.ACTION_STATUS), RECEIVER_NOT_EXPORTED)
        val filter = IntentFilter().apply {
            addAction(DW_ACTION)
            addAction(DW_ACTION_ALT)
        }
        registerReceiver(scanReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statusReceiver)
        unregisterReceiver(scanReceiver)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleScanIntent(intent)
    }

    private fun handleScanIntent(intent: Intent?) {
        val data = intent?.getStringExtra(DW_EXTRA_DATA)
            ?: intent?.getStringExtra(DW_EXTRA_ALT)
            ?: return
        onBarcodeScanned(data)
    }

    private fun onBarcodeScanned(data: String) {
        addLog("Scan: $data")
        if (hidService?.connectedHost != null) {
            hidService?.sendString(data)
        } else {
            addLog("⚠ Kein PC verbunden!")
            Toast.makeText(this, "Kein PC verbunden", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            binding.tvStatus.text = status
        }
    }

    private fun addLog(entry: String) {
        runOnUiThread {
            val current = binding.tvLog.text.toString()
            val lines = current.split("\n").takeLast(20) // Max 20 Zeilen
            binding.tvLog.text = (lines + entry).joinToString("\n")
        }
    }

    override fun onDestroy() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        super.onDestroy()
    }
}
