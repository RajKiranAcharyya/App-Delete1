package com.example.contactsapp.ui.contacts

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

    // Palette of avatar background colors
    private val avatarColors = intArrayOf(
        0xFF7986CB.toInt(), // indigo
        0xFF4DB6AC.toInt(), // teal
        0xFFF06292.toInt(), // pink
        0xFFFFB74D.toInt(), // orange
        0xFF81C784.toInt(), // green
        0xFF64B5F6.toInt(), // blue
        0xFFBA68C8.toInt(), // purple
        0xFFFF8A65.toInt()  // deep orange
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
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                b.avatarFrame.background = bg
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(o: Contact, n: Contact) = o.id == n.id
        override fun areContentsTheSame(o: Contact, n: Contact) = o == n
    }
}
