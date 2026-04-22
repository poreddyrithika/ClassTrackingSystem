package com.college.classtrackingsystem

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class RequestExtraClassActivity : AppCompatActivity() {

    private lateinit var etSubject: EditText
    private lateinit var spinnerSection: Spinner
    private lateinit var spinnerDay: Spinner
    private lateinit var spinnerPeriod: Spinner
    private lateinit var spinnerClassroom: Spinner
    private lateinit var btnSubmit: Button
    private lateinit var containerRequests: LinearLayout

    private val db = FirebaseFirestore.getInstance()

    private var selectedDayKey = ""
    private var selectedPeriodKey = ""
    private var facultyId: String = ""

    // 🔥 map: display → key
    private val dayDisplayMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_extra_class)

        etSubject = findViewById(R.id.etSubject)
        spinnerSection = findViewById(R.id.spinnerSection)
        spinnerDay = findViewById(R.id.spinnerDay)
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        spinnerClassroom = findViewById(R.id.spinnerClassroom)
        btnSubmit = findViewById(R.id.btnSubmit)
        containerRequests = findViewById(R.id.containerRequests)

        // ✅ GET FACULTY ID
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        facultyId = prefs.getString("userId", "")?.trim() ?: ""

        if (facultyId.isEmpty()) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        spinnerClassroom.adapter = createAdapter(listOf("Choose Classroom"))

        loadSections()
        setupDays()
        loadPeriods()

        spinnerDay.onItemSelectedListener = listener
        spinnerPeriod.onItemSelectedListener = listener

        spinnerClassroom.setOnTouchListener { _, _ ->
            if (selectedDayKey.isEmpty() || selectedPeriodKey.isEmpty()) {
                Toast.makeText(this, "Please select Day & Period first", Toast.LENGTH_SHORT).show()
                return@setOnTouchListener true
            }
            false
        }

        btnSubmit.setOnClickListener {
            submitRequest()
        }

        loadFacultyRequests()
    }

    // ---------------- LISTENER ----------------

    private val listener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {

            selectedDayKey = dayDisplayMap[spinnerDay.selectedItem.toString()] ?: ""

            selectedPeriodKey = if (spinnerPeriod.selectedItemPosition > 0) {
                "p${spinnerPeriod.selectedItemPosition}"
            } else ""

            if (selectedDayKey.isNotEmpty() && selectedPeriodKey.isNotEmpty()) {
                loadFreeClassrooms()
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    // ---------------- DAYS ----------------

    private fun setupDays() {

        val list = mutableListOf("Select Day")
        val cal = Calendar.getInstance()

        val sdf = SimpleDateFormat("EEEE (dd MMM)", Locale.getDefault())

        val keyMap = mapOf(
            "monday" to "mon",
            "tuesday" to "tues",
            "wednesday" to "wed",
            "thursday" to "thurs",
            "friday" to "fri",
            "saturday" to "sat"
        )

        var count = 0

        while (count < 7) {

            val fullDay = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())!!
            val key = keyMap[fullDay.lowercase()]

            if (key != null) {

                val display = sdf.format(cal.time)

                list.add(display)
                dayDisplayMap[display] = key

                count++
            }

            cal.add(Calendar.DATE, 1)
        }

        spinnerDay.adapter = createAdapter(list)
    }

    // ---------------- PERIODS ----------------

    private fun loadPeriods() {

        val list = mutableListOf("Select Period")

        db.collection("periods").get().addOnSuccessListener { docs ->

            val filtered = docs.filter { it.id.startsWith("p") }
                .sortedBy { it.id.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

            for (doc in filtered) {
                val start = doc.getString("start") ?: ""
                val end = doc.getString("end") ?: ""
                list.add("${doc.id.uppercase()} ($start - $end)")
            }

            spinnerPeriod.adapter = createAdapter(list)
        }
    }

    // ---------------- SECTIONS ----------------

    private fun loadSections() {
        val list = mutableListOf("Select Section")

        db.collection("sections").get().addOnSuccessListener {
            for (doc in it) list.add(doc.id)
            spinnerSection.adapter = createAdapter(list)
        }
    }

    // ---------------- CLASSROOM LOGIC ----------------

    private fun loadFreeClassrooms() {

        val list = mutableListOf("Choose Classroom")

        db.collection("extra_class_requests")
            .get()
            .addOnSuccessListener { extraDocs ->

                val extraRooms = mutableSetOf<String>()

                for (doc in extraDocs) {
                    val d = doc.getString("day")
                    val p = doc.getString("period")
                    val status = doc.getString("status")
                    val room = doc.getString("classroom")

                    if (d == selectedDayKey &&
                        p == selectedPeriodKey &&
                        status == "approved" &&
                        !room.isNullOrEmpty()
                    ) {
                        extraRooms.add(room.trim())
                    }
                }

                db.collection("classrooms")
                    .get()
                    .addOnSuccessListener { docs ->

                        for (doc in docs) {

                            val roomId = doc.id.trim()
                            val type = doc.getString("type") ?: "Room"
                            val capacity = doc.getString("capacity") ?: "N/A"

                            val dayData = doc.get(selectedDayKey) as? Map<*, *>
                            val value = dayData?.get(selectedPeriodKey)?.toString()?.trim()

                            val occupied =
                                !value.isNullOrEmpty() || extraRooms.contains(roomId)

                            if (!occupied) {
                                list.add("$roomId ($type, $capacity)")
                            }
                        }

                        if (list.size == 1) list.add("No classrooms available")

                        spinnerClassroom.adapter = createAdapter(list)
                    }
            }
    }

    // ---------------- SUBMIT ----------------

    private fun submitRequest() {

        val subject = etSubject.text.toString().trim()

        if (subject.isEmpty() ||
            spinnerSection.selectedItemPosition == 0 ||
            spinnerDay.selectedItemPosition == 0 ||
            spinnerPeriod.selectedItemPosition == 0 ||
            spinnerClassroom.selectedItemPosition == 0
        ) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            return
        }

        val roomText = spinnerClassroom.selectedItem.toString()
        val roomId = roomText.substringBefore(" (").trim()

        val data = hashMapOf(
            "facultyId" to facultyId,
            "subject" to subject,
            "section" to spinnerSection.selectedItem.toString(),
            "day" to selectedDayKey,
            "period" to selectedPeriodKey,
            "classroom" to roomId,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("extra_class_requests")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Request Sent!", Toast.LENGTH_SHORT).show()

                etSubject.setText("")
                spinnerSection.setSelection(0)
                spinnerDay.setSelection(0)
                spinnerPeriod.setSelection(0)
                spinnerClassroom.adapter = createAdapter(listOf("Choose Classroom"))

                loadFacultyRequests()
            }
    }

    // ---------------- READABLE DAY ----------------

    private fun getReadableDay(dayKey: String): String {

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEEE (dd MMM)", Locale.getDefault())

        val map = mapOf(
            "monday" to "mon",
            "tuesday" to "tues",
            "wednesday" to "wed",
            "thursday" to "thurs",
            "friday" to "fri",
            "saturday" to "sat"
        )

        var count = 0

        while (count < 7) {

            val currentDay = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())!!
            val key = map[currentDay.lowercase()]

            if (key == dayKey) {
                return sdf.format(cal.time)
            }

            cal.add(Calendar.DATE, 1)
            count++
        }

        return dayKey
    }

    // ---------------- REQUEST DISPLAY ----------------

    private fun loadFacultyRequests() {

        containerRequests.removeAllViews()

        db.collection("extra_class_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->

                var hasData = false
                var lastDate = ""

                for (doc in docs) {

                    val dbId = doc.getString("facultyId")?.trim()
                    if (dbId != facultyId) continue

                    hasData = true

                    val timestamp = when (val ts = doc.get("timestamp")) {
                        is Long -> ts
                        is Double -> ts.toLong()
                        is String -> ts.toLongOrNull() ?: 0L
                        is com.google.firebase.Timestamp -> ts.toDate().time
                        else -> 0L
                    }

                    val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                        .format(Date(timestamp))

                    // ✅ DATE HEADER
                    if (dateStr != lastDate) {
                        val dateHeader = TextView(this)
                        dateHeader.text = dateStr
                        dateHeader.textSize = 20f
                        dateHeader.setTypeface(null, android.graphics.Typeface.BOLD)
                        dateHeader.setPadding(10, 30, 10, 10)

                        containerRequests.addView(dateHeader)
                        lastDate = dateStr
                    }

                    val subject = doc.getString("subject") ?: "-"
                    val section = doc.getString("section") ?: "-"
                    val dayKey = doc.getString("day") ?: "-"
                    val period = doc.getString("period") ?: "-"
                    val room = doc.getString("classroom") ?: "-"
                    val status = doc.getString("status") ?: "pending"

                    val formattedDay = getFormattedDayWithYear(dayKey, timestamp)

                    val tv = TextView(this)

                    tv.text =
                        "Subject: $subject\n" +
                                "Section: $section\n" +
                                "Day: $formattedDay\n" +
                                "Period: $period\n" +
                                "Room: $room\n" +
                                "Status: $status"

                    // ✅ COLORS
                    when (status.lowercase()) {
                        "approved", "accepted" ->
                            tv.setBackgroundColor(0xFFC8E6C9.toInt())

                        "rejected" ->
                            tv.setBackgroundColor(0xFFFFCDD2.toInt())

                        else ->
                            tv.setBackgroundColor(0xFFFFF9C4.toInt())
                    }

                    tv.setPadding(20, 20, 20, 20)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 10, 0, 10)

                    tv.layoutParams = params

                    containerRequests.addView(tv)

                    // ✅ GAP (NO LINE)
                    val space = View(this)
                    space.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        20
                    )
                    containerRequests.addView(space)
                }

                // ✅ NO DATA MESSAGE
                if (!hasData) {
                    val tv = TextView(this)
                    tv.text = "No requests sent yet!"
                    tv.textSize = 16f
                    tv.setPadding(20, 20, 20, 20)
                    tv.setTextColor(android.graphics.Color.DKGRAY)

                    containerRequests.addView(tv)
                }
            }
    }
    private fun getFormattedDayWithYear(dayKey: String, timestamp: Long): String {

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

        val sdf = SimpleDateFormat("EEE (dd MMMM yyyy)", Locale.getDefault())
        return sdf.format(cal.time)
    }

    // ---------------- ADAPTER ----------------

    private fun createAdapter(list: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, list).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}