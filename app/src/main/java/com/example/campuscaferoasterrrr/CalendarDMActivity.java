package com.example.campuscaferoasterrrr;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class CalendarDMActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner workRoleSpinner, locationSpinner;
    private Button checkShiftsButton;
    private TextView shiftsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_dmactivity);

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

        // Normalize inputs to lowercase to avoid case sensitivity issues
        String normalizedWorkRole = workRole;
        String normalizedLocation = location;

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
                            ArrayList<String> shifts = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String studentEmail = document.getString("studentClockInId");
                                System.out.println("Shift found: " + studentEmail);
                                shifts.add(studentEmail);
                            }
                            displayShifts(shifts);
                        }
                    } else {
                        System.out.println("Error fetching shifts: " + task.getException().getMessage());
                        shiftsTextView.setText("Error fetching shifts: " + task.getException().getMessage());
                    }
                });
    }
    @SuppressLint("SetTextI18n")
    private void displayShifts(ArrayList<String> shifts) {
        if (shifts.isEmpty()) {
            shiftsTextView.setText("No shifts scheduled for this date.");
        } else {
            StringBuilder builder = new StringBuilder();
            for (String email : shifts) {
                builder.append(email).append("\n");
            }
            shiftsTextView.setText(builder.toString());
        }
    }

    // Optionally, you can test the query with hardcoded values for debugging
    private void testQueryWithHardcodedValues() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("shifts")
                .whereEqualTo("workRole", "cook")
                .whereEqualTo("location", "commons")
                .whereEqualTo("date", "15/10/2024")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<String> shifts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String studentEmail = document.getString("studentClockInId");
                            System.out.println("Shift found: " + studentEmail);
                            shifts.add(studentEmail);
                        }
                        displayShifts(shifts);
                    } else {
                        shiftsTextView.setText("Error fetching shifts: " + task.getException().getMessage());
                    }
                });
    }
}
