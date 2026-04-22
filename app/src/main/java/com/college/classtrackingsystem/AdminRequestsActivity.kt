package com.college.classtrackingsystem

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class AdminRequestsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_requests)

        container = findViewById(R.id.containerRequests)

        loadRequests()
    }

    private fun loadRequests() {

        container.removeViews(1, container.childCount - 1) // keep title

        db.collection("extra_class_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->

                if (result.isEmpty) {
                    val tv = TextView(this)
                    tv.text = "No requests available"
                    tv.textSize = 16f
                    tv.gravity = android.view.Gravity.CENTER
                    container.addView(tv)
                    return@addOnSuccessListener
                }

                var lastDate = ""

                for (doc in result) {

                    val timestamp = when (val ts = doc.get("timestamp")) {
                        is Long -> ts
                        is Double -> ts.toLong()
                        is String -> ts.toLongOrNull() ?: 0L
                        is com.google.firebase.Timestamp -> ts.toDate().time
                        else -> 0L
                    }

                    // ✅ UPDATED (year added)
                    val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                        .format(Date(timestamp))

                    // ✅ DATE HEADER
                    if (dateStr != lastDate) {
                        val dateHeader = TextView(this)
                        dateHeader.text = dateStr
                        dateHeader.textSize = 20f
                        dateHeader.setTypeface(null, Typeface.BOLD)
                        dateHeader.setPadding(10, 30, 10, 10)
                        container.addView(dateHeader)

                        lastDate = dateStr
                    }

                    val view = LayoutInflater.from(this)
                        .inflate(R.layout.request_item, container, false)

                    val txt = view.findViewById<TextView>(R.id.txtDetails)
                    val btnAccept = view.findViewById<Button>(R.id.btnAccept)
                    val btnReject = view.findViewById<Button>(R.id.btnReject)

                    val facultyId = doc.getString("facultyId") ?: "-"
                    val subject = doc.getString("subject") ?: "-"
                    val section = doc.getString("section") ?: "-"
                    val dayKey = doc.getString("day") ?: "-"
                    val period = doc.getString("period") ?: "-"
                    val room = doc.getString("classroom") ?: "-"
                    val status = doc.getString("status") ?: "pending"

                    val formattedDay = getFormattedDay(dayKey, timestamp)

                    val builder = SpannableStringBuilder()

                    fun addLine(label: String, value: String, bold: Boolean = false) {
                        builder.append("$label ")
                        val start = builder.length
                        builder.append(value)
                        if (bold) {
                            builder.setSpan(
                                StyleSpan(Typeface.BOLD),
                                start,
                                builder.length,
                                0
                            )
                        }
                        builder.append("\n")
                    }

                    addLine("FacultyId:", facultyId, true)
                    addLine("Subject:", subject, true)
                    addLine("Section:", section)
                    addLine("Day:", formattedDay)
                    addLine("Period:", period)
                    addLine("Room:", room)
                    addLine("Status:", status)

                    txt.text = builder

                    when (status.lowercase()) {
                        "accepted" -> view.setBackgroundColor(0xFFC8E6C9.toInt())
                        "rejected" -> view.setBackgroundColor(0xFFFFCDD2.toInt())
                        else -> view.setBackgroundColor(0xFFFFF9C4.toInt())
                    }

                    if (status == "pending") {
                        btnAccept.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                    } else {
                        btnAccept.visibility = View.GONE
                        btnReject.visibility = View.GONE
                    }

                    val docId = doc.id

                    btnAccept.setOnClickListener {
                        updateStatus(docId, "accepted")
                    }

                    btnReject.setOnClickListener {
                        updateStatus(docId, "rejected")
                    }

                    container.addView(view)

                    val space = View(this)
                    space.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        20
                    )
                    container.addView(space)
                }
            }
    }

    private fun updateStatus(docId: String, status: String) {
        db.collection("extra_class_requests")
            .document(docId)
            .update("status", status)
            .addOnSuccessListener {
                Toast.makeText(this, "Updated to $status", Toast.LENGTH_SHORT).show()
                loadRequests()
            }
    }

    private fun getFormattedDay(dayKey: String, timestamp: Long): String {

        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        val map = mapOf(
            "mon" to Calendar.MONDAY,
            "tues" to Calendar.TUESDAY,
            "wed" to Calendar.WEDNESDAY,
            "thurs" to Calendar.THURSDAY,
            "fri" to Calendar.FRIDAY,
            "sat" to Calendar.SATURDAY
        )

        val target = map[dayKey] ?: return dayKey

        while (cal.get(Calendar.DAY_OF_WEEK) != target) {
            cal.add(Calendar.DATE, 1)
        }

        // ✅ UPDATED (year added)
        val sdf = SimpleDateFormat("EEE (dd MMMM yyyy)", Locale.getDefault())
        return sdf.format(cal.time)
    }
}