package com.college.classtrackingsystem

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class ClassroomScheduleActivity : AppCompatActivity() {

    private lateinit var tableLayout: TableLayout
    private lateinit var loadingLayout: LinearLayout
    private lateinit var txtTitle: TextView
    private lateinit var db: FirebaseFirestore

    private val periods = listOf("p1","p2","p3","p4","p5","p6","p7","p8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classroom_schedule)

        val classroom = intent.getStringExtra("classroom") ?: "Unknown Room"

        tableLayout = findViewById(R.id.tableSchedule)
        loadingLayout = findViewById(R.id.loadingLayout)
        txtTitle = findViewById(R.id.titleRoom)

        if (classroom.isEmpty()) {
            Toast.makeText(this, "No classroom selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        txtTitle.text = "Room: $classroom"

        db = FirebaseFirestore.getInstance()

        loadSchedule(classroom)
    }

    // ✅ UPDATED CARD CELL (COMPACT + WRAP)
    private fun createCardCell(text: String, type: String): View {

        val card = CardView(this)

        val params = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(10, 10, 10, 10) // 🔥 increased spacing
        card.layoutParams = params

        card.radius = 16f
        card.cardElevation = 6f

        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER

        // 🔥 INCREASE SIZE
        tv.setPadding(30, 30, 30, 30)   // bigger padding
        tv.textSize = 15f               // bigger text
        tv.maxLines = 3                 // allow more content
        tv.isSingleLine = false

        when (type) {
            "header" -> {
                card.setCardBackgroundColor(Color.parseColor("#5D4037"))
                tv.setTextColor(Color.WHITE)
                tv.setTypeface(null, Typeface.BOLD)
            }
            "day" -> {
                card.setCardBackgroundColor(Color.parseColor("#C89B6D"))
                tv.setTextColor(Color.WHITE)
                tv.setTypeface(null, Typeface.BOLD)
            }
            else -> {
                card.setCardBackgroundColor(Color.WHITE)
                tv.setTextColor(Color.BLACK)
            }
        }

        card.addView(tv)
        return card
    }

    private fun loadSchedule(classroom: String) {

        loadingLayout.visibility = View.VISIBLE

        db.collection("classrooms").document(classroom)
            .get()
            .addOnSuccessListener { doc ->

                loadingLayout.visibility = View.GONE
                tableLayout.removeAllViews()

                val days = listOf("mon","tues","wed","thurs","fri","sat")

                // ✅ HEADER ROW
                val headerRow = TableRow(this)

                headerRow.addView(createCardCell("Day", "header"))

                for (p in periods) {
                    headerRow.addView(createCardCell(p.uppercase(), "header"))
                }

                tableLayout.addView(headerRow)

                // ✅ DATA ROWS
                for (day in days) {

                    val row = TableRow(this)

                    row.addView(createCardCell(day.uppercase(), "day"))

                    val dayMap = doc.get(day) as? Map<*, *> ?: emptyMap<String, String>()

                    for (p in periods) {

                        val value = dayMap[p]?.toString()?.takeIf { it.isNotBlank() } ?: "Free"

                        row.addView(createCardCell(value, "data"))
                    }

                    tableLayout.addView(row)
                }
            }
            .addOnFailureListener {
                loadingLayout.visibility = View.GONE
                Toast.makeText(this, "Failed to load schedule", Toast.LENGTH_SHORT).show()
            }
    }
}