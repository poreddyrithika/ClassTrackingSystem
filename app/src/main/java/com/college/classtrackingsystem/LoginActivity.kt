package com.college.classtrackingsystem

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerUserType: Spinner
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var imgEye: ImageView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAlreadyLoggedIn()

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        spinnerUserType = findViewById(R.id.spinnerUserType)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        imgEye = findViewById(R.id.imgEye)

        setupSpinner()
        setupLogin()
        setupForgotPassword()
        setupPasswordToggle()
    }

    private fun checkAlreadyLoggedIn() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val role = prefs.getString("userRole", null)

        if (isLoggedIn && role != null) {

            val intent = when (role.lowercase()) {
                "admin" -> Intent(this, AdminDashboardActivity::class.java)
                "faculty" -> Intent(this, FacultyDashboardActivity::class.java)
                else -> Intent(this, StudentDashboardActivity::class.java)
            }

            startActivity(intent)
            finish()
        }
    }

    private fun setupSpinner() {
        val userTypes = resources.getStringArray(R.array.user_types)

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            userTypes
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.WHITE)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUserType.adapter = adapter
    }

    private fun setupLogin() {
        btnLogin.setOnClickListener {

            val userId = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val userType = spinnerUserType.selectedItem.toString()

            if (userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userType == "Select User Type") {
                Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(userId, password, userType)
        }
    }

    private fun setupForgotPassword() {
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun loginUser(userId: String, password: String, userType: String) {

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val dbPassword = document.getString("Password")?.trim()
                    val dbRole = document.getString("Role")?.trim()

                    if (dbPassword == password && dbRole.equals(userType, true)) {

                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("isLoggedIn", true)
                            .putString("userRole", dbRole)
                            .putString("userId", userId)   // ✅ ADD THIS
                            .apply()

                        val intent = when (dbRole?.lowercase()) {
                            "admin" -> Intent(this, AdminDashboardActivity::class.java)
                            "faculty" -> Intent(this, FacultyDashboardActivity::class.java)
                            else -> Intent(this, StudentDashboardActivity::class.java)
                        }

                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this, "Invalid Password or Role", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                }

            }
            .addOnFailureListener {
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPasswordToggle() {

        var isVisible = false

        // Default: hidden → white icon
        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        imgEye.setImageResource(R.drawable.ic_visibility)
        imgEye.setColorFilter(getColor(android.R.color.white))

        imgEye.setOnClickListener {

            isVisible = !isVisible

            if (isVisible) {
                // Show password → grey
                etPassword.transformationMethod = null
                imgEye.setImageResource(R.drawable.ic_visibility_off)
                imgEye.setColorFilter(getColor(android.R.color.darker_gray))
            } else {
                // Hide password → white
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                imgEye.setImageResource(R.drawable.ic_visibility)
                imgEye.setColorFilter(getColor(android.R.color.white))
            }

            etPassword.setSelection(etPassword.text.length)
        }
    }
}