package com.example.contactsapp.overlay

import android.content.Context
import android.database.Cursor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.contactsapp.R
import com.example.contactsapp.databinding.OverlayCallPopupBinding
import kotlin.concurrent.thread

/**
 * Singleton that manages the floating call-info popup drawn via WindowManager.
 * Must be initialised once with an Application context before use.
 */
object OverlayController {

    private var wm: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private val avatarColors = intArrayOf(
        0xFF7986CB.toInt(), 0xFF4DB6AC.toInt(), 0xFFF06292.toInt(),
        0xFFFFB74D.toInt(), 0xFF81C784.toInt(), 0xFF64B5F6.toInt()
    )

    fun init(ctx: Context) {
        if (wm == null) wm = ctx.applicationContext
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun show(ctx: Context, number: String, isIncoming: Boolean) {
        handler.post {
            hideInternal()

            val appCtx = ctx.applicationContext
            val binding = OverlayCallPopupBinding.inflate(LayoutInflater.from(appCtx))

            // ── Initial state with number ─────────────────────────────────────
            val callLabel = if (isIncoming) "Incoming Call" else "Outgoing Call"
            binding.tvCallType.text    = callLabel
            binding.tvContactName.text = number
            binding.tvPhoneNumber.text = number
            binding.tvInitial.text     = number.lastOrNull()?.toString() ?: "#"

            val callColor = if (isIncoming)
                ContextCompat.getColor(appCtx, R.color.call_incoming)
            else
                ContextCompat.getColor(appCtx, R.color.call_outgoing)

            binding.callTypeBar.setBackgroundColor(callColor)
            binding.ivCallDirection.setImageResource(
                if (isIncoming) R.drawable.ic_call_incoming else R.drawable.ic_call_outgoing
            )
            binding.ivCallDirection.setColorFilter(callColor)

            // ── Dismiss button ────────────────────────────────────────────────
            binding.btnDismiss.setOnClickListener { hide() }

            // ── Look up contact name in background ───────────────────────────
            thread {
                val contact = lookupContact(appCtx, number)
                handler.post {
                    if (contact != null) {
                        binding.tvContactName.text = contact.first
                        binding.tvInitial.text     = contact.first.firstOrNull()?.uppercase() ?: "#"

                        val color = avatarColors[
                            contact.first.hashCode().and(0x7fffffff) % avatarColors.size
                        ]
                        val bg = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL; setColor(color)
                        }
                        binding.avatarFrame.background = bg

                        if (!contact.second.isNullOrBlank()) {
                            binding.imgAvatar.setImageURI(Uri.parse(contact.second))
                            binding.imgAvatar.visibility = View.VISIBLE
                            binding.tvInitial.visibility = View.GONE
                        }
                    }
                }
            }

            // ── WindowManager params ──────────────────────────────────────────
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 80
            }

            overlayView = binding.root
            wm?.addView(overlayView, params)
        }
    }

    fun hide() {
        handler.post { hideInternal() }
    }

    private fun hideInternal() {
        overlayView?.let {
            try { wm?.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun lookupContact(ctx: Context, number: String): Pair<String, String?>? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val cursor: Cursor? = ctx.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI
            ),
            null, null, null
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx  = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIdx = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                val name  = it.getString(nameIdx) ?: return null
                val photo = if (photoIdx >= 0) it.getString(photoIdx) else null
                Pair(name, photo)
            } else null
        }
    }
}
