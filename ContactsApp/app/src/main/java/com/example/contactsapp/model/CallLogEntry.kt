package com.example.contactsapp.model

data class CallLogEntry(
    val id: String,
    val name: String?,
    val phoneNumber: String,
    val type: Int,        // 1 = incoming, 2 = outgoing, 3 = missed
    val duration: Long,   // seconds
    val date: Long,       // millis
    val photoUri: String? = null
) {
    val displayName: String get() = if (!name.isNullOrBlank()) name else phoneNumber
    val initial: String get() = displayName.firstOrNull()?.uppercase() ?: "#"
}
