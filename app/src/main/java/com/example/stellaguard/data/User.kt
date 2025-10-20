package com.example.stellaguard.data

data class User(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val points: Int = 0
)