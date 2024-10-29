package com.example.campuscaferoasterrrr;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class RequestSwapsActivity extends AppCompatActivity {

    private Spinner shiftsSpinner;
    private EditText coveringStudentEmail;
    private Button submitSwapRequestButton;

    private FirebaseFirestore db;
    private ArrayList<String> shiftList; // Store shift IDs
    private ArrayList<String> shiftDetails; // Store displayable shift details

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_swaps);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        shiftsSpinner = findViewById(R.id.shifts_spinner);
        coveringStudentEmail = findViewById(R.id.covering_student_email);
        submitSwapRequestButton = findViewById(R.id.submit_swap_request_button);

        // Load shifts for the current day or the user's shifts
        loadShifts();

        // Set onClick listener for the button
        submitSwapRequestButton.setOnClickListener(v -> submitSwapRequest());
    }

    private void loadShifts() {
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // Replace this with actual user authentication to get the student's email
        String currentStudentEmail = "rutwik@gmail.com"; // Replace with actual email from Firebase Auth

        // Fetch shifts for the current student from Firestore
        db.collection("shifts")
                .whereEqualTo("studentClockInId", currentStudentEmail) // Assuming each shift has an associated student email
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        shiftList = new ArrayList<>();
                        shiftDetails = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String shiftId = document.getId();
                            String startTime = document.getString("startTime");
                            String endTime = document.getString("endTime");
                            String location = document.getString("location");
                            String shiftDetail = "Shift ID: " + shiftId + " | Start: " + startTime + " | End: " + endTime + " | Location: " + location;

                            shiftList.add(shiftId);
                            shiftDetails.add(shiftDetail);
                        }
                        setupShiftSpinner();
                    } else {
                        Toast.makeText(this, "Failed to load shifts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupShiftSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, shiftDetails);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shiftsSpinner.setAdapter(adapter);
    }

    private void submitSwapRequest() {
        if (shiftsSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a shift.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedShiftId = shiftList.get(shiftsSpinner.getSelectedItemPosition());
        String coveringEmail = coveringStudentEmail.getText().toString().trim();
        String requestedStudentEmail = "currentStudentEmail@example.com"; // Replace with actual student's email

        // Prepare the swap request data
        SwapRequest swapRequest = new SwapRequest(selectedShiftId, requestedStudentEmail, coveringEmail,
                "Pending", "Open", System.currentTimeMillis(), "start_time", "end_time", "duration", "date", "location");

        // Save to Firestore
        db.collection("swap_requests")
                .add(swapRequest)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Swap request submitted successfully", Toast.LENGTH_SHORT).show();
                    coveringStudentEmail.setText(""); // Clear the email field
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error submitting swap request: " + e.getMessage());
                    Toast.makeText(this, "Error submitting swap request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    // SwapRequest class to model the request
    public class SwapRequest {
        public String shiftId; // Shift ID
        public String requestedStudentEmail; // Requesting student's email
        public String coveringStudentEmail; // Covering student's email
        public String swapStatus; // Swap status (Accepted/Denied/Pending)
        public String shiftStatus; // Shift status (Open/Closed)
        public long requestedOn; // DateTime when the request is made
        public String startTime; // Shift start time
        public String endTime; // Shift end time
        public String duration; // Duration of the shift
        public String date; // Date of the shift
        public String location; // Location of the shift

        // Default constructor required for Firestore serialization
        public SwapRequest() {
        }

        // Constructor to create SwapRequest object
        public SwapRequest(String shiftId, String requestedStudentEmail, String coveringStudentEmail,
                           String swapStatus, String shiftStatus, long requestedOn,
                           String startTime, String endTime, String duration, String date, String location) {
            this.shiftId = shiftId;
            this.requestedStudentEmail = requestedStudentEmail;
            this.coveringStudentEmail = coveringStudentEmail;
            this.swapStatus = swapStatus;
            this.shiftStatus = shiftStatus;
            this.requestedOn = requestedOn;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.date = date;
            this.location = location;
        }
    }}

