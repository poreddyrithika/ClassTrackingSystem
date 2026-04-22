package com.college.classtrackingsystem

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class TodaysFreeClassesActivity : AppCompatActivity() {

    lateinit var containerPeriods: LinearLayout
    lateinit var loadingLayout: LinearLayout
    lateinit var db: FirebaseFirestore

    val periods = listOf("p1","p2","p3","p4","p5","p6","p7","p8")

    val periodTimings = mutableMapOf<String,String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todays_free_classes)

        containerPeriods = findViewById(R.id.containerPeriods)
        loadingLayout = findViewById(R.id.loadingLayout)

        db = FirebaseFirestore.getInstance()

        loadPeriodTimings()
    }

    private fun loadPeriodTimings() {

        loadingLayout.visibility = View.VISIBLE

        db.collection("periods")
            .get()
            .addOnSuccessListener { docs ->

                for(doc in docs){

                    val start = doc.getString("start") ?: ""
                    val end = doc.getString("end") ?: ""

                    periodTimings[doc.id] = "$start - $end"
                }

                loadFreeClasses()
            }
            .addOnFailureListener {
                loadingLayout.visibility = View.GONE
            }
    }

    private fun loadFreeClasses() {

        val today = when(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)){
            Calendar.MONDAY -> "mon"
            Calendar.TUESDAY -> "tues"
            Calendar.WEDNESDAY -> "wed"
            Calendar.THURSDAY -> "thurs"
            Calendar.FRIDAY -> "fri"
            Calendar.SATURDAY -> "sat"
            else -> "sun"
        }

        db.collection("extra_class_requests")
            .get()
            .addOnSuccessListener { extraDocs ->

                val extraRooms = mutableSetOf<String>()

                for (doc in extraDocs) {
                    val d = doc.getString("day")
                    val status = doc.getString("status")
                    val room = doc.getString("classroom")
                    if (d == today && status == "approved" && !room.isNullOrEmpty()) {
                        extraRooms.add(room)
                    }
                }

                // Now fetch classrooms
                db.collection("classrooms")
                    .get()
                    .addOnSuccessListener { docs ->

                        loadingLayout.visibility = View.GONE

                        val freeMap = mutableMapOf<String, MutableList<String>>()
                        for(p in periods) freeMap[p] = mutableListOf()

                        for(doc in docs){

                            val classroom = doc.id
                            val dayData = doc.get(today) as? Map<*, *>

                            for(p in periods){
                                val value = dayData?.get(p)?.toString()?.trim()
                                val occupied = !value.isNullOrEmpty() || extraRooms.contains(classroom)

                                if(!occupied) {
                                    freeMap[p]?.add(classroom)
                                }
                            }
                        }

                        showUI(freeMap)
                    }
                    .addOnFailureListener {
                        loadingLayout.visibility = View.GONE
                    }

            }
            .addOnFailureListener {
                loadingLayout.visibility = View.GONE
            }
    }

    private fun showUI(freeMap: Map<String,List<String>>) {

        containerPeriods.removeAllViews()

        for(p in periods){

            val block = layoutInflater.inflate(
                R.layout.item_period_block,
                containerPeriods,
                false
            )

            val title = block.findViewById<TextView>(R.id.txtPeriodTitle)

            val classroomContainer =
                block.findViewById<FlexboxLayout>(R.id.classroomContainer)

            val time = periodTimings[p] ?: ""

            title.text = "${p.uppercase()} ($time)"
            title.setTextColor(ContextCompat.getColor(this,R.color.light_caramel))

            val rooms = freeMap[p] ?: emptyList()

            if (rooms.isEmpty()) {
                // ✅ If no free classrooms, show message
                val noClassMsg = TextView(this).apply {
                    text = "No Classes Available!"
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(this@TodaysFreeClassesActivity, android.R.color.holo_red_dark))
                    gravity = Gravity.CENTER
                    setPadding(20, 20, 20, 20)
                }
                classroomContainer.addView(noClassMsg)
            } else {

                for (room in rooms) {

                    val card = CardView(this)

                    card.radius = 12f
                    card.setCardBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.white)
                    )

                    val tv = TextView(this)

                    tv.text = room
                    tv.gravity = Gravity.CENTER
                    tv.textSize = 15f
                    tv.setPadding(40, 25, 40, 25)

                    card.addView(tv)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    params.setMargins(12, 12, 12, 12)

                    card.layoutParams = params

                    // ✅ ADDED CLICK LISTENER
                    card.setOnClickListener {
                        showRoomDetails(room)
                    }

                    classroomContainer.addView(card)
                }
            }

            containerPeriods.addView(block)
        }
    }

    // ✅ NEW FUNCTION ADDED
    private fun showRoomDetails(room: String) {
        db.collection("classrooms").document(room).get()
            .addOnSuccessListener {
                val type = it.getString("type") ?: "N/A"
                val capacity = it.getString("capacity") ?: "N/A"

                android.app.AlertDialog.Builder(this)
                    .setTitle("Classroom Details")
                    .setMessage("Room: $room\nType: $type\nCapacity: $capacity")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }
}