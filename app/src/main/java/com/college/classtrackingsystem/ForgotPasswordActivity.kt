package com.college.classtrackingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etUserId: EditText
    private lateinit var spRole: Spinner
    private lateinit var btnGenerateOtp: Button

    private val db = FirebaseFirestore.getInstance()
    private var generatedOtp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        etUserId = findViewById(R.id.etUserId)
        spRole = findViewById(R.id.spRole)
        btnGenerateOtp = findViewById(R.id.btnGenerateOtp)

        val roles = arrayOf(
            "Select User Type",
            "Student",
            "Faculty",
            "Admin"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = adapter

        btnGenerateOtp.setOnClickListener {

            val userId = etUserId.text.toString().trim()
            val selectedPosition = spRole.selectedItemPosition
            val role = spRole.selectedItem.toString()

            if (userId.isEmpty()) {
                Toast.makeText(this, "Enter User ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedPosition == 0) {
                Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkUser(userId, role)
        }
    }

    private fun checkUser(userId: String, role: String) {

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val dbRole = document.getString("Role")
                    val email = document.getString("Email")

                    if (dbRole == null ||
                        dbRole.trim().lowercase() != role.trim().lowercase()) {

                        Toast.makeText(this, "Invalid Role", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    if (!email.isNullOrEmpty()) {

                        generatedOtp = (100000..999999).random().toString()

                        sendOtpToEmail(email, generatedOtp, userId)

                    } else {
                        Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendOtpToEmail(email: String, otp: String, userId: String) {

        // Disable button immediately
        btnGenerateOtp.isEnabled = false
        btnGenerateOtp.text = "Sending..."

        Thread {

            try {

                val senderEmail = "classtrackingsystem.vignan@gmail.com"
                val senderPassword = "hsnhafxdcsfzacea"

                val props = java.util.Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "587"

                val session = javax.mail.Session.getInstance(
                    props,
                    object : javax.mail.Authenticator() {
                        override fun getPasswordAuthentication():
                                javax.mail.PasswordAuthentication {
                            return javax.mail.PasswordAuthentication(
                                senderEmail,
                                senderPassword
                            )
                        }
                    })

                val message = javax.mail.internet.MimeMessage(session)
                message.setFrom(javax.mail.internet.InternetAddress(senderEmail))
                message.setRecipients(
                    javax.mail.Message.RecipientType.TO,
                    javax.mail.internet.InternetAddress.parse(email)
                )
                message.subject = "Your OTP Code"
                message.setText("Your OTP is: $otp")

                javax.mail.Transport.send(message)

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "OTP Sent Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Re-enable button
                    btnGenerateOtp.isEnabled = true
                    btnGenerateOtp.text = "Generate OTP"

                    val intent = Intent(this, OtpActivity::class.java)
                    intent.putExtra("otp", otp)
                    intent.putExtra("userId", userId)
                    intent.putExtra("email", email)   // ✅ ADD THIS
                    startActivity(intent)
                }

            } catch (e: Exception) {

                e.printStackTrace()

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Failed to send email",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Re-enable button on failure
                    btnGenerateOtp.isEnabled = true
                    btnGenerateOtp.text = "Generate OTP"
                }
            }

        }.start()
    }
}