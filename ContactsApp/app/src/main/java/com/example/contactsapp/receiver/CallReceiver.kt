package com.example.contactsapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import com.example.contactsapp.overlay.OverlayController
import com.example.contactsapp.service.CallDetectionService

/**
 * Receives PHONE_STATE and NEW_OUTGOING_CALL broadcasts.
 * Shows / hides the overlay via OverlayController.
 *
 * READ_CALL_LOG is required for EXTRA_INCOMING_NUMBER on API 29+.
 */
class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Make sure the foreground service (and OverlayController) is initialised
        ensureService(context)

        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state  = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        if (canDrawOverlay(context)) {
                            OverlayController.show(context, number ?: "Unknown", isIncoming = true)
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        OverlayController.hide()
                    }
                    // OFFHOOK — call in progress; keep overlay visible
                }
            }

            Intent.ACTION_NEW_OUTGOING_CALL -> {
                val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: "Unknown"
                if (canDrawOverlay(context)) {
                    OverlayController.show(context, number, isIncoming = false)
                }
            }
        }
    }

    private fun canDrawOverlay(ctx: Context) = Settings.canDrawOverlays(ctx)

    private fun ensureService(ctx: Context) {
        OverlayController.init(ctx)
        val svc = Intent(ctx, CallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(svc)
        } else {
            ctx.startService(svc)
        }
    }
}
