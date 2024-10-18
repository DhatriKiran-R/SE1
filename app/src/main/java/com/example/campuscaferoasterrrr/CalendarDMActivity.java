package com.example.campuscaferoasterrrr;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CalendarDMActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private Spinner workRoleSpinner, locationSpinner;
    private Button checkShiftsButton;
    private TextView shiftsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar_dmactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        checkShiftsButton.setOnClickListener(v -> checkShifts());
    }

    private void checkShifts() {
        // Get selected date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(calendarView.getDate());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String selectedDate = sdf.format(calendar.getTime());

        // Debugging: Print the selected date to verify it's correct
        System.out.println("Selected Date: " + selectedDate);

        String selectedWorkRole = workRoleSpinner.getSelectedItem().toString();
        String selectedLocation = locationSpinner.getSelectedItem().toString();

        // Fetch shifts from Firestore
        fetchShifts(selectedDate, selectedWorkRole, selectedLocation);
    }

    private void fetchShifts(String date, String workRole, String location) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Debugging: Check the parameters being passed
        System.out.println("Fetching shifts for Date: " + date + ", Role: " + workRole + ", Location: " + location);

        db.collection("shifts")
                .whereEqualTo("workRole", workRole)
                .whereEqualTo("location", location)
                .whereEqualTo("date", date) // Make sure 'date' is stored in the same format
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<String> shifts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String studentEmail = document.getString("studentClockInId"); // Adjust based on your structure
                            System.out.println(studentEmail);

                            shifts.add(studentEmail);
                        }
                        displayShifts(shifts);
                    } else {
                        shiftsTextView.setText("Error fetching shifts: " + task.getException().getMessage());
                    }
                });
    }


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
}
