package com.college.classtrackingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class OtpActivity : AppCompatActivity() {

    private lateinit var etOtp: EditText
    private lateinit var btnVerifyOtp: Button

    private lateinit var tvUserId: TextView
    private lateinit var tvEmail: TextView

    private var receivedOtp: String? = null
    private var userId: String? = null
    private var email: String? = null   // Added email variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        etOtp = findViewById(R.id.etOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)

        tvUserId = findViewById(R.id.tvUserId)   // Initialize TextView
        tvEmail = findViewById(R.id.tvEmail)     // Initialize TextView

        // Receive data from previous activity
        receivedOtp = intent.getStringExtra("otp")
        userId = intent.getStringExtra("userId")
        email = intent.getStringExtra("email")

        // Display User ID and Email
        tvUserId.text = "User ID: $userId"
        tvEmail.text = "OTP sent to: $email"

        btnVerifyOtp.setOnClickListener {

            val enteredOtp = etOtp.text.toString().trim()

            if (enteredOtp == receivedOtp) {

                val intent = Intent(this, ResetPasswordActivity::class.java)
                intent.putExtra("userId", userId)
                startActivity(intent)
                finish()

            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
