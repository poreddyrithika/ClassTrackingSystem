package com.college.classtrackingsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        runnable = () -> {

            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
            String role = prefs.getString("userRole", null);

            Intent intent;

            if (isLoggedIn && role != null) {

                switch (role.toLowerCase()) {

                    case "admin":
                        intent = new Intent(this, AdminDashboardActivity.class);
                        break;

                    case "faculty":
                        intent = new Intent(this, FacultyDashboardActivity.class);
                        break;

                    case "student":
                        intent = new Intent(this, StudentDashboardActivity.class);
                        break;

                    default:
                        intent = new Intent(this, LoginActivity.class);
                        break;
                }

            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        };

        handler.postDelayed(runnable, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ VERY IMPORTANT FIX
        handler.removeCallbacks(runnable);
    }
}