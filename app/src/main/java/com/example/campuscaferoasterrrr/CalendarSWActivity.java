package com.example.campuscaferoasterrrr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CalendarSWActivity extends AppCompatActivity {

    private TextView shiftsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_swactivity);

        shiftsTextView = findViewById(R.id.shifts_text_view);

        // Initialize BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_request_swaps) {
                intent = new Intent(CalendarSWActivity.this, RequestSwapsActivity.class);
            } else if (item.getItemId() == R.id.nav_view_swaps) {
                intent = new Intent(CalendarSWActivity.this, ViewSwapRequestsSW.class);
            } else if (item.getItemId() == R.id.nav_profile) {
                intent = new Intent(CalendarSWActivity.this, StudentWorkerActivity.class);
            }  else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(CalendarSWActivity.this, CalendarSWActivity.class);
            } else if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(CalendarSWActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true; // Exit the listener for logout
            } else {
                return false; // Return false for unhandled items
            }
            startActivity(intent);
            return true; // Return true to indicate the item was handled
        });

        // Fetch shifts for the current week
        fetchShifts();
    }

    private void fetchShifts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        // Get the current week ID
        Calendar calendar = Calendar.getInstance();
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        String weekId = year + "-W" + weekOfYear;

        db.collection("shifts")
                .whereEqualTo("weekId", weekId)
                .whereEqualTo("studentClockInId", userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<String> shifts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String studentEmail = document.getString("studentClockInId");
                            String startTime = document.getString("startTime");
                            String endTime = document.getString("endTime");
                            String location = document.getString("location");
                            String workRole = document.getString("workRole");
                            String date = document.getString("date");

                            // Format the shift information and add it to the shifts list
                            String shiftInfo =
                                    "Date: " + date + "\n"
                                    + "Start: " + startTime.substring(startTime.length() - 5) + "\n"
                                    + "End: " + endTime.substring(endTime.length() - 5) + "\n"
                                    + "Location: " + location + "\n"
                                    + "Role: " + workRole + "\n\n";
                            shifts.add(shiftInfo);
                        }
                        displayShifts(shifts);
                    } else {
                        shiftsTextView.setText("Error fetching shifts: " + task.getException().getMessage());
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void displayShifts(ArrayList<String> shifts) {
        if (shifts.isEmpty()) {
            shiftsTextView.setText("No shifts scheduled for this week.");
        } else {
            StringBuilder builder = new StringBuilder();
            for (String shiftInfo : shifts) {
                builder.append(shiftInfo).append("\n");
            }
            shiftsTextView.setText(builder.toString());
        }
    }

}
