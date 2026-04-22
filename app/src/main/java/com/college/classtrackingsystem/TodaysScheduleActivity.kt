package com.college.classtrackingsystem

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TodaysScheduleActivity : AppCompatActivity() {

    lateinit var tableLayout: TableLayout
    lateinit var loadingLayout: LinearLayout
    lateinit var txtTitle: TextView
    lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todays_schedule)

        tableLayout = findViewById(R.id.tableSchedule)
        loadingLayout = findViewById(R.id.loadingLayout)
        txtTitle = findViewById(R.id.txtTitle)

        db = FirebaseFirestore.getInstance()

        val section = intent.getStringExtra("section")

        if (section.isNullOrEmpty() || section == "Select a Section") {
            Toast.makeText(this, "Please select a section!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        txtTitle.text = "$section : Today's Schedule"

        loadSchedule(section)
    }

    private fun loadSchedule(section: String) {

        loadingLayout.visibility = View.VISIBLE

        // 🔥 FIXED DAY MAPPING
        val rawDay = SimpleDateFormat("EEE", Locale.getDefault())
            .format(Date())
            .lowercase()

        val today = when (rawDay) {
            "mon" -> "mon"
            "tue" -> "tues"
            "wed" -> "wed"
            "thu" -> "thurs"
            "fri" -> "fri"
            "sat" -> "sat"
            else -> "sun"
        }

        db.collection("sections")
            .document(section)
            .get()
            .addOnSuccessListener { doc ->

                loadingLayout.visibility = View.GONE

                val timetable = doc.get("timetable") as? Map<*, *> ?: return@addOnSuccessListener

                val todayMap = timetable[today] as? Map<*, *> ?: emptyMap<String, Any>()

                if (todayMap.isEmpty()) {
                    Toast.makeText(this, "No classes today", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                tableLayout.removeAllViews()

                addHeaderRow()

                // ✅ Ensure proper order P1 → P8
                val periodOrder = listOf("p1","p2","p3","p4","p5","p6","p7","p8")

                for (period in periodOrder) {

                    val data = todayMap[period] as? Map<*, *> ?: continue

                    val subject = data["subject"]
                        ?.toString()
                        ?.replace("\\s+".toRegex(), " ")
                        ?.trim()
                        ?: "-"

                    val room = data["class"]
                        ?.toString()
                        ?.replace("\\s+".toRegex(), " ")
                        ?.trim()
                        ?: "-"

                    val row = TableRow(this)

                    row.addView(createPeriodCard(period.uppercase()))
                    row.addView(createWhiteCard(subject))
                    row.addView(createWhiteCard(room))

                    tableLayout.addView(row)
                }
            }
    }

    private fun addHeaderRow() {

        val row = TableRow(this)

        row.addView(createHeaderCard("Period"))
        row.addView(createHeaderCard("Subject"))
        row.addView(createHeaderCard("Class"))

        tableLayout.addView(row)
    }

    private fun createHeaderCard(text: String): CardView {

        val card = CardView(this)
        card.setCardBackgroundColor(getColor(R.color.brownie))
        card.radius = 12f

        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(getColor(android.R.color.white))
        tv.textSize = 16f
        tv.gravity = Gravity.CENTER
        tv.setPadding(30, 30, 30, 30)

        card.addView(tv)

        val params = TableRow.LayoutParams()
        params.setMargins(8, 8, 8, 8)
        card.layoutParams = params

        return card
    }

    private fun createPeriodCard(text: String): CardView {

        val card = CardView(this)
        card.setCardBackgroundColor(getColor(R.color.light_caramel))
        card.radius = 12f

        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(getColor(android.R.color.white))
        tv.textSize = 15f
        tv.gravity = Gravity.CENTER
        tv.setPadding(30, 30, 30, 30)

        card.addView(tv)

        val params = TableRow.LayoutParams()
        params.setMargins(8, 8, 8, 8)
        card.layoutParams = params

        return card
    }

    private fun createWhiteCard(text: String): CardView {

        val card = CardView(this)
        card.setCardBackgroundColor(getColor(android.R.color.white))
        card.radius = 12f

        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(getColor(android.R.color.black))
        tv.textSize = 15f
        tv.gravity = Gravity.CENTER
        tv.setPadding(30, 30, 30, 30)

        card.addView(tv)

        val params = TableRow.LayoutParams()
        params.setMargins(8, 8, 8, 8)
        card.layoutParams = params

        return card
    }
}