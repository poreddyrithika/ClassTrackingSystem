package com.college.classtrackingsystem

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var spinnerSection: Spinner
    private lateinit var spinnerClassroom: Spinner

    private lateinit var cardCurrentFree: CardView
    private lateinit var cardTodayFree: CardView
    private lateinit var cardTodayClasses: CardView
    private lateinit var cardViewTimetable: CardView
    private lateinit var cardClassrooms: CardView
    private lateinit var cardClassReq: CardView
    private lateinit var cardEditDB: CardView

    private lateinit var db: FirebaseFirestore

    private val sectionList = mutableListOf<String>()
    private val classroomList = mutableListOf<String>()

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.dashboardToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 🔹 Initialize Views
        spinnerSection = findViewById(R.id.spinnerSection)
        spinnerClassroom = findViewById(R.id.spinnerClassroom)

        cardCurrentFree = findViewById(R.id.cardCurrentFree)
        cardTodayFree = findViewById(R.id.cardTodayFree)
        cardTodayClasses = findViewById(R.id.cardTodayClasses)
        cardViewTimetable = findViewById(R.id.cardViewTimetable)
        cardClassrooms = findViewById(R.id.cardClassrooms)
        cardClassReq = findViewById(R.id.cardClassReq)
        cardEditDB = findViewById(R.id.cardEditDB)

        db = FirebaseFirestore.getInstance()

        // 🔹 Load data into spinners
        loadSections()
        loadClassrooms()

        // 🔥 BUTTON LOGIC

        // Free classrooms now
        cardCurrentFree.setOnClickListener {
            startActivity(Intent(this, FreeClassroomActivity::class.java))
        }

        // Today's free classrooms
        cardTodayFree.setOnClickListener {
            startActivity(Intent(this, TodaysFreeClassesActivity::class.java))
        }

        // Classroom schedule (🔥 NEW)
        cardClassrooms.setOnClickListener {

            val selectedClassroom = spinnerClassroom.selectedItem.toString()

            if (selectedClassroom == "Select here") {
                Toast.makeText(this, "Please select a classroom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, ClassroomScheduleActivity::class.java)
            intent.putExtra("classroom", selectedClassroom)
            startActivity(intent)
        }

        // Today's section schedule
        cardTodayClasses.setOnClickListener {

            val selectedSection = spinnerSection.selectedItem.toString()

            if (selectedSection == "Select here") {
                Toast.makeText(this, "Please select a section", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, TodaysScheduleActivity::class.java)
            intent.putExtra("section", selectedSection)
            startActivity(intent)
        }

        // Full timetable
        cardViewTimetable.setOnClickListener {

            val selectedSection = spinnerSection.selectedItem.toString()

            if (selectedSection == "Select here") {
                Toast.makeText(this, "Please select a section", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, SectionTimetableActivity::class.java)
            intent.putExtra("section", selectedSection)
            startActivity(intent)
        }

        // (Optional buttons - you can implement later)
        cardClassReq.setOnClickListener {
            startActivity(Intent(this, AdminRequestsActivity::class.java))
        }

        cardEditDB.setOnClickListener {

            if (!isInternetAvailable()) {
                Toast.makeText(this, "Please make sure your network is ON", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Opening Database...", Toast.LENGTH_SHORT).show()

            val url = "https://console.firebase.google.com/project/classtrackingsystem/firestore/data"

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)

            startActivity(intent)
        }
    }

    // 🔹 Load Sections
    private fun loadSections() {
        sectionList.clear()
        sectionList.add("Select here")

        db.collection("sections").get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    sectionList.add(doc.id)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    sectionList
                )
                spinnerSection.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load sections", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Load Classrooms
    private fun loadClassrooms() {
        classroomList.clear()
        classroomList.add("Select here")

        db.collection("classrooms").get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    classroomList.add(doc.id)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    classroomList
                )
                spinnerClassroom.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load classrooms", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 Back Press
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finishAffinity() }
            .setNegativeButton("No", null)
            .show()
    }

    // 🔹 Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    // 🔹 Logout
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_logout -> {

                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes") { _, _ ->

                        val prefs: SharedPreferences =
                            getSharedPreferences("AppPrefs", MODE_PRIVATE)

                        prefs.edit().clear().apply()

                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("No", null)
                    .show()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}