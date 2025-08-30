package com.example.certify

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.certify.databinding.ActivityUploadBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.util.*

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private val PICK_IMAGE = 123
    private val PERMISSION_CODE = 456
    private var imageUri: Uri? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImage.setOnClickListener { checkPermissionAndPickImage() }
        binding.btnUploadCert.setOnClickListener { uploadCertificate() }
    }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickImage()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_CODE)
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImage()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_CODE)
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            binding.ivPreview.setImageURI(imageUri)
        }
    }

    private fun uploadCertificate() {
        val student = binding.etStudentName.text.toString().trim()
        val inst = binding.etInstitution.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val year = binding.etYear.text.toString().trim()

        if (student.isEmpty() || inst.isEmpty() || course.isEmpty() || year.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val id = UUID.randomUUID().toString()
        val qrData = id
        binding.btnUploadCert.isEnabled = false

        var imageBase64: String? = null
        imageUri?.let { uri ->
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val imageBytes = baos.toByteArray()
                imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e("Upload", "Image conversion failed", e)
                Toast.makeText(this, "Failed to convert image", Toast.LENGTH_SHORT).show()
                binding.btnUploadCert.isEnabled = true
                return
            }
        }

        writeCertificateDocument(id, student, inst, course, year, imageBase64, qrData)
    }

    private fun writeCertificateDocument(
        id: String,
        student: String,
        inst: String,
        course: String,
        year: String,
        imageBase64: String?,
        qrData: String
    ) {
        val doc = Certificate(
            id = id,
            studentName = student,
            institutionName = inst,
            course = course,
            year = year,
            imageUrl = imageBase64,
            createdBy = auth.currentUser?.uid,
            createdAt = Timestamp.now(),
            qrData = qrData
        )

        db.collection("certificates").document(id)
            .set(doc)
            .addOnSuccessListener {
                binding.tvGeneratedId.text = "Generated ID: $id"
                val bmp = generateQRCode(qrData)
                binding.ivQr.setImageBitmap(bmp)
                binding.btnUploadCert.isEnabled = true
                Toast.makeText(this, "Certificate uploaded", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.btnUploadCert.isEnabled = true
                Toast.makeText(this, "Failed to save certificate: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateQRCode(text: String, size: Int = 600): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            Log.e("QR", "generate error: ${e.message}")
            null
        }
    }
}
