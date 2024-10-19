package com.example.campuscaferoasterrrr;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShiftManagementDMActivity extends AppCompatActivity {
    private Spinner roleSpinner, studentSpinner;
    private RadioGroup locationRadioGroup;
    private TextView startTimeText, endTimeText;
    private Button submitButton;
    private String startTime, endTime;
    private Calendar startTimeCalendar, endTimeCalendar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_management_dmactivity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        roleSpinner = findViewById(R.id.role_spinner);
        studentSpinner = findViewById(R.id.student_spinner);
        locationRadioGroup = findViewById(R.id.location_radio_group);
        startTimeText = findViewById(R.id.start_time_text);
        endTimeText = findViewById(R.id.end_time_text);
        submitButton = findViewById(R.id.submit_button);

        // Initialize the Calendar instances
        startTimeCalendar = Calendar.getInstance();
        endTimeCalendar = Calendar.getInstance();

        // Initialize spinners with dummy data
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        // Fetch students from the database
        fetchStudents();

        // Set up listeners for TextViews to select start and end times
        startTimeText.setOnClickListener(v -> showDateTimePicker(startTimeCalendar, startTimeText));
        endTimeText.setOnClickListener(v -> showDateTimePicker(endTimeCalendar, endTimeText));

        submitButton.setOnClickListener(v -> submitShift());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_shift_management) {
                intent = new Intent(ShiftManagementDMActivity.this, ShiftManagementDMActivity.class);
            } else if (item.getItemId() == R.id.nav_swap_requests) {
                intent = new Intent(ShiftManagementDMActivity.this, SwapRequestsDMActivity.class);
            } else if (item.getItemId() == R.id.nav_student_tracking) {
                intent = new Intent(ShiftManagementDMActivity.this, StudentTrackingDMActivity.class);
            } else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(ShiftManagementDMActivity.this, CalendarDMActivity.class);
            } else if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(ShiftManagementDMActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true; // Exit the listener for logout
            } else {
                return false; // Return false for unhandled items
            }
            startActivity(intent);
            return true; // Return true to indicate the item was handled
        });
    }
    private void showDateTimePicker(Calendar calendar, TextView textView) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            calendar.set(Calendar.YEAR, selectedYear);
            calendar.set(Calendar.MONTH, selectedMonth);
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timeView, selectedHour, selectedMinute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                calendar.set(Calendar.MINUTE, selectedMinute);
                String formattedTime = String.format("%02d/%02d/%04d %02d:%02d", selectedDay, selectedMonth + 1, selectedYear, selectedHour, selectedMinute);
                textView.setText(formattedTime);

                // Set the selected start or end time
                if (textView == startTimeText) {
                    startTime = formattedTime;
                } else {
                    endTime = formattedTime;
                }
            }, hour, minute, true);
            timePickerDialog.show();
        }, year, month, day);
        datePickerDialog.show();
    }
    private void fetchStudents() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users") // Assuming 'users' is your collection name
                .whereEqualTo("role", "studentWorker")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<String> students = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String studentEmail = document.getString("email"); // Change here to get the email
                            if (studentEmail != null) {
                                students.add(studentEmail);
                            }
                        }
                        Log.d("ShiftManagementDM", "Fetched students: " + students);
                        if (students.isEmpty()) {
                            Toast.makeText(this, "No students found.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, students);
                        studentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        studentSpinner.setAdapter(studentAdapter);
                    } else {
                        Toast.makeText(this, "Failed to fetch students.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void submitShift() {
        String selectedRole = roleSpinner.getSelectedItem().toString();
        String selectedStudentClockInId = studentSpinner.getSelectedItem().toString(); // Replace with actual clockInId
        int selectedLocationId = locationRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedLocationButton = findViewById(selectedLocationId);
        String selectedLocation = selectedLocationButton.getText().toString();

        if (startTime == null || endTime == null) {
            Toast.makeText(this, "Please select start and end time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for overlapping shifts
        checkForOverlappingShifts(selectedStudentClockInId, startTime, endTime, selectedRole, selectedLocation);
    }

    private void checkForOverlappingShifts(String studentClockInId, String newStartTime, String newEndTime, String role, String location) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        try {
            Date newStart = sdf.parse(newStartTime);
            Date newEnd = sdf.parse(newEndTime);

            db.collection("shifts")
                    .whereEqualTo("studentClockInId", studentClockInId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String existingStartTime = document.getString("startTime");
                                String existingEndTime = document.getString("endTime");

                                Date existingStart = null;
                                try {
                                    existingStart = sdf.parse(existingStartTime);
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                                Date existingEnd = null;
                                try {
                                    existingEnd = sdf.parse(existingEndTime);
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }

                                // Check if the new shift overlaps with existing shift
                                if (newStart.before(existingEnd) && newEnd.after(existingStart)) {
                                    Toast.makeText(this, "Student has another shift during this time.", Toast.LENGTH_SHORT).show();
                                    resetFields();
                                    return;
                                }
                            }

                            // No overlapping shifts found, proceed to add the shift
                            addShiftToFirestore(role, location, studentClockInId, newStartTime, newEndTime);
                        } else {
                            Toast.makeText(this, "Error checking for overlapping shifts.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing date. Please check the format.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addShiftToFirestore(String role, String location, String studentClockInId, String startTime, String endTime) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Calculate duration (in hours) of the shift
        int duration = calculateDuration(startTime, endTime);

        // Get the week ID based on the start time
        String weekId = getWeekId(startTime);

        db.collection("shifts")
                .whereEqualTo("studentClockInId", studentClockInId)
                .whereEqualTo("weekId", weekId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalWorkHours = 0;

                        // Calculate the total work hours for the student in the week
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Long durationLong = document.getLong("duration");
                            int durationk = (durationLong != null) ? durationLong.intValue() : 0;
                            totalWorkHours += durationk; // Accumulate existing durations
                        }

                        // Now include the new shift's duration
                        totalWorkHours += duration;

                        // Check if the total work hours exceed 20
                        if (totalWorkHours > 20) {
                            Toast.makeText(this, "Student will exceed 20 hours.", Toast.LENGTH_SHORT).show();
                            resetFields();
                        } else {
                            // Prepare shift data
                            Map<String, Object> shiftData = new HashMap<>();
                            shiftData.put("location", location.trim().toLowerCase(Locale.getDefault()));
                            shiftData.put("startTime", startTime);
                            shiftData.put("endTime", endTime);
                            shiftData.put("duration", duration); // Use calculated duration
                            shiftData.put("workRole", role.trim().toLowerCase(Locale.getDefault()));
                            shiftData.put("studentClockInId", studentClockInId);
                            shiftData.put("weekId", weekId); // Add the week ID
                            shiftData.put("shiftId", generateShiftId()); // Generate a unique shift ID
                            shiftData.put("date", startTime.substring(0, 10));
                            // Insert the shift data into Firestore
                            db.collection("shifts").add(shiftData)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Shift added successfully!", Toast.LENGTH_SHORT).show();
                                        resetFields(); // Reset the fields for a new entry
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error adding shift: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Error checking total work hours.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private String getWeekId(String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date start = sdf.parse(startTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
            int year = calendar.get(Calendar.YEAR);
            return year + "-W" + weekOfYear; // Format as "YYYY-WX"
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int calculateDuration(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date start = sdf.parse(startTime);
            Date end = sdf.parse(endTime);
            long durationInMillis = end.getTime() - start.getTime();
            return (int) (durationInMillis / (1000 * 60 * 60)); // Convert milliseconds to hours
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Handle error as needed
        }
    }

    private String generateShiftId() {
        return "shift_" + System.currentTimeMillis(); // Example of a simple unique ID
    }

    private void resetFields() {
        // Reset the spinners and text views
        roleSpinner.setSelection(0); // Reset to the first item in the role spinner
        studentSpinner.setSelection(0); // Reset to the first item in the student spinner
        locationRadioGroup.clearCheck(); // Clear selected radio button
        startTimeText.setText(R.string.select_start_time); // Reset the start time TextView
        endTimeText.setText(R.string.select_end_time); // Reset the end time TextView
        startTime = null; // Clear the start time variable
        endTime = null; // Clear the end time variable
    }
}