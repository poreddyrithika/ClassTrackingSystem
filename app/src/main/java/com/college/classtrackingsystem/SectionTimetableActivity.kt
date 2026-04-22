package com.college.classtrackingsystem

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class SectionTimetableActivity : AppCompatActivity() {

    lateinit var tableLayout: TableLayout
    lateinit var loadingLayout: LinearLayout
    lateinit var txtTitle: TextView
    lateinit var db: FirebaseFirestore

    val days = listOf("mon","tues","wed","thurs","fri","sat")
    val periods = listOf("p1","p2","p3","p4","p5","p6","p7","p8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_section_timetable)

        tableLayout = findViewById(R.id.tableTimetable)
        loadingLayout = findViewById(R.id.loadingLayout)
        txtTitle = findViewById(R.id.txtTitle)

        db = FirebaseFirestore.getInstance()

        val section = intent.getStringExtra("section")

        if (section.isNullOrEmpty() || section == "Select a Section") {
            Toast.makeText(this, "Please select a section!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        txtTitle.text = "$section : Timetable"

        loadTimetable(section)
    }

    private fun loadTimetable(section: String){

        loadingLayout.visibility = View.VISIBLE

        db.collection("sections")
            .document(section)
            .get()
            .addOnSuccessListener { doc ->

                loadingLayout.visibility = View.GONE

                val timetable = doc.get("timetable") as? Map<*, *> ?: return@addOnSuccessListener

                tableLayout.removeAllViews()

                addHeaderRow()

                for(day in days){

                    val row = TableRow(this)

                    row.addView(createDayCard(day.uppercase()))

                    val dayMap = timetable[day] as? Map<*, *>

                    for(period in periods){

                        val periodMap = dayMap?.get(period) as? Map<*, *>

                        val subject = periodMap?.get("subject")
                            ?.toString()
                            ?.trim()
                            ?.replace("\n", " ")
                            ?: "-"

                        val classroom = periodMap?.get("class")
                            ?.toString()
                            ?.trim()
                            ?.replace("\n", " ")
                            ?: ""

                        row.addView(createWhiteCard(subject, classroom))
                    }

                    tableLayout.addView(row)
                }
            }
    }

    private fun addHeaderRow(){

        val row = TableRow(this)

        row.addView(createHeaderCard("Day"))

        for(period in periods){
            row.addView(createHeaderCard(period.uppercase()))
        }

        tableLayout.addView(row)
    }

    private fun createHeaderCard(text:String): CardView {

        val card = CardView(this)
        card.setCardBackgroundColor(resources.getColor(R.color.brownie))
        card.radius = 12f

        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setTextColor(resources.getColor(android.R.color.white))
        tv.textSize = 15f
        tv.setPadding(25,25,25,25)

        card.addView(tv)

        val params = TableRow.LayoutParams()
        params.setMargins(8,8,8,8)
        card.layoutParams = params

        return card
    }

    private fun createDayCard(text:String): CardView {

        val card = CardView(this)
        card.setCardBackgroundColor(resources.getColor(R.color.light_caramel))
        card.radius = 12f

        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setTextColor(resources.getColor(android.R.color.white))
        tv.textSize = 15f
        tv.setPadding(25,25,25,25)

        card.addView(tv)

        val params = TableRow.LayoutParams()
        params.setMargins(8,8,8,8)
        card.layoutParams = params

        return card
    }

    private fun createWhiteCard(subject: String, classroom: String): CardView {

        val card = CardView(this)
        card.setCardBackgroundColor(resources.getColor(android.R.color.white))
        card.radius = 12f

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(20, 20, 20, 20)

        // Subject (normal text)
        val subjectTv = TextView(this)
        subjectTv.text = subject
        subjectTv.setTextColor(resources.getColor(android.R.color.black))
        subjectTv.textSize = 14f
        subjectTv.gravity = Gravity.CENTER

        // Classroom
        val classTv = TextView(this)
        classTv.text = classroom
        classTv.setTextColor(resources.getColor(android.R.color.darker_gray))
        classTv.textSize = 12f
        classTv.gravity = Gravity.CENTER

        layout.addView(subjectTv)

        if (classroom.isNotEmpty()) {
            layout.addView(classTv)
        }

        card.addView(layout)

        val params = TableRow.LayoutParams()
        params.setMargins(8, 8, 8, 8)
        card.layoutParams = params

        return card
    }
}