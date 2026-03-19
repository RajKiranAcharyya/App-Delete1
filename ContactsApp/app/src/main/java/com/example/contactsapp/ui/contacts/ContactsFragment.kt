package com.example.contactsapp.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactsapp.databinding.FragmentContactsBinding
import com.example.contactsapp.model.Contact
import android.provider.ContactsContract
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
}












// package com.example.contactsapp.ui.contacts

// import android.Manifest
// import android.content.pm.PackageManager
// import android.database.Cursor
// import android.os.Bundle
// import android.text.Editable
// import android.text.TextWatcher
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import androidx.core.content.ContextCompat
// import androidx.fragment.app.Fragment
// import androidx.lifecycle.lifecycleScope
// import androidx.recyclerview.widget.LinearLayoutManager
// import com.example.contactsapp.databinding.FragmentContactsBinding
// import com.example.contactsapp.model.Contact
// import android.provider.ContactsContract
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.withContext

// class ContactsFragment : Fragment() {

//     private var _binding: FragmentContactsBinding? = null
//     private val binding get() = _binding!!

//     private lateinit var adapter: ContactsAdapter
//     private var allContacts: List<Contact> = emptyList()

//     override fun onCreateView(
//         inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//     ): View {
//         _binding = FragmentContactsBinding.inflate(inflater, container, false)
//         return binding.root
//     }

//     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//         super.onViewCreated(view, savedInstanceState)

//         adapter = ContactsAdapter()
//         binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
//         binding.recyclerContacts.adapter = adapter

//         binding.searchBar.addTextChangedListener(object : TextWatcher {
//             override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//             override fun afterTextChanged(s: Editable?) {}
//             override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                 filter(s?.toString().orEmpty())
//             }
//         })

//         loadContacts()
//     }

//     override fun onResume() {
//         super.onResume()
//         // Reload in case contacts changed while away
//         if (allContacts.isNotEmpty()) loadContacts()
//     }

//     private fun loadContacts() {
//         if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
//             != PackageManager.PERMISSION_GRANTED
//         ) {
//             binding.emptyView.text = "Contacts permission not granted."
//             binding.emptyView.visibility = View.VISIBLE
//             return
//         }

//         viewLifecycleOwner.lifecycleScope.launch {
//             binding.progressBar.visibility = View.VISIBLE
//             binding.emptyView.visibility = View.GONE

//             allContacts = withContext(Dispatchers.IO) { fetchContacts() }

//             binding.progressBar.visibility = View.GONE
//             filter(binding.searchBar.text?.toString().orEmpty())
//         }
//     }

//     private fun fetchContacts(): List<Contact> {
//         val list = mutableListOf<Contact>()
//         val cursor: Cursor? = requireContext().contentResolver.query(
//             ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//             arrayOf(
//                 ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
//                 ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
//                 ContactsContract.CommonDataKinds.Phone.NUMBER,
//                 ContactsContract.CommonDataKinds.Phone.PHOTO_URI
//             ),
//             null, null,
//             "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
//         ) ?: return list

//         cursor.use {
//             val idIdx    = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
//             val nameIdx  = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
//             val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
//             val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

//             val seen = mutableSetOf<String>()
//             while (it.moveToNext()) {
//                 val id = it.getString(idIdx) ?: continue
//                 // Show only first number per contact to avoid duplicates
//                 if (!seen.add(id)) continue
//                 val name  = it.getString(nameIdx) ?: "Unknown"
//                 val phone = it.getString(phoneIdx) ?: continue
//                 val photo = if (photoIdx >= 0) it.getString(photoIdx) else null
//                 list.add(Contact(id, name, phone, photo))
//             }
//         }
//         return list
//     }

//     private fun filter(query: String) {
//         val result = if (query.isBlank()) allContacts
//         else allContacts.filter {
//             it.name.contains(query, ignoreCase = true) ||
//             it.phoneNumber.contains(query)
//         }
//         adapter.submitList(result)
//         binding.emptyView.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
//         binding.emptyView.text = if (result.isEmpty()) "No contacts found" else ""
//     }

//     override fun onDestroyView() {
//         super.onDestroyView()
//         _binding = null
//     }
// }
