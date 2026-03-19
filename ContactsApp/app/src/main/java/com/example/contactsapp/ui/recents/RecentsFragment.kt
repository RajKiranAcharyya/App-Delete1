package com.example.contactsapp.ui.recents

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactsapp.databinding.FragmentRecentsBinding
import com.example.contactsapp.model.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentsFragment : Fragment() {

    private var _binding: FragmentRecentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RecentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecentsAdapter()
        binding.recyclerRecents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecents.adapter = adapter

        loadCallLog()
    }

    override fun onResume() {
        super.onResume()
        loadCallLog()
    }

    private fun loadCallLog() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            binding.emptyView.text = "Call log permission not granted."
            binding.emptyView.visibility = View.VISIBLE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE

            val entries = withContext(Dispatchers.IO) { fetchCallLog() }

            adapter.submitList(entries)
            binding.progressBar.visibility = View.GONE
            if (entries.isEmpty()) {
                binding.emptyView.text = "No recent calls"
                binding.emptyView.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchCallLog(): List<CallLogEntry> {
        val list = mutableListOf<CallLogEntry>()
        val cursor = requireContext().contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE,
                CallLog.Calls.CACHED_PHOTO_URI
            ),
            null, null,
            "${CallLog.Calls.DATE} DESC LIMIT 300"
        ) ?: return list

        cursor.use {
            val idIdx    = it.getColumnIndex(CallLog.Calls._ID)
            val nameIdx  = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx   = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx  = it.getColumnIndex(CallLog.Calls.TYPE)
            val durIdx   = it.getColumnIndex(CallLog.Calls.DURATION)
            val dateIdx  = it.getColumnIndex(CallLog.Calls.DATE)
            val photoIdx = it.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)

            while (it.moveToNext()) {
                list.add(
                    CallLogEntry(
                        id          = it.getString(idIdx) ?: continue,
                        name        = it.getString(nameIdx),
                        phoneNumber = it.getString(numIdx) ?: "",
                        type        = it.getInt(typeIdx),
                        duration    = it.getLong(durIdx),
                        date        = it.getLong(dateIdx),
                        photoUri    = if (photoIdx >= 0) it.getString(photoIdx) else null
                    )
                )
            }
        }
        return list
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
