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

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var spinnerSection: Spinner
    private lateinit var cardCurrentFree: CardView
    private lateinit var cardTodayFree: CardView
    private lateinit var cardTodayClasses: CardView
    private lateinit var cardViewTimetable: CardView
    private lateinit var db: FirebaseFirestore
    private val sectionList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.dashboardToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        spinnerSection = findViewById(R.id.spinnerSection)
        cardCurrentFree = findViewById(R.id.cardCurrentFree)
        cardTodayFree = findViewById(R.id.cardTodayFree)
        cardTodayClasses = findViewById(R.id.cardTodayClasses)
        cardViewTimetable = findViewById(R.id.cardViewTimetable)

        db = FirebaseFirestore.getInstance()
        loadSections()

        // Free Classrooms Now
        cardCurrentFree.setOnClickListener {
            try {
                startActivity(Intent(this, FreeClassroomActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "FreeClassroomActivity not found!", Toast.LENGTH_SHORT).show()
            }
        }

        // Today's free classes
        cardTodayFree.setOnClickListener {
            startActivity(Intent(this, TodaysFreeClassesActivity::class.java))
        }

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
    }

    private fun loadSections() {
        sectionList.clear()
        sectionList.add("Select here")

        db.collection("sections").get()
            .addOnSuccessListener { docs ->
                for (doc in docs) sectionList.add(doc.id)
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sectionList)
                spinnerSection.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load sections", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ EXIT CONFIRMATION (UNCHANGED)
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                finishAffinity()
            }
            .setNegativeButton("No", null)
            .show()
    }

    // ✅ MENU (3 DOTS)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    // ✅ LOGOUT
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