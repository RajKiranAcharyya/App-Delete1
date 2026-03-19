# ContactsApp — Android (Kotlin)

A 100% offline contacts viewer + call history app with an overlay popup for
incoming and outgoing calls.  No internet, no sync, no cloud.

---

## Features
| Screen | What it does |
|--------|-------------|
| **Contacts** | Browse all device contacts (name + number). Search by name or number. |
| **Recents**  | Last 300 call-log entries with call type (incoming ↙ / outgoing ↗ / missed ✕), duration, and timestamp. |
| **Call popup** | A floating card overlay appears automatically when a call rings (incoming) or is dialled (outgoing). Shows contact name, avatar initial, and phone number. Dismissed with the × button or automatically when the call ends. |

---

## How to open in Android Studio

1. **File → Open** → select the `ContactsApp` folder.
2. Let Gradle sync finish (it downloads dependencies — only needed once).
3. Connect an Android device (API 26+) or start an emulator.
4. **Run → Run 'app'**.

---

## First-launch permissions

The app asks for:

| Permission | Why |
|---|---|
| `READ_CONTACTS` | Show your contact list |
| `READ_CALL_LOG` | Show call history; also required on API 29+ to receive the incoming phone number in the broadcast |
| `READ_PHONE_STATE` | Detect call events |
| `READ_PHONE_NUMBERS` | Read SIM phone number |
| `POST_NOTIFICATIONS` | Foreground service notification (Android 13+) |
| **Draw over other apps** | Show the call popup overlay — grant via the system settings dialog that appears on first launch |

> **Draw over other apps** must be granted manually. The app will show a dialog
> explaining why and take you to the system settings screen.

---

## Architecture

```
CallReceiver (BroadcastReceiver)
    ├── listens for PHONE_STATE  →  shows/hides overlay on ring / idle
    └── listens for NEW_OUTGOING_CALL  →  shows overlay when dialling

OverlayController (singleton object)
    └── manages a WindowManager TYPE_APPLICATION_OVERLAY view

CallDetectionService (Foreground Service)
    └── keeps process alive + hosts the persistent notification

BootReceiver
    └── restarts CallDetectionService after device reboot
```

---

## Important notes

- The app **does not make calls** — it only observes them.  Actual dialling is
  handled by whatever dialler is set as default on the device.
- `ACTION_NEW_OUTGOING_CALL` is deprecated on API 29+ but still delivered.
  It remains the only way to intercept the outgoing number without being the
  default dialler.
- On Android 12+ the incoming number in the broadcast requires the
  `READ_CALL_LOG` permission at runtime (which the app already requests).
- The popup is a `TYPE_APPLICATION_OVERLAY` window — it requires the
  "Draw over other apps" system permission.
# App-Delete1
# App-Delete1
