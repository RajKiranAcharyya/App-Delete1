package com.example.contactsapp.model

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
) {
    val initial: String get() = name.firstOrNull()?.uppercase() ?: "#"
}
