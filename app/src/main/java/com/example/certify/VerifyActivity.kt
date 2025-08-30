package com.example.certify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.certify.databinding.ActivityVerifyBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import android.graphics.BitmapFactory
import android.util.Base64

class VerifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyBinding
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScan.setOnClickListener {
            IntentIntegrator(this)
                .setPrompt("Scan QR to verify")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .initiateScan()
        }

        binding.btnSearch.setOnClickListener {
            val id = binding.etSearchId.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, "Enter ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findCertificateById(id)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val scanned = result.contents
                findCertificateById(scanned)
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

private fun findCertificateById(id: String) {
    db.collection("certificates").document(id).get()
        .addOnSuccessListener { doc ->
            if (doc.exists()) {
                val cert = doc.toObject(Certificate::class.java)
                
                binding.tvResult.text = buildString {
                    append("ID: ${cert?.id}\n")
                    append("Student: ${cert?.studentName}\n")
                    append("Institution: ${cert?.institutionName}\n")
                    append("Course: ${cert?.course}\n")
                    append("Year: ${cert?.year}\n")
                }

                cert?.imageUrl?.let { base64 ->
                    if (base64.isNotEmpty()) {
                        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        binding.ivCertificate.setImageBitmap(bitmap)
                    } else {
                        binding.ivCertificate.setImageResource(0) 
                    }
                }

            } else {
                binding.tvResult.text = "Certificate not found"
                binding.ivCertificate.setImageResource(0)
            }
        }
        .addOnFailureListener { e ->
            binding.tvResult.text = "Error: ${e.message}"
            binding.ivCertificate.setImageResource(0)
        }
}
}
