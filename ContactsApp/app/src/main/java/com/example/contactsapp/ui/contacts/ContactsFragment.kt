package com.example.contactsapp.ui.contacts

import android.Manifest
import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactsapp.databinding.FragmentContactsBinding
import com.example.contactsapp.model.Contact
import android.provider.ContactsContract
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ContactsAdapter
    private var allContacts: List<Contact> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ContactsAdapter()
        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString().orEmpty())
            }
        })

        binding.fabAddContact.setOnClickListener {
            showAddContactDialog()
        }

        loadContacts()
    }

    override fun onResume() {
        super.onResume()
        if (allContacts.isNotEmpty()) loadContacts()
    }

    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            binding.emptyView.text = "Contacts permission not granted."
            binding.emptyView.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE

            allContacts = withContext(Dispatchers.IO) { fetchContacts() }

            binding.progressBar.visibility = View.GONE
            filter(binding.searchBar.text?.toString().orEmpty())
        }
    }

    private fun fetchContacts(): List<Contact> {
        val list = mutableListOf<Contact>()
        val seen = mutableSetOf<String>()

        requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idIdx    = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx  = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx) ?: continue
                if (!seen.add(id)) continue
                val name  = cursor.getString(nameIdx) ?: "Unknown"
                val phone = cursor.getString(phoneIdx) ?: continue
                val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                list.add(Contact(id, name, phone, photo))
            }
        }
        return list
    }

    private fun filter(query: String) {
        val result = if (query.isBlank()) allContacts
        else allContacts.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.phoneNumber.contains(query)
        }
        adapter.submitList(result)
        binding.emptyView.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyView.text = if (result.isEmpty()) "No contacts found" else ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showAddContactDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)
        }
        val nameInput = EditText(ctx).apply { hint = "Contact Name" }
        val phoneInput = EditText(ctx).apply {
            hint = "Phone Number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        layout.addView(nameInput)
        layout.addView(phoneInput)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Save to Google Contacts")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    saveContactToGoogle(name, phone)
                } else {
                    Toast.makeText(ctx, "Name and phone required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveContactToGoogle(name: String, phone: String) {
        val ctx = requireContext()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ctx, "Missing permissions to save contact", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accountManager = AccountManager.get(ctx)
                val accounts = accountManager.getAccountsByType("com.google")
                val accountName = if (accounts.isNotEmpty()) accounts[0].name else null
                val accountType = if (accounts.isNotEmpty()) "com.google" else null

                val ops = ArrayList<ContentProviderOperation>()
                val rawContactInsertIndex = ops.size

                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                        .build()
                )

                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                        .build()
                )

                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                )

                ctx.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Saved to Google: $name", Toast.LENGTH_SHORT).show()
                    loadContacts() 
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
