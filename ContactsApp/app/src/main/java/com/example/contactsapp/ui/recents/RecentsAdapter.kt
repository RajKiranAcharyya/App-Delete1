package com.example.contactsapp.ui.recents

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactsapp.R
import com.example.contactsapp.databinding.ItemRecentCallBinding
import com.example.contactsapp.model.CallLogEntry
import java.text.SimpleDateFormat
import java.util.*

class RecentsAdapter : ListAdapter<CallLogEntry, RecentsAdapter.VH>(Diff()) {

    private val avatarColors = intArrayOf(
        0xFF7986CB.toInt(), 0xFF4DB6AC.toInt(), 0xFFF06292.toInt(),
        0xFFFFB74D.toInt(), 0xFF81C784.toInt(), 0xFF64B5F6.toInt()
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRecentCallBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemRecentCallBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: CallLogEntry) {
            val ctx = b.root.context
            b.tvName.text  = e.displayName
            b.tvPhone.text = e.phoneNumber

            // ── Date / time ──────────────────────────────────────────────────
            val now = Calendar.getInstance()
            val callCal = Calendar.getInstance().apply { timeInMillis = e.date }
            b.tvDate.text = when {
                now.get(Calendar.DATE) == callCal.get(Calendar.DATE) &&
                now.get(Calendar.YEAR) == callCal.get(Calendar.YEAR) ->
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(e.date))
                now.get(Calendar.YEAR) == callCal.get(Calendar.YEAR) ->
                    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(e.date))
                else ->
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(e.date))
            }

            // ── Duration ─────────────────────────────────────────────────────
            b.tvDuration.text = if (e.duration > 0) {
                val m = e.duration / 60; val s = e.duration % 60
                if (m > 0) "${m}m ${s}s" else "${s}s"
            } else ""

            // ── Call type icon + colour ───────────────────────────────────────
            val (iconRes, colorRes) = when (e.type) {
                CallLog.Calls.INCOMING_TYPE -> R.drawable.ic_call_incoming to R.color.call_incoming
                CallLog.Calls.OUTGOING_TYPE -> R.drawable.ic_call_outgoing to R.color.call_outgoing
                CallLog.Calls.MISSED_TYPE   -> R.drawable.ic_call_missed   to R.color.call_missed
                else                        -> R.drawable.ic_call_incoming to R.color.call_incoming
            }
            b.ivCallType.setImageResource(iconRes)
            b.ivCallType.setColorFilter(ContextCompat.getColor(ctx, colorRes))

            // ── Avatar ───────────────────────────────────────────────────────
            if (!e.photoUri.isNullOrBlank()) {
                b.imgAvatar.setImageURI(Uri.parse(e.photoUri))
                b.imgAvatar.visibility = View.VISIBLE
                b.tvInitial.visibility = View.GONE
            } else {
                b.imgAvatar.visibility = View.GONE
                b.tvInitial.visibility = View.VISIBLE
                b.tvInitial.text = e.initial
                val color = avatarColors[e.displayName.hashCode().and(0x7fffffff) % avatarColors.size]
                val bg = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
                b.avatarFrame.background = bg
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<CallLogEntry>() {
        override fun areItemsTheSame(o: CallLogEntry, n: CallLogEntry) = o.id == n.id
        override fun areContentsTheSame(o: CallLogEntry, n: CallLogEntry) = o == n
    }
}
