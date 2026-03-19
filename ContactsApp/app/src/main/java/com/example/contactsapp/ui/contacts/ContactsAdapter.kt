package com.example.contactsapp.ui.contacts

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactsapp.databinding.ItemContactBinding
import com.example.contactsapp.model.Contact

class ContactsAdapter : ListAdapter<Contact, ContactsAdapter.VH>(Diff()) {

    private val avatarColors = intArrayOf(
        0xFF7986CB.toInt(), 0xFF4DB6AC.toInt(), 0xFFF06292.toInt(),
        0xFFFFB74D.toInt(), 0xFF81C784.toInt(), 0xFF64B5F6.toInt(),
        0xFFBA68C8.toInt(), 0xFFFF8A65.toInt()
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: Contact) {
            b.tvName.text  = c.name
            b.tvPhone.text = c.phoneNumber

            if (c.photoUri != null) {
                b.imgAvatar.setImageURI(Uri.parse(c.photoUri))
                b.imgAvatar.visibility = View.VISIBLE
                b.tvInitial.visibility = View.GONE
            } else {
                b.imgAvatar.visibility = View.GONE
                b.tvInitial.visibility = View.VISIBLE
                b.tvInitial.text = c.initial
                val color = avatarColors[c.name.hashCode().and(0x7fffffff) % avatarColors.size]
                b.avatarFrame.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL; setColor(color)
                }
            }

            b.btnCall.setOnClickListener {
                val ctx = b.root.context
                ctx.startActivity(Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${Uri.encode(c.phoneNumber)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(o: Contact, n: Contact) = o.id == n.id
        override fun areContentsTheSame(o: Contact, n: Contact) = o == n
    }
}