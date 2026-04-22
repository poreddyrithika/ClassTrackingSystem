package com.college.classtrackingsystem

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class FreeClassroomActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var freeRoomsContainer: LinearLayout
    private lateinit var loadingLayout: LinearLayout
    private lateinit var radioCurrent: RadioButton
    private lateinit var radioNext: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_classroom)

        db = FirebaseFirestore.getInstance()
        freeRoomsContainer = findViewById(R.id.freeRoomsContainer)
        loadingLayout = findViewById(R.id.loadingLayout)
        radioCurrent = findViewById(R.id.radioCurrent)
        radioNext = findViewById(R.id.radioNext)

        updateRadioTexts()

        radioCurrent.setOnClickListener { findClassrooms(false) }
        radioNext.setOnClickListener { findClassrooms(true) }

        radioCurrent.post {
            radioCurrent.isChecked = true
            findClassrooms(false)
        }
    }

    // --- Time Logic ---
    private fun getCurrentPeriod(): Int {
        val cal = Calendar.getInstance()
        val time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return when {
            time in 495..545 -> 1
            time in 545..595 -> 2
            time in 610..660 -> 3
            time in 660..710 -> 4
            time in 710..760 -> 5
            time in 820..870 -> 6
            time in 870..920 -> 7
            time in 920..965 -> 8
            else -> -1
        }
    }

    // --- Radio Text ---
    private fun updateRadioTexts() {

        val timings = mapOf(
            1 to "08:15-09:05",
            2 to "09:05-09:55",
            3 to "10:10-11:00",
            4 to "11:00-11:50",
            5 to "11:50-12:40",
            6 to "13:40-14:30",
            7 to "14:30-15:20",
            8 to "15:20-16:05"
        )

        val cal = Calendar.getInstance()
        val time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val startTimes = listOf(
            1 to 495,
            2 to 545,
            3 to 610,
            4 to 660,
            5 to 710,
            6 to 820,
            7 to 870,
            8 to 920
        )

        val current = getCurrentPeriod()

        if (current != -1) {
            val next = if (current < 8) current + 1 else -1

            radioCurrent.text = "Current (${timings[current] ?: "N/A"})"

            if (next != -1) {
                radioNext.text = "Upcoming (${timings[next] ?: "N/A"})"
            } else {
                radioNext.text = "Upcoming (No more classes for today)"
            }

        } else {
            var nextPeriod = -1

            for ((p, start) in startTimes) {
                if (time < start) {
                    nextPeriod = p
                    break
                }
            }

            radioCurrent.text = "Current (No class running)"

            if (nextPeriod != -1) {
                radioNext.text = "Upcoming (${timings[nextPeriod] ?: "N/A"})"
            } else {
                radioNext.text = "Upcoming (No more classes)"
            }
        }
    }

    // --- FETCH ---
    // --- FETCH --- (updated for approved extra classes)
    private fun findClassrooms(next: Boolean) {
        loadingLayout.visibility = View.VISIBLE

        val cal = Calendar.getInstance()
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            showMessage("No Classes Available!")
            return
        }

        val current = getCurrentPeriod()

        val startTimes = listOf(
            1 to 495, 2 to 545, 3 to 610, 4 to 660, 5 to 710, 6 to 820, 7 to 870, 8 to 920
        )

        var nextPeriod = -1
        val time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        for ((p, start) in startTimes) if (time < start) { nextPeriod = p; break }

        val period = when {
            current != -1 && !next -> current
            current != -1 && next && current < 8 -> current + 1
            current != -1 && next && current == 8 -> -1
            current == -1 && next -> nextPeriod
            else -> -1
        }

        if (period == -1) {
            showMessage(if (next) "No More Classes Today!" else "No classes scheduled now")
            return
        }

        val periodKey = "p$period"
        val day = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "mon"
            Calendar.TUESDAY -> "tues"
            Calendar.WEDNESDAY -> "wed"
            Calendar.THURSDAY -> "thurs"
            Calendar.FRIDAY -> "fri"
            Calendar.SATURDAY -> "sat"
            else -> "sun"
        }

        // --- Step 1: Get approved extra classes
        db.collection("extra_class_requests").get()
            .addOnSuccessListener { extraDocs ->
                val extraRooms = mutableSetOf<String>()
                for (doc in extraDocs) {
                    val d = doc.getString("day")
                    val p = doc.getString("period")
                    val status = doc.getString("status")
                    val room = doc.getString("classroom")
                    if (d == day && p == periodKey && status == "approved" && !room.isNullOrEmpty()) {
                        extraRooms.add(room)
                    }
                }

                // --- Step 2: Fetch classrooms
                db.collection("classrooms").get()
                    .addOnSuccessListener { docs ->
                        val roomsList = mutableListOf<Triple<String, Int, String>>()
                        for (doc in docs) {
                            val room = doc.id
                            val dayData = doc.get(day) as? Map<*, *>
                            val value = dayData?.get(periodKey)?.toString()?.trim()
                            val occupied = !value.isNullOrEmpty()

                            val status = when {
                                extraRooms.contains(room) -> 2       // ✅ approved extra class -> RED
                                !occupied -> 1                        // ✅ free -> GREEN
                                else -> 0                             // ❌ normal occupied -> GRAY / ignore
                            }
                            if (status == 0) continue

                            val floor = if (room.firstOrNull()?.isDigit() == true) {
                                val num = room.first().toString().toInt()
                                when (num) {1 -> "1st Floor"; 2 -> "2nd Floor"; 3 -> "3rd Floor"; else -> "${num}th Floor"}
                            } else "Other"

                            roomsList.add(Triple(room, status, floor))
                        }

                        if (roomsList.isEmpty()) {
                            showMessage("No Free Classrooms Available!")
                            return@addOnSuccessListener
                        }

                        displayRooms(roomsList)
                        loadingLayout.visibility = View.GONE
                    }
                    .addOnFailureListener { showMessage("Failed to load classrooms.") }
            }
            .addOnFailureListener { showMessage("Failed to load data. Check your internet.") }
    }


    private fun showMessage(msg: String) {
        freeRoomsContainer.removeAllViews()

        val tv = TextView(this).apply {
            text = msg
            textSize = 18f
            setTextColor(Color.parseColor("#6F4E37"))
            gravity = Gravity.CENTER
            setPadding(30, 60, 30, 60)
        }

        freeRoomsContainer.addView(tv)
        loadingLayout.visibility = View.GONE
    }

    // --- DISPLAY ---
    private fun displayRooms(rooms: List<Triple<String, Int, String>>) {

        freeRoomsContainer.removeAllViews()

        val floorMap = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        val otherRooms = mutableListOf<Pair<String, Int>>()

        for ((room, status, floor) in rooms) {
            if (floor == "Other") {
                otherRooms.add(Pair(room, status))
            } else {
                floorMap.getOrPut(floor) { mutableListOf() }
                    .add(Pair(room, status))
            }
        }

        // --- OTHER ROOMS (A01 etc.) ---
        if (otherRooms.isNotEmpty()) {
            val flex = FlexboxLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                flexWrap = com.google.android.flexbox.FlexWrap.WRAP
            }

            for ((room, status) in otherRooms) {
                flex.addView(createCard(room, status))
            }

            freeRoomsContainer.addView(flex)
        }

        // --- SORT FLOORS DESC ---
        val sortedFloors = floorMap.keys.sortedByDescending {
            it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        }

        for (floor in sortedFloors) {

            val title = TextView(this).apply {
                text = floor
                textSize = 18f
                setTextColor(getColor(R.color.light_caramel))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(10, 20, 10, 10)
            }

            freeRoomsContainer.addView(title)

            val flex = FlexboxLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                flexWrap = com.google.android.flexbox.FlexWrap.WRAP
            }

            for ((room, status) in floorMap[floor]!!) {
                flex.addView(createCard(room, status))
            }

            freeRoomsContainer.addView(flex)
        }
    }

    // --- CARD ---
    private fun createCard(room: String, status: Int): CardView {

        val card = CardView(this).apply {
            radius = 10f
            setCardBackgroundColor(
                when (status) {
                    1 -> Color.parseColor("#4CAF50")
                    2 -> Color.RED
                    else -> Color.GRAY
                }
            )
        }

        val params = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(12, 12, 12, 12)
        card.layoutParams = params

        val tv = TextView(this).apply {
            text = room
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(30, 20, 30, 20)
        }

        card.addView(tv)
        card.setOnClickListener { showRoomDetails(room) }

        return card
    }

    // --- DETAILS ---
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
