package com.example.campuscaferoasterrrr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

public class CalendarDMActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner workRoleSpinner, locationSpinner;
    private Button checkShiftsButton;
    private TextView shiftsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_dmactivity);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Initialize UI elements
        calendarView = findViewById(R.id.calendar_view);
        workRoleSpinner = findViewById(R.id.work_role_spinner);
        locationSpinner = findViewById(R.id.location_spinner);
        checkShiftsButton = findViewById(R.id.check_shifts_button);
        shiftsTextView = findViewById(R.id.shifts_text_view);

        // Populate spinners (you should replace these with your actual data)
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        workRoleSpinner.setAdapter(roleAdapter);

        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(this,
                R.array.locations_array, android.R.layout.simple_spinner_item);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        // Set default date to today's date initially
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        AtomicReference<String> selectedDate = new AtomicReference<>(sdf.format(calendar.getTime()));  // Set current date as default
        System.out.println("Initial Selected Date (default): " + selectedDate);

        // Attach a listener to capture date selection from CalendarView
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);  // Set the selected date
            selectedDate.set(sdf.format(selectedCalendar.getTime()));  // Update the selected date
            System.out.println("Selected Date: " + selectedDate);  // Debugging
        });

        // When the "Check Shifts" button is clicked
        checkShiftsButton.setOnClickListener(v -> checkShifts(selectedDate.get()));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_shift_management) {
                intent = new Intent(CalendarDMActivity.this, ShiftManagementDMActivity.class);
            } else if (item.getItemId() == R.id.nav_swap_requests) {
                intent = new Intent(CalendarDMActivity.this, SwapRequestsDMActivity.class);
            } else if (item.getItemId() == R.id.nav_student_tracking) {
                intent = new Intent(CalendarDMActivity.this, StudentTrackingDMActivity.class);
            } else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(CalendarDMActivity.this, CalendarDMActivity.class);
            } else if (item.getItemId() == R.id.nav_account) {
                // Show the submenu when Account item is clicked
                showAccountMenu();
                return true; // prevent going to next activity
            } else if (item.getItemId() == R.id.nav_logout) {
                // Handle logout directly
                mAuth.signOut();
                intent = new Intent(CalendarDMActivity.this, LoginActivity.class);
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
        PopupMenu popupMenu = new PopupMenu(CalendarDMActivity.this, view);
        getMenuInflater().inflate(R.menu.account_menu, popupMenu.getMenu());

        // Set click listeners for the submenu items (Profile and Logout)
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_profile) {
                // Handle profile option
                Intent intent = new Intent(CalendarDMActivity.this, DiningManagerActivity.class);
                startActivity(intent);
                return true;
            } else if (menuItem.getItemId() == R.id.nav_logout) {
                // Handle logout option
                FirebaseAuth.getInstance().signOut();
                Intent logoutIntent = new Intent(CalendarDMActivity.this, LoginActivity.class);
                startActivity(logoutIntent);
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show(); // Show the submenu
    }


    private void checkShifts(String selectedDate) {
        // Get selected work role and location from the spinners
        String selectedWorkRole = workRoleSpinner.getSelectedItem().toString().trim().toLowerCase(Locale.getDefault());
        String selectedLocation = locationSpinner.getSelectedItem().toString().trim().toLowerCase(Locale.getDefault());

        // Debugging: Print the values to ensure correct string matches
        System.out.println("Query Work Role: " + selectedWorkRole);
        System.out.println("Query Location: " + selectedLocation);

        // Fetch shifts from Firestore based on selected values
        fetchShifts(selectedDate, selectedWorkRole, selectedLocation);  // Use the updated selectedDate
    }

    @SuppressLint("SetTextI18n")
    private void fetchShifts(String date, String workRole, String location) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Debugging: Print normalized values
        System.out.println("Fetching shifts for Date: " + date + ", Role: " + workRole + ", Location: " + location);

        db.collection("shifts")
                .whereEqualTo("workRole", workRole)
                .whereEqualTo("location", location)
                .whereEqualTo("date", date)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            System.out.println("No shifts found for the given date, role, and location.");
                            shiftsTextView.setText("No shifts scheduled for this date.");
                        } else {
                            ArrayList<Map<String, Object>> shifts = new ArrayList<>();
                            // Use a counter to keep track of the number of requests processed
                            AtomicInteger processedCount = new AtomicInteger(0);
                            int totalDocuments = task.getResult().size();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Get the student email from the shift document
                                String studentEmail = document.getString("studentClockInId");

                                // Query the "users" collection to get the student's name
                                FirebaseFirestore db2 = FirebaseFirestore.getInstance();
                                db2.collection("users")
                                        .whereEqualTo("email", studentEmail) // Assuming the "users" collection has an "email" field
                                        .get()
                                        .addOnCompleteListener(userTask -> {
                                            if (userTask.isSuccessful() && !userTask.getResult().isEmpty()) {
                                                // Assuming "name" field exists in the "users" document
                                                String studentName = userTask.getResult().getDocuments().get(0).getString("name");

                                                // Add the studentName to the shift document data
                                                Map<String, Object> shiftData = document.getData();
                                                shiftData.put("studentName", studentName); // Add student name to the shift data

                                                // Add the updated shift data (with student name) to the list
                                                shifts.add(shiftData);
                                            } else {
                                                // If no user found, add a default value (like "Unknown") or skip adding this shift
                                                Map<String, Object> shiftData = document.getData();
                                                shiftData.put("studentName", "Unknown"); // Default value if student name is not found

                                                // Add the updated shift data (with student name) to the list
                                                shifts.add(shiftData);
                                            }

                                            // Increment the processed count
                                            if (processedCount.incrementAndGet() == totalDocuments) {
                                                // All shifts have been processed, display them
                                                System.out.println(shifts);  // Print all the shifts once all are processed
                                                displayShifts(shifts); // Pass the entire document data to the display method
                                            }
                                        });
                            }
                        }
                    } else {
                        System.out.println("Error fetching shifts: " + task.getException().getMessage());
                        shiftsTextView.setText("Error fetching shifts: " + task.getException().getMessage());
                    }
                });
    }


    @SuppressLint("SetTextI18n")
    private void displayShifts(ArrayList<Map<String, Object>> shifts) {
        if (shifts.isEmpty()) {
            shiftsTextView.setText("No shifts scheduled for this date.");
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < shifts.size(); i++) {
                Map<String, Object> shift = shifts.get(i);
                builder.append("Shift ").append(i + 1).append(":\n");
                if (shift.containsKey("studentName")) {
                    builder.append("Student Name: ").append(shift.get("studentName")).append("\n");
                }
                for (Map.Entry<String, Object> entry : shift.entrySet()) {
                    if (entry.getKey().equals("studentName")) {
                        continue;
                    }
                    switch (entry.getKey()) {
                        case "date":
                            builder.append("Date: ").append(entry.getValue()).append("\n");
                            break;
                        case "duration":
                            builder.append("Duration: ").append(entry.getValue()).append(" hours\n");
                            break;
                        case "endTime":
                            builder.append("End Time: ").append(entry.getValue()).append("\n");
                            break;
                        case "location":
                            builder.append("Location: ").append(entry.getValue()).append("\n");
                            break;
                        case "requestedSwap":
                            builder.append("Requested Swap: ").append(entry.getValue()).append("\n");
                            break;
                        case "startTime":
                            builder.append("Start Time: ").append(entry.getValue()).append("\n");
                            break;
                        case "studentClockInId":
                            builder.append("Student Email: ").append(entry.getValue()).append("\n");
                            break;
                        case "workRole":
                            builder.append("Work Role: ").append(entry.getValue()).append("\n");
                            break;
                        case "studentName":
                            builder.append("Student Name: ").append(entry.getValue()).append("\n");
                            break;
                        default:
                            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                            break;
                    }
                }
                builder.append("\n"); // Separate each shift with a blank line
            }
            shiftsTextView.setText(builder.toString());
        }
    }


    // Optionally, you can test the query with hardcoded values for debugging

}
