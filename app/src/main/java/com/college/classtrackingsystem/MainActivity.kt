package com.college.classtrackingsystem

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import android.text.method.PasswordTransformationMethod
import android.text.method.HideReturnsTransformationMethod
import android.content.res.ColorStateList

class MainActivity : AppCompatActivity() {

    private lateinit var etUserId: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerUserType: Spinner
    private lateinit var btnLogin: Button
    private lateinit var btnTogglePassword: ImageView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect UI
        etUserId = findViewById(R.id.etUserId)
        etPassword = findViewById(R.id.etPassword)
        spinnerUserType = findViewById(R.id.spinnerUserType)
        btnLogin = findViewById(R.id.btnLogin)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)

        // Set default icon color (GRAY when password hidden)
        btnTogglePassword.imageTintList =
            ColorStateList.valueOf(Color.GRAY)

        // Spinner setup
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.user_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUserType.adapter = adapter

        // Password toggle
        btnTogglePassword.setOnClickListener {

            if (etPassword.transformationMethod is PasswordTransformationMethod) {

                // SHOW password
                etPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

                btnTogglePassword.imageTintList =
                    ColorStateList.valueOf(Color.BLACK)

            } else {

                // HIDE password
                etPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                btnTogglePassword.imageTintList =
                    ColorStateList.valueOf(Color.GRAY)
            }

            etPassword.setSelection(etPassword.text.length)
        }

        // Login click
        btnLogin.setOnClickListener {
            val userId = etUserId.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val userType = spinnerUserType.selectedItem.toString()

            if (userId.isEmpty() || password.isEmpty() || userType == "Select Type of User") {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val docRef = db.collection("users").document(userId)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedPassword = document.getString("Password")?.trim()
                    val storedRole = document.getString("Role")?.trim()

                    if (storedPassword == password &&
                        storedRole.equals(userType, ignoreCase = true)
                    ) {
                        Toast.makeText(this, "Login Successful as $userType", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Invalid Password or Role", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "UserID not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
