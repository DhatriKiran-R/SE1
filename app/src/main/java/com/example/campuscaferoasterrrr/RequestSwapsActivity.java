package com.example.campuscaferoasterrrr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RequestSwapsActivity extends AppCompatActivity {

    private RecyclerView shiftsRecyclerView;
    private FirebaseFirestore db;
    private ArrayList<Shift> shiftList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_swaps);

        db = FirebaseFirestore.getInstance();
        shiftsRecyclerView = findViewById(R.id.shifts_recycler_view);
        shiftsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadShifts();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_request_swaps) {
                intent = new Intent(RequestSwapsActivity.this, RequestSwapsActivity.class);
            } else if (item.getItemId() == R.id.nav_view_swaps) {
                intent = new Intent(RequestSwapsActivity.this, ViewSwapRequestsSW.class);
            } else if (item.getItemId() == R.id.nav_profile) {
                intent = new Intent(RequestSwapsActivity.this, StudentWorkerActivity.class);
            }  else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(RequestSwapsActivity.this, CalendarSWActivity.class);
            } else if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(RequestSwapsActivity.this, LoginActivity.class);
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

    private void loadShifts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail(); // Replace with actual email from Firebase Auth

        db.collection("shifts")
                .whereEqualTo("studentClockInId", userEmail)
                .whereEqualTo("requestedSwap", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        shiftList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Shift shift = new Shift(
                                    document.getId(),
                                    document.getString("studentClockInId"),
                                    document.getString("startTime"),
                                    document.getString("endTime"),
                                    document.getString("location"),
                                    document.getString("workRole"),
                                    document.getString("date"),
                                    document.getString("weekId"),
                                    document.getLong("duration").intValue()
                            );
                            shiftList.add(shift);
                        }
                        ShiftsAdapter adapter = new ShiftsAdapter(shiftList, "rutwik@gmail.com");
                        shiftsRecyclerView.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "Failed to load shifts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class ShiftsAdapter extends RecyclerView.Adapter<ShiftsAdapter.ShiftViewHolder> {

        private ArrayList<Shift> shiftList;
        private String requestedStudentEmail;

        public ShiftsAdapter(ArrayList<Shift> shiftList, String requestedStudentEmail) {
            this.shiftList = shiftList;
            this.requestedStudentEmail = requestedStudentEmail;
        }

        @NonNull
        @Override
        public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_shift, parent, false);
            return new ShiftViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
            Shift shift = shiftList.get(position);
            String shiftDetail = "Start: " + shift.startTime + " | End: " + shift.endTime + " | Location: " + shift.location + " | Role: " + shift.workRole;
            holder.shiftDetailsTextView.setText(shiftDetail);

            holder.requestSwapButton.setOnClickListener(v -> {
                String coveringEmail = holder.coveringStudentEmail.getText().toString().trim();
                submitSwapRequest(shift, coveringEmail);
            });
        }

        @Override
        public int getItemCount() {
            return shiftList.size();
        }

        public class ShiftViewHolder extends RecyclerView.ViewHolder {
            TextView shiftDetailsTextView;
            EditText coveringStudentEmail;
            Button requestSwapButton;

            public ShiftViewHolder(@NonNull View itemView) {
                super(itemView);
                shiftDetailsTextView = itemView.findViewById(R.id.shift_details);
                coveringStudentEmail = itemView.findViewById(R.id.covering_student_email);
                requestSwapButton = itemView.findViewById(R.id.request_swap_button);
            }
        }
    }

    private void submitSwapRequest(Shift shift, String coveringEmail) {
        if (coveringEmail.isEmpty()) {
            createSwapRequest(shift, coveringEmail, "Open", 0);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Verify that the user exists and has the correct work role
        db.collection("users")
                .whereEqualTo("email", coveringEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                        String workRole = userDoc.getString("role");
                        System.out.println(userDoc);
                        if (!"studentWorker".equals(workRole)) {
                            Toast.makeText(this, "The covering student is not a student worker.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Step 2: Check the total weekly work hours for the covering student
                        String weekId = shift.weekId; // Use the same week ID generation logic
                        db.collection("shifts")
                                .whereEqualTo("studentClockInId", coveringEmail)
                                .whereEqualTo("weekId", weekId)
                                .get()
                                .addOnCompleteListener(shiftTask -> {
                                    if (shiftTask.isSuccessful()) {
                                        int totalWorkHours = 0;

                                        for (QueryDocumentSnapshot shiftDoc : shiftTask.getResult()) {
                                            Long duration = shiftDoc.getLong("duration");
                                            totalWorkHours += (duration != null) ? duration : 0;
                                        }

                                        // Add the duration of the new shift
                                        totalWorkHours += shift.duration;

                                        if (totalWorkHours > 20) {
                                            Toast.makeText(this, "The covering student will exceed 20 work hours this week.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        totalWorkHours -= shift.duration;
                                        // Step 3: Check for overlapping shifts
                                        int finalTotalWorkHours = totalWorkHours;
                                        checkForOverlappingShifts(coveringEmail, shift, (hasOverlap) -> {
                                            if (hasOverlap) {
                                                Toast.makeText(this, "The covering student has a shift during this time.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                // All checks passed, create the swap request
                                                createSwapRequest(shift, coveringEmail, "Close", finalTotalWorkHours);
                                            }
                                        });
                                    } else {
                                        Toast.makeText(this, "Error fetching covering student's shifts.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "No student found with this email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper to check for overlapping shifts
    private void checkForOverlappingShifts(String coveringEmail, Shift shift, OnOverlapCheckListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        try {
            Date newStart = sdf.parse(shift.startTime);
            Date newEnd = sdf.parse(shift.endTime);

            db.collection("shifts")
                    .whereEqualTo("studentClockInId", coveringEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            boolean hasOverlap = false;

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String existingStartTime = doc.getString("startTime");
                                String existingEndTime = doc.getString("endTime");

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

                                if (newStart.before(existingEnd) && newEnd.after(existingStart)) {
                                    hasOverlap = true;
                                    break;
                                }
                            }

                            listener.onResult(hasOverlap);
                        } else {
                            Toast.makeText(this, "Error checking overlapping shifts.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing shift times.", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper to create the swap request
    private void createSwapRequest(Shift shift, String coveringEmail, String shiftStatus, int totalWorkHours) {
        String swapStatus = "Pending";

        SwapRequest swapRequest = new SwapRequest(
                shift.shiftId,
                shift.studentClockInId,
                coveringEmail,
                swapStatus,
                shiftStatus,
                System.currentTimeMillis(),
                shift.startTime,
                shift.endTime,
                String.valueOf(shift.duration),
                shift.date,
                shift.location,
                totalWorkHours,
                shift.weekId,
                shift.workRole

        );

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("swap_requests")
                .add(swapRequest)
                .addOnSuccessListener(documentReference -> {
                    db.collection("shifts")
                            .document(shift.shiftId)
                            .update("requestedSwap", true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Swap request submitted successfully.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RequestSwapsActivity.this, RequestSwapsActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Swap request added but failed to update shift: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Listener interface for overlap checks
    private interface OnOverlapCheckListener {
        void onResult(boolean hasOverlap);
    }


    public static class SwapRequest {
        public String shiftId;
        public String requestedStudentEmail;
        public String coveringStudentEmail;
        public String swapStatus;
        public String shiftStatus;
        public long requestTimestamp;
        public String startTime;
        public String endTime;
        public int duration;
        public String date;
        public String location;
        public String weekId;
        public int totalWorkHours;

        public String role;

        public SwapRequest() {}

        public SwapRequest(String shiftId, String requestedStudentEmail, String coveringStudentEmail,
                           String swapStatus, String shiftStatus, long requestTimestamp,
                           String startTime, String endTime, String duration,
                           String date, String location, int totalWorkHours, String weekId, String role) {
            this.shiftId = shiftId;
            this.requestedStudentEmail = requestedStudentEmail;
            this.coveringStudentEmail = coveringStudentEmail;
            this.swapStatus = swapStatus;
            this.shiftStatus = shiftStatus;
            this.requestTimestamp = requestTimestamp;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = Integer.parseInt(duration);
            this.date = date;
            this.location = location;
            this.totalWorkHours = totalWorkHours;
            this.weekId = weekId;
            this.role = role;
        }
    }

    public static class Shift {
        public String shiftId;
        public String studentClockInId;
        public String startTime;
        public String endTime;
        public String location;
        public String workRole;
        public String date;
        public String weekId;
        public int duration;

        public Shift() {}

        public Shift(String shiftId, String studentClockInId, String startTime, String endTime,
                     String location, String workRole, String date, String weekId, int duration) {
            this.shiftId = shiftId;
            this.studentClockInId = studentClockInId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
            this.workRole = workRole;
            this.date = date;
            this.weekId = weekId;
            this.duration = duration;
        }
    }
}
