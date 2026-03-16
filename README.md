# Scanner Keyboard

Android-App für den Honeywell EDA52 – macht das Gerät zu einer Bluetooth-HID-Tastatur.
Gescannte Barcodes werden automatisch an den verbundenen PC gesendet.

---

## APK bauen (3 Wege)

### Option A: GitHub Actions (empfohlen, kein lokales Setup)
1. Diesen Ordner auf GitHub hochladen (neues Repository)
2. → Actions → "Build APK" → "Run workflow"
3. Nach ~3 Minuten: Artifacts → `ScannerKeyboard-debug.apk` herunterladen

### Option B: Android Studio (lokal)
1. [Android Studio](https://developer.android.com/studio) installieren
2. Projekt öffnen (`File → Open → diesen Ordner`)
3. `Build → Build Bundle(s) / APK(s) → Build APK(s)`
4. APK liegt in `app/build/outputs/apk/debug/`

### Option C: Kommandozeile
```bash
chmod +x gradlew
./gradlew assembleDebug
```

---

## Installation auf EDA52
1. APK per USB oder E-Mail auf das Gerät übertragen
2. `Einstellungen → Sicherheit → Unbekannte Quellen` aktivieren
3. APK öffnen und installieren

---

## Nutzung

### Einmalige Einrichtung
1. **App starten** auf dem EDA52
2. **Am Windows-PC:** Einstellungen → Bluetooth → Gerät hinzufügen
3. EDA52 in der Liste auswählen → koppeln
4. PC erkennt das Gerät als **Bluetooth-Tastatur**

### Scannen
- App öffnen lassen (oder im Hintergrund)
- Barcode scannen → Text wird sofort am PC eingegeben (inkl. Enter)
- Das aktive Fenster am PC empfängt den Text (Notepad, Excel, Browser, etc.)

### DataWedge konfigurieren
Damit DataWedge die Scandaten an diese App sendet:
1. DataWedge App öffnen
2. Profil bearbeiten (oder neues erstellen)
3. **Intent Output** aktivieren
4. Intent Action: `com.honeywell.aidc.action.ACTION_AIDC_BARCODE_DATA`
5. Intent Category: leer lassen
6. Intent Delivery: **Broadcast Intent**

---

## Funktionen
- Bluetooth HID (Tastatur-Modus) – kein Treiber am PC nötig
- DataWedge Integration (automatischer Scan-Empfang)
- Manuell Text senden (Test-Funktion in der App)
- Verbindungsstatus-Anzeige
- Scan-Log

---

## Technische Details
- **Minimum Android:** 9 (API 28)
- **Bluetooth Profil:** HID Device (BluetoothHidDevice API)
- **Kein Root erforderlich**
