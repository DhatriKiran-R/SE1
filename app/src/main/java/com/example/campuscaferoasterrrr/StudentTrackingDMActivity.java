package com.example.campuscaferoasterrrr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;

// Main Activity for Student Tracking (Dining Manager view)
public class StudentTrackingDMActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StudentTrackingAdapter adapter;
    private List<ShiftData> shiftDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_tracking_dmactivity);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.student_tracking_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        shiftDataList = new ArrayList<>();
        adapter = new StudentTrackingAdapter(shiftDataList);
        recyclerView.setAdapter(adapter);

        // Fetch the shift data for the current week
        fetchShiftDataForWeek();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_shift_management) {
                intent = new Intent(StudentTrackingDMActivity.this, ShiftManagementDMActivity.class);
            } else if (item.getItemId() == R.id.nav_swap_requests) {
                intent = new Intent(StudentTrackingDMActivity.this, SwapRequestsDMActivity.class);
            } else if (item.getItemId() == R.id.nav_student_tracking) {
                intent = new Intent(StudentTrackingDMActivity.this, StudentTrackingDMActivity.class);
            } else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(StudentTrackingDMActivity.this, CalendarDMActivity.class);
            } else if (item.getItemId() == R.id.nav_account) {
                // Show the submenu when Account item is clicked
                showAccountMenu();
                return true; // prevent going to next activity
            } else if (item.getItemId() == R.id.nav_logout) {
                // Handle logout directly
                mAuth.signOut();
                intent = new Intent(StudentTrackingDMActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else {
                return false; // For other items, return false
            }
            startActivity(intent);
            return true; // Return true to indicate the item was handled
        });
    }

    private void showAccountMenu() {
        View view = findViewById(R.id.nav_account); // This view is where the menu will be attached
        PopupMenu popupMenu = new PopupMenu(StudentTrackingDMActivity.this, view);
        getMenuInflater().inflate(R.menu.account_menu, popupMenu.getMenu());

        // Set click listeners for the submenu items (Profile and Logout)
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_profile) {
                // Handle profile option
                Intent intent = new Intent(StudentTrackingDMActivity.this, DiningManagerActivity.class);
                startActivity(intent);
                return true;
            } else if (menuItem.getItemId() == R.id.nav_logout) {
                // Handle logout option
                FirebaseAuth.getInstance().signOut();
                Intent logoutIntent = new Intent(StudentTrackingDMActivity.this, LoginActivity.class);
                startActivity(logoutIntent);
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show(); // Show the submenu
    }

    // Fetch shifts for the current week
    private void fetchShiftDataForWeek() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get the current weekId (example: "2024-W47")
        String currentWeekId = getCurrentWeekId();

        db.collection("shifts")
                .whereEqualTo("weekId", currentWeekId) // Filter for current week
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        shiftDataList.clear();
                        Map<String, ShiftData> studentShiftMap = new HashMap<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String studentEmail = document.getString("studentClockInId");
                            String shiftId = document.getString("shiftId");

                            // Get the worked hours from the 'duration' field
                            long workedHours = document.getLong("duration");
                            System.out.println(workedHours);
                            // Fetch student name from the "users" collection
                            fetchStudentName(studentEmail, studentName -> {
                                // Check if the student already exists in the map
                                if (studentShiftMap.containsKey(studentEmail)) {
                                    // Update the total worked hours for the student
                                    ShiftData existingData = studentShiftMap.get(studentEmail);
                                    System.out.println(existingData);
                                    existingData.setWorkedHours(existingData.getWorkedHours() + workedHours);
                                } else {
                                    // Add a new entry for the student
                                    ShiftData shiftData = new ShiftData(studentName, studentEmail, workedHours);
                                    studentShiftMap.put(studentEmail, shiftData);
                                }

                                // Update the shiftDataList and RecyclerView

                                shiftDataList.clear();
                                shiftDataList.addAll(studentShiftMap.values());
                                System.out.println(shiftDataList);
                                adapter.notifyDataSetChanged(); // Refresh the RecyclerView
                            });
                        }

                    } else {
                        Toast.makeText(this, "Failed to load shifts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Get current week ID in the format "YYYY-Wxx"
    private String getCurrentWeekId() {
        Calendar calendar = Calendar.getInstance();
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        return year + "-W" + String.format("%02d", weekOfYear);
    }

    // Fetch student name from "users" collection
    private void fetchStudentName(String email, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String studentName = task.getResult().getDocuments().get(0).getString("name");
                        callback.onCallback(studentName);
                    } else {
                        callback.onCallback("Unknown");
                    }
                });
    }

    private interface FirestoreCallback {
        void onCallback(String name);
    }

    // Model class for shift data
    private static class ShiftData {
        private String studentName;
        private String studentEmail;
        private String totalWorkedHours;

        private long workedHours;

        // Constructor
        public ShiftData(String studentName, String studentEmail, long workedHours) {
            this.studentName = studentName;
            this.studentEmail = studentEmail;
            this.workedHours = workedHours;
        }

        // Getters and Setters
        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getStudentEmail() {
            return studentEmail;
        }

        public void setStudentEmail(String studentEmail) {
            this.studentEmail = studentEmail;
        }

        public String getTotalWorkedHours() {
            return totalWorkedHours;
        }

        public void setTotalWorkedHours(String totalWorkedHours) {
            this.totalWorkedHours = totalWorkedHours;
        }

        public long getWorkedHours() {
            return workedHours;
        }

        public void setWorkedHours(long workedHours) {
            this.workedHours = workedHours;
        }
    }

    // Adapter for RecyclerView
    private static class StudentTrackingAdapter extends RecyclerView.Adapter<StudentTrackingAdapter.ViewHolder> {

        private final List<ShiftData> shiftDataList;

        public StudentTrackingAdapter(List<ShiftData> shiftDataList) {
            this.shiftDataList = shiftDataList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_tracking, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ShiftData shiftData = shiftDataList.get(position);
            holder.studentNameTextView.setText(shiftData.getStudentName());
            holder.totalWorkedHoursTextView.setText(String.valueOf(shiftData.getWorkedHours()) + " hours/20 hours");

        }

        @Override
        public int getItemCount() {
            return shiftDataList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView studentNameTextView;
            TextView totalWorkedHoursTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                studentNameTextView = itemView.findViewById(R.id.student_name);
                totalWorkedHoursTextView = itemView.findViewById(R.id.total_worked_hours);
            }
        }
    }
}
