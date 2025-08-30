package com.example.certify

import com.google.firebase.Timestamp

data class Certificate(
    val id: String = "",
    val studentName: String = "",
    val institutionName: String = "",
    val course: String = "",
    val year: String = "",
    val imageUrl: String? = null,
    val createdBy: String? = null,
    val createdAt: Timestamp? = null,
    val qrData: String = ""
)
