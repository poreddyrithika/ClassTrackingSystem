package com.college.classtrackingsystem

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var tvPasswordStrength: TextView

    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        etNewPassword = findViewById(R.id.etNewPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength)

        userId = intent.getStringExtra("userId")

        // Show rules initially
        setPasswordRules()

        // Password strength listener
        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val background = tvPasswordStrength.background.mutate()
                if (background is GradientDrawable) {
                    if (password.isEmpty()) {
                        setPasswordRules()
                    } else if (isStrongPassword(password)) {
                        tvPasswordStrength.text = "Strong Password"
                        tvPasswordStrength.setTextColor(Color.parseColor("#2E7D32"))
                        background.setStroke(2, Color.parseColor("#2E7D32"))
                    } else {
                        tvPasswordStrength.text = """
                            Weak Password!
                            • Minimum 8 characters
                            • At least 1 Uppercase letter
                            • At least 1 Lowercase letter
                            • At least 1 Number
                            • At least 1 Special character (~`!@#$%^&*()-_+={}[]|\;:"<>,./?)
                        """.trimIndent()
                        tvPasswordStrength.setTextColor(Color.parseColor("#C62828"))
                        background.setStroke(2, Color.parseColor("#C62828"))
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Reset password button
        btnResetPassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            if (!isStrongPassword(newPassword)) {
                Toast.makeText(this, "Please enter a strong password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            userId?.let {
                db.collection("users")
                    .document(it)
                    .update("Password", newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    // Function to display password rules initially
    private fun setPasswordRules() {
        val background = tvPasswordStrength.background.mutate()
        if (background is GradientDrawable) {
            background.setStroke(2, Color.parseColor("#C62828")) // Red stroke for rules
        }
        tvPasswordStrength.text = """
            Password must contain:
            • Minimum 8 characters
            • At least 1 Uppercase letter
            • At least 1 Lowercase letter
            • At least 1 Number
            • At least 1 Special character (~`!@#$%^&*()-_+={}[]|\;:"<>,./?)
        """.trimIndent()
        tvPasswordStrength.setTextColor(Color.parseColor("#C62828"))
    }

    // Regex for strong password with updated special characters
    private fun isStrongPassword(password: String): Boolean {
        val passwordPattern = Regex(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[~`!@#\$%^&*()\\-_=+{}\\[\\]|\\\\;:\\\"<>,./?])(?=\\S+\$).{8,}\$"
        )
        return passwordPattern.containsMatchIn(password)
    }
}

